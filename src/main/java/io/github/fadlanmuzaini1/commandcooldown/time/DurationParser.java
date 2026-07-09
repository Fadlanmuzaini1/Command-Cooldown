package io.github.fadlanmuzaini1.commandcooldown.time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mengubah representasi durasi dalam bentuk string ringkas (mis. {@code "1h30m"})
 * menjadi nilai milliseconds.
 *
 * <p>Format yang didukung adalah satu atau lebih pasangan {@code <angka><unit>}
 * yang ditulis berurutan tanpa pemisah, dengan unit berikut:</p>
 * <ul>
 *   <li>{@code d} - hari (days)</li>
 *   <li>{@code h} - jam (hours)</li>
 *   <li>{@code m} - menit (minutes)</li>
 *   <li>{@code s} - detik (seconds)</li>
 * </ul>
 *
 * <p>Contoh input valid: {@code "30s"}, {@code "10m"}, {@code "2h"}, {@code "1d"},
 * {@code "1h30m"}, {@code "2m15s"}, {@code "1d12h"}.</p>
 *
 * <p>Class ini stateless dan thread-safe. Semua method bersifat statis karena
 * parsing durasi adalah operasi murni (pure function) tanpa kebutuhan state
 * internal apa pun.</p>
 */
public final class DurationParser {

    /**
     * Pola regex untuk memvalidasi sekaligus mengekstrak seluruh pasangan
     * angka-unit dalam satu string durasi. Di-compile sekali karena
     * {@link Pattern} bersifat immutable dan thread-safe.
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d+)([dhms])");

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long MILLIS_PER_MINUTE = 60L * MILLIS_PER_SECOND;
    private static final long MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE;
    private static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;

    private DurationParser() {
        // Utility class, tidak boleh diinstansiasi.
    }

    /**
     * Mem-parse string durasi menjadi nilai milliseconds.
     *
     * @param input string durasi, mis. {@code "1h30m"}
     * @return durasi dalam milliseconds
     * @throws DurationFormatException jika input null, kosong, atau formatnya tidak valid
     */
    public static long parseToMillis(String input) {
        if (input == null || input.isBlank()) {
            throw new DurationFormatException(String.valueOf(input), "Input tidak boleh kosong.");
        }

        String normalized = input.trim().toLowerCase();
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);

        long totalMillis = 0L;
        int matchedLength = 0;
        boolean foundAtLeastOneToken = false;

        while (matcher.find()) {
            foundAtLeastOneToken = true;
            matchedLength += matcher.group().length();

            long value = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            totalMillis += toMillis(value, unit, normalized);
        }

        if (!foundAtLeastOneToken || matchedLength != normalized.length()) {
            throw new DurationFormatException(input);
        }

        return totalMillis;
    }

    /**
     * Mengonversi satu pasangan nilai-unit menjadi milliseconds.
     *
     * @param value          nilai numerik
     * @param unit           karakter unit ({@code d}, {@code h}, {@code m}, atau {@code s})
     * @param originalInput  input asli, digunakan untuk pesan error yang informatif
     * @return nilai dalam milliseconds
     */
    private static long toMillis(long value, char unit, String originalInput) {
        return switch (unit) {
            case 'd' -> value * MILLIS_PER_DAY;
            case 'h' -> value * MILLIS_PER_HOUR;
            case 'm' -> value * MILLIS_PER_MINUTE;
            case 's' -> value * MILLIS_PER_SECOND;
            default -> throw new DurationFormatException(originalInput, "Unit '" + unit + "' tidak dikenali.");
        };
    }

    /**
     * Mengubah nilai milliseconds menjadi representasi string yang mudah dibaca
     * manusia, mis. {@code 5400000} menjadi {@code "1h30m"}.
     *
     * <p>Digunakan terutama untuk menampilkan sisa waktu cooldown kepada pemain
     * melalui placeholder {@code %time%} pada pesan cooldown.</p>
     *
     * @param millis durasi dalam milliseconds, harus bernilai non-negatif
     * @return representasi string ringkas, mis. {@code "1h30m"}; {@code "0s"} jika millis &lt;= 0
     */
    public static String formatFromMillis(long millis) {
        if (millis <= 0) {
            return "0s";
        }

        long remaining = millis;

        long days = remaining / MILLIS_PER_DAY;
        remaining %= MILLIS_PER_DAY;

        long hours = remaining / MILLIS_PER_HOUR;
        remaining %= MILLIS_PER_HOUR;

        long minutes = remaining / MILLIS_PER_MINUTE;
        remaining %= MILLIS_PER_MINUTE;

        long seconds = remaining / MILLIS_PER_SECOND;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append('d');
        }
        if (hours > 0) {
            builder.append(hours).append('h');
        }
        if (minutes > 0) {
            builder.append(minutes).append('m');
        }
        // Detik tetap ditampilkan jika ini satu-satunya komponen,
        // atau jika tidak ada komponen lain yang lebih besar.
        if (seconds > 0 || builder.isEmpty()) {
            builder.append(seconds).append('s');
        }

        return builder.toString();
    }
}
