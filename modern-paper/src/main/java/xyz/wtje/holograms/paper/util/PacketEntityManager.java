package xyz.wtje.holograms.paper.util;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public class PacketEntityManager {
    private static final Map<Integer, Set<Player>> hiddenEntities = new ConcurrentHashMap<>();
    private static boolean packetEventsAvailable = false;
    static {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            packetEventsAvailable = PacketEvents.getAPI() != null;
        } catch (ClassNotFoundException e) {
        }
    }
    public static void hideEntityFromPlayer(Entity entity, Player player) {
        try {
            int entityId = entity.getEntityId();
            hiddenEntities.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet()).add(player);
            org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin");
            if (plugin != null) {
                player.hideEntity(plugin, entity);
            }
            if (packetEventsAvailable) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        try {
                            WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityId);
                            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
                        } catch (Exception e) {
                            System.err.println("PacketEntityManager: Failed to send destroy packet: " + e.getMessage());
                        }
                    }, 1L);
            }
        } catch (Throwable t) {
            System.err.println("PacketEntityManager: Failed to hide entity: " + t.getMessage());
        }
    }
    public static void showEntityToPlayer(Entity entity, Player player) {
        try {
            int entityId = entity.getEntityId();
            Set<Player> hiddenFromPlayers = hiddenEntities.get(entityId);
            boolean wasHidden = hiddenFromPlayers != null && hiddenFromPlayers.contains(player);
            if (hiddenFromPlayers != null) {
                hiddenFromPlayers.remove(player);
                if (hiddenFromPlayers.isEmpty()) {
                    hiddenEntities.remove(entityId);
                }
            }
            if (wasHidden) {
                org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("HologramPlugin");
                if (plugin != null) {
                    player.showEntity(plugin, entity);
                }
            }
        } catch (Throwable t) {
            System.err.println("PacketEntityManager: Failed to show entity: " + t.getMessage());
        }
    }
    public static boolean isEntityHiddenFromPlayer(Entity entity, Player player) {
        Set<Player> hiddenFromPlayers = hiddenEntities.get(entity.getEntityId());
        return hiddenFromPlayers != null && hiddenFromPlayers.contains(player);
    }
    public static void cleanupPlayer(Player player) {
        hiddenEntities.values().forEach(players -> players.remove(player));
        hiddenEntities.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    public static void cleanupEntity(Entity entity) {
        hiddenEntities.remove(entity.getEntityId());
    }
    public static boolean isAvailable() {
        return packetEventsAvailable;
    }
}
