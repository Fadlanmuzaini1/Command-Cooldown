package io.github.fadlanmuzaini1.commandcooldown.command;

import io.github.fadlanmuzaini1.commandcooldown.config.CommandConfig;
import io.github.fadlanmuzaini1.commandcooldown.config.ConfigManager;
import io.github.fadlanmuzaini1.commandcooldown.cooldown.CooldownRegistry;
import io.github.fadlanmuzaini1.commandcooldown.cooldown.PlayerCooldownTracker;
import io.github.fadlanmuzaini1.commandcooldown.permission.BypassChecker;
import io.github.fadlanmuzaini1.commandcooldown.permission.PermissionEvaluator;
import io.github.fadlanmuzaini1.commandcooldown.util.MessageFormatter;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.UUID;

/**
 * Inti logika cooldown command: menentukan apakah sebuah percobaan eksekusi
 * command harus diizinkan atau diblokir, dan memulai cooldown setelah
 * eksekusi berhasil.
 *
 * <p>Class ini sepenuhnya tidak mengetahui <b>dari mana</b> sebuah command
 * berasal (vanilla, mod lain, dsb) maupun <b>bagaimana</b> ia dipanggil
 * (Mixin, event, atau cara lain) — ia hanya menerima
 * {@link ServerCommandSource} dan string command mentah, lalu mengembalikan
 * keputusan dalam bentuk {@link InterceptionOutcome}. Pemisahan ini membuat
 * class ini sepenuhnya dapat diuji secara independen dari mekanisme
 * intersepsi Minecraft yang sesungguhnya.</p>
 *
 * <p>Konfigurasi (registry cooldown, opsBypass, dll) selalu dibaca secara
 * langsung dari {@link ConfigManager} pada setiap pemanggilan, sehingga
 * hasil {@link ConfigManager#reload()} langsung berlaku tanpa perlu
 * membuat ulang instance {@link CommandInterceptor}.</p>
 */
public final class CommandInterceptor {

    private final ConfigManager configManager;
    private final PlayerCooldownTracker cooldownTracker;
    private final PermissionEvaluator permissionEvaluator;

    /**
     * Membuat {@link CommandInterceptor} baru.
     *
     * @param configManager       sumber konfigurasi dan registry cooldown terkini
     * @param cooldownTracker     state cooldown runtime per pemain
     * @param permissionEvaluator evaluator permission untuk fitur permission bypass
     */
    public CommandInterceptor(
            ConfigManager configManager,
            PlayerCooldownTracker cooldownTracker,
            PermissionEvaluator permissionEvaluator
    ) {
        this.configManager = configManager;
        this.cooldownTracker = cooldownTracker;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * Mengevaluasi apakah sebuah percobaan eksekusi command boleh dilanjutkan.
     *
     * <p>Dipanggil sebelum Brigadier benar-benar mengeksekusi command.</p>
     *
     * @param source       sumber command (umumnya pemain, bisa juga console/command block)
     * @param commandInput teks command mentah persis seperti yang diketik pemain,
     *                     tanpa prefix {@code "/"} (mis. {@code "repair iron_sword"})
     * @return {@link InterceptionOutcome} yang menyatakan keputusan untuk command ini
     */
    public InterceptionOutcome intercept(ServerCommandSource source, String commandInput) {
        String rootCommandName = extractRootCommandName(commandInput);
        if (rootCommandName == null) {
            return InterceptionOutcome.notManaged();
        }

        CooldownRegistry registry = configManager.getCooldownRegistry();
        Optional<CommandConfig> maybeConfig = registry.find(rootCommandName);
        if (maybeConfig.isEmpty()) {
            return InterceptionOutcome.notManaged();
        }

        // Cooldown hanya berlaku untuk pemain sungguhan; console dan command
        // block tidak memiliki UUID pemain sehingga tidak relevan untuk dilacak.
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return InterceptionOutcome.notManaged();
        }

        BypassChecker bypassChecker = new BypassChecker(configManager.getModConfig(), permissionEvaluator);
        if (bypassChecker.canBypass(source)) {
            return InterceptionOutcome.notManaged();
        }

        UUID playerId = player.getUuid();
        long remainingMillis = cooldownTracker.getRemainingMillis(playerId, rootCommandName);
        if (remainingMillis > 0) {
            Text message = MessageFormatter.formatAsText(
                    configManager.getModConfig().cooldownMessage(),
                    remainingMillis,
                    rootCommandName
            );
            return InterceptionOutcome.blocked(message);
        }

        CommandConfig commandConfig = maybeConfig.get();
        return InterceptionOutcome.trackable(playerId, rootCommandName, commandConfig.cooldownMillis());
    }

    /**
     * Memulai cooldown jika diperlukan, dipanggil setelah command berhasil
     * dieksekusi tanpa exception.
     *
     * <p>Jika {@code outcome} berasal dari command yang tidak dikelola,
     * method ini tidak melakukan apa pun.</p>
     *
     * @param outcome hasil dari pemanggilan {@link #intercept(ServerCommandSource, String)}
     *                sebelumnya untuk command yang sama
     */
    public void onSuccessfulExecution(InterceptionOutcome outcome) {
        if (outcome.shouldStartCooldownOnSuccess()) {
            cooldownTracker.startCooldown(outcome.playerId(), outcome.commandName(), outcome.cooldownMillis());
        }
    }

    /**
     * Mengekstrak dan menormalisasi nama root command dari teks command mentah.
     *
     * @param commandInput teks command mentah, mis. {@code "repair iron_sword"}
     * @return nama root command yang sudah dinormalisasi, atau {@code null}
     *         jika input kosong/tidak valid
     */
    private String extractRootCommandName(String commandInput) {
        if (commandInput == null || commandInput.isBlank()) {
            return null;
        }

        String trimmed = commandInput.trim();
        int spaceIndex = trimmed.indexOf(' ');
        String firstToken = spaceIndex == -1 ? trimmed : trimmed.substring(0, spaceIndex);

        try {
            return CommandNameNormalizer.normalize(firstToken);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
