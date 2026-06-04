/*
 * Mohist - OneWorldCore
 * Copyright (C) 2018-2024.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.oneworldstudiomc.libraries;

import com.oneworldstudiomc.OneWorldCoreStart;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.SneakyThrows;
import lombok.ToString;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

@ToString
public class LibrariesDownloadQueue {

    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;

    @ToString.Exclude
    public final Set<Libraries> allLibraries = new HashSet<>();
    @ToString.Exclude
    private final Set<Libraries> fail = new HashSet<>();
    @ToString.Exclude
    private final Map<String, String> librarySourceJars = new HashMap<>();
    @ToString.Exclude
    private final Map<String, String> librarySourceEntries = new HashMap<>();
    @ToString.Exclude
    public InputStream inputStream = null;
    @ToString.Exclude
    public Set<Libraries> need_download = new LinkedHashSet<>();

    public String parentDirectory = "libraries";
    public String systemProperty = null;
    public boolean debug = false;


    public static LibrariesDownloadQueue create() {
        return new LibrariesDownloadQueue();
    }

    private static boolean isTargetFile(JarEntry entry) {
        String name = entry.getName().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".txt");
    }

    /**
     * Set the input stream for the list that needs to be downloaded
     *
     * @param inputStream The input stream of the target file
     * @return Returns the current real column
     */
    public LibrariesDownloadQueue inputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /**
     * Set the file download directory
     *
     * @param parentDirectory The path to which the file is downloaded
     * @return Returns the current real column
     */
    public LibrariesDownloadQueue parentDirectory(String parentDirectory) {
        this.parentDirectory = parentDirectory;
        return this;
    }

    /**
     * Construct the final column
     *
     * @return Construct the final column
     */
    @SneakyThrows
    public LibrariesDownloadQueue build() {
        scanFromJar();
        return this;
    }

    /**
     * Download in the form of a progress bar
     */
    public void progressBar() {
        for (int attempt = 1; attempt <= MAX_DOWNLOAD_ATTEMPTS; attempt++) {
            fail.clear();
            need_download.clear();
            if (!needDownload()) {
                return;
            }

            ProgressBarBuilder builder = new ProgressBarBuilder()
                    .setTaskName("")
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUpdateIntervalMillis(100)
                    .setInitialMax(need_download.size());
            try (ProgressBar pb = builder.build()) {
                for (Libraries lib : need_download) {
                    String libraryPath = normalizeLibraryPath(lib.path);
                    File file = new File(parentDirectory, libraryPath);
                    file.getParentFile().mkdirs();
                    String url = "META-INF/libraries/" + libraryPath;
                    if (copyFileFromJar(file, url, lib)) {
                        debug("downloadFile: OK");
                        fail.remove(lib);
                    } else {
                        debug("downloadFile: No " + url);
                        fail.add(lib);
                    }
                    pb.step();
                }
            }

            if (fail.isEmpty()) {
                return;
            }

            System.out.println("[OneWorldCore] Library extraction retry " + attempt + "/" + MAX_DOWNLOAD_ATTEMPTS + ", failed files: " + fail.size());
        }

        if (!fail.isEmpty()) {
            throw new RuntimeException("Failed to extract OneWorldCore libraries after " + MAX_DOWNLOAD_ATTEMPTS + " attempts: " + fail);
        }
    }

    protected boolean copyFileFromJar(File file, String pathInJar, Libraries lib) {
        if (isValidLibrary(file, lib.getSha256())) {
            return true;
        }

        file.getParentFile().mkdirs();
        try {
            String libraryPath = normalizeLibraryPath(lib.path);
            if (!copyFromScannedJar(file, libraryPath, lib.getSha256()) && !copyMatchingResource(file, pathInJar, lib.getSha256())) {
                System.out.println("[OneWorldCore] The file " + file.getPath() + " doesn't exist in the OneWorldCore jar or has an invalid checksum!");
                return false;
            }
        } catch (IOException exception) {
            return false;
        }

        if (isValidLibrary(file, lib.getSha256())) {
            return true;
        }

        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
        return false;
    }

    public boolean needDownload() {
        for (Libraries libraries : allLibraries) {
            File lib = new File(parentDirectory, normalizeLibraryPath(libraries.path));
            if (isValidLibrary(lib, libraries.sha256)) {
                continue;
            }
            if (lib.exists()) {
                try {
                    Files.deleteIfExists(lib.toPath());
                } catch (IOException ignored) {
                }
            }
            debug("sha256: %s : %s%n".formatted(lib, libraries.sha256));
            need_download.add(libraries);
        }
        return !need_download.isEmpty();
    }

    public void scanFromJar() throws IOException {
        Enumeration<URL> resources = LibrariesDownloadQueue.class.getClassLoader().getResources("META-INF/libraries");
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if ("jar".equals(url.getProtocol())) {
                JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                JarFile jarFile = jarConnection.getJarFile();
                String entryPrefix = jarConnection.getEntryName();
                if (!entryPrefix.endsWith("/")) {
                    entryPrefix += "/";
                }
                String librariesEntryPrefix = entryPrefix;

                jarFile.stream()
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().startsWith(librariesEntryPrefix))
                        .filter(LibrariesDownloadQueue::isTargetFile)
                        .forEach(entry -> {
                            String line = entry.getName().substring(librariesEntryPrefix.length()).replace('\\', '/');
                            while (line.startsWith("/")) {
                                line = line.substring(1);
                            }
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                Libraries libraries = new Libraries(line, sha256(is), entry.getSize());
                                allLibraries.add(libraries);
                                librarySourceJars.put(line, jarFile.getName());
                                librarySourceEntries.put(line, entry.getName());
                                debug("Find the resource: " + libraries);
                            } catch (IOException exception) {
                                throw new RuntimeException("Could not read bundled library " + entry.getName(), exception);
                            }
                        });
            }
        }
    }

    public void debug(String log) {
        if (debug) System.out.println(log + "\n");
    }

    private static String normalizeLibraryPath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String sha256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static boolean isValidLibrary(File file, String expectedSha256) {
        if (!file.exists() || file.length() <= 1) {
            return false;
        }
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return expectedSha256.equalsIgnoreCase(sha256(inputStream));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean copyMatchingResource(File file, String pathInJar, String expectedSha256) throws IOException {
        Enumeration<URL> resources = OneWorldCoreStart.class.getClassLoader().getResources(pathInJar);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            Path tempFile = file.toPath().resolveSibling(file.getName() + ".tmp");
            String actualSha256 = copyWithSha256(resource, tempFile);
            if (expectedSha256.equalsIgnoreCase(actualSha256)) {
                Files.move(tempFile, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
            Files.deleteIfExists(tempFile);
        }
        return false;
    }

    private boolean copyFromScannedJar(File file, String libraryPath, String expectedSha256) throws IOException {
        String sourceJar = librarySourceJars.get(libraryPath);
        String sourceEntry = librarySourceEntries.get(libraryPath);
        if (sourceJar == null || sourceEntry == null) {
            return false;
        }

        Path tempFile = file.toPath().resolveSibling(file.getName() + ".tmp");
        try (JarFile jarFile = new JarFile(sourceJar)) {
            Optional<JarEntry> matchingEntry = jarFile.stream()
                    .filter(entry -> sourceEntry.equals(entry.getName()))
                    .filter(entry -> {
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            return expectedSha256.equalsIgnoreCase(sha256(inputStream));
                        } catch (IOException exception) {
                            return false;
                        }
                    })
                    .findFirst();
            if (matchingEntry.isEmpty()) {
                return false;
            }
            try (InputStream inputStream = jarFile.getInputStream(matchingEntry.get())) {
                String actualSha256 = copyWithSha256(inputStream, tempFile);
                if (!expectedSha256.equalsIgnoreCase(actualSha256)) {
                    Files.deleteIfExists(tempFile);
                    return false;
                }
            }
        }

        Files.move(tempFile, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static String copyWithSha256(URL resource, Path outputFile) throws IOException {
        try (InputStream inputStream = resource.openStream()) {
            return copyWithSha256(inputStream, outputFile);
        }
    }

    private static String copyWithSha256(InputStream inputStream, Path outputFile) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (var outputStream = Files.newOutputStream(outputFile)) {
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
