package io.github.fadlanmuzaini1.commandcooldown.permission;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Abstraksi untuk mengevaluasi apakah sebuah {@link ServerCommandSource}
 * memiliki node permission tertentu.
 *
 * <p>Interface ini sengaja dipisahkan dari {@link BypassChecker} agar
 * sumber permission dapat diganti tanpa mengubah logika bypass itu
 * sendiri (Dependency Inversion Principle). Implementasi default mod ini
 * menggunakan level OP vanilla sebagai pengganti permission node ketika
 * tidak ada mod permission pihak ketiga (mis. LuckPerms / Fabric
 * Permissions API) yang terpasang di server.</p>
 */
@FunctionalInterface
public interface PermissionEvaluator {

    /**
     * Mengecek apakah {@code source} memiliki node permission yang diberikan.
     *
     * @param source         sumber command yang sedang dievaluasi
     * @param permissionNode node permission, mis. {@code "commandcooldown.bypass"}
     * @return {@code true} jika {@code source} memiliki permission tersebut
     */
    boolean hasPermission(ServerCommandSource source, String permissionNode);
}
