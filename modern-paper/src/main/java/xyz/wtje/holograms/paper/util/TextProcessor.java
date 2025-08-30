package xyz.wtje.holograms.paper.util;
import xyz.wtje.holograms.paper.integration.PlaceholderIntegration;
import xyz.wtje.holograms.paper.animation.AnimationManager;
import org.bukkit.entity.Player;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class TextProcessor {
    private static final Pattern ANIMATION_PATTERN = Pattern.compile("%animation:([a-zA-Z0-9_-]+)%");
    private static AnimationManager animationManager;
    public static void initialize(AnimationManager manager) {
        animationManager = manager;
    }
    public static String processText(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String processedText = processAnimations(text);
        if (PlaceholderIntegration.isEnabled() && player != null) {
            processedText = PlaceholderIntegration.parsePlaceholders(player, processedText);
        }
        processedText = convertLegacyColors(processedText);
        return processedText;
    }
    public static boolean hasAnimations(String text) {
        if (text == null || animationManager == null) {
            return false;
        }
        return ANIMATION_PATTERN.matcher(text).find();
    }
    public static boolean hasPlaceholders(String text) {
        if (text == null || !PlaceholderIntegration.isEnabled()) {
            return false;
        }
        return text.contains("%") && !text.matches(".*%animation:[a-zA-Z0-9_-]+%.*");
    }
    public static boolean isDynamic(String text) {
        return hasAnimations(text) || hasPlaceholders(text);
    }
    private static String processAnimations(String text) {
        if (animationManager == null) {
            return text;
        }
        Matcher matcher = ANIMATION_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String animationName = matcher.group(1);
            String animationText = animationManager.getCurrentText(animationName);
            animationText = Matcher.quoteReplacement(animationText);
            matcher.appendReplacement(result, animationText);
        }
        matcher.appendTail(result);
        return result.toString();
    }
    private static String convertLegacyColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (text.contains("<color:") || text.contains("<#") || text.contains("</") || text.contains("<bold>") || text.contains("<italic>")) {
            return text;
        }
        String originalText = text;
        Pattern hexPattern = Pattern.compile("&#([0-9a-fA-F]{6})");
        Matcher hexMatcher = hexPattern.matcher(text);
        StringBuffer hexResult = new StringBuffer();
        while (hexMatcher.find()) {
            String hexCode = hexMatcher.group(1);
            hexMatcher.appendReplacement(hexResult, "<color:#" + hexCode + ">");
        }
        hexMatcher.appendTail(hexResult);
        text = hexResult.toString();
    text = text.replaceAll("(?<!&)(?<!<color:)#([0-9a-fA-F]{6})&l", "<color:#$1><bold>");
    text = text.replaceAll("(?<!&)(?<!<color:)#([0-9a-fA-F]{6})&n", "<color:#$1><underlined>");
    text = text.replaceAll("(?<!&)(?<!<color:)#([0-9a-fA-F]{6})&o", "<color:#$1><italic>");
    text = text.replaceAll("(?<!&)(?<!<color:)#([0-9a-fA-F]{6})&m", "<color:#$1><strikethrough>");
    text = text.replaceAll("(?<!&)(?<!<color:)#([0-9a-fA-F]{6})&k", "<color:#$1><obfuscated>");
    text = text.replaceAll("(?<!&)(?<!<color:)#([0-9a-fA-F]{6})&r", "<color:#$1><reset>");
    text = text.replaceAll("(?<!&)(?<!<color:)#([0-9a-fA-F]{6})", "<color:#$1>");
        text = text.replaceAll("&l", "<bold>");
        text = text.replaceAll("&n", "<underlined>");
        text = text.replaceAll("&o", "<italic>");
        text = text.replaceAll("&m", "<strikethrough>");
        text = text.replaceAll("&k", "<obfuscated>");
        text = text.replaceAll("&r", "<reset>");
        text = text.replaceAll("&0", "<black>");
        text = text.replaceAll("&1", "<dark_blue>");
        text = text.replaceAll("&2", "<dark_green>");
        text = text.replaceAll("&3", "<dark_aqua>");
        text = text.replaceAll("&4", "<dark_red>");
        text = text.replaceAll("&5", "<dark_purple>");
        text = text.replaceAll("&6", "<gold>");
        text = text.replaceAll("&7", "<gray>");
        text = text.replaceAll("&8", "<dark_gray>");
        text = text.replaceAll("&9", "<blue>");
        text = text.replaceAll("&a", "<green>");
        text = text.replaceAll("&b", "<aqua>");
        text = text.replaceAll("&c", "<red>");
        text = text.replaceAll("&d", "<light_purple>");
        text = text.replaceAll("&e", "<yellow>");
        text = text.replaceAll("&f", "<white>");
        text = text.replaceAll("§0", "<black>");
        text = text.replaceAll("§1", "<dark_blue>");
        text = text.replaceAll("§2", "<dark_green>");
        text = text.replaceAll("§3", "<dark_aqua>");
        text = text.replaceAll("§4", "<dark_red>");
        text = text.replaceAll("§5", "<dark_purple>");
        text = text.replaceAll("§6", "<gold>");
        text = text.replaceAll("§7", "<gray>");
        text = text.replaceAll("§8", "<dark_gray>");
        text = text.replaceAll("§9", "<blue>");
        text = text.replaceAll("§a", "<green>");
        text = text.replaceAll("§b", "<aqua>");
        text = text.replaceAll("§c", "<red>");
        text = text.replaceAll("§d", "<light_purple>");
        text = text.replaceAll("§e", "<yellow>");
        text = text.replaceAll("§f", "<white>");
        text = text.replaceAll("§l", "<bold>");
        text = text.replaceAll("§n", "<underlined>");
        text = text.replaceAll("§o", "<italic>");
        text = text.replaceAll("§m", "<strikethrough>");
        text = text.replaceAll("§k", "<obfuscated>");
        text = text.replaceAll("§r", "<reset>");
        return text;
    }
}
