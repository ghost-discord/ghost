package com.github.coleb1911.ghost2.buildtools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.github.coleb1911.ghost2.buildtools.SystemUtils.BUFFER_SIZE;
import static com.github.coleb1911.ghost2.buildtools.SystemUtils.USER_AGENT;

class JDKDownloadUtils {
    private static final String URL_FORMAT = "https://api.adoptopenjdk.net/v2/info/releases/openjdk11?openjdk_impl=hotspot&type=jdk&heap_size=normal&release=latest&arch={ARCH}&os={OS}";

    /**
     * Download the latest Java 11 JDK for a platform.<br>
     * Blocks until finished.
     *
     * @param platform Target platform
     * @param saveDir  Folder to save JDK archive in
     * @throws IOException If a general I/O error occurs while downloading the file
     */
    static File download(final Platform platform, final File saveDir) throws IOException {
        // Get download URI
        final URI downloadUri = fetchDownloadUri(platform);

        // Start download
        final File outputFile;
        try (Download download = new Download(downloadUri, saveDir)) {
            outputFile = download.start();
        }

        // Return result
        return outputFile;
    }

    /**
     * Queries the AdoptOpenJDK API for a JDK download link for the specified platform.
     *
     * @param platform Target platform
     * @return URI for the JDK
     */
    private static URI fetchDownloadUri(final Platform platform) throws IOException {
        final URI uri;
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setUserAgent(USER_AGENT)
                .build()) {

            // Make initial link request to AdoptOpenJDK API
            String requestString = URL_FORMAT.replace("{ARCH}", platform.osArch).replace("{OS}", platform.osName);
            HttpGet request = new HttpGet(requestString);
            try (CloseableHttpResponse response = client.execute(request)) {
                // Check status
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new IOException("Failed to get download link from AdoptOpenJDK API.");
                }

                // Get URL
                String uriString = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).lines()
                        .filter(s -> s.contains("binary_link"))
                        .map(s -> s.replaceAll("[\",]", ""))
                        .map(s -> s.split(":", 2)[1])
                        .map(String::trim)
                        .findFirst().orElse("");

                uri = new URI(uriString);
            }
        } catch (URISyntaxException e) {
            throw new IOException("AdoptOpenJDK API returned an invalid download URL");
        }
        return uri;
    }

    /**
     * Enum of supported platforms
     */
    public enum Platform {
        WINDOWS("windows", "x64"),
        LINUX("linux", "x64"),
        OSX("mac", "x64"),
        ARM32("linux", "arm");

        /**
         * Name of this platform in the AdoptOpenJDK API
         */
        private final String osName;

        /**
         * Architecture of this platform in the AdoptOpenJDK API
         */
        private final String osArch;

        Platform(final String osName, final String osArch) {
            this.osName = osName;
            this.osArch = osArch;
        }

        /**
         * Get the {@code Platform} equivalent of a platform string from {@code System.getProperty("os.name")}.
         *
         * @param name Platform string
         * @return Enum value
         * @throws IllegalArgumentException If the platform is unsupported or nonexistent
         */
        public static Platform fromPlatformString(final String name, final String arch) {
            if (name.startsWith("Windows")) return WINDOWS;
            else if (name.startsWith("Mac")) return OSX;
            else if (name.contains("Linux") && arch.contains("amd64")) return LINUX;
            else if (name.contains("Linux") && arch.contains("arm")) return ARM32;
            throw new IllegalArgumentException("Unsupported or nonexistent platform");
        }

        /**
         * Get all the {@code Platforms} that aren't the specified platform.
         *
         * @param platform Target platform
         * @return All the other {@code Platform} values
         */
        public static Set<Platform> not(final Platform platform) {
            Set<Platform> others = new HashSet<>(Arrays.asList(Platform.values()));
            others.remove(platform);
            return others;
        }

        /**
         * Returns this {@code Platform}'s name in title-case.
         *
         * @return This {@code Platform}'s name in title-case
         */
        @Override
        public String toString() {
            return this == OSX ? name() : StringUtils.capitalize(name().toLowerCase());
        }
    }

    /**
     * A single download.
     */
    private static class Download implements AutoCloseable {
        private final URI uri;
        private final File saveDir;

        private CloseableHttpClient client;

        /**
         * Construct a new {@code Download}.
         *
         * @param uri     Direct URI to target file
         * @param saveDir Directory to save file in
         * @throws IOException If an I/O error occurs while creating {@code saveDir} or the parent directory doesn't exist
         */
        Download(final URI uri, final File saveDir) throws IOException {
            this.uri = uri;
            this.saveDir = saveDir;

            if (!saveDir.exists())
                FileUtils.forceMkdir(saveDir);
            else if (!saveDir.isDirectory())
                throw new IllegalArgumentException("Save directory given is not a directory");
        }

        /**
         * Start this {@code Download.}<br>
         * Blocks until finished.
         *
         * @return Downloaded file
         * @throws IOException If an I/O exception occurs while downloading the file
         */
        File start() throws IOException {
            File outputFile;
            try (CloseableHttpResponse response = sendDownloadRequest()) {
                // Create file
                String fileName = StringUtils.substringAfterLast(uri.getPath(), "/");
                outputFile = new File(saveDir, fileName);
                long length = response.getEntity().getContentLength();

                if (outputFile.exists()) {
                    if (outputFile.length() == length) {
                        Logger.info(fileName + " has already been downloaded. Skipping.");
                        return outputFile;
                    }
                } else {
                    if (!outputFile.createNewFile()) {
                        throw new IOException("Failed to create file for " + fileName);
                    }
                }

                try (InputStream in = response.getEntity().getContent()) {
                    try (OutputStream out = new FileOutputStream(outputFile)) {
                        IOUtils.copy(in, out, BUFFER_SIZE);
                    }
                }
            }
            return outputFile;
        }

        /**
         * Sends a download request to the server for the given URI.
         *
         * @return The server's response
         */
        private CloseableHttpResponse sendDownloadRequest() throws IOException {
            // Build client
            client = HttpClientBuilder.create()
                    .setUserAgent(USER_AGENT)
                    .build();

            // Send request
            HttpGet request = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(request);
            // Check status
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new IOException("Received non-OK response code " + status + " while attempting to initiate download");
            }

            // Check type
            final String octetStream = "application/octet-stream";
            if (!octetStream.equals(response.getEntity().getContentType().getValue())) {
                throw new IOException("Download URL didn't return an octet stream");
            }

            return response;
        }

        @Override
        public void close() throws IOException {
            if (client != null) {
                client.close();
            }
        }
    }
}
