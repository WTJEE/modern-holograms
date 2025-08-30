package xyz.wtje.holograms.paper.model;
import xyz.wtje.holograms.core.model.TextHologram;
import xyz.wtje.holograms.paper.integration.PlaceholderIntegration;
import xyz.wtje.holograms.paper.util.PacketEntityManager;
import xyz.wtje.holograms.paper.util.TextProcessor;
import xyz.wtje.holograms.paper.animation.AnimationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
public class LegacyTextHologram extends TextHologram {
    private final List<ArmorStand> armorStands = new ArrayList<>();
    private final Set<Player> viewers = new HashSet<>();
    private final double lineOffset = 0.3; 
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final Map<UUID, List<String>> playerTextCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final List<String> baseTextLines = new ArrayList<>();
    private boolean hasPlaceholders = false;
    private boolean hasAnimations = false;
    private long cacheTTL = 1000; 
    private AnimationManager.AnimationRefreshCallback animationCallback;
    private void initializeTextLines() {
        baseTextLines.clear();
        hasPlaceholders = false;
        for (Component c : lines) {
            String mm = MM.serialize(c);
            if (TextProcessor.isDynamic(mm)) {
                hasPlaceholders = true;
            }
            String[] parts = mm.split("(?i)<newline>|<br>|\\\\n|\\n");
            for (String part : parts) {
                if (!part.isBlank()) {
                    baseTextLines.add(part.trim());
                }
            }
        }
        if (hasPlaceholders) {
            hasAnimations = false;
            for (String line : baseTextLines) {
                if (TextProcessor.hasAnimations(line)) {
                    hasAnimations = true;
                    break;
                }
            }
            if (hasAnimations) {
                this.cacheTTL = 200;
                setupAnimationCallback();
            }
        }
    }
    private List<String> getSingleLineStrings() {
        if (!hasPlaceholders) {
            return getStaticTextLines();
        }
        return getStaticTextLines();
    }
    private List<String> getSingleLineStrings(Player player) {
        if (!hasPlaceholders || player == null) {
            return getStaticTextLines();
        }
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        List<String> cached = playerTextCache.get(playerId);
        Long cacheTime = cacheTimestamps.get(playerId);
        if (cached != null && cacheTime != null && 
            (currentTime - cacheTime) < cacheTTL) {
            return cached;
        }
        List<String> playerLines = new ArrayList<>();
        for (String baseLine : baseTextLines) {
            String parsed = TextProcessor.processText(player, baseLine);
            try {
                Component component = MM.deserialize(parsed);
                String legacyText = LEGACY.serialize(component);
                if (legacyText.length() > 150) {
                    legacyText = legacyText.substring(0, 147) + "...";
                }
                playerLines.add(legacyText);
            } catch (Exception e) {
                String plainText = parsed.length() > 150 ? parsed.substring(0, 147) + "..." : parsed;
                playerLines.add(plainText);
            }
        }
        if (playerTextCache.size() < 50) {
            playerTextCache.put(playerId, playerLines);
            cacheTimestamps.put(playerId, currentTime);
        }
        return playerLines;
    }
    private List<String> getStaticTextLines() {
        List<String> staticLines = new ArrayList<>();
        for (String baseLine : baseTextLines) {
            try {
                Component component = MM.deserialize(baseLine);
                String legacyText = LEGACY.serialize(component);
                if (legacyText.length() > 150) {
                    legacyText = legacyText.substring(0, 147) + "...";
                }
                staticLines.add(legacyText);
            } catch (Exception e) {
                String plainText = baseLine.length() > 150 ? baseLine.substring(0, 147) + "..." : baseLine;
                staticLines.add(plainText);
            }
        }
        return staticLines;
    }
    private void setStandName(ArmorStand stand, String legacyLine) {
        try {
            stand.customName(LEGACY.deserialize(legacyLine));
            stand.setCustomNameVisible(true);
        } catch (NoSuchMethodError e) {
            try {
                stand.setCustomName(legacyLine);
                stand.setCustomNameVisible(true);
            } catch (Exception e2) {
                try {
                    java.lang.reflect.Method setCustomName = stand.getClass().getMethod("setCustomName", String.class);
                    java.lang.reflect.Method setCustomNameVisible = stand.getClass().getMethod("setCustomNameVisible", boolean.class);
                    setCustomName.invoke(stand, legacyLine);
                    setCustomNameVisible.invoke(stand, true);
                } catch (Exception e3) {
                }
            }
        }
    }
    private void setStandInvisible(ArmorStand stand) {
        try {
            stand.setVisible(false);
        } catch (NoSuchMethodError e) {
            try {
                stand.setInvisible(true);
            } catch (NoSuchMethodError e2) {
                try {
                    java.lang.reflect.Method setInvisible = stand.getClass().getMethod("setInvisible", boolean.class);
                    setInvisible.invoke(stand, true);
                } catch (Exception e3) {
                }
            }
        }
    }
    public LegacyTextHologram(String name, Location location, List<Component> lines) {
        super(name, location, lines);
        initializeTextLines(); 
    }
    @Override
    public void spawn() {
        if (!armorStands.isEmpty()) return;
        List<String> textLines = getSingleLineStrings();
        for (int i = 0; i < textLines.size(); i++) {
            Location lineLocation = location.clone().add(0, -(i * lineOffset), 0);
            ArmorStand stand = location.getWorld().spawn(lineLocation, ArmorStand.class);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setPersistent(false);
            setStandName(stand, textLines.get(i));
            setStandInvisible(stand);
            try {
                stand.setMarker(true);
            } catch (NoSuchMethodError e) {
            }
            try {
                stand.setCollidable(false);
            } catch (NoSuchMethodError e) {
            }
            armorStands.add(stand);
        }
    }
    @Override
    public void despawn() {
        cleanupAnimationCallback(); 
        for (ArmorStand stand : armorStands) {
            PacketEntityManager.cleanupEntity(stand);
            stand.remove();
        }
        armorStands.clear();
        viewers.clear();
    }
    @Override
    public void update() {
        clearCache();
        List<String> textLines = getSingleLineStrings();
        while (armorStands.size() > textLines.size()) {
            ArmorStand stand = armorStands.remove(armorStands.size() - 1);
            stand.remove();
        }
        while (armorStands.size() < textLines.size()) {
            int index = armorStands.size();
            Location lineLocation = location.clone().add(0, -(index * lineOffset), 0);
            ArmorStand stand = location.getWorld().spawn(lineLocation, ArmorStand.class);
            stand.setGravity(false);
            setStandInvisible(stand);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            try {
                stand.setMarker(true);
            } catch (NoSuchMethodError e) {
            }
            stand.setPersistent(false);
            armorStands.add(stand);
        }
        for (int i = 0; i < textLines.size(); i++) {
            ArmorStand stand = armorStands.get(i);
            Location lineLocation = location.clone().add(0, -(i * lineOffset), 0);
            stand.teleport(lineLocation);
            setStandName(stand, textLines.get(i)); 
        }
        if (hasPlaceholders && !viewers.isEmpty()) {
            for (Player viewer : viewers) {
                updateForPlayer(viewer);
            }
        }
    }
    @Override
    public boolean isVisible(Player player) {
        return viewers.contains(player);
    }
    @Override
    public void teleport(Location location) {
        this.location = location;
        for (int i = 0; i < armorStands.size(); i++) {
            Location lineLocation = location.clone().add(0, -(i * lineOffset), 0);
            armorStands.get(i).teleport(lineLocation);
        }
    }
    public void setCacheTTL(long ttlMs) {
        this.cacheTTL = ttlMs;
    }
    public void clearCache() {
        playerTextCache.clear();
        cacheTimestamps.clear();
        initializeTextLines();
    }
    public void clearPlayerCache(Player player) {
        UUID playerId = player.getUniqueId();
        playerTextCache.remove(playerId);
        cacheTimestamps.remove(playerId);
    }
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        cacheTimestamps.entrySet().removeIf(entry -> {
            boolean expired = (currentTime - entry.getValue()) > cacheTTL;
            if (expired) {
                playerTextCache.remove(entry.getKey());
            }
            return expired;
        });
    }
    public void updateForPlayer(Player player) {
        if (armorStands.isEmpty()) return;
        List<String> playerLines = getSingleLineStrings(player);
        for (int i = 0; i < Math.min(armorStands.size(), playerLines.size()); i++) {
            ArmorStand stand = armorStands.get(i);
            String line = playerLines.get(i);
            String currentName = null;
            try {
                currentName = stand.getCustomName();
            } catch (Exception e) {
            }
            if (!line.equals(currentName)) {
                if (line != null && !line.isEmpty()) {
                    try {
                        stand.setCustomName(line);
                        stand.setCustomNameVisible(true);
                    } catch (NoSuchMethodError e) {
                        try {
                            java.lang.reflect.Method setCustomName = stand.getClass().getMethod("setCustomName", String.class);
                            java.lang.reflect.Method setCustomNameVisible = stand.getClass().getMethod("setCustomNameVisible", boolean.class);
                            setCustomName.invoke(stand, line);
                            setCustomNameVisible.invoke(stand, true);
                        } catch (Exception e2) {
                        }
                    }
                } else {
                    try {
                        stand.setCustomNameVisible(false);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
    public void showTo(Player player) {
        viewers.add(player);
        updateForPlayer(player);
        if (!PacketEntityManager.isAvailable()) {
            for (ArmorStand stand : armorStands) {
                try {
                    player.showEntity(org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), stand);
                } catch (NoSuchMethodError e) {
                    stand.setCustomNameVisible(true);
                }
            }
            return;
        }
        for (ArmorStand stand : armorStands) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), 
                () -> PacketEntityManager.showEntityToPlayer(stand, player), 
                1L);
        }
    }
    public void hideFrom(Player player) {
        viewers.remove(player);
        clearPlayerCache(player); 
        if (!PacketEntityManager.isAvailable()) {
            for (ArmorStand stand : armorStands) {
                try {
                    player.hideEntity(org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), stand);
                } catch (NoSuchMethodError e) {
                }
            }
            return;
        }
        for (ArmorStand stand : armorStands) {
            PacketEntityManager.hideEntityFromPlayer(stand, player);
        }
    }
    private void setupAnimationCallback() {
        try {
            org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin");
            if (plugin != null && plugin instanceof xyz.wtje.holograms.paper.HologramPlugin) {
                AnimationManager animationManager = ((xyz.wtje.holograms.paper.HologramPlugin) plugin).getAnimationManager();
                if (animationManager != null && animationCallback == null) {
                    animationCallback = () -> {
                        clearCache();
                        for (Player viewer : viewers) {
                            updateForPlayer(viewer);
                        }
                    };
                    animationManager.addRefreshCallback(animationCallback);
                }
            }
        } catch (Exception e) {
        }
    }
    private void cleanupAnimationCallback() {
        if (animationCallback != null) {
            try {
                org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin");
                if (plugin != null && plugin instanceof xyz.wtje.holograms.paper.HologramPlugin) {
                    AnimationManager animationManager = ((xyz.wtje.holograms.paper.HologramPlugin) plugin).getAnimationManager();
                    if (animationManager != null) {
                        animationManager.removeRefreshCallback(animationCallback);
                    }
                }
            } catch (Exception e) {
            }
            animationCallback = null;
        }
    }
}
