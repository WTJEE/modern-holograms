package xyz.wtje.holograms.paper.adapter;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
import xyz.wtje.holograms.core.model.Hologram;
import xyz.wtje.holograms.paper.model.LegacyTextHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import java.util.List;
public class LegacyAdapter implements VersionAdapter {
    @Override
    public String getVersion() {
        return "Legacy Armor Stands (<1.19.4)";
    }
    @Override
    public boolean supportsDisplayEntities() {
        return false;
    }
    @Override
    public Hologram createTextHologram(String name, Location location, List<Component> lines) {
        return new LegacyTextHologram(name, location, lines);
    }
    @Override
    public Hologram createItemHologram(String name, Location location, ItemStack item) {
        throw new UnsupportedOperationException("Item holograms not supported in legacy mode");
    }
    @Override
    public Hologram createBlockHologram(String name, Location location, BlockData blockData) {
        throw new UnsupportedOperationException("Block holograms not supported in legacy mode");
    }
    @Override
    public void updateVisibility(Hologram hologram, Player player, boolean visible) {
        if (visible && !hologram.isVisible(player)) {
            showToPlayer(hologram, player);
        } else if (!visible && hologram.isVisible(player)) {
            hideFromPlayer(hologram, player);
        }
    }
    private void showToPlayer(Hologram hologram, Player player) {
    }
    private void hideFromPlayer(Hologram hologram, Player player) {
    }
}
