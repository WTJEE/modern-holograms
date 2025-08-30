package xyz.wtje.holograms.core.model;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
public abstract class ItemHologram extends Hologram {
    protected ItemStack item;
    protected Transform transform;
    public ItemHologram(String name, Location location, ItemStack item) {
        super(name, location, HologramType.ITEM);
        this.item = item;
        this.transform = Transform.FIXED;
    }
    public ItemStack getItem() { return item; }
    public Transform getTransform() { return transform; }
    public void setItem(ItemStack item) { this.item = item; }
    public void setTransform(Transform transform) { this.transform = transform; }
    public enum Transform {
        FIXED,
        GROUND,
        GUI,
        HEAD,
        THIRDPERSON_LEFTHAND,
        THIRDPERSON_RIGHTHAND,
        FIRSTPERSON_LEFTHAND,
        FIRSTPERSON_RIGHTHAND
    }
}
