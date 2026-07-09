package io.github.fadlanmuzaini1.commandcooldown.command;

import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Merepresentasikan hasil keputusan {@link CommandInterceptor} terhadap
 * sebuah percobaan eksekusi command.
 *
 * <p>Tiga kemungkinan keadaan:</p>
 * <ul>
 *   <li><b>Tidak dikelola</b> - command tidak ada di konfigurasi, atau
 *       pemain berhak bypass, atau sumber command bukan pemain (console/command block).
 *       Command dibiarkan berjalan normal, tanpa efek apa pun setelahnya.</li>
 *   <li><b>Diblokir</b> - command sedang dalam masa cooldown untuk pemain ini.
 *       Eksekusi harus dibatalkan dan {@link #blockMessage()} ditampilkan ke pemain.</li>
 *   <li><b>Dapat dilacak</b> - command dikelola, tidak dalam cooldown, dan boleh
 *       dijalankan; jika eksekusi berhasil, cooldown baru harus dimulai
 *       menggunakan {@link #playerId()}, {@link #commandName()}, dan {@link #cooldownMillis()}.</li>
 * </ul>
 */
public record InterceptionOutcome(
        boolean blocked,
        Text blockMessage,
        boolean managed,
        UUID playerId,
        String commandName,
        long cooldownMillis
) {

    /**
     * @return outcome untuk command yang tidak dikelola mod ini sama sekali
     *         (tidak ada di config, sumber bukan pemain, atau pemain bypass)
     */
    public static InterceptionOutcome notManaged() {
        return new InterceptionOutcome(false, null, false, null, null, 0L);
    }

    /**
     * @param message pesan cooldown yang akan ditampilkan ke pemain
     * @return outcome yang menandakan command harus diblokir
     */
    public static InterceptionOutcome blocked(Text message) {
        return new InterceptionOutcome(true, message, true, null, null, 0L);
    }

    /**
     * @param playerId       UUID pemain yang menjalankan command
     * @param commandName    nama command yang sudah dinormalisasi
     * @param cooldownMillis durasi cooldown yang harus dimulai jika eksekusi berhasil
     * @return outcome yang mengizinkan eksekusi, dengan info untuk memulai cooldown setelahnya
     */
    public static InterceptionOutcome trackable(UUID playerId, String commandName, long cooldownMillis) {
        return new InterceptionOutcome(false, null, true, playerId, commandName, cooldownMillis);
    }

    /**
     * @return {@code true} jika, setelah eksekusi berhasil, cooldown baru perlu dimulai
     */
    public boolean shouldStartCooldownOnSuccess() {
        return managed && !blocked;
    }
}
