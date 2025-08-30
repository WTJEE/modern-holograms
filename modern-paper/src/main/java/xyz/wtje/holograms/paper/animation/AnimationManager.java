package xyz.wtje.holograms.paper.animation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
public class AnimationManager {
    private final Plugin plugin;
    private final Map<String, Animation> animations = new ConcurrentHashMap<>();
    private final Map<String, Integer> animationStates = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    private final Set<AnimationRefreshCallback> refreshCallbacks = new CopyOnWriteArraySet<>();
    private BukkitTask refreshTask;
    public AnimationManager(Plugin plugin) {
        this.plugin = plugin;
        loadAnimations();
        startRefreshTask();
    }
    public interface AnimationRefreshCallback {
        void onAnimationRefresh();
    }
    public void addRefreshCallback(AnimationRefreshCallback callback) {
        refreshCallbacks.add(callback);
    }
    public void removeRefreshCallback(AnimationRefreshCallback callback) {
        refreshCallbacks.remove(callback);
    }
    private void startRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            boolean anyChanged = false;
            long currentTime = System.currentTimeMillis();
            for (String animationName : animations.keySet()) {
                Animation animation = animations.get(animationName);
                Long lastUpdate = lastUpdateTimes.get(animationName);
                if (lastUpdate == null) lastUpdate = currentTime;
                long timeSinceUpdate = currentTime - lastUpdate;
                boolean shouldUpdate = timeSinceUpdate >= animation.getChangeInterval();
                if (shouldUpdate) {
                    int currentState = animationStates.getOrDefault(animationName, 0);
                    int nextState = (currentState + 1) % animation.getTexts().size();
                    animationStates.put(animationName, nextState);
                    lastUpdateTimes.put(animationName, currentTime);
                    anyChanged = true;
                }
            }
            if (anyChanged) {
                for (AnimationRefreshCallback callback : refreshCallbacks) {
                    try {
                        callback.onAnimationRefresh();
                    } catch (Exception e) {
                        System.err.println("AnimationManager: Error in refresh callback: " + e.getMessage());
                    }
                }
            }
        }, 5L, 5L); 
    }
    public void loadAnimations() {
        animations.clear();
        animationStates.clear();
        lastUpdateTimes.clear();
        File animationFile = new File(plugin.getDataFolder(), "animation.yml");
        if (!animationFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                InputStream defaultConfig = plugin.getResource("animation.yml");
                if (defaultConfig != null) {
                    Files.copy(defaultConfig, animationFile.toPath());
                    System.out.println("AnimationManager: Created default animation.yml");
                }
            } catch (IOException e) {
                System.err.println("AnimationManager: Failed to create animation.yml: " + e.getMessage());
                return;
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(animationFile);
        ConfigurationSection animationsSection = config.getConfigurationSection("animations");
        if (animationsSection == null) {
            System.out.println("AnimationManager: No animations section found in animation.yml");
            return;
        }
        for (String animationName : animationsSection.getKeys(false)) {
            ConfigurationSection animSection = animationsSection.getConfigurationSection(animationName);
            if (animSection == null) continue;
            int changeInterval = animSection.getInt("change-interval", 1000);
            List<String> texts = animSection.getStringList("texts");
            if (texts.isEmpty()) {
                System.err.println("AnimationManager: Animation '" + animationName + "' has no texts defined");
                continue;
            }
            Animation animation = new Animation(animationName, changeInterval, texts);
            animations.put(animationName, animation);
            animationStates.put(animationName, 0); 
            lastUpdateTimes.put(animationName, System.currentTimeMillis());
            System.out.println("AnimationManager: Loaded animation '" + animationName + "' with " + texts.size() + " frames, interval: " + changeInterval + "ms");
        }
        System.out.println("AnimationManager: Loaded " + animations.size() + " animations");
    }
    public String getCurrentText(String animationName) {
        Animation animation = animations.get(animationName);
        if (animation == null) {
            return "%animation:" + animationName + "%"; 
        }
        long currentTime = System.currentTimeMillis();
        String animationKey = animationName;
        Long lastUpdate = lastUpdateTimes.get(animationKey);
        if (lastUpdate == null) lastUpdate = currentTime;
        if (currentTime - lastUpdate >= animation.getChangeInterval()) {
            int currentState = animationStates.getOrDefault(animationKey, 0);
            int nextState = (currentState + 1) % animation.getTexts().size();
            animationStates.put(animationKey, nextState);
            lastUpdateTimes.put(animationKey, currentTime);
        }
        int currentState = animationStates.getOrDefault(animationKey, 0);
        return animation.getTexts().get(currentState);
    }
    public boolean hasAnimation(String name) {
        return animations.containsKey(name);
    }
    public String[] getAnimationNames() {
        return animations.keySet().toArray(new String[0]);
    }
    public void reload() {
        System.out.println("AnimationManager: Reloading animations...");
        loadAnimations();
        startRefreshTask(); 
    }
    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        refreshCallbacks.clear();
    }
    public static class Animation {
        private final String name;
        private final int changeInterval;
        private final List<String> texts;
        public Animation(String name, int changeInterval, List<String> texts) {
            this.name = name;
            this.changeInterval = changeInterval;
            this.texts = texts;
        }
        public String getName() { return name; }
        public int getChangeInterval() { return changeInterval; }
        public List<String> getTexts() { return texts; }
    }
}
