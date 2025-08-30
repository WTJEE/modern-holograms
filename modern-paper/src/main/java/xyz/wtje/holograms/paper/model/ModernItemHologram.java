package xyz.wtje.holograms.paper.model;
import xyz.wtje.holograms.core.model.ItemHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.HashSet;
import java.util.Set;
public class ModernItemHologram extends ItemHologram {
    private ItemDisplay entity;
    private final Set<Player> viewers = new HashSet<>();
    public ModernItemHologram(String name, Location location, ItemStack item) {
        super(name, location, item);
    }
    @Override
    public void spawn() {
        if (entity != null) return;
        entity = location.getWorld().spawn(location, ItemDisplay.class);
        entity.setPersistent(false);
        updateDisplay();
    }
    @Override
    public void despawn() {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
        viewers.clear();
    }
    @Override
    public void update() {
        if (entity == null) return;
        updateDisplay();
    }
    private void updateDisplay() {
        entity.setItemStack(item);
        entity.setItemDisplayTransform(convertTransform(transform));
        switch (billboard) {
            case FIXED -> entity.setBillboard(Display.Billboard.FIXED);
            case VERTICAL -> entity.setBillboard(Display.Billboard.VERTICAL);
            case HORIZONTAL -> entity.setBillboard(Display.Billboard.HORIZONTAL);
            default -> entity.setBillboard(Display.Billboard.CENTER);
        }
        Vector3f scaleVec = new Vector3f((float)scale.getX(), (float)scale.getY(), (float)scale.getZ());
        Quaternionf rotationQuat = new Quaternionf()
            .rotateXYZ((float)Math.toRadians(rotation.getX()), 
                      (float)Math.toRadians(rotation.getY()), 
                      (float)Math.toRadians(rotation.getZ()));
        Transformation transform = new Transformation(
            new Vector3f(0, 0, 0), 
            rotationQuat,          
            scaleVec,              
            rotationQuat           
        );
        entity.setTransformation(transform);
    }
    @Override
    public boolean isVisible(Player player) {
        return viewers.contains(player);
    }
    @Override
    public void teleport(Location location) {
        this.location = location;
        if (entity != null) {
            entity.teleport(location);
        }
    }
    public void showTo(Player player) {
        viewers.add(player);
    }
    public void hideFrom(Player player) {
        viewers.remove(player);
    }
    private ItemDisplay.ItemDisplayTransform convertTransform(Transform transform) {
        return switch (transform) {
            case FIXED -> ItemDisplay.ItemDisplayTransform.FIXED;
            case GROUND -> ItemDisplay.ItemDisplayTransform.GROUND;
            case GUI -> ItemDisplay.ItemDisplayTransform.GUI;
            case HEAD -> ItemDisplay.ItemDisplayTransform.HEAD;
            case THIRDPERSON_LEFTHAND -> ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND;
            case THIRDPERSON_RIGHTHAND -> ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND;
            case FIRSTPERSON_LEFTHAND -> ItemDisplay.ItemDisplayTransform.FIRSTPERSON_LEFTHAND;
            case FIRSTPERSON_RIGHTHAND -> ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND;
        };
    }
}
