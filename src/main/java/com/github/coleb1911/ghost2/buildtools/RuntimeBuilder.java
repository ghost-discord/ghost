package com.github.coleb1911.ghost2.buildtools;

import com.github.coleb1911.ghost2.shared.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes dependencies of a ghost2 fat JAR, produces a custom runtime image with
 * {@code jlink}, then bundles it with the JAR and a start script.
 * <p>
 * Don't run this tool manually. Use the {@code ghostPackage} Gradle task.<br>
 * Also, mind the dust. This package is a mess.
 */
public final class RuntimeBuilder {
    private static final String MESSAGE_DONE = "Finished successfully in {} seconds. Results saved to {}.";
    private static final String SCRIPT_UNIX = "SCRIPTDIR=$(dirname $0)\ncd ${SCRIPTDIR}\n${SCRIPTDIR}/jre/bin/java -Dloader.path=${SCRIPTDIR} -jar ${SCRIPTDIR}/";
    private static final String SCRIPT_WIN = "cd %~dp0\r\n%~dp0jre\\bin\\java.exe -Dloader.path=%~dp0 -jar %~dp0";

    private static Set<String> dependencies = new HashSet<>();
    private static File fatJar;

    public static void main(String[] args) throws JDKNotFoundException, IOException {
        // Log config
        Configurator.defaultConfig()
                .level(Level.INFO)
                .level("org.apache.commons", Level.OFF)
                .writingThread(true)
                .activate();

        long start = System.currentTimeMillis();
        analyzeDependencies(); // Writes directly to the dependency set, no need to assign

        // Make temp folder
        final File temp = new File("bt_temp");
        if (temp.exists()) {
            FileUtils.cleanDirectory(temp);
        }
        FileUtils.forceMkdir(temp);

        // Fetch JDKs for other platforms
        final File jdkDir = new File("xplatformjdk");
        Map<JDKDownloadUtils.Platform, File> jdks = new EnumMap<>(JDKDownloadUtils.Platform.class);
        JDKDownloadUtils.Platform p;
        switch (JDKDownloadUtils.Platform.fromPlatformString(SystemUtils.getPlatform())) {
            case WINDOWS:
                jdks.put(p = JDKDownloadUtils.Platform.OSX, JDKDownloadUtils.download(p, jdkDir));
                jdks.put(p = JDKDownloadUtils.Platform.LINUX, JDKDownloadUtils.download(p, jdkDir));
                break;
            case OSX:
                jdks.put(p = JDKDownloadUtils.Platform.WINDOWS, JDKDownloadUtils.download(p, jdkDir));
                jdks.put(p = JDKDownloadUtils.Platform.LINUX, JDKDownloadUtils.download(p, jdkDir));
                break;
            case LINUX:
                jdks.put(p = JDKDownloadUtils.Platform.WINDOWS, JDKDownloadUtils.download(p, jdkDir));
                jdks.put(p = JDKDownloadUtils.Platform.OSX, JDKDownloadUtils.download(p, jdkDir));
                break;
        }

        // Unzip JDK archives
        for (Map.Entry<JDKDownloadUtils.Platform, File> jdk : jdks.entrySet()) {
            // Get and log info
            String name = jdk.getKey().toString();
            File archive = jdk.getValue();
            File unzippedFolder = new File(temp, "jdks");
            File outputFolder = new File(unzippedFolder, name);
            Logger.info("Unzipping {} JDK...", name);

            // Unzip and replace path in entry set
            FileUtils.forceMkdir(outputFolder);
            SystemUtils.extract(archive, outputFolder);
            jdks.put(jdk.getKey(), outputFolder);
        }

        // Run jlink for each platform
        final File jlink = SystemUtils.locateBinary("jlink");
        for (JDKDownloadUtils.Platform platform : JDKDownloadUtils.Platform.values()) {
            File modulePath = null;
            if (jdks.containsKey(platform)) {
                File unzipped = jdks.get(platform);
                File[] unzippedJdks = unzipped.listFiles();
                assert unzippedJdks != null;
                for (File f : unzippedJdks) {
                    if (f.getName().contains("jdk-11") && !f.getName().contains("._")) {
                        String path = platform == JDKDownloadUtils.Platform.OSX ?
                                "Contents" + File.separator + "Home" + File.separator + "jmods" :
                                "jmods";
                        modulePath = new File(f, path);
                        break;
                    }
                }
            } else {
                modulePath = new File(SystemUtils.getJdkPath(), "jmods");
            }

            if (modulePath == null) {
                System.exit(0);
            }

            File jres = new File(temp, "jres");
            FileUtils.forceMkdir(jres);
            Logger.info("Linking {} JRE...", platform.toString());
            Process jlinkProcess = new ProcessBuilder(
                    jlink.getAbsolutePath(),
                    "--strip-debug",
                    "--no-header-files",
                    "--no-man-pages",
                    "--compress", "2",
                    "--module-path", modulePath.getAbsolutePath(),
                    "--add-modules", String.join(",", dependencies),
                    "--output", new File(jres, platform.toString()).getAbsolutePath()
            ).start();

            try (BufferedReader r = new BufferedReader(new InputStreamReader(jlinkProcess.getInputStream()))) {
                r.lines().forEach(Logger::info);
            }
        }

        // Copy JAR to temp folder
        FileUtils.copyFile(fatJar, new File(temp, fatJar.getName()));

        // Copy start scripts to temp folder
        FileUtils.write(new File(temp, "start.bat"), SCRIPT_WIN + fatJar.getName(), StandardCharsets.UTF_8);
        FileUtils.write(new File(temp, "start.sh"), SCRIPT_UNIX + fatJar.getName(), StandardCharsets.UTF_8);

        // Package builds
        File buildFolder = new File("bt_output");
        if (buildFolder.exists())
            FileUtils.cleanDirectory(buildFolder);
        for (JDKDownloadUtils.Platform platform : JDKDownloadUtils.Platform.values()) {
            // Locate files
            String archiveName = String.format("ghost2-%s-%s", platform.toString(), Constants.VERSION_STRING);
            File jreFolder = new File(temp, "jres" + File.separator + platform.toString());
            File script = platform == JDKDownloadUtils.Platform.WINDOWS ? new File(temp, "start.bat") : new File(temp, "start.sh");
            File outputFolder = new File(buildFolder, archiveName);
            FileUtils.forceMkdir(outputFolder);

            // Copy files
            Logger.info("Finalizing {}: copying files", platform.toString());
            FileUtils.copyDirectory(jreFolder, new File(outputFolder, "jre"));
            FileUtils.copyFile(script, new File(outputFolder, script.getName()));
            FileUtils.copyFile(fatJar, new File(outputFolder, fatJar.getName()));

            Logger.info("Finalizing {}: compressing", platform.toString());
            SystemUtils.compress(outputFolder, new File(buildFolder, archiveName + ".zip"));

            Logger.info("Finalizing {}: cleaning up", platform.toString());
            FileUtils.deleteDirectory(outputFolder);
        }

        // Finish, clean up, and log time
        long end = System.currentTimeMillis();
        Logger.info(MESSAGE_DONE, new DecimalFormat("#.##").format((end - start) / 1000.0), buildFolder.getAbsolutePath());
        FileUtils.deleteDirectory(temp);
    }

    /**
     * Analyzes and saves dependencies of the ghost2 fat JAR.
     *
     * @throws JDKNotFoundException If a valid {@code jdeps} executable couldn't be located
     * @throws IOException          If an I/O exception occurs while running {@code jdeps}
     */
    private static void analyzeDependencies() throws JDKNotFoundException, IOException {
        /* Figure out which modules are actually supplied by Java itself.
         * Tons of our JAR dependencies use automatic module names, which jlink
         * doesn't support. For that reason, we just supply them as JARs and
         * worry only about Java modules.
         */
        Set<String> jdkModules = new HashSet<>();
        String[] modfiles = new File(SystemUtils.getJdkPath(), "jmods").list();
        assert modfiles != null;
        for (String s : modfiles) {
            jdkModules.add(s.replace(".jmod", ""));
        }

        // Locate ghost2 JAR
        File[] jars = new File("build/libs").listFiles();
        assert jars != null;
        for (File j : jars) {
            if (j.isFile() && j.getName().contains("ghost2")) {
                fatJar = j;
                break;
            }
        }

        // Run jdeps
        final File jdeps = SystemUtils.locateBinary("jdeps");
        Process jdepsProcess = new ProcessBuilder(
                jdeps.getAbsolutePath(),
                "-s",
                "-q",
                "--multi-release", "11",
                "--class-path", SystemUtils.getClasspath(),
                "--module-path", SystemUtils.getClasspath(),
                fatJar.getAbsolutePath()
        ).start();

        // Add modules to set
        try (BufferedReader r = new BufferedReader(new InputStreamReader(jdepsProcess.getInputStream()))) {
            r.lines()
                    .filter(s -> s.contains("->"))
                    .forEach(line -> {
                        if (StringUtils.isBlank(line)) return;
                        String[] components = line.replaceAll("\\s+", " ").trim().split(" ");
                        String module = components[components.length - 1];
                        if (jdkModules.contains(module)) dependencies.add(module);
                    });
        }
        Logger.info("Found {} Java module dependencies.", dependencies.size());
    }
}