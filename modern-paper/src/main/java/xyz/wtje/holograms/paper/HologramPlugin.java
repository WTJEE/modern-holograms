package xyz.wtje.holograms.paper;
import xyz.wtje.holograms.core.manager.HologramManager;
import xyz.wtje.holograms.paper.manager.PaperHologramManager;
import xyz.wtje.holograms.core.config.ConfigManager;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
import xyz.wtje.holograms.core.storage.HologramStorage;
import xyz.wtje.holograms.paper.adapter.AdapterFactory;
import xyz.wtje.holograms.paper.command.HologramCommand;
import xyz.wtje.holograms.paper.listener.PlayerListener;
import xyz.wtje.holograms.paper.integration.PlaceholderIntegration;
import xyz.wtje.holograms.paper.util.MessageManager;
import xyz.wtje.holograms.paper.util.PacketEntityManager;
import xyz.wtje.holograms.paper.util.TextProcessor;
import xyz.wtje.holograms.paper.api.HologramAPI;
import xyz.wtje.holograms.paper.animation.AnimationManager;
import org.bukkit.plugin.java.JavaPlugin;
public class HologramPlugin extends JavaPlugin {
    private HologramManager hologramManager;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private AnimationManager animationManager;
    private VersionAdapter adapter;
    private HologramStorage storage;
    @Override
    public void onEnable() {
        PlaceholderIntegration.initialize();
        if (PacketEntityManager.isAvailable()) {
            getLogger().info("PacketEvents integration enabled");
        } else {
            getLogger().warning("PacketEvents not found - entity visibility may not work properly");
        }
        configManager = new ConfigManager(getDataFolder());
        messageManager = new MessageManager(this);
        animationManager = new AnimationManager(this);
        TextProcessor.initialize(animationManager);
        configManager.loadAsync().thenRun(() -> {
            getLogger().info("Configuration loaded");
            adapter = AdapterFactory.createAdapter();
            hologramManager = new PaperHologramManager(adapter, this);
            hologramManager.setConfigManager(configManager);
            HologramAPI.initialize(hologramManager);
            storage = new HologramStorage(getDataFolder(), adapter);
            hologramManager.setStorage(storage);
            getServer().getScheduler().runTaskLater(this, () -> {
                hologramManager.loadHolograms();
                getLogger().info("Holograms loaded successfully");
                getServer().getScheduler().runTaskTimer(this, () -> {
                    for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                        hologramManager.updateVisibility(player);
                    }
                }, 100L, 100L); 
                long cleanupInterval = configManager.getPlaceholderCacheCleanupInterval();
                getServer().getScheduler().runTaskTimer(this, () -> {
                    hologramManager.cleanupExpiredCache();
                }, cleanupInterval, cleanupInterval);
            }, 40L); 
            getCommand("holo").setExecutor(new HologramCommand(hologramManager, configManager, messageManager, animationManager, this));
            getServer().getPluginManager().registerEvents(new PlayerListener(hologramManager), this);
            getLogger().info("HologramPlugin enabled using " + adapter.getVersion() + " adapter");
            getLogger().info("Display Entities support: " + adapter.supportsDisplayEntities());
            getLogger().info("PlaceholderAPI integration: " + PlaceholderIntegration.isEnabled());
        });
    }
    @Override
    public void onDisable() {
        if (animationManager != null) {
            animationManager.shutdown();
        }
        if (hologramManager != null) {
            hologramManager.shutdown();
        }
        if (configManager != null) {
            configManager.saveAsync().thenRun(() -> {
                getLogger().info("Configuration saved");
            });
        }
    }
    public AnimationManager getAnimationManager() {
        return animationManager;
    }
}
