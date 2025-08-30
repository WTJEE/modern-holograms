package xyz.wtje.holograms.paper.util;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
public class MessageManager {
    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private final Map<String, String> messageCache = new HashMap<>();
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messages.setDefaults(defConfig);
        }
        messageCache.clear();
    }
    public void reloadMessages() {
        loadMessages();
    }
    public String getMessage(String path) {
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }
        String message = messages.getString(path, "&cMessage not found: " + path);
        String prefix = messages.getString("prefix", "");
        if (!message.startsWith("&6===") && !path.equals("prefix")) {
            message = prefix + message;
        }
        String colored = ColorUtils.colorize(message);
        messageCache.put(path, colored);
        return colored;
    }
    public String getMessage(String path, Map<String, Object> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            message = message.replace(placeholder, value);
        }
        return message;
    }
    public String getHologramCreated(String hologramName) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("hologram", hologramName);
        return getMessage("create.success", placeholders);
    }
    public String getHologramNotFound(String hologramName) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("hologram", hologramName);
        return getMessage("edit.not-found", placeholders);
    }
    public String getHologramMoved(String hologramName, Location location) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("hologram", hologramName);
        placeholders.put("location", formatLocation(location));
        return getMessage("move.success", placeholders);
    }
    public String getPropertySet(String property, String value) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("property", property);
        placeholders.put("value", value);
        return getMessage("property." + property.toLowerCase(), placeholders);
    }
    public String getUsage(String command) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usage", command);
        return getMessage("invalid-usage", placeholders);
    }
    private String formatLocation(Location loc) {
        String format = messages.getString("formats.location", "{world} {x}, {y}, {z}");
        return format
            .replace("{world}", loc.getWorld().getName())
            .replace("{x}", String.valueOf(loc.getBlockX()))
            .replace("{y}", String.valueOf(loc.getBlockY()))
            .replace("{z}", String.valueOf(loc.getBlockZ()));
    }
    public String formatScale(double x, double y, double z) {
        String format = messages.getString("formats.scale", "{x}, {y}, {z}");
        return format
            .replace("{x}", String.valueOf(x))
            .replace("{y}", String.valueOf(y))
            .replace("{z}", String.valueOf(z));
    }
    public String formatRotation(double x, double y, double z) {
        String format = messages.getString("formats.rotation", "{x}°, {y}°, {z}°");
        return format
            .replace("{x}", String.valueOf(x))
            .replace("{y}", String.valueOf(y))
            .replace("{z}", String.valueOf(z));
    }
}
