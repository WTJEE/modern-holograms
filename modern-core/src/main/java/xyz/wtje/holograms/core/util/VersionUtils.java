package xyz.wtje.holograms.core.util;
import org.bukkit.Bukkit;
public class VersionUtils {
    private static final String VERSION = Bukkit.getMinecraftVersion();
    private static final int[] VERSION_NUMBERS = parseVersion(VERSION);
    static {
    }
    public static boolean isDisplayEntitiesSupported() {
        return isVersionAtLeast(1, 19, 4);
    }
    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        if (VERSION_NUMBERS[0] > major) return true;
        if (VERSION_NUMBERS[0] < major) return false;
        if (VERSION_NUMBERS[1] > minor) return true;
        if (VERSION_NUMBERS[1] < minor) return false;
        return VERSION_NUMBERS[2] >= patch;
    }
    private static int[] parseVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new int[]{major, minor, patch};
        } catch (Exception e) {
            return new int[]{1, 20, 0};
        }
    }
    public static String getVersion() {
        return VERSION;
    }
    public static int[] getVersionNumbers() {
        return VERSION_NUMBERS.clone();
    }
}
