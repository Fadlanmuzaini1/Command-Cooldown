package io.github.fadlanmuzaini1.commandcooldown.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.fadlanmuzaini1.commandcooldown.config.ConfigManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Mendaftarkan command administratif {@code /commandcooldown reload} yang
 * memuat ulang {@code commandcooldown.json} dari disk dan menerapkan ulang
 * wrapper cooldown tanpa memerlukan restart server.
 */
public final class ReloadCommand {

    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private ReloadCommand() {}

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            ConfigManager configManager,
            CommandWrapperService wrapperService
    ) {
        dispatcher.register(
                literal("commandcooldown")
                        .requires(source -> source.hasPermissionLevel(REQUIRED_PERMISSION_LEVEL))
                        .then(literal("reload")
                                .executes(context -> executeReload(context.getSource(), configManager, wrapperService)))
        );
    }

    private static int executeReload(
            ServerCommandSource source,
            ConfigManager configManager,
            CommandWrapperService wrapperService
    ) {
        ConfigManager.ConfigLoadResult result = configManager.reload();

        if (result.success()) {
            wrapperService.reapplyWrappers();
            int count = result.modConfig().commands().size();
            source.sendFeedback(
                    () -> Text.literal("§a[CommandCooldown] Konfigurasi berhasil dimuat ulang (" + count + " command)."),
                    true
            );
            return 1;
        }

        source.sendError(Text.literal("§c[CommandCooldown] Gagal reload: " + result.errorMessage()));
        return 0;
    }
}
