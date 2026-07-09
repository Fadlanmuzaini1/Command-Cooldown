package io.github.fadlanmuzaini1.commandcooldown.time;

/**
 * Dilemparkan ketika sebuah string durasi tidak dapat diparse menjadi
 * representasi waktu yang valid oleh {@link DurationParser}.
 *
 * <p>Exception ini bersifat unchecked karena kesalahan format durasi
 * pada umumnya merupakan kesalahan konfigurasi (config error) yang
 * seharusnya menghentikan proses loading config secara eksplisit,
 * bukan ditangani secara diam-diam di tengah eksekusi command.</p>
 */
public class DurationFormatException extends IllegalArgumentException {

    /**
     * Membuat exception baru dengan pesan yang menjelaskan input
     * yang gagal diparse.
     *
     * @param rawInput string durasi mentah yang gagal diparse
     */
    public DurationFormatException(String rawInput) {
        super("Format durasi tidak valid: '" + rawInput + "'. " +
                "Contoh format yang didukung: 30s, 10m, 2h, 1d, 1h30m, 2m15s, 1d12h.");
    }

    /**
     * Membuat exception baru dengan pesan kustom.
     *
     * @param rawInput string durasi mentah yang gagal diparse
     * @param reason   alasan spesifik kegagalan parsing
     */
    public DurationFormatException(String rawInput, String reason) {
        super("Format durasi tidak valid: '" + rawInput + "'. " + reason);
    }
}
