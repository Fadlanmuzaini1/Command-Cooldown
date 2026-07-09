package io.github.fadlanmuzaini1.commandcooldown.permission;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Implementasi {@link PermissionEvaluator} default yang tidak bergantung
 * pada mod permission pihak ketiga apa pun.
 *
 * <p>Karena requirement mod ini adalah <b>sangat ringan</b> dan tidak boleh
 * mewajibkan dependency tambahan, implementasi ini menggunakan level
 * permission OP vanilla ({@code level 4}) sebagai syarat minimum untuk
 * dianggap memiliki sebuah node permission. Ini memberi perilaku yang
 * masuk akal secara default: pemain dengan akses administratif penuh
 * otomatis dianggap memiliki permission bypass apa pun.</p>
 *
 * <p>Jika server menggunakan mod permission khusus seperti
 * <i>Fabric Permissions API</i> atau <i>LuckPerms</i>, integrasi dapat
 * dilakukan di masa depan dengan menyediakan implementasi
 * {@link PermissionEvaluator} alternatif tanpa mengubah {@link BypassChecker}
 * sama sekali — inilah manfaat dari pemisahan lewat interface ini.</p>
 */
public final class VanillaOpPermissionEvaluator implements PermissionEvaluator {

    /**
     * Level permission OP minimum yang dianggap memiliki seluruh node
     * permission khusus mod ini.
     */
    private static final int OP_PERMISSION_LEVEL = 4;

    @Override
    public boolean hasPermission(ServerCommandSource source, String permissionNode) {
        return source.hasPermissionLevel(OP_PERMISSION_LEVEL);
    }
}
