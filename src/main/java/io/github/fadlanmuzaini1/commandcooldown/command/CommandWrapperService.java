package io.github.fadlanmuzaini1.commandcooldown.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import io.github.fadlanmuzaini1.commandcooldown.config.CommandConfig;
import io.github.fadlanmuzaini1.commandcooldown.config.ConfigManager;
import io.github.fadlanmuzaini1.commandcooldown.cooldown.PlayerCooldownTracker;
import io.github.fadlanmuzaini1.commandcooldown.permission.BypassChecker;
import io.github.fadlanmuzaini1.commandcooldown.permission.PermissionEvaluator;
import io.github.fadlanmuzaini1.commandcooldown.util.MessageFormatter;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Menerapkan cooldown pada command dengan membungkus executor Brigadier
 * secara langsung menggunakan reflection, tanpa Mixin.
 *
 * <p>Pendekatan ini bekerja untuk <b>semua</b> command yang terdaftar
 * melalui Brigadier — baik vanilla maupun dari mod lain — karena kita
 * memodifikasi node command itu sendiri, bukan titik intersepsinya.</p>
 *
 * <p>Wrapping dilakukan sekali saat server selesai start, dan dapat
 * diulang saat config di-reload melalui {@link #reapplyWrappers()}.</p>
 */
public final class CommandWrapperService {

    private static final Logger LOGGER = LoggerFactory.getLogger("commandcooldown");

    private final ConfigManager configManager;
    private final PlayerCooldownTracker cooldownTracker;
    private final PermissionEvaluator permissionEvaluator;

    private CommandDispatcher<ServerCommandSource> lastDispatcher;

    public CommandWrapperService(
            ConfigManager configManager,
            PlayerCooldownTracker cooldownTracker,
            PermissionEvaluator permissionEvaluator
    ) {
        this.configManager = configManager;
        this.cooldownTracker = cooldownTracker;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * Menerapkan cooldown wrapper pada semua command yang dikonfigurasi.
     * Menyimpan referensi dispatcher untuk keperluan {@link #reapplyWrappers()}.
     *
     * @param dispatcher Brigadier dispatcher dari server
     */
    public void applyWrappers(CommandDispatcher<ServerCommandSource> dispatcher) {
        this.lastDispatcher = dispatcher;
        unwrapAll(dispatcher);

        int wrapped = 0;
        for (CommandConfig commandConfig : configManager.getModConfig().commands()) {
            CommandNode<ServerCommandSource> node = dispatcher.getRoot().getChild(commandConfig.name());
            if (node == null) {
                LOGGER.warn("[CommandCooldown] Command '{}' tidak ditemukan di dispatcher, dilewati.", commandConfig.name());
                continue;
            }
            wrapNodeRecursively(node, commandConfig, new HashSet<>());
            wrapped++;
            LOGGER.info("[CommandCooldown] Cooldown wrapper diterapkan pada '/{}' ({}).",
                    commandConfig.name(), commandConfig.cooldownRaw());
        }
        LOGGER.info("[CommandCooldown] Total {} command ter-wrap.", wrapped);
    }

    /**
     * Menerapkan ulang wrapper setelah config di-reload.
     * Harus dipanggil setelah {@link ConfigManager#reload()} berhasil.
     */
    public void reapplyWrappers() {
        if (lastDispatcher == null) {
            LOGGER.warn("[CommandCooldown] Dispatcher belum tersedia, reapply dilewati.");
            return;
        }
        applyWrappers(lastDispatcher);
    }

    /**
     * Melepas semua wrapper yang pernah dipasang pada dispatcher,
     * sehingga executor dikembalikan ke kondisi aslinya sebelum di-wrap ulang.
     */
    private void unwrapAll(CommandDispatcher<ServerCommandSource> dispatcher) {
        unwrapNodeRecursively(dispatcher.getRoot(), new HashSet<>());
    }

    private void unwrapNodeRecursively(CommandNode<ServerCommandSource> node, Set<CommandNode<ServerCommandSource>> visited) {
        if (!visited.add(node)) return;

        Command<ServerCommandSource> existing = node.getCommand();
        if (existing instanceof CooldownAwareCommand cac) {
            setCommandField(node, cac.original);
        }

        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
            unwrapNodeRecursively(child, visited);
        }
    }

    private void wrapNodeRecursively(
            CommandNode<ServerCommandSource> node,
            CommandConfig commandConfig,
            Set<CommandNode<ServerCommandSource>> visited
    ) {
        if (!visited.add(node)) return;

        Command<ServerCommandSource> existing = node.getCommand();
        if (existing != null && !(existing instanceof CooldownAwareCommand)) {
            setCommandField(node, new CooldownAwareCommand(existing, commandConfig));
        }

        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
            wrapNodeRecursively(child, commandConfig, visited);
        }
    }

    private void setCommandField(CommandNode<ServerCommandSource> node, Command<ServerCommandSource> command) {
        try {
            Field field = CommandNode.class.getDeclaredField("command");
            field.setAccessible(true);
            field.set(node, command);
        } catch (Exception e) {
            LOGGER.error("[CommandCooldown] Gagal memodifikasi command node via reflection: {}", e.getMessage());
        }
    }

    /**
     * Wrapper executor yang menyelipkan logika cooldown sebelum dan sesudah
     * eksekusi command asli. Menyimpan referensi ke executor asli untuk
     * keperluan unwrap saat config di-reload.
     */
    private final class CooldownAwareCommand implements Command<ServerCommandSource> {

        private final Command<ServerCommandSource> original;
        private final CommandConfig commandConfig;

        CooldownAwareCommand(Command<ServerCommandSource> original, CommandConfig commandConfig) {
            this.original = original;
            this.commandConfig = commandConfig;
        }

        @Override
        public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
            ServerCommandSource source = context.getSource();

            if (source.getEntity() instanceof ServerPlayerEntity player) {
                BypassChecker bypassChecker = new BypassChecker(configManager.getModConfig(), permissionEvaluator);

                if (!bypassChecker.canBypass(source)) {
                    long remaining = cooldownTracker.getRemainingMillis(player.getUuid(), commandConfig.name());
                    if (remaining > 0) {
                        source.sendError(MessageFormatter.formatAsText(
                                configManager.getModConfig().cooldownMessage(),
                                remaining,
                                commandConfig.name()
                        ));
                        return 0;
                    }
                }
            }

            int result = original.run(context);

            if (source.getEntity() instanceof ServerPlayerEntity player && result > 0) {
                cooldownTracker.startCooldown(player.getUuid(), commandConfig.name(), commandConfig.cooldownMillis());
            }

            return result;
        }
    }
}
