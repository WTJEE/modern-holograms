package xyz.wtje.holograms.core.manager;
import xyz.wtje.holograms.core.model.Hologram;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
import xyz.wtje.holograms.core.storage.HologramStorage;
import xyz.wtje.holograms.core.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Collection;
public class HologramManager {
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final VersionAdapter adapter;
    private final Plugin plugin;
    private HologramStorage storage;
    private ConfigManager configManager;
    public HologramManager(VersionAdapter adapter, Plugin plugin) {
        this.adapter = adapter;
        this.plugin = plugin;
    }
    public void setStorage(HologramStorage storage) {
        this.storage = storage;
    }
    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
    public void loadHolograms() {
        if (storage == null) return;
        storage.loadAllHologramsAsync().thenAccept(loadedHolograms -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Hologram hologram : loadedHolograms) {
                    holograms.put(hologram.getName(), hologram);
                    hologram.spawn();
                }
            });
        }).exceptionally(throwable -> {
            System.err.println("Failed to load holograms: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }
    public void addHologram(Hologram hologram) {
        holograms.put(hologram.getName(), hologram);
        hologram.spawn();
        if (storage != null) {
            storage.saveHologramAsync(hologram).exceptionally(throwable -> {
                System.err.println("Failed to save hologram: " + throwable.getMessage());
                return null;
            });
        }
    }
    public void removeHologram(String name) {
        Hologram hologram = holograms.remove(name);
        if (hologram != null) {
            hologram.despawn();
            if (storage != null) {
                storage.deleteHologramAsync(name).exceptionally(throwable -> {
                    System.err.println("Failed to delete hologram file: " + throwable.getMessage());
                    return null;
                });
            }
        }
    }
    public Hologram getHologram(String name) {
        return holograms.get(name);
    }
    public void updateHologram(Hologram hologram) {
        hologram.update();
        if (storage != null) {
            storage.saveHologramAsync(hologram).exceptionally(throwable -> {
                System.err.println("Failed to save updated hologram: " + throwable.getMessage());
                return null;
            });
        }
    }
    public Collection<Hologram> getAllHolograms() {
        return holograms.values();
    }
    public void clearHolograms() {
        holograms.clear();
    }
    public void updateVisibility(Player player) {
        for (Hologram hologram : holograms.values()) {
            boolean canSee = hologram.canSee(player);
            adapter.updateVisibility(hologram, player, canSee);
        }
    }
    public void cleanupExpiredCache() {
    }
    public void invalidatePlayerCache(Player player) {
    }
    private void configurePlaceholderCache(Hologram hologram) {
    }
    public void shutdown() {
        for (Hologram hologram : holograms.values()) {
            hologram.despawn();
        }
        holograms.clear();
        if (storage != null) {
            storage.shutdown();
        }
    }
}
