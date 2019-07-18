package com.github.coleb1911.ghost2.buildtools;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pmw.tinylog.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class JDKDownloader {
    private static final String URL_FORMAT = "https://api.adoptopenjdk.net/v2/info/releases/openjdk11?openjdk_impl=hotspot&arch=x64&type=jdk&heap_size=normal&release=latest&os=";

    private static CloseableHttpClient client;

    /**
     * Download the latest Java 11 JDK for a platform.
     *
     * @param platform Platform to download JDK for
     * @param folder   Folder to save JDK archive in
     * @throws IOException          If a general I/O error occurs while downloading the file
     * @throws InterruptedException If the download thread is interrupted
     */
    static File download(Platform platform, File folder) throws IOException, InterruptedException {
        client = HttpClientBuilder.create()
                .setUserAgent(System.getProperty("java.version") + "/" + "Apache HttpClient 4.5.9")
                .build();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Make initial link request to AdoptOpenJDK API
        HttpGet rq = new HttpGet(URL_FORMAT + platform.apiName);

        String downloadUrl;
        try (CloseableHttpResponse rs = client.execute(rq)) {
            // Check status
            if (rs.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Failed to get download link from AdoptOpenJDK API.");
            }

            // Get URL
            downloadUrl = new BufferedReader(new InputStreamReader(rs.getEntity().getContent())).lines()
                    .filter(s -> s.contains("binary_link"))
                    .map(s -> s.replaceAll("[\",]", ""))
                    .map(s -> s.split(":", 2)[1])
                    .map(String::trim)
                    .findFirst().orElse("");
        }

        // Log link
        Logger.info("Downloading {} JDK: {}", platform.toString(), downloadUrl);

        // Download
        try (DownloadProgress progress = new DownloadProgress()) {
            final AtomicInteger lastLogged = new AtomicInteger();
            final double bytesToMb = Math.pow(10, 6);
            final int totalSegments = 40;

            progress.subscribe(update -> {
                synchronized (lastLogged) {
                    final int currentSegments = update.current / (update.total / totalSegments);
                    if (Math.abs((currentSegments % 10) - 10) == 1 && lastLogged.get() != currentSegments) {
                        final double mbCurrent = update.current / bytesToMb;
                        final double mbTotal = update.total / bytesToMb;
                        final String line = String.format("[%s%s] %.1fmb / %.1fmb >> %s",
                                "|".repeat(currentSegments),
                                " ".repeat(totalSegments - currentSegments),
                                mbCurrent,
                                mbTotal,
                                progress.targetFile.getName());

                        Logger.info(line);
                        lastLogged.set(currentSegments);
                    }
                }
            });

            // Start download runnable
            DownloadRunnable runnable = new DownloadRunnable(downloadUrl, folder, progress);
            executor.submit(runnable, null);
            progress.latch.await();

            // Cleanup and exit
            client.close();
            executor.shutdown();
            Logger.info("Done.");
            return progress.targetFile;
        }
    }

    /**
     * Enum of supported platforms
     */
    public enum Platform {
        WINDOWS("windows"),
        LINUX("linux"),
        OSX("mac");

        private String apiName;

        Platform(String name) {
            this.apiName = name;
        }

        /**
         * Convert a platform string from {@code System.getProperty("os.name")} to an enum value
         *
         * @param name Platform string
         * @return Enum value
         * @throws IllegalArgumentException If the platform is unsupported or nonexistent
         */
        public static Platform fromPlatformString(String name) {
            if (name.startsWith("Windows")) return WINDOWS;
            else if (name.startsWith("Mac")) return OSX;
            else if (name.contains("Linux")) return LINUX;
            throw new IllegalArgumentException("Unsupported or nonexistent platform");
        }

        @Override
        public String toString() {
            return this == OSX ? name() : name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    /**
     * A download thread.
     */
    private static class DownloadRunnable implements Runnable {
        private String url;
        private File path;
        private DownloadProgress progress;

        /**
         * Construct a new DownloadRunnable.
         *
         * @param url      Direct URL to target file
         * @param saveDir  Directory to save file in
         * @param progress {@code DownloadProgress} to dispatch events to
         * @throws IOException If an I/O error occurs while creating {@code saveDir} or the parent directory doesn't exist
         */
        DownloadRunnable(String url, File saveDir, DownloadProgress progress) throws IOException {
            this.url = url;
            this.path = saveDir;
            this.progress = progress;

            if (!saveDir.exists())
                FileUtils.forceMkdir(saveDir);
            else if (!saveDir.isDirectory())
                throw new IllegalArgumentException("File given is not a directory");
        }

        @Override
        public void run() {
            // Send GET request
            HttpGet rq = new HttpGet(url);
            HttpResponse rs;
            int length;
            File output;

            try {
                rs = client.execute(rq);

                // Check status
                int status = rs.getStatusLine().getStatusCode();
                if (status != HttpStatus.SC_OK) {
                    Logger.error(new IOException(String.format("Failed to download %s: status %d", url, status)));
                    return;
                }

                // Fetch filename and check if already downloaded
                String filename = rs.getFirstHeader("Content-Disposition").getValue().split("=")[1];
                output = new File(path, filename);
                progress.targetFile = output;
                length = Integer.parseInt(rs.getFirstHeader("Content-Length").getValue());
                if (output.exists()) {
                    if (output.isFile() && output.length() == length) {
                        Logger.info("{} already downloaded, skipping", url);
                        progress.latch.countDown();
                        return;
                    }
                    FileUtils.forceDelete(output);
                }
            } catch (IOException e) {
                Logger.error(e);
                return;
            }

            // Fetch expected bytes and download
            final int BUFFER_SIZE = 8192;
            int bytes;
            byte[] buffer = new byte[BUFFER_SIZE];
            int total = 0;
            try (BufferedInputStream in = new BufferedInputStream(rs.getEntity().getContent());
                 BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output), BUFFER_SIZE)) {
                while ((bytes = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    out.write(buffer, 0, bytes);
                    progress.update(total += bytes, length);
                }
                progress.latch.countDown();
            } catch (IOException e) {
                Logger.error(e);
            }
        }
    }

    /**
     * Dispatches progress events and provides a latch for download threads
     */
    private static class DownloadProgress implements Closeable {
        private int current;
        private int total;
        private List<Consumer<DownloadProgress>> subscribers;
        private ExecutorService executor;
        private CountDownLatch latch;
        private File targetFile;

        DownloadProgress() {
            this.current = 0;
            this.total = -1;
            this.subscribers = new ArrayList<>();
            executor = Executors.newCachedThreadPool();
            latch = new CountDownLatch(1);
        }

        /**
         * Update progress values and dispatch an update event
         *
         * @param current New current progress
         * @param total  New total progress
         */
        void update(int current, int total) {
            this.current = current;
            this.total = total;
            executor.execute(() -> subscribers.forEach(consumer -> consumer.accept(this)));
        }

        /**
         * Subscribe to progress update events
         *
         * @param consumer Event consumer
         */
        void subscribe(Consumer<DownloadProgress> consumer) {
            subscribers.add(consumer);
        }

        @Override
        public void close() {
            subscribers.clear();
            executor.shutdownNow();
        }
    }
}
