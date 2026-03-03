package com.oneworldstudiomc.feature;

import com.mohistmc.tools.FileUtils;
import com.oneworldstudiomc.util.I18n;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Why is there such a class?
 * Because we have included some MOD optimizations and modifications,
 * as well as some mods that are only used on the client, these cannot be loaded in Mohist
 */
public class AutoDeleteMods {
    private static final String CONNECTOR_EXTRAS_PREFIX = "ConnectorExtras-";
    private static final String REACH_INNER_JAR = "META-INF/jarjar/reach-entity-attributes-2.4.0.jar";
    private static final String REACH_MIXIN_CONFIG = "mixins.reach-entity-attributes.json";
    private static final String BROKEN_REACH_MIXIN = "PlayerEntityInteractionHandlerMixin";

    public static final List<String> classlist = new ArrayList<>(Arrays.asList(
            "org.spongepowered.mod.SpongeMod" /*SpongeForge*/,
            "me.wesley1808.servercore.common.ServerCore" /*ServerCore*/,
            "i18nupdatemod.I18nUpdateMod" /*I18nUpdateMod*/,
            "net.irisshaders.iris.Iris" /*oculus*/,
            "com.nakuring.enhanced_boss_bars.EnhancedBossBars" /*enhanced_boss_bars*/,
            "me.flashyreese.mods.sodiumextra.EmbeddiumExtraMod" /*embeddium_extra*/,
            "com.nekotune.battlemusic.BattleMusic" /*BattleMusic*/,
            "com.zergatul.freecam.ModMain" /*freecam*/,
            "io.github.reserveword.imblocker.IMBlocker" /*IMBlocker*/,
            "me.towdium.jecharacters.JustEnoughCharacters" /*JustEnoughCharacters*/,
            "com.lootbeams.LootBeams" /*LootBeams*/,
            "net.darkhax.maxhealthfix.MaxHealthFixForge" /*Max-Health-Fix*/,
            "optifine.Differ" /*OptiFine*/));

    public static void jar() throws Exception {
        System.out.println(I18n.as("update.mods"));
        for (String t : classlist) {
            check(t);
        }
        patchConnectorExtrasReachMixin();
    }

    public static void check(String content) throws Exception {
        String cl = content.split("\\|")[0].replaceAll("\\.", "/") + ".class";
        File mods = new File("mods");
        if (!mods.exists()) mods.mkdir();
        File[] listFiles = mods.listFiles((dir, name) -> name.endsWith(".jar"));
        if (listFiles != null) {
            for (File f : listFiles) {
                if (FileUtils.fileExists(f, cl)) {
                    System.out.println(I18n.as("update.deleting", f.getName()));
                    System.gc();
                    Thread.sleep(100);
                    File newf = new File("delete/mods");
                    File qnewf = new File("delete", f.getPath());
                    if (!newf.exists()) {
                        newf.mkdirs();
                    } else {
                        if (qnewf.exists()) qnewf.delete();
                    }
                    Files.copy(f.toPath(), qnewf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    f.delete();
                }
            }
        }
    }

    private static void patchConnectorExtrasReachMixin() throws Exception {
        File mods = new File("mods");
        File[] listFiles = mods.listFiles((dir, name) -> name.endsWith(".jar") && name.startsWith(CONNECTOR_EXTRAS_PREFIX));
        if (listFiles == null) {
            return;
        }
        for (File connectorExtrasJar : listFiles) {
            if (!FileUtils.fileExists(connectorExtrasJar, REACH_INNER_JAR)) {
                continue;
            }
            try {
                if (patchConnectorExtrasJar(connectorExtrasJar)) {
                    System.out.println("[OneWorldCore] Patched ConnectorExtras reach-entity-attributes mixin for command suggestion stability: " + connectorExtrasJar.getName());
                }
            } catch (Throwable t) {
                System.out.println("[OneWorldCore] Failed to patch ConnectorExtras jar: " + connectorExtrasJar.getName());
                t.printStackTrace(System.out);
            }
        }
    }

    private static boolean patchConnectorExtrasJar(File connectorExtrasJar) throws Exception {
        byte[] innerJarBytes = readZipEntry(connectorExtrasJar, REACH_INNER_JAR);
        if (innerJarBytes == null) {
            return false;
        }

        byte[] patchedInnerJarBytes = patchReachInnerJar(innerJarBytes);
        if (patchedInnerJarBytes == null) {
            return false;
        }

        Path tempPath = Files.createTempFile(connectorExtrasJar.toPath().getParent(), "connectorextras-patch-", ".jar");
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(connectorExtrasJar.toPath()));
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(tempPath))) {
            byte[] buffer = new byte[8192];
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                ZipEntry outEntry = new ZipEntry(entry.getName());
                outEntry.setTime(entry.getTime());
                zout.putNextEntry(outEntry);

                if (REACH_INNER_JAR.equals(entry.getName())) {
                    zout.write(patchedInnerJarBytes);
                } else {
                    int len;
                    while ((len = zin.read(buffer)) > 0) {
                        zout.write(buffer, 0, len);
                    }
                }

                zout.closeEntry();
                zin.closeEntry();
            }
        } catch (Throwable t) {
            Files.deleteIfExists(tempPath);
            throw t;
        }

        Files.move(tempPath, connectorExtrasJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static byte[] readZipEntry(File zipFile, String entryName) throws IOException {
        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry entry = zf.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream in = zf.getInputStream(entry)) {
                return in.readAllBytes();
            }
        }
    }

    private static byte[] patchReachInnerJar(byte[] innerJarBytes) throws IOException {
        boolean changed = false;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(innerJarBytes));
             ZipOutputStream zout = new ZipOutputStream(out)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (REACH_MIXIN_CONFIG.equals(entry.getName())) {
                    String mixinConfig = new String(data, StandardCharsets.UTF_8);
                    String patchedConfig = mixinConfig
                            .replace("    \"" + BROKEN_REACH_MIXIN + "\",\r\n", "")
                            .replace("    \"" + BROKEN_REACH_MIXIN + "\"\r\n", "")
                            .replace("    \"" + BROKEN_REACH_MIXIN + "\",\n", "")
                            .replace("    \"" + BROKEN_REACH_MIXIN + "\"\n", "");
                    if (!mixinConfig.equals(patchedConfig)) {
                        data = patchedConfig.getBytes(StandardCharsets.UTF_8);
                        changed = true;
                    }
                }

                ZipEntry outEntry = new ZipEntry(entry.getName());
                outEntry.setTime(entry.getTime());
                zout.putNextEntry(outEntry);
                zout.write(data);
                zout.closeEntry();
                zin.closeEntry();
            }
        }

        return changed ? out.toByteArray() : null;
    }
}
