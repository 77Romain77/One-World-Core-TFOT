package com.oneworldstudiomc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.YamlConfiguration;

public final class OneWorldUpdater {

    private static final Path CONFIG_PATH = Path.of("oneworldupdate.yml");
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JAR_ASSET_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]*oneworldcore[^\"]*\\.jar)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAR_VERSION_PATTERN = Pattern.compile("oneworldcore-([0-9][0-9A-Za-z._-]*)\\.jar", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_PROPERTY_PATTERN = Pattern.compile("(?m)^(?:version|core-version|oneworldcore-version)\\s*=\\s*(.+)$");
    private static final Pattern DOWNLOAD_PROPERTY_PATTERN = Pattern.compile("(?m)^(?:download-url|downloadUrl)\\s*=\\s*(.+)$");

    private OneWorldUpdater() {
    }

    public static void checkOnStartup() {
        YamlConfiguration config = loadConfig();
        if (!config.getBoolean("enabled", true)) {
            OneWorldCore.LOGGER.info("OneWorldCore auto update is disabled in oneworldupdate.yml");
            return;
        }

        Thread updaterThread = new Thread(() -> runUpdateCheck(config), "OneWorldCore-Updater");
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    private static YamlConfiguration loadConfig() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(CONFIG_PATH.toFile());
        config.addDefault("enabled", true);
        config.addDefault("check-url", "https://api.github.com/repos/OneWorldStudio/One-World-Core/releases/latest");
        config.addDefault("download-url", "");
        config.addDefault("download-directory", "updates");
        config.addDefault("timeout-seconds", 20);
        config.options().copyDefaults(true);
        try {
            config.save(CONFIG_PATH.toFile());
        } catch (IOException exception) {
            OneWorldCore.LOGGER.warn("Could not save oneworldupdate.yml", exception);
        }
        return config;
    }

    private static void runUpdateCheck(YamlConfiguration config) {
        try {
            int timeoutSeconds = Math.max(5, config.getInt("timeout-seconds", 20));
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            UpdateInfo updateInfo = resolveUpdateInfo(client, config.getString("check-url", ""), config.getString("download-url", ""), timeoutSeconds);
            if (updateInfo.version == null || updateInfo.version.isBlank()) {
                OneWorldCore.LOGGER.warn("OneWorldCore updater could not resolve latest version");
                return;
            }

            String currentVersion = normalizeVersion(OneWorldCore.version);
            String latestVersion = normalizeVersion(updateInfo.version);
            if (compareVersions(latestVersion, currentVersion) <= 0) {
                OneWorldCore.LOGGER.info("OneWorldCore is up to date: {}", currentVersion);
                return;
            }

            if (updateInfo.downloadUrl == null || updateInfo.downloadUrl.isBlank()) {
                OneWorldCore.LOGGER.warn("OneWorldCore {} is available, but download-url is empty in oneworldupdate.yml", latestVersion);
                return;
            }

            Path outputDirectory = Path.of(config.getString("download-directory", "updates"));
            Files.createDirectories(outputDirectory);
            Path outputFile = outputDirectory.resolve("oneworldcore-" + latestVersion + ".jar");
            if (Files.exists(outputFile) && Files.size(outputFile) > 0) {
                OneWorldCore.LOGGER.info("OneWorldCore update {} already downloaded: {}", latestVersion, outputFile.toAbsolutePath());
                return;
            }

            downloadJar(client, updateInfo.downloadUrl.replace("{version}", latestVersion), outputFile, timeoutSeconds);
            OneWorldCore.LOGGER.info("Downloaded OneWorldCore {} to {}. Restart the server with this jar to finish the update.", latestVersion, outputFile.toAbsolutePath());
        } catch (Exception exception) {
            OneWorldCore.LOGGER.warn("OneWorldCore auto update check failed", exception);
        }
    }

    private static UpdateInfo resolveUpdateInfo(HttpClient client, String checkUrl, String configuredDownloadUrl, int timeoutSeconds) throws IOException, InterruptedException {
        if (checkUrl == null || checkUrl.isBlank()) {
            return new UpdateInfo(null, configuredDownloadUrl);
        }

        String body = sendTextRequest(client, checkUrl, timeoutSeconds);
        String version = firstMatch(TAG_PATTERN, body);
        String downloadUrl = configuredDownloadUrl;

        if (version == null) {
            version = firstMatch(VERSION_PROPERTY_PATTERN, body);
        }
        String bodyDownloadUrl = firstMatch(JAR_ASSET_PATTERN, body);
        if (bodyDownloadUrl == null) {
            bodyDownloadUrl = firstMatch(DOWNLOAD_PROPERTY_PATTERN, body);
        } else {
            String jarVersion = firstMatch(JAR_VERSION_PATTERN, bodyDownloadUrl);
            if (jarVersion != null && !jarVersion.isBlank()) {
                version = jarVersion;
            }
        }
        if (downloadUrl == null || downloadUrl.isBlank()) {
            downloadUrl = bodyDownloadUrl;
        }

        return new UpdateInfo(version, downloadUrl);
    }

    private static String sendTextRequest(HttpClient client, String url, int timeoutSeconds) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("User-Agent", "OneWorldCore-Updater")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private static void downloadJar(HttpClient client, String url, Path outputFile, int timeoutSeconds) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("User-Agent", "OneWorldCore-Updater")
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }

        Path tempFile = outputFile.resolveSibling(outputFile.getFileName() + ".tmp");
        try (InputStream stream = response.body()) {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.size(tempFile) <= 0) {
            Files.deleteIfExists(tempFile);
            throw new IOException("Downloaded update is empty");
        }
        Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String firstMatch(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String normalizeVersion(String version) {
        if (version == null) {
            return "0";
        }
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static int compareVersions(String left, String right) {
        int[] leftParts = versionParts(left);
        int[] rightParts = versionParts(right);
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftPart = index < leftParts.length ? leftParts[index] : 0;
            int rightPart = index < rightParts.length ? rightParts[index] : 0;
            int comparison = Integer.compare(leftPart, rightPart);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static int[] versionParts(String version) {
        return Pattern.compile("[^0-9]+")
                .splitAsStream(version)
                .filter(part -> !part.isBlank())
                .map(part -> {
                    try {
                        return Integer.parseInt(part);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private record UpdateInfo(String version, String downloadUrl) {
    }
}
