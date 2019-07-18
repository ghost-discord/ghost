package com.github.coleb1911.ghost2.buildtools;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Various system-related tools.
 */
final class SystemUtils {
    static final int BUFFER_SIZE = 8192;
    static final String USER_AGENT = System.getProperty("java.version") + "/" + "Apache HttpClient 4.5.9";

    private static final String PLATFORM = System.getProperty("os.name");
    private static final String CLASSPATH = System.getProperty("java.class.path");
    private static final String EXEC_EXT = (PLATFORM.startsWith("Windows")) ? ".exe" : "";
    private static final String ENV_HOME = "JAVA_HOME";
    private static final String ENV_PATH = "PATH";

    private static File jdkPath;
    private static File jdkBin;

    private SystemUtils() {
    }

    /**
     * Finds a JDK binary by name via JAVA_HOME or PATH.
     *
     * @param name Name of the binary (excluding the file extension)
     * @return Binary file
     * @throws IllegalArgumentException If the JDK doesn't contain the given binary
     * @throws JDKNotFoundException     If a valid JDK can't be located on the system
     * @throws IOException              If an I/O exception occurs while attempting to locate the binary
     */
    static File locateBinary(String name) throws JDKNotFoundException, IOException {
        if (jdkPath == null || !jdkPath.exists())
            locateJdk();

        final String execName = name + EXEC_EXT;
        File exec = new File(jdkBin, execName);
        if (exec.isFile() && exec.canExecute())
            return exec;
        throw new IllegalArgumentException("Cannot find " + name + " in the JDK binaries");
    }

    /**
     * Attempts to find a JDK via JAVA_HOME or PATH.
     *
     * @throws JDKNotFoundException If a valid JDK can't be located on the system
     * @throws IOException          If an I/O exception occurs while attempting to locate the JDK
     */
    private static void locateJdk() throws JDKNotFoundException, IOException {
        final String home = System.getenv(ENV_HOME);
        final String path = System.getenv(ENV_PATH);

        if (StringUtils.isNotBlank(home)) {
            findJdkFromHome(home);
        } else if (StringUtils.isNotBlank(path)) {
            findJdkFromPath(path);
        } else {
            throw new JDKNotFoundException();
        }
        Logger.info("Found local JDK installation: {}", jdkPath.getAbsolutePath());
    }

    /**
     * Find the JDK via JAVA_HOME.
     *
     * @param home JAVA_HOME variable
     */
    private static void findJdkFromHome(final String home) {
        // Use JAVA_HOME if defined
        File binDir = new File(home + File.separator + "bin");
        File javaExec = new File(binDir, "java" + EXEC_EXT);

        if (javaExec.exists() && javaExec.canExecute()) {
            jdkPath = new File(home);
            jdkBin = binDir;
        }
    }

    /**
     * Find the JDK via the PATH.
     *
     * @param path PATH variable
     * @throws IOException If an I/O exception occurs while attempting to locate the JDK
     */
    private static void findJdkFromPath(final String path) throws IOException {
        for (String dirname : path.split(File.pathSeparator)) {
            File binDir = new File(dirname);
            File javaExec = new File(binDir, "java" + EXEC_EXT);

            if (FileUtils.isSymlink(javaExec)) {
                javaExec = findSymlinkTarget(javaExec);
                binDir = javaExec.getParentFile();
            }
            if (javaExec.isFile() && javaExec.canExecute()) {
                jdkPath = binDir.getParentFile();
                jdkBin = binDir;
                return;
            }
        }
    }

    /**
     * Finds the canonicalized target of a symbolic link.
     *
     * @param link Link to follow
     * @return Canonicalized target of the link
     * @throws IOException If an I/O error occurs while processing the link
     */
    private static File findSymlinkTarget(File link) throws IOException {
        if (FileUtils.isSymlink(link)) {
            File target = link.toPath().toRealPath().toFile();
            if (FileUtils.isSymlink(target)) {
                findSymlinkTarget(target);
            }
            return target;
        }
        throw new IllegalArgumentException("File is not a symbolic link");
    }

    /**
     * Extracts an archive.<br>
     * Supports {@code .tar.gz} and {@code .zip} formats.
     *
     * @param archive File to extract
     * @param target  Directory to extract to
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some other reason cannot be opened for reading
     */
    static void extract(File archive, File target) throws IOException {
        if (!target.isDirectory()) {
            throw new IllegalArgumentException("Target directory does not exist or is not a directory.");
        }

        if (archive.isDirectory()) {
            throw new IllegalArgumentException("Archive file is a directory.");
        }

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(archive))) {
            if (archive.getName().endsWith(".tar.gz")) {
                try (TarArchiveInputStream archiveIn = new TarArchiveInputStream(new GzipCompressorInputStream(in))) {
                    extract(archiveIn, target);
                }
            } else if (archive.getName().endsWith(".zip")) {
                try (ZipArchiveInputStream archiveIn = new ZipArchiveInputStream(in)) {
                    extract(archiveIn, target);
                }
            } else {
                throw new IllegalArgumentException("Archive is not in a supported format.");
            }
        }
    }

    /**
     * Extracts an archive.
     *
     * @param in     Archive file input stream
     * @param target Directory to extract to
     * @throws IOException If an I/O error occurs while extracting the archive
     */
    private static void extract(final ArchiveInputStream in, final File target) throws IOException {
        ArchiveEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                File dir = new File(target, entry.getName());
                FileUtils.forceMkdir(dir);
            } else {
                try (BufferedOutputStream out = new BufferedOutputStream(FileUtils.openOutputStream(new File(target, entry.getName()), false), BUFFER_SIZE)) {
                    IOUtils.copy(in, out, BUFFER_SIZE);
                }
            }
        }
    }

    /**
     * Compress a directory and all files in it.
     *
     * @param dir        Directory to compress
     * @param outputFile Output archive
     * @throws IOException If an I/O error occurs while compressing the directory
     */
    static void compress(File dir, File outputFile) throws IOException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Target directory is not a directory");
        }

        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(outputFile)) {
            compressDirectory(dir.getAbsoluteFile(), dir, out);
        }
    }

    /**
     * Compress a directory and all files in it.
     *
     * @param root   Root directory
     * @param source Current directory in recursive call
     * @param out    Archive output stream
     * @throws IOException If an I/O error occurs while compressing the directory
     */
    private static void compressDirectory(File root, File source, ZipArchiveOutputStream out) throws IOException {
        for (File file : FileUtils.listFiles(source, null, true)) {
            if (file.isDirectory()) {
                compressDirectory(root, new File(source, file.getName()), out);
            } else {
                String entryName = StringUtils.removeStart(file.getAbsolutePath(), root.getAbsolutePath()).replaceFirst("\\\\", "");
                ZipArchiveEntry entry = (ZipArchiveEntry) out.createArchiveEntry(file, entryName);

                out.putArchiveEntry(entry);
                try (FileInputStream in = new FileInputStream(file)) {
                    IOUtils.copy(in, out);
                }
                out.closeArchiveEntry();
            }
        }
    }

    /**
     * @return This system's platform string
     */
    static String getPlatform() {
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
    static File getJdkPath() throws JDKNotFoundException, IOException {
        if (jdkPath == null)
            locateJdk();
        return jdkPath;
    }
}
