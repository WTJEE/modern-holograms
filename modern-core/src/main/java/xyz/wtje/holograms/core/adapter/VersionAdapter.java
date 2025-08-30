package xyz.wtje.holograms.core.adapter;
import xyz.wtje.holograms.core.model.Hologram;
import org.bukkit.entity.Player;
public interface VersionAdapter {
    String getVersion();
    boolean supportsDisplayEntities();
    Hologram createTextHologram(String name, org.bukkit.Location location, java.util.List<net.kyori.adventure.text.Component> lines);
    Hologram createItemHologram(String name, org.bukkit.Location location, org.bukkit.inventory.ItemStack item);
    Hologram createBlockHologram(String name, org.bukkit.Location location, org.bukkit.block.data.BlockData blockData);
    void updateVisibility(Hologram hologram, Player player, boolean visible);
}
