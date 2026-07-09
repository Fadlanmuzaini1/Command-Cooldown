package io.github.fadlanmuzaini1.commandcooldown.config;

import java.util.List;

/**
 * Representasi immutable untuk keseluruhan isi file konfigurasi mod
 * ({@code config.json}).
 *
 * <p>Struktur ini secara langsung dipetakan dari JSON menggunakan Gson,
 * sehingga nama field sengaja dibuat identik dengan key JSON agar tidak
 * memerlukan anotasi {@code @SerializedName} tambahan. Contoh isi JSON
 * yang dipetakan:</p>
 *
 * <pre>{@code
 * {
 *   "opsBypass": true,
 *   "permissionBypass": "commandcooldown.bypass",
 *   "cooldownMessage": "§cPlease wait %time% before using /%command% again.",
 *   "commands": [
 *     { "name": "repair", "cooldown": "10m" }
 *   ]
 * }
 * }</pre>
 *
 * <p>Class ini hanya bertanggung jawab merepresentasikan <b>apa isi</b>
 * konfigurasi. Pembentukan struktur lookup cepat (O(1)) untuk pencarian
 * cooldown per command adalah tanggung jawab terpisah, ditangani oleh
 * {@code CooldownRegistry}.</p>
 *
 * @param opsBypass        jika {@code true}, pemain dengan status operator (OP) melewati cooldown
 * @param permissionBypass node permission yang jika dimiliki pemain akan melewati cooldown;
 *                         boleh {@code null} atau kosong jika fitur ini tidak digunakan
 * @param cooldownMessage  template pesan yang ditampilkan saat command masih dalam cooldown,
 *                         mendukung placeholder {@code %time%} dan {@code %command%}
 * @param commands         daftar command yang dikonfigurasi memiliki cooldown
 */
public record ModConfig(
        boolean opsBypass,
        String permissionBypass,
        String cooldownMessage,
        List<CommandConfig> commands
) {

    /**
     * Pesan default yang digunakan apabila {@code cooldownMessage} pada
     * file konfigurasi kosong atau tidak diisi.
     */
    public static final String DEFAULT_COOLDOWN_MESSAGE =
            "§cPlease wait %time% before using /%command% again.";

    /**
     * Compact constructor: melakukan defensive copy terhadap {@code commands}
     * agar list tidak dapat dimutasi dari luar setelah {@link ModConfig}
     * dibuat, serta menerapkan fallback untuk {@code cooldownMessage}
     * apabila kosong.
     */
    public ModConfig {
        commands = commands == null ? List.of() : List.copyOf(commands);
        if (cooldownMessage == null || cooldownMessage.isBlank()) {
            cooldownMessage = DEFAULT_COOLDOWN_MESSAGE;
        }
    }

    /**
     * Mengecek apakah fitur permission bypass dikonfigurasi (node permission
     * tidak kosong).
     *
     * @return {@code true} jika {@code permissionBypass} terisi dan tidak kosong
     */
    public boolean hasPermissionBypass() {
        return permissionBypass != null && !permissionBypass.isBlank();
    }
}
