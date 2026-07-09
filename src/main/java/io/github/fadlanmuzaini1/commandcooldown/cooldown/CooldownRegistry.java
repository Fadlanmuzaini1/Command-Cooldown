package io.github.fadlanmuzaini1.commandcooldown.cooldown;

import io.github.fadlanmuzaini1.commandcooldown.config.CommandConfig;
import io.github.fadlanmuzaini1.commandcooldown.config.ModConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Menyediakan pencarian {@link CommandConfig} berdasarkan nama command
 * dengan kompleksitas waktu O(1).
 *
 * <p>Registry ini dibangun sekali dari {@link ModConfig} saat mod dimuat
 * atau saat config di-reload, sehingga proses parsing durasi dan
 * normalisasi nama command tidak perlu diulang setiap kali sebuah
 * command dieksekusi oleh pemain.</p>
 *
 * <p>Instance bersifat immutable setelah dibangun. Untuk me-reload
 * konfigurasi, buat instance {@link CooldownRegistry} yang baru dan
 * gantikan referensi lama — jangan memutasi instance yang sedang
 * digunakan, untuk menghindari race condition saat reload terjadi
 * bersamaan dengan command yang sedang dieksekusi oleh pemain lain.</p>
 */
public final class CooldownRegistry {

    private final Map<String, CommandConfig> commandsByName;

    /**
     * Membangun registry baru dari daftar {@link CommandConfig} pada
     * sebuah {@link ModConfig}.
     *
     * @param modConfig konfigurasi mod yang sudah dimuat dan divalidasi
     * @throws IllegalArgumentException jika ditemukan dua entri command dengan nama yang sama
     */
    public CooldownRegistry(ModConfig modConfig) {
        Map<String, CommandConfig> map = new HashMap<>();
        for (CommandConfig command : modConfig.commands()) {
            CommandConfig previous = map.put(command.name(), command);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Command duplikat ditemukan pada konfigurasi: '" + command.name() + "'. " +
                                "Setiap command hanya boleh didefinisikan sekali.");
            }
        }
        this.commandsByName = Map.copyOf(map);
    }

    /**
     * Mencari konfigurasi cooldown untuk command dengan nama tertentu.
     *
     * @param normalizedCommandName nama command yang sudah dinormalisasi
     *                               (lowercase, tanpa prefix {@code "/"})
     * @return {@link Optional} berisi {@link CommandConfig} jika command
     *         tersebut dikonfigurasi memiliki cooldown, atau kosong jika tidak
     */
    public Optional<CommandConfig> find(String normalizedCommandName) {
        return Optional.ofNullable(commandsByName.get(normalizedCommandName));
    }

    /**
     * Mengecek apakah sebuah command dikonfigurasi memiliki cooldown.
     *
     * @param normalizedCommandName nama command yang sudah dinormalisasi
     * @return {@code true} jika command tersebut ada di dalam registry
     */
    public boolean isManaged(String normalizedCommandName) {
        return commandsByName.containsKey(normalizedCommandName);
    }

    /**
     * @return jumlah command yang terdaftar dalam registry ini
     */
    public int size() {
        return commandsByName.size();
    }
}
