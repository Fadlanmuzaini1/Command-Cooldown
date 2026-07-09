package io.github.fadlanmuzaini1.commandcooldown.command;

/**
 * Menyediakan normalisasi nama command secara konsisten di seluruh mod.
 *
 * <p>Normalisasi diperlukan karena nama command dapat berasal dari dua
 * sumber yang berbeda karakteristiknya:</p>
 * <ul>
 *   <li>File konfigurasi ({@code config.json}), ditulis manual oleh admin
 *       server sehingga rawan variasi seperti spasi tambahan, huruf kapital,
 *       atau prefix {@code "/"} yang tidak konsisten.</li>
 *   <li>Root command Brigadier, hasil ekstraksi dari
 *       {@code CommandNode#getName()} saat command dieksekusi, yang pada
 *       umumnya sudah bersih namun tetap dinormalisasi untuk konsistensi
 *       penuh dengan sumber pertama.</li>
 * </ul>
 *
 * <p>Dengan memusatkan logika ini pada satu class, kedua sisi pencocokan
 * (loading config dan intersepsi command) dijamin selalu menghasilkan
 * key yang sama untuk command yang sama, sehingga lookup pada
 * {@code CooldownRegistry} dapat diandalkan.</p>
 */
public final class CommandNameNormalizer {

    private static final String COMMAND_PREFIX = "/";

    private CommandNameNormalizer() {
        // Utility class, tidak boleh diinstansiasi.
    }

    /**
     * Menormalisasi nama command: menghapus whitespace di awal/akhir,
     * mengubah ke lowercase, dan menghapus prefix {@code "/"} jika ada.
     *
     * @param rawName nama command mentah, dari config maupun dari Brigadier
     * @return nama command yang sudah dinormalisasi
     * @throws IllegalArgumentException jika {@code rawName} null atau kosong
     */
    public static String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Nama command tidak boleh kosong.");
        }

        String trimmed = rawName.trim().toLowerCase();
        return trimmed.startsWith(COMMAND_PREFIX)
                ? trimmed.substring(COMMAND_PREFIX.length())
                : trimmed;
    }
}
