package com.github.coleb1911.ghost2.buildtools;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Various system-related tools
 */
public final class SystemUtils {
    private static final String PLATFORM = System.getProperty("os.name");
    private static final String CLASSPATH = System.getProperty("java.class.path");
    private static final String EXEC_EXT = (PLATFORM.startsWith("Windows")) ? ".exe" : "";
    private static final String ENV_HOME = "JAVA_HOME";
    private static final String ENV_PATH = "PATH";

    private static File jdkPath;
    private static File jdkBin;

    /**
     * Finds a JDK binary by name via JAVA_HOME or PATH.
     *
     * @param name Name of the binary (excluding the file extension)
     * @return Binary file
     * @throws IllegalArgumentException If the JDK doesn't contain the given binary
     * @throws JDKNotFoundException     If a valid JDK can't be located on the system
     */
    static File locateBinary(String name) throws JDKNotFoundException {
        if (jdkPath == null || !jdkPath.exists())
            locateJdk();

        final String execName = name + EXEC_EXT;
        File exec = new File(jdkBin, execName);
        if (exec.isFile() && exec.canExecute())
            return exec;
        throw new IllegalArgumentException(String.format("Cannot find %s in the JDK binaries", name));
    }

    /**
     * Attempts to find a JDK via JAVA_HOME or PATH.
     *
     * @throws JDKNotFoundException If a valid JDK can't be located on the system
     */
    private static void locateJdk() throws JDKNotFoundException {
        final String home = System.getenv(ENV_HOME);
        final String path = System.getenv(ENV_PATH);
        final String javaExecName = "java" + EXEC_EXT;
        File javaExec;

        if (StringUtils.isNotBlank(home)) {
            // Use JAVA_HOME if defined
            File bin = new File(home + File.separator + "bin");
            javaExec = new File(bin, javaExecName);
            if (javaExec.exists() && javaExec.canExecute()) {
                jdkPath = new File(home);
                jdkBin = bin;
                return;
            }
        } else if (StringUtils.isNotBlank(path)) {
            // Scan the PATH for the binary
            for (String dirname : path.split(File.pathSeparator)) {
                File bin = new File(dirname);
                javaExec = new File(bin, javaExecName);
                if (javaExec.isFile() && javaExec.canExecute()) {
                    jdkPath = bin.getParentFile();
                    jdkBin = bin;
                    return;
                }
            }
        }
        throw new JDKNotFoundException();
    }

    /**
     * Unzips a {@code .zip} or {@code .tar.gz} file
     *
     * @param zipFile File to unzip
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some other reason cannot be opened for reading
     */
    public static void unzip(File zipFile, File target) throws IOException {
        final int BUFFER_SIZE = 8192;
        InputStream fileIn = new FileInputStream(zipFile);
        if (zipFile.getName().endsWith(".tar.gz")) {
            try (TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(fileIn))) {
                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) in.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        File dir = new File(target, entry.getName());
                        FileUtils.forceMkdir(dir);
                    } else {
                        int bytes;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        try (BufferedOutputStream out = new BufferedOutputStream(FileUtils.openOutputStream(new File(target, entry.getName()), false), BUFFER_SIZE)) {
                            while ((bytes = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                                out.write(buffer, 0, bytes);
                            }
                        }
                    }
                }
            }
        } else {
            try (ZipArchiveInputStream in = new ZipArchiveInputStream(fileIn)) {
                ZipArchiveEntry entry;
                while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        File dir = new File(target, entry.getName());
                        FileUtils.forceMkdir(dir);
                    } else {
                        int bytes;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        try (BufferedOutputStream out = new BufferedOutputStream(FileUtils.openOutputStream(new File(target, entry.getName()), false), BUFFER_SIZE)) {
                            while ((bytes = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                                out.write(buffer, 0, bytes);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @return This system's platform string
     */
    public static String getPlatform() {
        return PLATFORM;
    }

    /**
     * @return The classpath passed to this JVM instance
     */
    static String getClasspath() {
        return CLASSPATH;
    }

    /**
     * @return The JDK path as defined by JAVA_HOME or PATH
     * @throws JDKNotFoundException If a valid JDK can't be located on the system
     */
    public static File getJdkPath() throws JDKNotFoundException {
        if (jdkPath == null)
            locateJdk();
        return jdkPath;
    }
}
