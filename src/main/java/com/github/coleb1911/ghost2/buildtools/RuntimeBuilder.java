package com.github.coleb1911.ghost2.buildtools;

import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.buildtools.JDKDownloadUtils.Platform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Analyzes dependencies of a ghost2 fat JAR, produces a custom runtime image with
 * {@code jlink}, then bundles it with the JAR and a start script.
 * <p>
 * Don't run this tool manually. Use the {@code ghostPackage} Gradle task.<br>
 * If you want more detailed output, set the {@code BT_VERBOSE} environment variable to {@code true}.
 */
public final class RuntimeBuilder {
    private static final String ERROR_MODULES_MISSING = "Couldn't find the Java module directory. Ensure you have a valid Java 11 installation and that your JAVA_HOME or PATH points to it.";
    private static final String ERROR_ARTIFACT_MISSING = "A ghost2 JAR artifact was not found. Check to make sure bootJar completed without errors.";
    private static final String MESSAGE_DONE = "Finished successfully in {} seconds. Results saved to {}.";
    private static final String SCRIPT_UNIX = "#!/bin/bash\nSCRIPTDIR=$(dirname $0)\ncd ${SCRIPTDIR}\n${SCRIPTDIR}/jre/bin/java -Dloader.path=${SCRIPTDIR} -jar ${SCRIPTDIR}/";
    private static final String SCRIPT_WIN = "cd %~dp0\r\n%~dp0jre\\bin\\java.exe -Dloader.path=%~dp0 -jar %~dp0";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static File artifact;

    public static void main(String[] args) throws IOException, InterruptedException {
        final Instant start = Instant.now();
        Configurator.defaultConfig()
                .level(SystemUtils.VERBOSE ? Level.DEBUG : Level.INFO)
                .level("org.apache.commons", Level.OFF)
                .writingThread(true)
                .activate();

        // Prepare work folders
        final File tempDir = new File("bt_temp");
        final File outputDir = new File("bt_out");
        final File jdkArchiveDir = new File("bt_xplatform");
        SystemUtils.cleanAndCreate(tempDir, outputDir);
        FileUtils.forceMkdir(jdkArchiveDir);

        // Download JDKs
        Logger.info("[1/5] Downloading JDKs...");
        final Map<Platform, File> compressedJdks = downloadJdks(jdkArchiveDir);

        // Extract JDKs
        Logger.info("[2/5] Extracting JDKs...");
        final File extractedJdkDir = new File(tempDir, "jdks");
        FileUtils.forceMkdir(extractedJdkDir);
        final Map<Platform, File> jdks = extractJdks(compressedJdks, extractedJdkDir);

        // Get module dependencies
        final Set<String> dependencies = analyzeDependencies();

        // Run jlink for each platform
        Logger.info("[3/5] Linking JDKs...");
        final File jreDir = new File(tempDir, "jres");
        FileUtils.forceMkdir(jreDir);
        final Map<Platform, File> jres = link(jdks, dependencies, jreDir);

        // Package releases for each platform
        Logger.info("[4/5] Packaging releases...");
        packageReleases(jres, outputDir);

        // Clean up
        Logger.info("[5/5] Cleaning up...");
        FileUtils.forceDelete(tempDir);
        EXECUTOR.shutdown();
        EXECUTOR.awaitTermination(10L, TimeUnit.SECONDS);

        // Log time and exit
        final Instant end = Instant.now();
        Logger.info(MESSAGE_DONE, start.until(end, ChronoUnit.SECONDS), outputDir.getPath());
    }

    /**
     * Downloads the JDKs for all other platforms.
     *
     * @param targetDir Directory to download JDKs to
     * @return The locations of the downloaded JDKs, mapped to their platform
     * @throws InterruptedException If a download thread is interrupted
     */
    private static Map<Platform, File> downloadJdks(final File targetDir) throws InterruptedException {
        final Set<Platform> requiredPlatforms = Platform.not(SystemUtils.PLATFORM);
        final CountDownLatch latch = new CountDownLatch(requiredPlatforms.size());
        final Map<Platform, File> archives = new EnumMap<>(Platform.class);

        for (Platform platform : requiredPlatforms) {
            EXECUTOR.execute(() -> {
                try {
                    archives.put(platform, JDKDownloadUtils.download(platform, targetDir));
                    Logger.debug("JDK download for {} finished.", platform.toString());
                } catch (IOException e) {
                    Logger.error(e, "JDK download for " + platform.toString() + " encountered an I/O error");
                    System.exit(1);
                }
                latch.countDown();
            });
        }

        latch.await();
        return archives;
    }

    /**
     * Extracts JDKs from their archives.
     *
     * @param archives  Archives to extract
     * @param targetDir Directory to extract JDKs to
     * @return Directories of extracted JDKs
     * @throws IOException          If an I/O error occurs while extracting the JDKs
     * @throws InterruptedException If an extraction thread is interrupted
     */
    private static Map<Platform, File> extractJdks(final Map<Platform, File> archives, final File targetDir) throws IOException, InterruptedException {
        final Map<Platform, File> ret = new EnumMap<>(Platform.class);
        final CountDownLatch latch = new CountDownLatch(archives.size());

        for (Map.Entry<Platform, File> pair : archives.entrySet()) {
            // Get archive and platform
            final Platform platform = pair.getKey();
            final File jdkArchive = pair.getValue();

            // Create target directory
            final File platformDir = new File(targetDir, platform.toString());
            FileUtils.forceMkdir(platformDir);
            EXECUTOR.execute(() -> {
                try {
                    // Extract JDK
                    SystemUtils.extract(jdkArchive, platformDir);
                    File jdkDir = SystemUtils.listFiles(platformDir)
                            .stream()
                            .filter(file -> file.getName().startsWith("jdk"))
                            .findFirst()
                            .orElseThrow();

                    // Clean up OSX JDK garbage
                    if (platform == Platform.OSX) {
                        cleanOsxJdk(platformDir, jdkDir);
                    }
                    // Add to results and signal finished
                    ret.put(platform, jdkDir);
                    latch.countDown();
                    Logger.debug("Finished unzipping {} JDK.", platform.toString());
                } catch (IOException e) {
                    Logger.error(e, "Archive extraction for " + platform.toString() + " JDK encountered an I/O error");
                    System.exit(1);
                }
            });
        }

        // Wait for threads to finish
        latch.await();

        // Add local JDK
        ret.put(SystemUtils.PLATFORM, SystemUtils.getJdkPath());

        // Return
        return ret;
    }

    /**
     * Removes all the extra garbage from the OSX JDK.
     *
     * @param platformDir Temporary directory for the platform
     * @param jdkDir      Actual JDK directory
     * @throws IOException If an I/O error occurs while removing the extra files
     */
    private static void cleanOsxJdk(final File platformDir, final File jdkDir) throws IOException {
        // Find actual JDK directory
        File dotfile = new File(platformDir, "._" + jdkDir.getName());
        File contentsDir = new File(jdkDir, "Contents");
        File homeDir = new File(contentsDir, "Home");

        // Move actual JDK files
        List<File> homeChildren = SystemUtils.listFiles(homeDir);
        for (File file : homeChildren) {
            if (file.isDirectory()) {
                FileUtils.moveDirectoryToDirectory(file, jdkDir, true);
            } else {
                FileUtils.moveFileToDirectory(file, jdkDir, true);
            }
        }

        // Delete the garbage
        FileUtils.forceDelete(contentsDir);
        FileUtils.forceDelete(dotfile);
    }

    /**
     * Builds a custom runtime for each of the provided JDKs.
     *
     * @param jdks         JDKs to link
     * @param dependencies Modules to add to the JRE
     * @param targetDir    Directory
     * @return Directories containing linked JREs from each JDK
     * @throws IOException          If an I/O error occurs while linking a JRE
     * @throws InterruptedException If a jlink thread is interrupted
     */
    private static Map<Platform, File> link(final Map<Platform, File> jdks, final Set<String> dependencies, final File targetDir) throws IOException, InterruptedException {
        Map<Platform, File> ret = new EnumMap<>(Platform.class);
        CountDownLatch latch = new CountDownLatch(jdks.size());
        File jlinkExec = SystemUtils.locateBinary("jlink");

        for (Map.Entry<Platform, File> jdk : jdks.entrySet()) {
            Platform platform = jdk.getKey();
            File jdkDir = jdk.getValue();

            // Locate modules
            File moduleDir = new File(jdkDir, "jmods");
            if (!moduleDir.exists()) {
                throw new FileNotFoundException("Couldn't find modules for " + platform.toString() + " JDK.");
            }

            // Create directory for platform
            File platformJreDir = new File(targetDir, platform.toString());
            EXECUTOR.execute(() -> {
                // Start jlink
                try {
                    Process jlinkProcess = new ProcessBuilder(
                            jlinkExec.getAbsolutePath(),
                            "--strip-debug",
                            "--no-header-files",
                            "--no-man-pages",
                            "--compress", "2",
                            "--module-path", moduleDir.getAbsolutePath(),
                            "--add-modules", String.join(",", dependencies),
                            "--output", platformJreDir.getAbsolutePath()
                    ).start();

                    // Wait for jlink to finish
                    jlinkProcess.waitFor();

                    // Add linked JRE to results
                    ret.put(platform, platformJreDir);
                    latch.countDown();
                    Logger.debug("Finished linking {} JRE.", platform);
                } catch (InterruptedException e) {
                    Logger.error(e, "jlink process for " + platform.toString() + " was interrupted");
                    Thread.currentThread().interrupt();
                    System.exit(1);
                } catch (IOException e) {
                    Logger.error(e, "jlink process for " + platform.toString() + " encountered an I/O error");
                    System.exit(1);
                }
            });
        }

        // Wait for threads to complete and return
        latch.await();
        return ret;
    }

    /**
     * Packages a release with each of the given JREs.
     *
     * @param jres      JREs to use
     * @param targetDir Directory to package releases in
     * @throws InterruptedException If a packaging thread is interrupted
     */
    private static void packageReleases(final Map<Platform, File> jres, final File targetDir) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(jres.size());

        for (Map.Entry<Platform, File> jre : jres.entrySet()) {
            Platform platform = jre.getKey();
            File jreDir = jre.getValue();

            EXECUTOR.execute(() -> {
                try {
                    // Create directory for platform
                    File releaseDir = new File(targetDir, platform.toString());
                    FileUtils.forceMkdir(releaseDir);

                    // Copy launch script
                    if (platform == Platform.WINDOWS) {
                        File scriptFile = new File(releaseDir, "start.bat");
                        FileUtils.write(scriptFile, SCRIPT_WIN + artifact.getName(), StandardCharsets.UTF_8);
                    } else {
                        File scriptFile = new File(releaseDir, "start.sh");
                        FileUtils.write(scriptFile, SCRIPT_UNIX + artifact.getName(), StandardCharsets.UTF_8);
                    }

                    // Copy JAR and JRE
                    FileUtils.copyFileToDirectory(artifact, releaseDir);
                    FileUtils.copyDirectory(jreDir, new File(releaseDir, "jre"));

                    // Compress release
                    String archiveName = "ghost2-" +
                            platform.toString() +
                            "-" +
                            References.VERSION_STRING +
                            ".zip";
                    SystemUtils.compress(releaseDir, new File(targetDir, archiveName));

                    // Remove uncompressed copy
                    FileUtils.forceDelete(releaseDir);
                    Logger.debug("Finished packaging {} release.", platform.toString());
                } catch (IOException e) {
                    Logger.error(e, "I/O error occurred while packaging release for " + platform.toString());
                    System.exit(1);
                }

                latch.countDown();
            });
        }

        // Wait for threads to complete
        latch.await();
    }

    /**
     * Analyzes the dependencies of the ghost2 fat JAR.
     *
     * @return Module names of dependencies
     * @throws FileNotFoundException If a valid {@code jdeps} executable couldn't be located
     * @throws IOException           If an I/O exception occurs while running {@code jdeps}
     */
    private static Set<String> analyzeDependencies() throws IOException {
        // Figure out which modules are supplied by the installed JDK
        Set<String> jdkModules = getJdkModules();

        // Locate ghost2 artifact
        artifact = locateArtifact();

        // Run jdeps on the artifact
        final File jdepsBin = SystemUtils.locateBinary("jdeps");
        Process jdepsProcess = new ProcessBuilder(
                jdepsBin.getAbsolutePath(),
                "-s",
                "-q",
                "--multi-release", "11",
                "--class-path", SystemUtils.CLASSPATH,
                "--module-path", SystemUtils.CLASSPATH,
                artifact.getAbsolutePath()
        ).start();

        // Add modules to set
        Set<String> deps = new HashSet<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(jdepsProcess.getInputStream()))) {
            r.lines()
                    .filter(s -> s.contains("->"))
                    .forEach(line -> {
                        if (StringUtils.isBlank(line)) return;
                        String[] components = line.replaceAll("\\s+", " ").trim().split(" ");
                        String module = components[components.length - 1];
                        if (jdkModules.contains(module)) deps.add(module);
                    });
        }
        Logger.debug("Found {} Java module dependencies.", deps.size());
        return deps;
    }

    /**
     * Gets all the modules supplied by the local JDK.
     *
     * @return Names of modules supplied by the local JDK
     * @throws FileNotFoundException if the modules directory is missing
     * @throws IOException           if an I/O exception occurs
     */
    private static Set<String> getJdkModules() throws IOException {
        Set<String> modules = new HashSet<>();
        File moduleDir = new File(SystemUtils.getJdkPath(), "jmods");
        if (!moduleDir.exists()) {
            throw new FileNotFoundException(ERROR_MODULES_MISSING);
        }

        for (File module : FileUtils.listFiles(moduleDir, null, true)) {
            String filename = module.getName();
            modules.add(StringUtils.removeEnd(filename, ".jmod"));
        }
        return modules;
    }

    /**
     * Attempts to locate the JAR artifact generated by bootJar.
     *
     * @throws FileNotFoundException If an artifact can't be found
     */
    private static File locateArtifact() throws FileNotFoundException {
        File artifactDir = new File("build/libs");
        for (File artifact : FileUtils.listFiles(artifactDir, null, true)) {
            if (artifact.isFile() && artifact.getName().matches("ghost2-\\d\\.\\d\\.jar")) {
                return artifact;
            }
        }

        throw new FileNotFoundException(ERROR_ARTIFACT_MISSING);
    }
}