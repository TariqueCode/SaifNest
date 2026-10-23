package bd.saifnest.player;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small persistent browser data store for bookmarks and browsing history. */
public final class BrowserDataStore {
    private static final String PREFS = "saifnest_browser_data";
    private static final String HISTORY = "history";
    private static final String BOOKMARKS = "bookmarks";
    private static final int MAX_HISTORY = 300;
    private static final int MAX_BOOKMARKS = 500;

    private BrowserDataStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static JSONArray readArray(Context context, String key) {
        try { return new JSONArray(prefs(context).getString(key, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    public static synchronized void addHistory(Context context, String title, String url) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) return;
        JSONArray old = readArray(context, HISTORY);
        JSONArray next = new JSONArray();
        JSONObject item = new JSONObject();
        try {
            item.put("title", title == null || title.trim().isEmpty() ? url : title.trim());
            item.put("url", url);
            item.put("visitedAt", System.currentTimeMillis());
            next.put(item);
            for (int i = 0; i < old.length() && next.length() < MAX_HISTORY; i++) {
                JSONObject previous = old.optJSONObject(i);
                if (previous == null || url.equals(previous.optString("url"))) continue;
                next.put(previous);
            }
            prefs(context).edit().putString(HISTORY, next.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized List<Entry> getHistory(Context context) {
        JSONArray array = readArray(context, HISTORY);
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) result.add(new Entry(item.optString("title"), item.optString("url"), item.optLong("visitedAt")));
        }
        return result;
    }

    public static synchronized void clearHistory(Context context) {
        prefs(context).edit().remove(HISTORY).apply();
    }

    public static synchronized boolean isBookmarked(Context context, String url) {
        JSONArray array = readArray(context, BOOKMARKS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null && url.equals(item.optString("url"))) return true;
        }
        return false;
    }

    public static synchronized boolean toggleBookmark(Context context, String title, String url) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) return false;
        JSONArray old = readArray(context, BOOKMARKS);
        JSONArray next = new JSONArray();
        boolean removed = false;
        try {
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item == null) continue;
                if (url.equals(item.optString("url"))) { removed = true; continue; }
                next.put(item);
            }
            if (!removed && next.length() < MAX_BOOKMARKS) {
                JSONObject item = new JSONObject();
                item.put("title", title == null || title.trim().isEmpty() ? url : title.trim());
                item.put("url", url);
                item.put("savedAt", System.currentTimeMillis());
                next.put(0, item);
            }
            prefs(context).edit().putString(BOOKMARKS, next.toString()).apply();
        } catch (Exception ignored) {}
        return !removed;
    }

    public static synchronized List<Entry> getBookmarks(Context context) {
        JSONArray array = readArray(context, BOOKMARKS);
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) result.add(new Entry(item.optString("title"), item.optString("url"), item.optLong("savedAt")));
        }
        return result;
    }

    public static final class Entry {
        public final String title;
        public final String url;
        public final long timestamp;
        Entry(String title, String url, long timestamp) {
            this.title = title; this.url = url; this.timestamp = timestamp;
        }
    }
}
