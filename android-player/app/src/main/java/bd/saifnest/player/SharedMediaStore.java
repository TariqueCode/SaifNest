package bd.saifnest.player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory media candidates for the current browser session. */
public final class SharedMediaStore {
    public static final class MediaItem {
        public final String url;
        public final String source;
        public final String pageUrl;
        public MediaItem(String url, String source, String pageUrl) {
            this.url = url;
            this.source = source;
            this.pageUrl = pageUrl;
        }
    }
    private static final Map<String, MediaItem> ITEMS = new LinkedHashMap<>();
    private SharedMediaStore() {}
    public static synchronized boolean add(String url, String source, String pageUrl) {
        if (url == null || url.trim().isEmpty() || ITEMS.containsKey(url)) return false;
        ITEMS.put(url, new MediaItem(url, source, pageUrl));
        return true;
    }
    public static synchronized List<MediaItem> all() {
        return new ArrayList<>(ITEMS.values());
    }
    public static synchronized void clear() { ITEMS.clear(); }
}
