package xyz.wtje.holograms.core.model;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.List;
import java.util.UUID;
public abstract class Hologram {
    protected String name;
    protected Location location;
    protected HologramType type;
    protected double viewDistance;
    protected List<UUID> whitelist;
    protected List<UUID> blacklist;
    protected boolean useWhitelist;
    protected boolean useBlacklist;
    protected Vector scale;
    protected Vector rotation;
    protected Billboard billboard;
    public Hologram(String name, Location location, HologramType type) {
        this.name = name;
        this.location = location;
        this.type = type;
        this.viewDistance = 32.0;
        this.useWhitelist = false;
        this.useBlacklist = false;
        this.scale = new Vector(1.0, 1.0, 1.0);
        this.rotation = new Vector(0.0, 0.0, 0.0);
        this.billboard = Billboard.CENTER;
    }
    public abstract void spawn();
    public abstract void despawn();
    public abstract void update();
    public abstract boolean isVisible(Player player);
    public abstract void teleport(Location location);
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public HologramType getType() { return type; }
    public double getViewDistance() { return viewDistance; }
    public Vector getScale() { return scale; }
    public Vector getRotation() { return rotation; }
    public Billboard getBillboard() { return billboard; }
    public void setViewDistance(double distance) { this.viewDistance = distance; }
    public void setLocation(Location location) { this.location = location; }
    public void setScale(Vector scale) { this.scale = scale; }
    public void setRotation(Vector rotation) { this.rotation = rotation; }
    public void setBillboard(Billboard billboard) { this.billboard = billboard; }
    public boolean canSee(Player player) {
        if (useWhitelist && !whitelist.contains(player.getUniqueId())) {
            return false;
        }
        if (useBlacklist && blacklist.contains(player.getUniqueId())) {
            return false;
        }
        if (location.getWorld() == null || player.getLocation().getWorld() == null) {
            return false;
        }
        if (!location.getWorld().equals(player.getLocation().getWorld())) {
            return false;
        }
        double distance = player.getLocation().distance(location);
        boolean canSee = distance <= viewDistance;
        return canSee;
    }
    public enum Billboard {
        FIXED,
        VERTICAL, 
        HORIZONTAL,
        CENTER
    }
}
