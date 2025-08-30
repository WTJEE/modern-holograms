package xyz.wtje.holograms.paper.command;
import xyz.wtje.holograms.core.manager.HologramManager;
import xyz.wtje.holograms.core.config.ConfigManager;
import xyz.wtje.holograms.core.model.Hologram;
import xyz.wtje.holograms.core.model.HologramType;
import xyz.wtje.holograms.paper.util.ColorUtils;
import xyz.wtje.holograms.paper.util.MessageManager;
import xyz.wtje.holograms.paper.animation.AnimationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class HologramCommand implements CommandExecutor, TabCompleter {
    private final HologramManager manager;
    private final ConfigManager config;
    private final MessageManager messages;
    private final AnimationManager animationManager;
    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    public HologramCommand(HologramManager manager, ConfigManager config, MessageManager messages, AnimationManager animationManager, Plugin plugin) {
        this.manager = manager;
        this.config = config;
        this.messages = messages;
        this.animationManager = animationManager;
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "move" -> handleMove(sender, args);
            case "tp" -> handleTeleport(sender, args);
            case "movehere" -> handleMoveHere(sender, args);
            case "center" -> handleCenter(sender, args);
            case "copy" -> handleCopy(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender, args);
            case "refresh" -> handleRefresh(sender, args);
            case "decentmigration" -> handleDecentMigration(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }
    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cOnly players can create holograms"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo create <text|item|block> <name> [args...]"));
            return;
        }
        String type = args[1].toLowerCase();
        String name = args[2];
        Location location = player.getLocation();
        switch (type) {
            case "text" -> createTextHologram(sender, name, location, Arrays.copyOfRange(args, 3, args.length));
            case "item" -> createItemHologram(sender, name, location, Arrays.copyOfRange(args, 3, args.length));
            case "block" -> createBlockHologram(sender, name, location, Arrays.copyOfRange(args, 3, args.length));
            default -> sender.sendMessage(ColorUtils.colorize("&cUnknown hologram type: " + type));
        }
    }
    private void createTextHologram(CommandSender sender, String name, Location location, String[] textArgs) {
        if (textArgs.length == 0) {
            sender.sendMessage(ColorUtils.colorize("&cPlease specify text for the hologram"));
            return;
        }
        List<Component> lines = new ArrayList<>();
        for (String arg : textArgs) {
            if (arg.contains("\\n")) {
                String[] subLines = arg.split("\\\\n");
                for (String subLine : subLines) {
                    if (!subLine.trim().isEmpty()) {
                        lines.add(miniMessage.deserialize(subLine.trim()));
                    }
                }
            } else {
                lines.add(miniMessage.deserialize(arg));
            }
        }
        try {
            Hologram hologram = xyz.wtje.holograms.paper.adapter.AdapterFactory.createAdapter()
                    .createTextHologram(name, location, lines);
            manager.addHologram(hologram);
            sender.sendMessage(messages.getHologramCreated(name));
        } catch (Exception e) {
            sender.sendMessage(ColorUtils.colorize("&cFailed to create hologram: " + e.getMessage()));
        }
    }
    private void createItemHologram(CommandSender sender, String name, Location location, String[] itemArgs) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can create item holograms");
            return;
        }
        ItemStack item;
        if (itemArgs.length > 0) {
            try {
                Material material = Material.valueOf(itemArgs[0].toUpperCase());
                item = new ItemStack(material);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("Invalid material: " + itemArgs[0]);
                return;
            }
        } else {
            item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                sender.sendMessage("Please hold an item or specify a material");
                return;
            }
        }
        try {
            Hologram hologram = xyz.wtje.holograms.paper.adapter.AdapterFactory.createAdapter()
                    .createItemHologram(name, location, item);
            manager.addHologram(hologram);
            sender.sendMessage("Item hologram '" + name + "' created successfully");
        } catch (UnsupportedOperationException e) {
            sender.sendMessage("Item holograms are not supported in this server version");
        } catch (Exception e) {
            sender.sendMessage("Failed to create hologram: " + e.getMessage());
        }
    }
    private void createBlockHologram(CommandSender sender, String name, Location location, String[] blockArgs) {
        if (blockArgs.length == 0) {
            sender.sendMessage("Please specify a block type");
            return;
        }
        try {
            Material material = Material.valueOf(blockArgs[0].toUpperCase());
            if (!material.isBlock()) {
                sender.sendMessage(material.name() + " is not a block");
                return;
            }
            Hologram hologram = xyz.wtje.holograms.paper.adapter.AdapterFactory.createAdapter()
                    .createBlockHologram(name, location, material.createBlockData());
            manager.addHologram(hologram);
            sender.sendMessage("Block hologram '" + name + "' created successfully");
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid block type: " + blockArgs[0]);
        } catch (UnsupportedOperationException e) {
            sender.sendMessage("Block holograms are not supported in this server version");
        } catch (Exception e) {
            sender.sendMessage("Failed to create hologram: " + e.getMessage());
        }
    }
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /holo remove <name>");
            return;
        }
        String name = args[1];
        if (manager.getHologram(name) != null) {
            manager.removeHologram(name);
            sender.sendMessage("Hologram '" + name + "' removed successfully");
        } else {
            sender.sendMessage("Hologram '" + name + "' not found");
        }
    }
    private void handleEdit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /holo edit <name> <property> [value...]");
            sender.sendMessage("Properties: scale, rotation, pitch, yaw, roll, rotate, billboard, shadow, background, seethrough, linewidth, alignment, addline, removeline, setline");
            return;
        }
        String name = args[1];
        String property = args[2].toLowerCase();
        Hologram hologram = manager.getHologram(name);
        if (hologram == null) {
            sender.sendMessage("Hologram '" + name + "' not found");
            return;
        }
        switch (property) {
            case "scale" -> handleScale(sender, hologram, args);
            case "rotation" -> handleRotation(sender, hologram, args);
            case "pitch" -> handlePitch(sender, hologram, args);
            case "yaw" -> handleYaw(sender, hologram, args);
            case "roll" -> handleRoll(sender, hologram, args);
            case "rotate" -> handleRelativeRotation(sender, hologram, args);
            case "billboard" -> handleBillboard(sender, hologram, args);
            case "shadow" -> handleShadow(sender, hologram, args);
            case "background" -> handleBackground(sender, hologram, args);
            case "seethrough" -> handleSeeThrough(sender, hologram, args);
            case "linewidth" -> handleLineWidth(sender, hologram, args);
            case "alignment" -> handleAlignment(sender, hologram, args);
            case "addline" -> handleAddLine(sender, hologram, args);
            case "removeline" -> handleRemoveLine(sender, hologram, args);
            case "setline" -> handleSetLine(sender, hologram, args);
            default -> sender.sendMessage("Unknown property: " + property);
        }
    }
    private void handleScale(CommandSender sender, Hologram hologram, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo edit <name> scale <value> [y] [z]"));
            return;
        }
        try {
            double x = Double.parseDouble(args[3]);
            double y = args.length > 4 ? Double.parseDouble(args[4]) : x;
            double z = args.length > 5 ? Double.parseDouble(args[5]) : x;
            hologram.setScale(new org.bukkit.util.Vector(x, y, z));
            manager.updateHologram(hologram);
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        manager.updateVisibility(player);
                    }
                    System.out.println("HologramCommand: Post-scale visibility refresh completed");
                },
                2L 
            );
            sender.sendMessage(ColorUtils.colorize("&aScale set to " + x + ", " + y + ", " + z));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid scale values"));
        }
    }
    private void handleRotation(CommandSender sender, Hologram hologram, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo edit <name> rotation <y> [x] [z]"));
            return;
        }
        try {
            double y = Double.parseDouble(args[3]); 
            double x = args.length > 4 ? Double.parseDouble(args[4]) : 0;
            double z = args.length > 5 ? Double.parseDouble(args[5]) : 0;
            hologram.setRotation(new org.bukkit.util.Vector(x, y, z));
            manager.updateHologram(hologram);
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        manager.updateVisibility(player);
                    }
                    System.out.println("HologramCommand: Post-rotation visibility refresh completed");
                },
                2L 
            );
            sender.sendMessage(ColorUtils.colorize("&aRotation set to " + x + "°, " + y + "°, " + z + "°"));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid rotation values"));
        }
    }
    
    private void handlePitch(CommandSender sender, Hologram hologram, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo edit <name> pitch <degrees>"));
            return;
        }
        try {
            double pitch = Double.parseDouble(args[3]);
            org.bukkit.util.Vector currentRotation = hologram.getRotation();
            hologram.setRotation(new org.bukkit.util.Vector(pitch, currentRotation.getY(), currentRotation.getZ()));
            manager.updateHologram(hologram);
            refreshVisibility();
            sender.sendMessage(ColorUtils.colorize("&aPitch set to " + pitch + "°"));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid pitch value"));
        }
    }
    
    private void handleYaw(CommandSender sender, Hologram hologram, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo edit <name> yaw <degrees>"));
            return;
        }
        try {
            double yaw = Double.parseDouble(args[3]);
            org.bukkit.util.Vector currentRotation = hologram.getRotation();
            hologram.setRotation(new org.bukkit.util.Vector(currentRotation.getX(), yaw, currentRotation.getZ()));
            manager.updateHologram(hologram);
            refreshVisibility();
            sender.sendMessage(ColorUtils.colorize("&aYaw set to " + yaw + "°"));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid yaw value"));
        }
    }
    
    private void handleRoll(CommandSender sender, Hologram hologram, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo edit <name> roll <degrees>"));
            return;
        }
        try {
            double roll = Double.parseDouble(args[3]);
            org.bukkit.util.Vector currentRotation = hologram.getRotation();
            hologram.setRotation(new org.bukkit.util.Vector(currentRotation.getX(), currentRotation.getY(), roll));
            manager.updateHologram(hologram);
            refreshVisibility();
            sender.sendMessage(ColorUtils.colorize("&aRoll set to " + roll + "°"));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid roll value"));
        }
    }
    
    private void handleRelativeRotation(CommandSender sender, Hologram hologram, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo edit <name> rotate <+/-y> [+/-x] [+/-z]"));
            return;
        }
        try {
            String yStr = args[3];
            String xStr = args.length > 4 ? args[4] : "0";
            String zStr = args.length > 5 ? args[5] : "0";
            
            double deltaY = parseRelativeValue(yStr);
            double deltaX = parseRelativeValue(xStr);
            double deltaZ = parseRelativeValue(zStr);
            
            org.bukkit.util.Vector currentRotation = hologram.getRotation();
            double newX = currentRotation.getX() + deltaX;
            double newY = currentRotation.getY() + deltaY;
            double newZ = currentRotation.getZ() + deltaZ;
            
            hologram.setRotation(new org.bukkit.util.Vector(newX, newY, newZ));
            manager.updateHologram(hologram);
            refreshVisibility();
            sender.sendMessage(ColorUtils.colorize("&aRotation adjusted by " + deltaX + "°, " + deltaY + "°, " + deltaZ + "° to " + newX + "°, " + newY + "°, " + newZ + "°"));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid rotation values"));
        }
    }
    
    private double parseRelativeValue(String value) throws NumberFormatException {
        if (value.startsWith("+")) {
            return Double.parseDouble(value.substring(1));
        } else if (value.startsWith("-")) {
            return Double.parseDouble(value);
        } else {
            return Double.parseDouble(value);
        }
    }
    
    private void refreshVisibility() {
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
            () -> {
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    manager.updateVisibility(player);
                }
            },
            2L 
        );
    }
    
    private void handleBillboard(CommandSender sender, Hologram hologram, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> billboard <fixed|center|horizontal|vertical>");
            return;
        }
        try {
            Hologram.Billboard billboard = Hologram.Billboard.valueOf(args[3].toUpperCase());
            hologram.setBillboard(billboard);
            manager.updateHologram(hologram);
            sender.sendMessage("Billboard set to " + billboard.name().toLowerCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid billboard type. Use: fixed, center, horizontal, vertical");
        }
    }
    private void handleShadow(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("Shadow can only be set on text holograms");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> shadow <true|false>");
            return;
        }
        boolean shadow = Boolean.parseBoolean(args[3]);
        textHolo.setShadow(shadow);
        manager.updateHologram(hologram);
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
            () -> {
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    manager.updateVisibility(player);
                }
                System.out.println("HologramCommand: Post-shadow visibility refresh completed");
            },
            2L 
        );
        sender.sendMessage("Shadow " + (shadow ? "enabled" : "disabled"));
    }
    private void handleBackground(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("Background can only be set on text holograms");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> background <#RRGGBB> or <red> <green> <blue> [alpha]");
            return;
        }
        try {
            if (args[3].startsWith("#")) {
                String hex = args[3].substring(1);
                int rgb = Integer.parseInt(hex, 16);
                int alpha = args.length > 4 ? Integer.parseInt(args[4]) : 255;
                org.bukkit.Color color = org.bukkit.Color.fromARGB(alpha, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                textHolo.setBackgroundColor(color);
                manager.updateHologram(hologram);
                sender.sendMessage("Background color set to #" + hex);
            } else {
                int r = Integer.parseInt(args[3]);
                int g = Integer.parseInt(args[4]);
                int b = Integer.parseInt(args[5]);
                int a = args.length > 6 ? Integer.parseInt(args[6]) : 255;
                org.bukkit.Color color = org.bukkit.Color.fromARGB(a, r, g, b);
                textHolo.setBackgroundColor(color);
                manager.updateHologram(hologram);
                sender.sendMessage("Background color set to RGB(" + r + ", " + g + ", " + b + ", " + a + ")");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid color values");
        }
    }
    private void handleSeeThrough(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("See-through can only be set on text holograms");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> seethrough <true|false>");
            return;
        }
        boolean seeThrough = Boolean.parseBoolean(args[3]);
        textHolo.setSeeThrough(seeThrough);
        manager.updateHologram(hologram);
        sender.sendMessage("See-through " + (seeThrough ? "enabled" : "disabled"));
    }
    private void handleLineWidth(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("Line width can only be set on text holograms");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> linewidth <width>");
            return;
        }
        try {
            int width = Integer.parseInt(args[3]);
            textHolo.setLineWidth(width);
            manager.updateHologram(hologram);
            sender.sendMessage("Line width set to " + width);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid line width");
        }
    }
    private void handleAlignment(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("Alignment can only be set on text holograms");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> alignment <left|center|right>");
            return;
        }
        try {
            xyz.wtje.holograms.core.model.TextHologram.TextAlignment alignment = 
                xyz.wtje.holograms.core.model.TextHologram.TextAlignment.valueOf(args[3].toUpperCase());
            textHolo.setAlignment(alignment);
            manager.updateHologram(hologram);
            sender.sendMessage("Alignment set to " + alignment.name().toLowerCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid alignment. Use: left, center, right");
        }
    }
    private void handleAddLine(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("Lines can only be added to text holograms");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> addline <text>");
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        Component line = ColorUtils.parseComponent(text);
        textHolo.addLine(line);
        manager.updateHologram(hologram);
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
            () -> {
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    manager.updateVisibility(player);
                }
                System.out.println("HologramCommand: Post-addline visibility refresh completed");
            },
            2L 
        );
        sender.sendMessage(ColorUtils.colorize("&aLine added: ") + text);
    }
    private void handleRemoveLine(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("Lines can only be removed from text holograms");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /holo edit <name> removeline <line_number>");
            return;
        }
        try {
            int lineNumber = Integer.parseInt(args[3]) - 1; 
            if (lineNumber < 0 || lineNumber >= textHolo.getLines().size()) {
                sender.sendMessage("Invalid line number. Available lines: 1-" + textHolo.getLines().size());
                return;
            }
            textHolo.removeLine(lineNumber);
            manager.updateHologram(hologram);
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        manager.updateVisibility(player);
                    }
                    System.out.println("HologramCommand: Post-removeline visibility refresh completed");
                },
                2L 
            );
            sender.sendMessage("Line " + (lineNumber + 1) + " removed");
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid line number");
        }
    }
    private void handleSetLine(CommandSender sender, Hologram hologram, String[] args) {
        if (!(hologram instanceof xyz.wtje.holograms.core.model.TextHologram textHolo)) {
            sender.sendMessage("Lines can only be set on text holograms");
            return;
        }
        if (args.length < 5) {
            sender.sendMessage("Usage: /holo edit <name> setline <line_number> <text>");
            return;
        }
        try {
            int lineNumber = Integer.parseInt(args[3]) - 1; 
            if (lineNumber < 0 || lineNumber >= textHolo.getLines().size()) {
                sender.sendMessage("Invalid line number. Available lines: 1-" + textHolo.getLines().size());
                return;
            }
            String text = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
            Component line = miniMessage.deserialize(text);
            textHolo.setLine(lineNumber, line);
            manager.updateHologram(hologram);
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        manager.updateVisibility(player);
                    }
                    System.out.println("HologramCommand: Post-setline visibility refresh completed");
                },
                2L 
            );
            sender.sendMessage("Line " + (lineNumber + 1) + " set to: " + text);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid line number");
        }
    }
    private void handleMove(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("Usage: /holo move <name> <x> <y> <z>");
            return;
        }
        String name = args[1];
        Hologram hologram = manager.getHologram(name);
        if (hologram == null) {
            sender.sendMessage("Hologram '" + name + "' not found");
            return;
        }
        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);
            Location newLocation = new Location(hologram.getLocation().getWorld(), x, y, z);
            hologram.teleport(newLocation);
            sender.sendMessage("Hologram '" + name + "' moved to " + x + ", " + y + ", " + z);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid coordinates");
        }
    }
    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /holo tp <name>");
            return;
        }
        String name = args[1];
        Hologram hologram = manager.getHologram(name);
        if (hologram == null) {
            sender.sendMessage("Hologram '" + name + "' not found");
            return;
        }
        hologram.teleport(player.getLocation());
        sender.sendMessage(ColorUtils.colorize("&aHologram '" + name + "' teleported to your location"));
    }
    private void handleMoveHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cOnly players can use this command"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo movehere <name>"));
            return;
        }
        String name = args[1];
        Hologram hologram = manager.getHologram(name);
        if (hologram == null) {
            sender.sendMessage(ColorUtils.colorize("&cHologram '" + name + "' not found"));
            return;
        }
        Location centerLoc = player.getLocation().clone();
        centerLoc.setX(centerLoc.getBlockX() + 0.5);
        centerLoc.setZ(centerLoc.getBlockZ() + 0.5);
        centerLoc.add(0, 0.5, 0); 
        hologram.teleport(centerLoc);
        sender.sendMessage(ColorUtils.colorize("&aHologram '" + name + "' moved to your centered location"));
    }
    private void handleCenter(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&eUsage: /holo center <name>"));
            return;
        }
        String name = args[1];
        Hologram hologram = manager.getHologram(name);
        if (hologram == null) {
            sender.sendMessage(ColorUtils.colorize("&cHologram '" + name + "' not found"));
            return;
        }
        Location loc = hologram.getLocation().clone();
        loc.setX(loc.getBlockX() + 0.5);
        loc.setZ(loc.getBlockZ() + 0.5);
        hologram.teleport(loc);
        sender.sendMessage(ColorUtils.colorize("&aHologram '" + name + "' centered"));
    }
    private void handleCopy(CommandSender sender, String[] args) {
        sender.sendMessage("Copy command not yet implemented");
    }
    private void handleList(CommandSender sender) {
        var holograms = manager.getAllHolograms();
        if (holograms.isEmpty()) {
            sender.sendMessage("No holograms found");
            return;
        }
        sender.sendMessage("Holograms (" + holograms.size() + "):");
        for (Hologram hologram : holograms) {
            Location loc = hologram.getLocation();
            sender.sendMessage("- " + hologram.getName() + " (" + hologram.getType() + ") at " +
                    (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ());
        }
    }
    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("holo.admin")) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to reload holograms"));
            return;
        }
        if (args.length > 0 && "messages".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ColorUtils.colorize("&eReloading messages..."));
            messages.reloadMessages();
            sender.sendMessage(ColorUtils.colorize("&aMessages reloaded successfully!"));
            return;
        }
        if (args.length > 0 && "animations".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ColorUtils.colorize("&eReloading animations..."));
            animationManager.reload();
            sender.sendMessage(ColorUtils.colorize("&aAnimations reloaded successfully!"));
            return;
        }
        sender.sendMessage(ColorUtils.colorize("&eReloading holograms..."));
        org.bukkit.Bukkit.getScheduler().runTask(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), 
            () -> {
                for (Hologram hologram : manager.getAllHolograms()) {
                    hologram.despawn();
                }
                manager.clearHolograms();
                manager.loadHolograms();
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                    () -> {
                        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                            manager.updateVisibility(player);
                        }
                        System.out.println("HologramCommand: Post-reload visibility refresh completed");
                    },
                    160L 
                );
                sender.sendMessage(ColorUtils.colorize("&aHolograms reloaded successfully!"));
            }
        );
    }
    private void handleRefresh(CommandSender sender, String[] args) {
        if (!sender.hasPermission("holo.admin")) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to refresh placeholders"));
            return;
        }
        if (args.length > 0) {
            Player target = org.bukkit.Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ColorUtils.colorize("&cPlayer not found: " + args[0]));
                return;
            }
            manager.invalidatePlayerCache(target);
            sender.sendMessage(ColorUtils.colorize("&aPlaceholders refreshed for player: &e" + target.getName()));
        } else {
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                manager.invalidatePlayerCache(player);
            }
            sender.sendMessage(ColorUtils.colorize("&aPlaceholders refreshed for all online players"));
        }
    }
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6=== &eHologram Commands &6==="));
        sender.sendMessage(ColorUtils.colorize("&a/holo create text <name> <text> &7- Create text hologram"));
        sender.sendMessage(ColorUtils.colorize("&a/holo create item <name> [material] &7- Create item hologram"));
        sender.sendMessage(ColorUtils.colorize("&a/holo create block <name> <block> &7- Create block hologram"));
        sender.sendMessage(ColorUtils.colorize("&c/holo remove <name> &7- Remove hologram"));
        sender.sendMessage(ColorUtils.colorize("&b/holo move <name> <x> <y> <z> &7- Move hologram"));
        sender.sendMessage(ColorUtils.colorize("&b/holo tp <name> &7- Teleport hologram to your location"));
        sender.sendMessage(ColorUtils.colorize("&3/holo movehere <name> &7- Move hologram to your centered location"));
        sender.sendMessage(ColorUtils.colorize("&3/holo center <name> &7- Center hologram at current position"));
        sender.sendMessage(ColorUtils.colorize("&e/holo edit <name> <property> <value> &7- Edit hologram"));
        sender.sendMessage(ColorUtils.colorize("&e/holo edit <name> pitch <degrees> &7- Set pitch rotation (up/down)"));
        sender.sendMessage(ColorUtils.colorize("&e/holo edit <name> yaw <degrees> &7- Set yaw rotation (left/right)"));
        sender.sendMessage(ColorUtils.colorize("&e/holo edit <name> roll <degrees> &7- Set roll rotation (sideways)"));
        sender.sendMessage(ColorUtils.colorize("&e/holo edit <name> rotate <+/-y> [+/-x] [+/-z] &7- Adjust rotation relative to current"));
        sender.sendMessage(ColorUtils.colorize("&e/holo copy <from> <to> &7- Copy hologram"));
        sender.sendMessage(ColorUtils.colorize("&d/holo list &7- List all holograms"));
        sender.sendMessage(ColorUtils.colorize("&9/holo reload &7- Reload all holograms"));
        sender.sendMessage(ColorUtils.colorize("&9/holo reload messages &7- Reload message configuration"));
        sender.sendMessage(ColorUtils.colorize("&9/holo reload animations &7- Reload animation configuration"));
        sender.sendMessage(ColorUtils.colorize("&6/holo refresh [player] &7- Force refresh placeholders"));
        sender.sendMessage(ColorUtils.colorize("&c/holo decentmigration &7- Convert DecentHolograms files to this format"));
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "remove", "edit", "move", "tp", "movehere", "center", "copy", "list", "reload", "refresh", "decentmigration"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "create" -> completions.addAll(Arrays.asList("text", "item", "block"));
                case "reload" -> {
                    completions.add("messages");
                    completions.add("animations");
                }
                case "refresh" -> {
                    for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
                case "remove", "edit", "move", "tp", "movehere", "center", "copy" -> {
                    completions.addAll(manager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .collect(Collectors.toList()));
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            completions.addAll(Arrays.asList(
                "addline", "removeline", "setline", "text", "material", "scale", 
                "rotation", "pitch", "yaw", "roll", "rotate", "billboard", "shadow", "background", "linewidth", 
                "alignment", "opacity", "glow", "viewrange"
            ));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("edit")) {
            switch (args[2].toLowerCase()) {
                case "billboard" -> completions.addAll(Arrays.asList("FIXED", "VERTICAL", "HORIZONTAL", "CENTER"));
                case "alignment" -> completions.addAll(Arrays.asList("LEFT", "CENTER", "RIGHT"));
                case "shadow", "glow" -> completions.addAll(Arrays.asList("true", "false"));
                case "scale" -> completions.addAll(Arrays.asList("0.5", "1.0", "1.5", "2.0"));
                case "rotation" -> completions.addAll(Arrays.asList("0", "90", "180", "270"));
                case "pitch", "yaw", "roll" -> completions.addAll(Arrays.asList("0", "45", "90", "135", "180", "225", "270", "315"));
                case "rotate" -> completions.addAll(Arrays.asList("+90", "-90", "+45", "-45", "+180", "-180"));
                case "material" -> {
                    for (Material mat : Material.values()) {
                        if (mat.isItem()) {
                            completions.add(mat.name());
                        }
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            if (args[1].equalsIgnoreCase("item")) {
                for (Material mat : Material.values()) {
                    if (mat.isItem()) {
                        completions.add(mat.name());
                    }
                }
            } else if (args[1].equalsIgnoreCase("block")) {
                for (Material mat : Material.values()) {
                    if (mat.isBlock()) {
                        completions.add(mat.name());
                    }
                }
            }
        }
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
    
    private void handleDecentMigration(CommandSender sender, String[] args) {
        if (!sender.hasPermission("holograms.admin")) {
            sender.sendMessage(ColorUtils.colorize("&cNo permission!"));
            return;
        }
        
        sender.sendMessage(ColorUtils.colorize("&eStarting DecentHolograms migration..."));
        
        try {
            java.io.File hologramsFolder = new java.io.File(plugin.getDataFolder(), "holograms");
            if (!hologramsFolder.exists()) {
                hologramsFolder.mkdirs();
            }
            
            java.io.File[] files = hologramsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null || files.length == 0) {
                sender.sendMessage(ColorUtils.colorize("&cNo .yml files found in holograms folder!"));
                return;
            }
            
            int converted = 0;
            int skipped = 0;
            
            for (java.io.File file : files) {
                try {
                    org.bukkit.configuration.file.YamlConfiguration config = 
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                    
                    if (isAlreadyConverted(config)) {
                        skipped++;
                        continue;
                    }
                    
                    if (convertDecentHologram(config, file)) {
                        converted++;
                        sender.sendMessage(ColorUtils.colorize("&aConverted: " + file.getName()));
                    } else {
                        sender.sendMessage(ColorUtils.colorize("&cFailed to convert: " + file.getName()));
                    }
                    
                } catch (Exception e) {
                    sender.sendMessage(ColorUtils.colorize("&cError processing " + file.getName() + ": " + e.getMessage()));
                }
            }
            
            sender.sendMessage(ColorUtils.colorize("&eMigration completed!"));
            sender.sendMessage(ColorUtils.colorize("&aConverted: " + converted + " files"));
            sender.sendMessage(ColorUtils.colorize("&eSkipped: " + skipped + " files (already converted)"));
            
            sender.sendMessage(ColorUtils.colorize("&eReloading holograms..."));
            manager.clearHolograms();
            manager.loadHolograms();
            sender.sendMessage(ColorUtils.colorize("&aHolograms reloaded successfully!"));
            
        } catch (Exception e) {
            sender.sendMessage(ColorUtils.colorize("&cMigration failed: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private boolean isAlreadyConverted(org.bukkit.configuration.file.YamlConfiguration config) {
        return config.contains("type") && config.contains("view-distance") && config.contains("billboard");
    }
    
    private boolean convertDecentHologram(org.bukkit.configuration.file.YamlConfiguration decentConfig, java.io.File file) {
        try {
            String locationStr = decentConfig.getString("location");
            if (locationStr == null) {
                return false;
            }
            
            String[] locationParts = locationStr.split(":");
            if (locationParts.length != 4) {
                return false;
            }
            
            String world = locationParts[0];
            double x = Double.parseDouble(locationParts[1]);
            double y = Double.parseDouble(locationParts[2]);
            double z = Double.parseDouble(locationParts[3]);
            
            double viewDistance = decentConfig.getDouble("display-range", 32.0);
            
            List<String> lines = new ArrayList<>();
            
            if (decentConfig.contains("pages")) {
                List<?> pages = decentConfig.getList("pages");
                if (pages != null && !pages.isEmpty()) {
                    Object firstPage = pages.get(0);
                    if (firstPage instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> pageMap = (java.util.Map<String, Object>) firstPage;
                        
                        if (pageMap.containsKey("lines")) {
                            List<?> pageLines = (List<?>) pageMap.get("lines");
                            if (pageLines != null) {
                                for (Object lineObj : pageLines) {
                                    if (lineObj instanceof java.util.Map) {
                                        @SuppressWarnings("unchecked")
                                        java.util.Map<String, Object> lineMap = (java.util.Map<String, Object>) lineObj;
                                        String content = (String) lineMap.get("content");
                                        if (content != null && !content.trim().isEmpty()) {
                                            lines.add(content);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (lines.isEmpty()) {
                return false;
            }
            
            org.bukkit.configuration.file.YamlConfiguration newConfig = new org.bukkit.configuration.file.YamlConfiguration();
            
            newConfig.set("type", "TEXT");
            newConfig.set("view-distance", viewDistance);
            
            newConfig.set("location.world", world);
            newConfig.set("location.x", x);
            newConfig.set("location.y", y);
            newConfig.set("location.z", z);
            newConfig.set("location.yaw", 0.0);
            newConfig.set("location.pitch", 0.0);
            
            newConfig.set("scale.x", 1.0);
            newConfig.set("scale.y", 1.0);
            newConfig.set("scale.z", 1.0);
            
            newConfig.set("rotation.x", 0.0);
            newConfig.set("rotation.y", 0.0);
            newConfig.set("rotation.z", 0.0);
            
            newConfig.set("billboard", "CENTER");
            newConfig.set("lines", lines);
            newConfig.set("shadow", false);
            newConfig.set("see-through", false);
            newConfig.set("line-width", 200);
            newConfig.set("alignment", "CENTER");
            newConfig.set("background-color", 0);
            
            newConfig.save(file);
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
