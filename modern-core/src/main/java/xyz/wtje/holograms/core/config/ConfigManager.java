package xyz.wtje.holograms.core.config;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
public class ConfigManager {
    private final File configFile;
    private YamlConfiguration config;
    public ConfigManager(File dataFolder) {
        this.configFile = new File(dataFolder, "config.yml");
    }
    public CompletableFuture<Void> loadAsync() {
        return CompletableFuture.runAsync(() -> {
            config = YamlConfiguration.loadConfiguration(configFile);
            if (!configFile.exists()) {
                setDefaults();
                saveConfig();
            }
        });
    }
    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(() -> {
            saveConfig();
        });
    }
    private void setDefaults() {
        config.set("view-distance.display-entities", 32.0);
        config.set("view-distance.armor-stands", 24.0);
        config.set("defaults.text.shadow", false);
        config.set("defaults.text.see-through", false);
        config.set("defaults.text.line-width", 200);
        config.set("defaults.text.alignment", 0);
        config.set("refresh-interval", 20);
        config.set("line-offset", 0.25);
        config.set("log-level", "INFO");
        config.set("placeholder-cache-ttl", 1000); 
        config.set("placeholder-cache-cleanup-interval", 100); 
    }
    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public YamlConfiguration getConfig() { return config; }
    public double getDisplayEntityViewDistance() {
        return config.getDouble("view-distance.display-entities", 32.0);
    }
    public double getArmorStandViewDistance() {
        return config.getDouble("view-distance.armor-stands", 24.0);
    }
    public int getRefreshInterval() {
        return config.getInt("refresh-interval", 20);
    }
    public double getLineOffset() {
        return config.getDouble("line-offset", 0.25);
    }
    public long getPlaceholderCacheTTL() {
        return config.getLong("placeholder-cache-ttl", 1000);
    }
    public long getPlaceholderCacheCleanupInterval() {
        return config.getLong("placeholder-cache-cleanup-interval", 100);
    }
}
