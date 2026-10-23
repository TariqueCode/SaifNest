package bd.saifnest.player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Session media registry backed by SaifNest's media detection engine. */
public final class SharedMediaStore {
    public static final class MediaItem {
        public final String url;
        public final String source;
        public final String pageUrl;
        public final MediaDetectionEngine.Candidate candidate;

        public MediaItem(String url, String source, String pageUrl,
                         MediaDetectionEngine.Candidate candidate) {
            this.url = url;
            this.source = source;
            this.pageUrl = pageUrl;
            this.candidate = candidate;
        }
    }

    // Use normalized identity so URL fragments do not create duplicate entries.
    private static final Map<String, MediaItem> ITEMS = new LinkedHashMap<>();

    private SharedMediaStore() {}

    public static synchronized boolean add(String url, String source, String pageUrl) {
        MediaDetectionEngine.Candidate candidate = MediaDetectionEngine.inspect(url, "", source);
        if (candidate == null) return false;
        String key = candidate.identity;
        if (key.isEmpty() || ITEMS.containsKey(key)) return false;
        ITEMS.put(key, new MediaItem(candidate.url, source, pageUrl, candidate));
        return true;
    }

    public static synchronized List<MediaItem> all() {
        return new ArrayList<>(ITEMS.values());
    }

    public static synchronized List<MediaItem> forPage(String pageUrl) {
        List<MediaItem> matches = new ArrayList<>();
        if (pageUrl == null) return matches;
        for (MediaItem item : ITEMS.values()) {
            if (pageUrl.equals(item.pageUrl)) matches.add(item);
        }
        return matches;
    }

    public static synchronized int count() {
        return ITEMS.size();
    }

    public static synchronized void clear() {
        ITEMS.clear();
    }
}
