package xyz.wtje.holograms.paper.adapter;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
import xyz.wtje.holograms.core.model.Hologram;
import xyz.wtje.holograms.paper.model.LegacyTextHologram;
import xyz.wtje.holograms.paper.model.ModernTextHologram;
import xyz.wtje.holograms.paper.util.ProtocolUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import java.util.List;
public class HybridAdapter implements VersionAdapter {
    private final DisplayEntityAdapter modernAdapter = new DisplayEntityAdapter();
    private final LegacyAdapter legacyAdapter = new LegacyAdapter();
    @Override
    public String getVersion() {
        return "Hybrid (Display Entities + ArmorStands)";
    }
    @Override
    public boolean supportsDisplayEntities() {
        return true; 
    }
    @Override
    public Hologram createTextHologram(String name, Location location, List<Component> lines) {
        return new HybridTextHologram(name, location, lines);
    }
    @Override
    public Hologram createItemHologram(String name, Location location, ItemStack item) {
        return modernAdapter.createItemHologram(name, location, item);
    }
    @Override
    public Hologram createBlockHologram(String name, Location location, BlockData blockData) {
        return modernAdapter.createBlockHologram(name, location, blockData);
    }
    @Override
    public void updateVisibility(Hologram hologram, Player player, boolean visible) {
        if (hologram instanceof HybridTextHologram hybridHolo) {
            hybridHolo.updateVisibilityForPlayer(player, visible);
        } else {
            modernAdapter.updateVisibility(hologram, player, visible);
        }
    }
    private static class HybridTextHologram extends xyz.wtje.holograms.core.model.TextHologram {
        private ModernTextHologram modernHologram;
        private LegacyTextHologram legacyHologram;
        private boolean modernSpawned = false;
        private boolean legacySpawned = false;
        public HybridTextHologram(String name, Location location, List<Component> lines) {
            super(name, location, lines);
        }
        @Override
        public void spawn() {
        }
        @Override
        public void despawn() {
            if (modernHologram != null) {
                modernHologram.despawn();
                modernHologram = null;
                modernSpawned = false;
            }
            if (legacyHologram != null) {
                legacyHologram.despawn();
                legacyHologram = null;
                legacySpawned = false;
            }
        }
        @Override
        public void update() {
            if (modernHologram != null) {
                modernHologram.setLines(this.lines);
                modernHologram.setShadow(this.shadow);
                modernHologram.setSeeThrough(this.seeThrough);
                modernHologram.setLineWidth(this.lineWidth);
                modernHologram.setBackgroundColor(this.backgroundColor);
                modernHologram.setAlignment(this.alignment);
                modernHologram.setBillboard(this.billboard);
                modernHologram.setScale(this.scale);
                modernHologram.setRotation(this.rotation);
                modernHologram.update();
            }
            if (legacyHologram != null) {
                legacyHologram.setLines(this.lines);
                legacyHologram.setScale(this.scale);
                legacyHologram.setRotation(this.rotation);
                legacyHologram.update();
            }
        }
        @Override
        public boolean isVisible(Player player) {
            if (ProtocolUtils.supportsDisplayEntities(player)) {
                return modernHologram != null && modernHologram.isVisible(player);
            } else {
                return legacyHologram != null && legacyHologram.isVisible(player);
            }
        }
        @Override
        public void teleport(Location location) {
            this.location = location;
            if (modernHologram != null) {
                modernHologram.teleport(location);
            }
            if (legacyHologram != null) {
                legacyHologram.teleport(location);
            }
        }
        public void updateVisibilityForPlayer(Player player, boolean visible) {
            boolean supportsDisplayEntities = ProtocolUtils.supportsDisplayEntities(player);
            if (supportsDisplayEntities) {
                if (legacyHologram != null) {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), 
                        () -> legacyHologram.hideFrom(player), 
                        2L);
                }
                if (visible) {
                    if (!modernSpawned || modernHologram == null) {
                        if (modernHologram != null) {
                            modernHologram.despawn();
                        }
                        modernHologram = new ModernTextHologram(name, location, lines);
                        modernHologram.setShadow(this.shadow);
                        modernHologram.setSeeThrough(this.seeThrough);
                        modernHologram.setLineWidth(this.lineWidth);
                        modernHologram.setBackgroundColor(this.backgroundColor);
                        modernHologram.setAlignment(this.alignment);
                        modernHologram.setBillboard(this.billboard);
                        modernHologram.setScale(this.scale);
                        modernHologram.setRotation(this.rotation);
                        modernHologram.spawn();
                        modernSpawned = true;
                        org.bukkit.Bukkit.getScheduler().runTaskLater(
                            org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), 
                            () -> {
                                for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                                    if (!ProtocolUtils.supportsDisplayEntities(onlinePlayer)) {
                                        modernHologram.hideFrom(onlinePlayer);
                                    }
                                }
                            }, 3L); 
                    }
                    if (modernHologram != null && !modernHologram.isVisible(player)) {
                        modernHologram.showTo(player);
                    }
                } else {
                    if (modernHologram != null && modernHologram.isVisible(player)) {
                        modernHologram.hideFrom(player);
                    }
                }
            } else {
                if (modernHologram != null) {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), 
                        () -> modernHologram.hideFrom(player), 
                        2L);
                }
                if (visible) {
                    if (!legacySpawned || legacyHologram == null) {
                        if (legacyHologram != null) {
                            legacyHologram.despawn();
                        }
                        legacyHologram = new LegacyTextHologram(name, location, lines);
                        legacyHologram.setScale(this.scale);
                        legacyHologram.setRotation(this.rotation);
                        legacyHologram.spawn();
                        legacySpawned = true;
                        org.bukkit.Bukkit.getScheduler().runTaskLater(
                            org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin"), 
                            () -> {
                                for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                                    if (ProtocolUtils.supportsDisplayEntities(onlinePlayer)) {
                                        legacyHologram.hideFrom(onlinePlayer);
                                    }
                                }
                            }, 3L); 
                    }
                    if (legacyHologram != null && !legacyHologram.isVisible(player)) {
                        legacyHologram.showTo(player);
                    }
                } else {
                    if (legacyHologram != null && legacyHologram.isVisible(player)) {
                        legacyHologram.hideFrom(player);
                    }
                }
            }
        }
    }
}
