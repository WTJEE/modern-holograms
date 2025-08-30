package xyz.wtje.holograms.paper.model;
import xyz.wtje.holograms.core.model.TextHologram;
import xyz.wtje.holograms.paper.integration.PlaceholderIntegration;
import xyz.wtje.holograms.paper.util.PacketEntityManager;
import xyz.wtje.holograms.paper.util.TextProcessor;
import xyz.wtje.holograms.paper.animation.AnimationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
public class ModernTextHologram extends TextHologram {
    private TextDisplay entity;
    private final Set<Player> viewers = new HashSet<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Component> playerTextCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private boolean hasPlaceholders = false;
    private boolean hasAnimations = false;
    private long cacheTTL = 1000; 
    private AnimationManager.AnimationRefreshCallback animationCallback;
    public ModernTextHologram(String name, Location location, List<Component> lines) {
        super(name, location, lines);
        checkForPlaceholders();
    }
    private void checkForPlaceholders() {
        hasPlaceholders = false;
        hasAnimations = false;
        for (Component line : lines) {
            String serialized = miniMessage.serialize(line);
            if (TextProcessor.isDynamic(serialized)) {
                hasPlaceholders = true;
                if (TextProcessor.hasAnimations(serialized)) {
                    hasAnimations = true;
                }
            }
        }
        if (hasAnimations) {
            this.cacheTTL = 200;
            setupAnimationCallback();
        }
    }
    @Override
    public void spawn() {
        if (entity != null) {
            return;
        }
        double lineOffset = 0.3; 
        double centerOffset = lines.size() > 1 ? -((lines.size() - 1) * lineOffset / 2.0) : 0;
        double additionalOffset = -0.1 * lines.size(); 
        Location textDisplayLocation = location.clone().add(0, centerOffset + additionalOffset, 0);
        entity = location.getWorld().spawn(textDisplayLocation, TextDisplay.class);
        entity.setPersistent(false);
        entity.setGravity(false);
        updateDisplay();
    }
    @Override
    public void despawn() {
        cleanupAnimationCallback(); 
        if (entity != null) {
            PacketEntityManager.cleanupEntity(entity);
            entity.remove();
            entity = null;
        }
        viewers.clear();
    }
    private void updateDisplay() {
        if (entity == null) return;
        List<Component> processedLines = new ArrayList<>();
        for (Component line : lines) {
            String serialized = miniMessage.serialize(line);
            Component processed = miniMessage.deserialize(serialized);
            processedLines.add(processed);
        }
        entity.text(Component.join(Component.newline(), processedLines));
        entity.setShadowed(shadow);
        entity.setSeeThrough(seeThrough);
        entity.setLineWidth(lineWidth);
        entity.setBackgroundColor(backgroundColor);
        switch (alignment) {
            case LEFT -> entity.setAlignment(TextDisplay.TextAlignment.LEFT);
            case RIGHT -> entity.setAlignment(TextDisplay.TextAlignment.RIGHT);
            default -> entity.setAlignment(TextDisplay.TextAlignment.CENTER);
        }
        switch (billboard) {
            case FIXED -> entity.setBillboard(Display.Billboard.FIXED);
            case VERTICAL -> entity.setBillboard(Display.Billboard.VERTICAL);
            case HORIZONTAL -> entity.setBillboard(Display.Billboard.HORIZONTAL);
            default -> entity.setBillboard(Display.Billboard.CENTER);
        }
        Vector3f scaleVec = new Vector3f((float)scale.getX(), (float)scale.getY(), (float)scale.getZ());
        Quaternionf rotationQuat = new Quaternionf()
            .rotateXYZ((float)Math.toRadians(rotation.getX()), 
                      (float)Math.toRadians(rotation.getY()), 
                      (float)Math.toRadians(rotation.getZ()));
        Transformation transform = new Transformation(
            new Vector3f(0, 0, 0), 
            new Quaternionf(),     
            scaleVec,              
            rotationQuat           
        );
        entity.setTransformation(transform);
    }
    public void setCacheTTL(long ttlMs) {
        this.cacheTTL = ttlMs;
    }
    public void clearCache() {
        playerTextCache.clear();
        cacheTimestamps.clear();
        checkForPlaceholders();
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
    private Component getProcessedTextForPlayer(Player player) {
        if (!hasPlaceholders || player == null) {
            return Component.join(Component.newline(), lines);
        }
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Component cached = playerTextCache.get(playerId);
        Long cacheTime = cacheTimestamps.get(playerId);
        if (cached != null && cacheTime != null && 
            (currentTime - cacheTime) < cacheTTL) {
            return cached;
        }
        List<Component> processedLines = new ArrayList<>();
        for (Component line : lines) {
            String serialized = miniMessage.serialize(line);
            String parsed = TextProcessor.processText(player, serialized);
            Component processed = miniMessage.deserialize(parsed);
            processedLines.add(processed);
        }
        Component result = Component.join(Component.newline(), processedLines);
        if (playerTextCache.size() < 50) {
            playerTextCache.put(playerId, result);
            cacheTimestamps.put(playerId, currentTime);
        }
        return result;
    }
    public void updateForPlayer(Player player) {
        if (entity == null) return;
        Component processedText = getProcessedTextForPlayer(player);
        entity.text(processedText);
    }
    @Override
    public void update() {
        if (entity != null) {
            clearCache();
            updateDisplay();
            if (hasPlaceholders) {
                for (Player viewer : viewers) {
                    updateForPlayer(viewer);
                }
            } else {
                updateDisplay();
            }
        }
    }
    @Override
    public boolean isVisible(Player player) {
        return viewers.contains(player) && 
               (entity == null || !PacketEntityManager.isEntityHiddenFromPlayer(entity, player));
    }
    @Override
    public void teleport(Location location) {
        this.location = location;
        if (entity != null) {
            double lineOffset = 0.3;
            double centerOffset = lines.size() > 1 ? -((lines.size() - 1) * lineOffset / 2.0) : 0;
            double additionalOffset = -0.1 * lines.size(); 
            Location textDisplayLocation = location.clone().add(0, centerOffset + additionalOffset, 0);
            entity.teleport(textDisplayLocation);
        }
    }
    public void showTo(Player player) {
        viewers.add(player);
        updateForPlayer(player);
        if (entity != null) {
            PacketEntityManager.showEntityToPlayer(entity, player);
        }
    }
    public void hideFrom(Player player) {
        viewers.remove(player);
        clearPlayerCache(player); 
        if (entity != null) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), 
                () -> PacketEntityManager.hideEntityFromPlayer(entity, player), 
                1L);
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
            System.err.println("ModernTextHologram: Failed to setup animation callback: " + e.getMessage());
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
                System.err.println("ModernTextHologram: Failed to cleanup animation callback: " + e.getMessage());
            }
            animationCallback = null;
        }
    }
}
