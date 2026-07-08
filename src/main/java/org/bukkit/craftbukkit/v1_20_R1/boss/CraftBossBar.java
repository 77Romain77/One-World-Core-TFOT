package org.bukkit.craftbukkit.v1_20_R1.boss;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CraftBossBar implements BossBar {

    private final ServerBossEvent handle;
    private Map<BarFlag, FlagContainer> flags;

    public CraftBossBar(String title, BarColor color, BarStyle style, BarFlag... flags) {
        handle = new ServerBossEvent(
                fromBossBarTitle(title),
                convertColor(color),
                convertStyle(style)
        );

        this.initialize();

        for (BarFlag flag : flags) {
            this.addFlag(flag);
        }

        this.setColor(color);
        this.setStyle(style);
    }

    public CraftBossBar(ServerBossEvent bossBattleServer) {
        this.handle = bossBattleServer;
        this.initialize();
    }

    private void initialize() {
        this.flags = new HashMap<>();
        this.flags.put(BarFlag.DARKEN_SKY, new FlagContainer(handle::shouldDarkenScreen, handle::setDarkenScreen));
        this.flags.put(BarFlag.PLAY_BOSS_MUSIC, new FlagContainer(handle::shouldPlayBossMusic, handle::setPlayBossMusic));
        this.flags.put(BarFlag.CREATE_FOG, new FlagContainer(handle::shouldCreateWorldFog, handle::setCreateWorldFog));
    }

    private BarColor convertColor(BossEvent.BossBarColor color) {
        BarColor bukkitColor = BarColor.valueOf(color.name());
        return (bukkitColor == null) ? BarColor.WHITE : bukkitColor;
    }

    private BossEvent.BossBarColor convertColor(BarColor color) {
        BossEvent.BossBarColor nmsColor = BossEvent.BossBarColor.valueOf(color.name());
        return (nmsColor == null) ? BossEvent.BossBarColor.WHITE : nmsColor;
    }

    private BossEvent.BossBarOverlay convertStyle(BarStyle style) {
        switch (style) {
            default:
            case SOLID:
                return BossEvent.BossBarOverlay.PROGRESS;
            case SEGMENTED_6:
                return BossEvent.BossBarOverlay.NOTCHED_6;
            case SEGMENTED_10:
                return BossEvent.BossBarOverlay.NOTCHED_10;
            case SEGMENTED_12:
                return BossEvent.BossBarOverlay.NOTCHED_12;
            case SEGMENTED_20:
                return BossEvent.BossBarOverlay.NOTCHED_20;
        }
    }

    private BarStyle convertStyle(BossEvent.BossBarOverlay style) {
        switch (style) {
            default:
            case PROGRESS:
                return BarStyle.SOLID;
            case NOTCHED_6:
                return BarStyle.SEGMENTED_6;
            case NOTCHED_10:
                return BarStyle.SEGMENTED_10;
            case NOTCHED_12:
                return BarStyle.SEGMENTED_12;
            case NOTCHED_20:
                return BarStyle.SEGMENTED_20;
        }
    }

    private static Component fromBossBarTitle(String title) {
        if (title == null || title.indexOf(ChatColor.COLOR_CHAR) < 0) {
            return CraftChatMessage.fromString(title, true)[0];
        }

        MutableComponent root = Component.empty();
        StringBuilder text = new StringBuilder();
        StyleState state = new StyleState();
        int length = title.length();

        for (int i = 0; i < length; i++) {
            char current = title.charAt(i);
            if (current != ChatColor.COLOR_CHAR || i + 1 >= length) {
                text.append(current);
                continue;
            }

            char code = Character.toLowerCase(title.charAt(++i));
            if (code == 'x') {
                String hex = readLegacyHex(title, i);
                if (hex != null) {
                    appendBossBarText(root, text, state.style());
                    state.color = TextColor.fromRgb(Integer.parseInt(hex.substring(1), 16));
                    i += 12;
                    continue;
                }
            }

            appendBossBarText(root, text, state.style());
            switch (code) {
                case '0': state.color = TextColor.fromRgb(0x000000); break;
                case '1': state.color = TextColor.fromRgb(0x0000AA); break;
                case '2': state.color = TextColor.fromRgb(0x00AA00); break;
                case '3': state.color = TextColor.fromRgb(0x00AAAA); break;
                case '4': state.color = TextColor.fromRgb(0xAA0000); break;
                case '5': state.color = TextColor.fromRgb(0xAA00AA); break;
                case '6': state.color = TextColor.fromRgb(0xFFAA00); break;
                case '7': state.color = TextColor.fromRgb(0xAAAAAA); break;
                case '8': state.color = TextColor.fromRgb(0x555555); break;
                case '9': state.color = TextColor.fromRgb(0x5555FF); break;
                case 'a': state.color = TextColor.fromRgb(0x55FF55); break;
                case 'b': state.color = TextColor.fromRgb(0x55FFFF); break;
                case 'c': state.color = TextColor.fromRgb(0xFF5555); break;
                case 'd': state.color = TextColor.fromRgb(0xFF55FF); break;
                case 'e': state.color = TextColor.fromRgb(0xFFFF55); break;
                case 'f': state.color = TextColor.fromRgb(0xFFFFFF); break;
                case 'k': state.obfuscated = true; break;
                case 'l': state.bold = true; break;
                case 'm': state.strikethrough = true; break;
                case 'n': state.underlined = true; break;
                case 'o': state.italic = true; break;
                case 'r': state.reset(); break;
                default:
                    text.append(ChatColor.COLOR_CHAR).append(code);
                    break;
            }
        }

        appendBossBarText(root, text, state.style());
        return root;
    }

    private static String readLegacyHex(String title, int xIndex) {
        if (xIndex + 12 >= title.length()) {
            return null;
        }

        StringBuilder hex = new StringBuilder("#");
        for (int digitNumber = 1; digitNumber <= 6; digitNumber++) {
            int sectionIndex = xIndex + (digitNumber * 2) - 1;
            int digitIndex = sectionIndex + 1;
            if (title.charAt(sectionIndex) != ChatColor.COLOR_CHAR) {
                return null;
            }
            char digit = title.charAt(digitIndex);
            if (Character.digit(digit, 16) == -1) {
                return null;
            }
            hex.append(digit);
        }
        return hex.toString();
    }

    private static void appendBossBarText(MutableComponent root, StringBuilder text, Style style) {
        if (text.length() == 0) {
            return;
        }
        root.append(Component.literal(text.toString()).setStyle(style));
        text.setLength(0);
    }

    private static final class StyleState {
        private TextColor color;
        private boolean bold;
        private boolean italic;
        private boolean underlined;
        private boolean strikethrough;
        private boolean obfuscated;

        private Style style() {
            Style style = Style.EMPTY;
            if (this.color != null) {
                style = style.withColor(this.color);
            }
            if (this.bold) {
                style = style.withBold(true);
            }
            if (this.italic) {
                style = style.withItalic(true);
            }
            if (this.underlined) {
                style = style.withUnderlined(true);
            }
            if (this.strikethrough) {
                style = style.withStrikethrough(true);
            }
            if (this.obfuscated) {
                style = style.withObfuscated(true);
            }
            return style;
        }

        private void reset() {
            this.color = null;
            this.bold = false;
            this.italic = false;
            this.underlined = false;
            this.strikethrough = false;
            this.obfuscated = false;
        }
    }

    @Override
    public String getTitle() {
        return CraftChatMessage.fromComponent(handle.name);
    }

    @Override
    public void setTitle(String title) {
        handle.name = fromBossBarTitle(title);
        handle.broadcast(ClientboundBossEventPacket::createUpdateNamePacket);
    }

    @Override
    public BarColor getColor() {
        return convertColor(handle.color);
    }

    @Override
    public void setColor(BarColor color) {
        handle.color = convertColor(color);
        handle.broadcast(ClientboundBossEventPacket::createUpdateStylePacket);
    }

    @Override
    public BarStyle getStyle() {
        return convertStyle(handle.overlay);
    }

    @Override
    public void setStyle(BarStyle style) {
        handle.overlay = convertStyle(style);
        handle.broadcast(ClientboundBossEventPacket::createUpdateStylePacket);
    }

    @Override
    public void addFlag(BarFlag flag) {
        FlagContainer flagContainer = flags.get(flag);
        if (flagContainer != null) {
            flagContainer.set.accept(true);
        }
    }

    @Override
    public void removeFlag(BarFlag flag) {
        FlagContainer flagContainer = flags.get(flag);
        if (flagContainer != null) {
            flagContainer.set.accept(false);
        }
    }

    @Override
    public boolean hasFlag(BarFlag flag) {
        FlagContainer flagContainer = flags.get(flag);
        if (flagContainer != null) {
            return flagContainer.get.get();
        }
        return false;
    }

    @Override
    public void setProgress(double progress) {
        Preconditions.checkArgument(progress >= 0.0 && progress <= 1.0, "Progress must be between 0.0 and 1.0 (%s)", progress);
        handle.setProgress((float) progress);
    }

    @Override
    public double getProgress() {
        return handle.getProgress();
    }

    @Override
    public void addPlayer(Player player) {
        Preconditions.checkArgument(player != null, "player == null");
        Preconditions.checkArgument(((CraftPlayer) player).getHandle().connection != null, "player is not fully connected (wait for PlayerJoinEvent)");

        handle.addPlayer(((CraftPlayer) player).getHandle());
    }

    @Override
    public void removePlayer(Player player) {
        Preconditions.checkArgument(player != null, "player == null");

        handle.removePlayer(((CraftPlayer) player).getHandle());
    }

    @Override
    public List<Player> getPlayers() {
        ImmutableList.Builder<Player> players = ImmutableList.builder();
        for (ServerPlayer p : handle.getPlayers()) {
            players.add(p.getBukkitEntity());
        }
        return players.build();
    }

    @Override
    public void setVisible(boolean visible) {
        handle.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return handle.visible;
    }

    @Override
    public void show() {
        handle.setVisible(true);
    }

    @Override
    public void hide() {
        handle.setVisible(false);
    }

    @Override
    public void removeAll() {
        for (Player player : getPlayers()) {
            removePlayer(player);
        }
    }

    private final class FlagContainer {

        private final Supplier<Boolean> get;
        private final Consumer<Boolean> set;

        private FlagContainer(Supplier<Boolean> get, Consumer<Boolean> set) {
            this.get = get;
            this.set = set;
        }
    }
}
