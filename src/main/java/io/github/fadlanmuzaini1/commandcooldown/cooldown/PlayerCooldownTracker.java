package io.github.fadlanmuzaini1.commandcooldown.cooldown;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menyimpan dan mengelola state cooldown command secara runtime, dikaitkan
 * dengan UUID pemain.
 *
 * <p>Class ini murni mengelola <b>kapan</b> sebuah command boleh kembali
 * dijalankan oleh pemain tertentu. Ia tidak mengetahui apa pun tentang
 * konfigurasi cooldown (durasi, bypass, dll) — tanggung jawab tersebut
 * berada pada {@link CooldownRegistry} dan pemanggil di lapisan command.</p>
 *
 * <p>Instance class ini dimaksudkan untuk dibuat sekali oleh composition
 * root ({@code CommandCooldownMod}) dan hidup selama server berjalan,
 * lalu di-inject ke komponen yang membutuhkannya. Class ini <b>bukan</b>
 * singleton statis, sehingga tetap mudah diuji secara terisolasi.</p>
 *
 * <p>Thread-safe: menggunakan {@link ConcurrentHashMap} pada kedua level
 * agar aman diakses dari thread mana pun yang mungkin memicu eksekusi
 * command (umumnya server main thread, namun beberapa mod dapat memicu
 * command secara terprogram dari thread lain).</p>
 */
public final class PlayerCooldownTracker {

    /**
     * Map dua tingkat: UUID pemain -> (nama command -> timestamp epoch ms
     * kapan cooldown berakhir).
     */
    private final Map<UUID, Map<String, Long>> expirationByPlayer = new ConcurrentHashMap<>();

    /**
     * Mengecek apakah sebuah command masih dalam masa cooldown untuk pemain
     * tertentu.
     *
     * @param playerId               UUID pemain
     * @param normalizedCommandName  nama command yang sudah dinormalisasi
     * @return {@code true} jika cooldown masih aktif (belum kedaluwarsa)
     */
    public boolean isOnCooldown(UUID playerId, String normalizedCommandName) {
        return getRemainingMillis(playerId, normalizedCommandName) > 0;
    }

    /**
     * Menghitung sisa waktu cooldown untuk sebuah command pada pemain tertentu.
     *
     * @param playerId               UUID pemain
     * @param normalizedCommandName  nama command yang sudah dinormalisasi
     * @return sisa waktu dalam milliseconds, atau {@code 0} jika tidak ada
     *         cooldown aktif (baik karena belum pernah dipakai maupun sudah kedaluwarsa)
     */
    public long getRemainingMillis(UUID playerId, String normalizedCommandName) {
        Map<String, Long> playerCooldowns = expirationByPlayer.get(playerId);
        if (playerCooldowns == null) {
            return 0L;
        }

        Long expiresAt = playerCooldowns.get(normalizedCommandName);
        if (expiresAt == null) {
            return 0L;
        }

        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(remaining, 0L);
    }

    /**
     * Memulai (atau memperbarui) cooldown untuk sebuah command pada pemain
     * tertentu, dihitung dari waktu saat ini.
     *
     * @param playerId               UUID pemain
     * @param normalizedCommandName  nama command yang sudah dinormalisasi
     * @param cooldownMillis         durasi cooldown dalam milliseconds
     */
    public void startCooldown(UUID playerId, String normalizedCommandName, long cooldownMillis) {
        long expiresAt = System.currentTimeMillis() + cooldownMillis;
        expirationByPlayer
                .computeIfAbsent(playerId, id -> new ConcurrentHashMap<>())
                .put(normalizedCommandName, expiresAt);
    }

    /**
     * Menghapus seluruh data cooldown milik seorang pemain.
     *
     * <p>Berguna dipanggil saat pemain disconnect dari server untuk
     * membebaskan memori, terutama pada server dengan jumlah pemain
     * dan command yang besar.</p>
     *
     * @param playerId UUID pemain yang datanya akan dihapus
     */
    public void clearPlayer(UUID playerId) {
        expirationByPlayer.remove(playerId);
    }

    /**
     * Menghapus seluruh data cooldown untuk semua pemain.
     *
     * <p>Dipanggil saat reload konfigurasi jika administrator menginginkan
     * cooldown direset bersamaan dengan reload (perilaku ini dikendalikan
     * oleh pemanggil, bukan oleh class ini).</p>
     */
    public void clearAll() {
        expirationByPlayer.clear();
    }
}
