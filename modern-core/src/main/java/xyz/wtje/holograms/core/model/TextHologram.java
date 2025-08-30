package xyz.wtje.holograms.core.model;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Color;
import java.util.List;
import java.util.ArrayList;
public abstract class TextHologram extends Hologram {
    protected List<Component> lines;
    protected boolean shadow;
    protected boolean seeThrough;
    protected Color backgroundColor;
    protected int lineWidth;
    protected TextAlignment alignment;
    protected float opacity;
    public TextHologram(String name, Location location, List<Component> lines) {
        super(name, location, HologramType.TEXT);
        this.lines = new ArrayList<>(lines);
        this.shadow = false;
        this.seeThrough = false;
        this.backgroundColor = Color.fromARGB(0, 0, 0, 0);
        this.lineWidth = 200;
        this.alignment = TextAlignment.CENTER;
        this.opacity = 1.0f;
    }
    public List<Component> getLines() { return lines; }
    public boolean hasShadow() { return shadow; }
    public boolean isSeeThrough() { return seeThrough; }
    public Color getBackgroundColor() { return backgroundColor; }
    public int getLineWidth() { return lineWidth; }
    public TextAlignment getAlignment() { return alignment; }
    public float getOpacity() { return opacity; }
    public void setLines(List<Component> lines) { this.lines = new ArrayList<>(lines); }
    public void setShadow(boolean shadow) { this.shadow = shadow; }
    public void setSeeThrough(boolean seeThrough) { this.seeThrough = seeThrough; }
    public void setBackgroundColor(Color color) { this.backgroundColor = color; }
    public void setLineWidth(int width) { this.lineWidth = width; }
    public void setAlignment(TextAlignment alignment) { this.alignment = alignment; }
    public void setOpacity(float opacity) { this.opacity = opacity; }
    public void addLine(Component line) {
        lines.add(line);
    }
    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
        }
    }
    public void setLine(int index, Component line) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, line);
        }
    }
    public enum TextAlignment {
        LEFT(0),
        CENTER(1), 
        RIGHT(2);
        private final int value;
        TextAlignment(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
}
