package io.github.fadlanmuzaini1.commandcooldown;

import io.github.fadlanmuzaini1.commandcooldown.command.CommandWrapperService;
import io.github.fadlanmuzaini1.commandcooldown.command.ReloadCommand;
import io.github.fadlanmuzaini1.commandcooldown.config.ConfigManager;
import io.github.fadlanmuzaini1.commandcooldown.cooldown.PlayerCooldownTracker;
import io.github.fadlanmuzaini1.commandcooldown.permission.PermissionEvaluator;
import io.github.fadlanmuzaini1.commandcooldown.permission.VanillaOpPermissionEvaluator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint sekaligus composition root mod CommandCooldown.
 *
 * <p>Pendekatan ini tidak menggunakan Mixin sama sekali. Cooldown diterapkan
 * dengan membungkus executor Brigadier secara langsung via reflection
 * menggunakan {@link CommandWrapperService}, setelah semua mod selesai
 * mendaftarkan command-nya ({@code SERVER_STARTED}).</p>
 */
public final class CommandCooldownMod implements ModInitializer {

    public static final String MOD_ID = "commandcooldown";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Menginisialisasi CommandCooldown...");

        ConfigManager configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir());
        configManager.loadInitial();
        LOGGER.info("Konfigurasi dimuat: {} command terdaftar.",
                configManager.getModConfig().commands().size());

        PlayerCooldownTracker cooldownTracker = new PlayerCooldownTracker();
        PermissionEvaluator permissionEvaluator = new VanillaOpPermissionEvaluator();
        CommandWrapperService wrapperService = new CommandWrapperService(
                configManager, cooldownTracker, permissionEvaluator);

        // Daftarkan /commandcooldown reload
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ReloadCommand.register(dispatcher, configManager, wrapperService));

        // Terapkan cooldown wrapper setelah SEMUA mod selesai mendaftarkan command
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                wrapperService.applyWrappers(server.getCommandManager().getDispatcher()));

        // Bersihkan data cooldown pemain saat logout
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                cooldownTracker.clearPlayer(handler.getPlayer().getUuid()));

        LOGGER.info("CommandCooldown siap digunakan.");
    }
}
