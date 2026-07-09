package io.github.fadlanmuzaini1.commandcooldown.permission;

import io.github.fadlanmuzaini1.commandcooldown.config.ModConfig;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Mengevaluasi apakah seorang pemain berhak melewati (bypass) cooldown
 * command, berdasarkan konfigurasi {@code opsBypass} dan
 * {@code permissionBypass} pada {@link ModConfig}.
 *
 * <p>Class ini sengaja dipisahkan dari {@link io.github.fadlanmuzaini1.commandcooldown.command.CommandInterceptor}
 * agar logika "siapa yang boleh bypass" dapat diuji dan dipahami secara
 * independen dari logika intersepsi command itu sendiri (Single
 * Responsibility Principle).</p>
 */
public final class BypassChecker {

    private final ModConfig modConfig;
    private final PermissionEvaluator permissionEvaluator;

    /**
     * Membuat {@link BypassChecker} baru.
     *
     * @param modConfig            konfigurasi mod yang berlaku saat ini
     * @param permissionEvaluator  evaluator yang digunakan untuk mengecek
     *                             node permission bypass
     */
    public BypassChecker(ModConfig modConfig, PermissionEvaluator permissionEvaluator) {
        this.modConfig = modConfig;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * Mengecek apakah sumber command tertentu berhak melewati cooldown,
     * baik melalui jalur OP bypass maupun permission bypass.
     *
     * @param source sumber command yang sedang dievaluasi
     * @return {@code true} jika {@code source} berhak melewati cooldown
     */
    public boolean canBypass(ServerCommandSource source) {
        if (modConfig.opsBypass() && source.hasPermissionLevel(4)) {
            return true;
        }

        if (modConfig.hasPermissionBypass()) {
            return permissionEvaluator.hasPermission(source, modConfig.permissionBypass());
        }

        return false;
    }
}
