package xyz.wtje.holograms.paper.model;
import xyz.wtje.holograms.core.model.BlockHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.HashSet;
import java.util.Set;
public class ModernBlockHologram extends BlockHologram {
    private BlockDisplay entity;
    private final Set<Player> viewers = new HashSet<>();
    public ModernBlockHologram(String name, Location location, BlockData blockData) {
        super(name, location, blockData);
    }
    @Override
    public void spawn() {
        if (entity != null) return;
        entity = location.getWorld().spawn(location, BlockDisplay.class);
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
        entity.setBlock(blockData);
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
}
