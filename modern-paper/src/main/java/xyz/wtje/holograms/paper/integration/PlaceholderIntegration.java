package xyz.wtje.holograms.paper.integration;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
public class PlaceholderIntegration {
    private static boolean enabled = false;
    public static void initialize() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            enabled = true;
        }
    }
    public static boolean isEnabled() {
        return enabled;
    }
    public static String parsePlaceholders(Player player, String text) {
        if (!enabled || player == null) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
