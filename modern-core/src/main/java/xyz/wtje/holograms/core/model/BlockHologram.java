package xyz.wtje.holograms.core.model;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
public abstract class BlockHologram extends Hologram {
    protected BlockData blockData;
    public BlockHologram(String name, Location location, BlockData blockData) {
        super(name, location, HologramType.BLOCK);
        this.blockData = blockData;
    }
    public BlockData getBlockData() { return blockData; }
    public void setBlockData(BlockData blockData) { this.blockData = blockData; }
}
