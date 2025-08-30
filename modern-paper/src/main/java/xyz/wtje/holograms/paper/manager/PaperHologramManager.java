package xyz.wtje.holograms.paper.manager;
import xyz.wtje.holograms.core.manager.HologramManager;
import xyz.wtje.holograms.core.model.Hologram;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
import xyz.wtje.holograms.core.config.ConfigManager;
import xyz.wtje.holograms.paper.model.LegacyTextHologram;
import xyz.wtje.holograms.paper.model.ModernTextHologram;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
public class PaperHologramManager extends HologramManager {
    public PaperHologramManager(VersionAdapter adapter, Plugin plugin) {
        super(adapter, plugin);
    }
    @Override
    public void addHologram(Hologram hologram) {
        configurePlaceholderCache(hologram);
        super.addHologram(hologram);
    }
    @Override
    public void updateHologram(Hologram hologram) {
        configurePlaceholderCache(hologram);
        super.updateHologram(hologram);
    }
    @Override
    public void cleanupExpiredCache() {
        for (Hologram hologram : getAllHolograms()) {
            try {
                if (hologram instanceof LegacyTextHologram) {
                    ((LegacyTextHologram) hologram).cleanupExpiredCache();
                } else if (hologram instanceof ModernTextHologram) {
                    ((ModernTextHologram) hologram).cleanupExpiredCache();
                }
            } catch (Exception e) {
            }
        }
    }
    @Override
    public void invalidatePlayerCache(Player player) {
        for (Hologram hologram : getAllHolograms()) {
            try {
                if (hologram instanceof LegacyTextHologram) {
                    LegacyTextHologram legacy = (LegacyTextHologram) hologram;
                    legacy.clearPlayerCache(player);
                    if (legacy.isVisible(player)) {
                        legacy.updateForPlayer(player);
                    }
                } else if (hologram instanceof ModernTextHologram) {
                    ModernTextHologram modern = (ModernTextHologram) hologram;
                    modern.clearPlayerCache(player);
                    if (modern.isVisible(player)) {
                        modern.updateForPlayer(player);
                    }
                }
            } catch (Exception e) {
            }
        }
    }
    private void configurePlaceholderCache(Hologram hologram) {
        ConfigManager configManager = getConfigManager();
        if (configManager == null) return;
        long ttl = configManager.getPlaceholderCacheTTL();
        try {
            if (hologram instanceof LegacyTextHologram) {
                ((LegacyTextHologram) hologram).setCacheTTL(ttl);
            } else if (hologram instanceof ModernTextHologram) {
                ((ModernTextHologram) hologram).setCacheTTL(ttl);
            }
        } catch (Exception e) {
        }
    }
}
