package xyz.wtje.holograms.core.storage;
import xyz.wtje.holograms.core.model.Hologram;
import xyz.wtje.holograms.core.model.HologramType;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class HologramStorage {
    private final File hologramsFolder;
    private final VersionAdapter adapter;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    public HologramStorage(File dataFolder, VersionAdapter adapter) {
        this.hologramsFolder = new File(dataFolder, "holograms");
        this.adapter = adapter;
        if (!hologramsFolder.exists()) {
            boolean created = hologramsFolder.mkdirs();
            if (created) {
                System.out.println("Created holograms folder: " + hologramsFolder.getAbsolutePath());
            }
        }
    }
    public CompletableFuture<List<Hologram>> loadAllHologramsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Hologram> holograms = new ArrayList<>();
            File[] files = hologramsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) return holograms;
            for (File file : files) {
                try {
                    Hologram hologram = loadHologramFromFile(file);
                    if (hologram != null) {
                        holograms.add(hologram);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load hologram from " + file.getName() + ": " + e.getMessage());
                }
            }
            return holograms;
        }, executor);
    }
    public CompletableFuture<Void> saveHologramAsync(Hologram hologram) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(hologramsFolder, hologram.getName() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            saveHologramToConfig(hologram, config);
            try {
                String content = config.saveToString();
                content = fixYamlQuoting(content);
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content);
                }
            } catch (IOException e) {
                System.err.println("Failed to save hologram " + hologram.getName() + ": " + e.getMessage());
            }
        }, executor);
    }
    public CompletableFuture<Void> deleteHologramAsync(String name) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(hologramsFolder, name + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }, executor);
    }
    private Hologram loadHologramFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String name = file.getName().replace(".yml", "");
        String typeStr = config.getString("type");
        if (typeStr == null) return null;
        HologramType type = HologramType.valueOf(typeStr.toUpperCase());
        Location location = loadLocation(config.getConfigurationSection("location"));
        if (location == null) return null;
        Hologram hologram = createHologram(name, type, location, config);
        if (hologram != null) {
            hologram.setViewDistance(config.getDouble("view-distance", 32.0));
            ConfigurationSection scaleSection = config.getConfigurationSection("scale");
            if (scaleSection != null) {
                double x = scaleSection.getDouble("x", 1.0);
                double y = scaleSection.getDouble("y", 1.0);
                double z = scaleSection.getDouble("z", 1.0);
                hologram.setScale(new org.bukkit.util.Vector(x, y, z));
            }
            ConfigurationSection rotationSection = config.getConfigurationSection("rotation");
            if (rotationSection != null) {
                double x = rotationSection.getDouble("x", 0.0);
                double y = rotationSection.getDouble("y", 0.0);
                double z = rotationSection.getDouble("z", 0.0);
                hologram.setRotation(new org.bukkit.util.Vector(x, y, z));
            }
            String billboardStr = config.getString("billboard", "CENTER");
            try {
                Hologram.Billboard billboard = Hologram.Billboard.valueOf(billboardStr.toUpperCase());
                hologram.setBillboard(billboard);
            } catch (IllegalArgumentException e) {
                hologram.setBillboard(Hologram.Billboard.CENTER);
            }
        }
        return hologram;
    }
    private Hologram createHologram(String name, HologramType type, Location location, YamlConfiguration config) {
        try {
            switch (type) {
                case TEXT -> {
                    List<String> lines = config.getStringList("lines");
                    List<Component> components = lines.stream()
                            .map(this::parseText)
                            .toList();
                    Hologram hologram = adapter.createTextHologram(name, location, components);
                    if (hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo) {
                        textHolo.setShadow(config.getBoolean("shadow", false));
                        textHolo.setSeeThrough(config.getBoolean("see-through", false));
                        textHolo.setLineWidth(config.getInt("line-width", 200));
                        String alignmentStr = config.getString("alignment", "CENTER");
                        try {
                            xyz.wtje.holograms.core.model.TextHologram.TextAlignment alignment = 
                                xyz.wtje.holograms.core.model.TextHologram.TextAlignment.valueOf(alignmentStr.toUpperCase());
                            textHolo.setAlignment(alignment);
                        } catch (IllegalArgumentException e) {
                            textHolo.setAlignment(xyz.wtje.holograms.core.model.TextHologram.TextAlignment.CENTER);
                        }
                        if (config.contains("background-color")) {
                            int argb = config.getInt("background-color");
                            org.bukkit.Color color = org.bukkit.Color.fromARGB(argb);
                            textHolo.setBackgroundColor(color);
                        }
                    }
                    return hologram;
                }
                case ITEM -> {
                    String materialName = config.getString("material");
                    if (materialName == null) return null;
                    Material material = Material.valueOf(materialName);
                    ItemStack item = new ItemStack(material, config.getInt("amount", 1));
                    Hologram hologram = adapter.createItemHologram(name, location, item);
                    if (hologram instanceof xyz.wtje.holograms.core.model.ItemHologram itemHolo) {
                        String transformStr = config.getString("transform", "FIXED");
                        itemHolo.setTransform(xyz.wtje.holograms.core.model.ItemHologram.Transform.valueOf(transformStr));
                    }
                    return hologram;
                }
                case BLOCK -> {
                    String materialName = config.getString("material");
                    if (materialName == null) return null;
                    Material material = Material.valueOf(materialName);
                    return adapter.createBlockHologram(name, location, material.createBlockData());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to create hologram " + name + ": " + e.getMessage());
        }
        return null;
    }
    private void saveHologramToConfig(Hologram hologram, YamlConfiguration config) {
        config.set("type", hologram.getType().name());
        config.set("view-distance", hologram.getViewDistance());
        saveLocation(hologram.getLocation(), config.createSection("location"));
        org.bukkit.util.Vector scale = hologram.getScale();
        ConfigurationSection scaleSection = config.createSection("scale");
        scaleSection.set("x", scale.getX());
        scaleSection.set("y", scale.getY());
        scaleSection.set("z", scale.getZ());
        org.bukkit.util.Vector rotation = hologram.getRotation();
        ConfigurationSection rotationSection = config.createSection("rotation");
        rotationSection.set("x", rotation.getX());
        rotationSection.set("y", rotation.getY());
        rotationSection.set("z", rotation.getZ());
        config.set("billboard", hologram.getBillboard().name());
        switch (hologram.getType()) {
            case TEXT -> {
                if (hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo) {
                    List<String> lines = new ArrayList<>();
                    for (Component line : textHolo.getLines()) {
                        String originalText = legacySerializer.serialize(line);
                        originalText = originalText.replace('§', '&');
                        lines.add(originalText);
                    }
                    config.set("lines", lines);
                    config.set("shadow", textHolo.hasShadow());
                    config.set("see-through", textHolo.isSeeThrough());
                    config.set("line-width", textHolo.getLineWidth());
                    config.set("alignment", textHolo.getAlignment().name());
                    if (textHolo.getBackgroundColor() != null) {
                        config.set("background-color", textHolo.getBackgroundColor().asARGB());
                    }
                }
            }
            case ITEM -> {
                if (hologram instanceof xyz.wtje.holograms.core.model.ItemHologram itemHolo) {
                    config.set("material", itemHolo.getItem().getType().name());
                    config.set("amount", itemHolo.getItem().getAmount());
                    config.set("transform", itemHolo.getTransform().name());
                }
            }
            case BLOCK -> {
                if (hologram instanceof xyz.wtje.holograms.core.model.BlockHologram blockHolo) {
                    config.set("material", blockHolo.getBlockData().getMaterial().name());
                }
            }
        }
    }
    private Location loadLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            System.err.println("World '" + worldName + "' not found! Skipping hologram...");
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }
    private void saveLocation(Location location, ConfigurationSection section) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }
    private boolean needsQuoting(String text) {
        return text.contains("#") || text.contains("&") || text.contains(":") || 
               text.contains("[") || text.contains("]") || text.contains("{") || 
               text.contains("}") || text.contains("'") || text.contains("\"") ||
               text.startsWith("-") || text.startsWith("*") || text.startsWith("?") ||
               text.contains("\\") || text.trim().isEmpty();
    }
    private String fixYamlQuoting(String yamlContent) {
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (line.trim().startsWith("- ") && !line.trim().startsWith("- '")) {
                String prefix = line.substring(0, line.indexOf("- ") + 2);
                String content = line.substring(line.indexOf("- ") + 2).trim();
                if (!content.startsWith("'")) {
                    content = "'" + content.replace("'", "''") + "'";
                    result.append(prefix).append(content).append("\n");
                } else {
                    result.append(line).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }
    private Component parseText(String text) {
        try {
            if (text.contains("§")) {
                return legacySerializer.deserialize(text);
            } else if (text.contains("&#") || text.contains("&x") || text.contains("&")) {
                String converted = text.replace('&', '§');
                return legacySerializer.deserialize(converted);
            } else if (text.contains("<") && text.contains(">")) {
                return miniMessage.deserialize(text);
            } else {
                return Component.text(text);
            }
        } catch (Exception e) {
            return Component.text(text);
        }
    }
    public void shutdown() {
        executor.shutdown();
    }
}
