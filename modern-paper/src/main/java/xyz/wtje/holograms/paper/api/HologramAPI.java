package xyz.wtje.holograms.paper.api;
import xyz.wtje.holograms.core.manager.HologramManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
public class HologramAPI {
    private static HologramManager hologramManager;
    public static void initialize(HologramManager manager) {
        hologramManager = manager;
    }
    public static void refreshPlaceholders(Player player) {
        if (hologramManager != null) {
            hologramManager.invalidatePlayerCache(player);
        }
    }
    public static void refreshAllPlaceholders() {
        if (hologramManager != null) {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                hologramManager.invalidatePlayerCache(player);
            }
        }
    }
    public static boolean isAvailable() {
        return hologramManager != null;
    }
}
