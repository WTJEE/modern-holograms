package xyz.wtje.holograms.paper.adapter;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
import xyz.wtje.holograms.core.model.Hologram;
import xyz.wtje.holograms.paper.model.ModernTextHologram;
import xyz.wtje.holograms.paper.model.ModernItemHologram;
import xyz.wtje.holograms.paper.model.ModernBlockHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import java.util.List;
public class DisplayEntityAdapter implements VersionAdapter {
    @Override
    public String getVersion() {
        return "Display Entities (1.19.4+)";
    }
    @Override
    public boolean supportsDisplayEntities() {
        return true;
    }
    @Override
    public Hologram createTextHologram(String name, Location location, List<Component> lines) {
        return new ModernTextHologram(name, location, lines);
    }
    @Override
    public Hologram createItemHologram(String name, Location location, ItemStack item) {
        return new ModernItemHologram(name, location, item);
    }
    @Override
    public Hologram createBlockHologram(String name, Location location, BlockData blockData) {
        return new ModernBlockHologram(name, location, blockData);
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
        if (hologram instanceof xyz.wtje.holograms.paper.model.ModernTextHologram textHolo) {
            textHolo.showTo(player);
        } else if (hologram instanceof xyz.wtje.holograms.paper.model.ModernItemHologram itemHolo) {
            itemHolo.showTo(player);
        } else if (hologram instanceof xyz.wtje.holograms.paper.model.ModernBlockHologram blockHolo) {
            blockHolo.showTo(player);
        }
    }
    private void hideFromPlayer(Hologram hologram, Player player) {
        if (hologram instanceof xyz.wtje.holograms.paper.model.ModernTextHologram textHolo) {
            textHolo.hideFrom(player);
        } else if (hologram instanceof xyz.wtje.holograms.paper.model.ModernItemHologram itemHolo) {
            itemHolo.hideFrom(player);
        } else if (hologram instanceof xyz.wtje.holograms.paper.model.ModernBlockHologram blockHolo) {
            blockHolo.hideFrom(player);
        }
    }
}
