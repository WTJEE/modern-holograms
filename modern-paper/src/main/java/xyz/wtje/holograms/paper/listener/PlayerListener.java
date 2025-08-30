package xyz.wtje.holograms.paper.listener;
import xyz.wtje.holograms.core.manager.HologramManager;
import xyz.wtje.holograms.paper.util.PacketEntityManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
public class PlayerListener implements Listener {
    private final HologramManager manager;
    public PlayerListener(HologramManager manager) {
        this.manager = manager;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                manager.updateVisibility(event.getPlayer());
            }
        }.runTaskLater(JavaPlugin.getProvidingPlugin(getClass()), 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                manager.updateVisibility(event.getPlayer());
            }
        }.runTaskLater(JavaPlugin.getProvidingPlugin(getClass()), 60L);
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PacketEntityManager.cleanupPlayer(event.getPlayer());
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            manager.updateVisibility(event.getPlayer());
        }
    }
}
