package io.github.fadlanmuzaini1.commandcooldown.util;

import io.github.fadlanmuzaini1.commandcooldown.time.DurationParser;
import net.minecraft.text.Text;

/**
 * Memformat template pesan cooldown menjadi pesan akhir yang siap
 * ditampilkan ke pemain, dengan menggantikan placeholder yang didukung.
 *
 * <p>Placeholder yang didukung pada template {@code cooldownMessage}:</p>
 * <ul>
 *   <li>{@code %time%} - sisa waktu cooldown dalam format ringkas, mis. {@code "9m32s"}</li>
 *   <li>{@code %command%} - nama command yang sedang dalam cooldown, mis. {@code "repair"}</li>
 * </ul>
 *
 * <p>Class ini stateless dan tidak bergantung pada konfigurasi mod secara
 * langsung — ia hanya menerima template string dan nilai-nilai yang
 * dibutuhkan untuk substitusi, sehingga mudah diuji secara terisolasi.</p>
 */
public final class MessageFormatter {

    private static final String PLACEHOLDER_TIME = "%time%";
    private static final String PLACEHOLDER_COMMAND = "%command%";

    private MessageFormatter() {
        // Utility class, tidak boleh diinstansiasi.
    }

    /**
     * Membentuk pesan cooldown akhir dari template, dengan mengganti
     * placeholder {@code %time%} dan {@code %command%}.
     *
     * @param template               template pesan dari konfigurasi, mis.
     *                               {@code "§cPlease wait %time% before using /%command% again."}
     * @param remainingCooldownMillis sisa waktu cooldown dalam milliseconds
     * @param commandName            nama command yang sedang dalam cooldown
     * @return string pesan yang sudah disubstitusi, siap dibungkus menjadi {@link Text}
     */
    public static String format(String template, long remainingCooldownMillis, String commandName) {
        String formattedTime = DurationParser.formatFromMillis(remainingCooldownMillis);
        return template
                .replace(PLACEHOLDER_TIME, formattedTime)
                .replace(PLACEHOLDER_COMMAND, commandName);
    }

    /**
     * Membentuk pesan cooldown akhir dan langsung membungkusnya menjadi
     * {@link Text} Minecraft, siap dikirim ke pemain melalui
     * {@code ServerCommandSource}.
     *
     * <p>Kode warna legacy ({@code §}) pada template akan otomatis
     * dikenali oleh Minecraft karena dibungkus menggunakan
     * {@link Text#literal(String)}.</p>
     *
     * @param template               template pesan dari konfigurasi
     * @param remainingCooldownMillis sisa waktu cooldown dalam milliseconds
     * @param commandName            nama command yang sedang dalam cooldown
     * @return {@link Text} yang siap dikirim ke pemain
     */
    public static Text formatAsText(String template, long remainingCooldownMillis, String commandName) {
        return Text.literal(format(template, remainingCooldownMillis, commandName));
    }
}
