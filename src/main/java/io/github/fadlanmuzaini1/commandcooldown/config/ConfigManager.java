package io.github.fadlanmuzaini1.commandcooldown.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import io.github.fadlanmuzaini1.commandcooldown.cooldown.CooldownRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bertanggung jawab membaca, mem-validasi, dan me-reload file konfigurasi
 * mod ({@code config.json}) dari disk.
 *
 * <p>Class ini menjembatani representasi JSON mentah (yang toleran
 * terhadap kesalahan ketik admin server) dengan model domain yang sudah
 * tervalidasi dan immutable ({@link ModConfig} dan {@link CooldownRegistry}).
 * Parsing durasi dan normalisasi nama command dilakukan tepat satu kali
 * di sini, baik saat startup maupun saat {@link #reload()} dipanggil.</p>
 *
 * <p>Instance class ini dimaksudkan untuk dibuat sekali oleh composition
 * root ({@code CommandCooldownMod}) dan dipegang selama umur server.
 * State internal ({@link #currentModConfig} dan {@link #currentRegistry})
 * bersifat {@code volatile} agar pembacaan dari thread command (yang
 * sangat sering terjadi) selalu melihat hasil reload terbaru tanpa
 * memerlukan locking penuh pada jalur baca.</p>
 */
public final class ConfigManager {

    private static final String CONFIG_FILE_NAME = "commandcooldown.json";

    private final Path configFilePath;
    private final Gson gson;

    private volatile ModConfig currentModConfig;
    private volatile CooldownRegistry currentRegistry;

    /**
     * Membuat {@link ConfigManager} baru yang akan membaca/menulis config
     * pada {@code configDirectory}.
     *
     * @param configDirectory direktori config Fabric (biasanya {@code run/config})
     */
    public ConfigManager(Path configDirectory) {
        this.configFilePath = configDirectory.resolve(CONFIG_FILE_NAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Memuat konfigurasi untuk pertama kalinya saat mod diinisialisasi.
     *
     * <p>Jika file config belum ada, sebuah file default akan dibuat
     * terlebih dahulu sebelum dibaca, sehingga instalasi pertama kali
     * tetap berjalan tanpa intervensi manual dari admin server.</p>
     *
     * @throws ConfigException jika file tidak dapat dibuat/dibaca atau isinya tidak valid
     */
    public void loadInitial() {
        if (Files.notExists(configFilePath)) {
            writeDefaultConfig();
        }
        ConfigLoadResult result = doLoad();
        if (!result.success()) {
            throw new ConfigException(result.errorMessage(), result.cause());
        }
        applyResult(result);
    }

    /**
     * Memuat ulang konfigurasi dari disk tanpa menghentikan server.
     *
     * <p>Jika file pada disk tidak valid, konfigurasi yang sedang aktif
     * <b>tidak</b> diganti — mod akan tetap menggunakan konfigurasi lama
     * yang valid, dan kegagalan dilaporkan melalui {@link ConfigLoadResult}
     * agar admin dapat memperbaiki file sebelum mencoba reload lagi.</p>
     *
     * @return hasil reload, berisi status sukses/gagal beserta pesannya
     */
    public ConfigLoadResult reload() {
        ConfigLoadResult result = doLoad();
        if (result.success()) {
            applyResult(result);
        }
        return result;
    }

    /**
     * @return konfigurasi mod yang sedang aktif saat ini
     */
    public ModConfig getModConfig() {
        return currentModConfig;
    }

    /**
     * @return registry cooldown yang sedang aktif saat ini, sinkron dengan {@link #getModConfig()}
     */
    public CooldownRegistry getCooldownRegistry() {
        return currentRegistry;
    }

    private void applyResult(ConfigLoadResult result) {
        this.currentModConfig = result.modConfig();
        this.currentRegistry = new CooldownRegistry(result.modConfig());
    }

    private ConfigLoadResult doLoad() {
        String json;
        try {
            json = Files.readString(configFilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return ConfigLoadResult.failure("Tidak dapat membaca file config: " + configFilePath, e);
        }

        RawModConfig raw;
        try {
            raw = gson.fromJson(json, RawModConfig.class);
        } catch (JsonSyntaxException e) {
            return ConfigLoadResult.failure("Format JSON pada config.json tidak valid.", e);
        }

        if (raw == null) {
            return ConfigLoadResult.failure("File config.json kosong atau tidak valid.", null);
        }

        try {
            ModConfig modConfig = raw.toModConfig();
            return ConfigLoadResult.success(modConfig);
        } catch (IllegalArgumentException e) {
            return ConfigLoadResult.failure("Konfigurasi tidak valid: " + e.getMessage(), e);
        }
    }

    private void writeDefaultConfig() {
        try {
            Files.createDirectories(configFilePath.getParent());
            Files.writeString(configFilePath, gson.toJson(RawModConfig.createDefault()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigException("Tidak dapat membuat file config default: " + configFilePath, e);
        }
    }

    /**
     * Representasi JSON mentah dari keseluruhan file config, sebelum
     * divalidasi dan diubah menjadi {@link ModConfig}.
     *
     * <p>Dipisahkan dari {@link ModConfig} karena Gson memerlukan struktur
     * data yang "longgar" (string apa adanya) untuk deserialisasi, sedangkan
     * {@link ModConfig} dan {@link CommandConfig} sengaja dibuat immutable
     * dengan validasi ketat yang tidak dapat dilakukan oleh Gson secara
     * otomatis.</p>
     */
    private static final class RawModConfig {
        boolean opsBypass = true;
        String permissionBypass = "commandcooldown.bypass";
        String cooldownMessage = ModConfig.DEFAULT_COOLDOWN_MESSAGE;
        List<RawCommandEntry> commands = new ArrayList<>();

        static RawModConfig createDefault() {
            RawModConfig raw = new RawModConfig();
            raw.commands.add(new RawCommandEntry("repair", "10m"));
            raw.commands.add(new RawCommandEntry("teach", "1h"));
            raw.commands.add(new RawCommandEntry("enchant", "30m"));
            return raw;
        }

        ModConfig toModConfig() {
            List<CommandConfig> parsedCommands = new ArrayList<>(commands.size());
            for (RawCommandEntry entry : commands) {
                parsedCommands.add(CommandConfig.fromRaw(entry.name, entry.cooldown));
            }
            return new ModConfig(opsBypass, permissionBypass, cooldownMessage, parsedCommands);
        }
    }

    /**
     * Representasi JSON mentah untuk satu entri pada array {@code "commands"}.
     */
    private static final class RawCommandEntry {
        String name;
        String cooldown;

        RawCommandEntry(String name, String cooldown) {
            this.name = name;
            this.cooldown = cooldown;
        }
    }

    /**
     * Hasil dari sebuah operasi load/reload konfigurasi.
     *
     * @param success      apakah operasi berhasil
     * @param modConfig    konfigurasi hasil parsing, hanya terisi jika {@code success} bernilai {@code true}
     * @param errorMessage pesan error yang ramah manusia, hanya terisi jika {@code success} bernilai {@code false}
     * @param cause        exception penyebab kegagalan, boleh {@code null}
     */
    public record ConfigLoadResult(boolean success, ModConfig modConfig, String errorMessage, Throwable cause) {

        static ConfigLoadResult success(ModConfig modConfig) {
            return new ConfigLoadResult(true, Objects.requireNonNull(modConfig), null, null);
        }

        static ConfigLoadResult failure(String errorMessage, Throwable cause) {
            return new ConfigLoadResult(false, null, errorMessage, cause);
        }
    }

    /**
     * Dilemparkan ketika konfigurasi gagal dimuat pada saat startup mod,
     * di mana mod tidak dapat melanjutkan inisialisasi tanpa konfigurasi
     * yang valid.
     */
    public static final class ConfigException extends RuntimeException {
        public ConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
