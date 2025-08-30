package xyz.wtje.holograms.paper.util;
import org.bukkit.entity.Player;
public final class ProtocolUtils {
    private static boolean viaVersionChecked = false;
    private static boolean viaVersionAvailable = false;
    private ProtocolUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    private static boolean isViaVersionAvailable() {
        if (!viaVersionChecked) {
            try {
                Class.forName("com.viaversion.viaversion.api.Via");
                viaVersionAvailable = true;
            } catch (ClassNotFoundException e) {
                viaVersionAvailable = false;
            }
            viaVersionChecked = true;
        }
        return viaVersionAvailable;
    }
    private static int getClientProtocolVersion(Player player) {
        try {
            if (isViaVersionAvailable()) {
                int clientVersion = com.viaversion.viaversion.api.Via.getAPI().getPlayerVersion(player.getUniqueId());
                return clientVersion;
            }
        } catch (Exception e) {
        }
        try {
            int serverVersion = player.getProtocolVersion();
            return serverVersion;
        } catch (NoSuchMethodError e) {
            return -1;
        }
    }
    public static boolean supportsDisplayEntities(Player player) {
        int clientProtocolVersion = getClientProtocolVersion(player);
        if (clientProtocolVersion == -1) {
            return true;
        }
        int minVersionForDisplayEntities = 762; 
        try {
            if (isViaVersionAvailable()) {
                minVersionForDisplayEntities = com.viaversion.viaversion.api.protocol.version.ProtocolVersion.v1_19_4.getVersion();
            }
        } catch (Exception e) {
        }
        boolean supports = clientProtocolVersion >= minVersionForDisplayEntities;
        return supports;
    }
    public static int getProtocolVersion(Player player) {
        return getClientProtocolVersion(player);
    }
}
