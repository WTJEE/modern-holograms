package xyz.wtje.holograms.paper.adapter;
import xyz.wtje.holograms.core.adapter.VersionAdapter;
public class AdapterFactory {
    public static VersionAdapter createAdapter() {
        return new HybridAdapter();
    }
}
