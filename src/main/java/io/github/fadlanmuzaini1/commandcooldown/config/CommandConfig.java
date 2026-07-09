package io.github.fadlanmuzaini1.commandcooldown.config;

import io.github.fadlanmuzaini1.commandcooldown.command.CommandNameNormalizer;
import io.github.fadlanmuzaini1.commandcooldown.time.DurationParser;

/**
 * Representasi immutable untuk satu entri command yang dikonfigurasi
 * untuk memiliki cooldown.
 *
 * <p>Setiap instance mewakili satu baris pada array {@code "commands"}
 * di file konfigurasi, mis.:</p>
 * <pre>{@code
 * {
 *   "name": "repair",
 *   "cooldown": "10m"
 * }
 * }</pre>
 *
 * <p>Durasi cooldown disimpan dalam dua bentuk: {@link #cooldownRaw()}
 * (string asli dari konfigurasi, untuk keperluan logging/debug) dan
 * {@link #cooldownMillis()} (hasil parsing dalam milliseconds, dipakai
 * untuk perhitungan cooldown secara langsung tanpa parsing ulang).</p>
 *
 * @param name           nama command yang sudah dinormalisasi (lowercase, tanpa garis miring)
 * @param cooldownRaw    representasi string asli dari durasi, mis. {@code "1h30m"}
 * @param cooldownMillis durasi cooldown dalam milliseconds, hasil parsing dari {@code cooldownRaw}
 */
public record CommandConfig(String name, String cooldownRaw, long cooldownMillis) {

    /**
     * Compact constructor untuk validasi invariant dasar.
     */
    public CommandConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nama command tidak boleh kosong.");
        }
        if (cooldownRaw == null || cooldownRaw.isBlank()) {
            throw new IllegalArgumentException("Cooldown untuk command '" + name + "' tidak boleh kosong.");
        }
        if (cooldownMillis <= 0) {
            throw new IllegalArgumentException(
                    "Cooldown untuk command '" + name + "' harus bernilai positif, didapat: " + cooldownMillis + "ms.");
        }
    }

    /**
     * Membuat {@link CommandConfig} dari data mentah hasil deserialisasi JSON.
     *
     * <p>Method ini bertanggung jawab melakukan normalisasi nama command
     * (lowercase, trim, hapus prefix {@code "/"} jika ada) serta mem-parse
     * string durasi menjadi milliseconds satu kali di sini, sehingga
     * pemanggil tidak perlu mengetahui detail proses ini.</p>
     *
     * @param rawName     nama command mentah dari JSON, mis. {@code "repair"} atau {@code "/repair"}
     * @param rawCooldown string durasi mentah dari JSON, mis. {@code "10m"}
     * @return instance {@link CommandConfig} yang sudah dinormalisasi dan di-parse
     * @throws io.github.fadlanmuzaini1.commandcooldown.time.DurationFormatException jika {@code rawCooldown} tidak valid
     * @throws IllegalArgumentException jika {@code rawName} kosong
     */
    public static CommandConfig fromRaw(String rawName, String rawCooldown) {
        String normalizedName = CommandNameNormalizer.normalize(rawName);
        long millis = DurationParser.parseToMillis(rawCooldown);
        return new CommandConfig(normalizedName, rawCooldown, millis);
    }
}
