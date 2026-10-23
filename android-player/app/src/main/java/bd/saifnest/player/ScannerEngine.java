package bd.saifnest.player;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * First native site inventory engine. Crawls same-host HTML pages and extracts
 * visible media/link candidates. It deliberately does not bypass authentication,
 * DRM, paywalls, CAPTCHA, or site access controls.
 */
public final class ScannerEngine {
    public interface Listener {
        void onPage(String url, int pagesVisited, int queuedPages);
        void onCandidate(String pageUrl, String mediaUrl, String kind);
        void onError(String url, String message);
        void onFinished(int pagesVisited, int candidateCount);
    }

    private static final Pattern MEDIA = Pattern.compile("(?i)\\.(mp4|m4v|webm|mov|mkv|mp3|m4a|aac|ogg|opus|m3u8|mpd)(?:$|[?#])");
    private static final Pattern PAGE = Pattern.compile("(?i)/(watch|video|videos|media|player)(/|$)");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true).followSslRedirects(true)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS).build();
    private volatile boolean cancelled;

    public void cancel() { cancelled = true; }

    public void scan(String startUrl, int maxPages, Listener listener) {
        cancelled = false;
        Set<String> visited = new LinkedHashSet<>();
        Set<String> queued = new LinkedHashSet<>();
        Set<String> candidates = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        String start = normalize(startUrl);
        if (start == null) {
            listener.onError(startUrl, "Enter a valid HTTP(S) website URL.");
            listener.onFinished(0, 0);
            return;
        }
        String host = host(start);
        queue.add(start);
        queued.add(start);

        while (!queue.isEmpty() && visited.size() < Math.max(1, Math.min(maxPages, 1000)) && !cancelled) {
            String pageUrl = queue.removeFirst();
            if (!visited.add(pageUrl)) continue;
            listener.onPage(pageUrl, visited.size(), queue.size());
            Request request = new Request.Builder().url(pageUrl)
                    .header("User-Agent", "SaifNest/0.2 (Android; media discovery)")
                    .header("Accept", "text/html,application/xhtml+xml,video/*,audio/*,*/*;q=0.8")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    listener.onError(pageUrl, "HTTP " + response.code());
                    continue;
                }
                String type = response.header("Content-Type", "");
                String finalUrl = response.request().url().toString();
                if (type.toLowerCase().contains("video/") || type.toLowerCase().contains("audio/")
                        || MEDIA.matcher(finalUrl).find()) {
                    emit(candidates, listener, pageUrl, finalUrl, type.isEmpty() ? "media response" : type);
                    continue;
                }
                if (!type.toLowerCase().contains("html") && !type.isEmpty()) continue;
                String html = response.body().string();
                Document doc = Jsoup.parse(html, finalUrl);

                for (Element el : doc.select("video[src], video source[src], audio[src], audio source[src], source[src], a[href], iframe[src], embed[src]")) {
                    String raw = el.hasAttr("src") ? el.attr("src") : el.attr("href");
                    String resolved = normalize(resolve(finalUrl, raw));
                    if (resolved == null) continue;
                    String tag = el.tagName().toLowerCase();
                    String mime = el.attr("type");
                    if (MEDIA.matcher(resolved).find() || mime.startsWith("video/") || mime.startsWith("audio/")) {
                        emit(candidates, listener, pageUrl, resolved, mime.isEmpty() ? tag : mime);
                    } else if (host.equals(host(resolved)) && isPageLink(resolved) && !visited.contains(resolved)
                            && queued.add(resolved) && queue.size() < 2000) {
                        queue.addLast(resolved);
                    }
                }
                for (Element el : doc.select("meta[property=og:video], meta[property=og:video:url], meta[name=twitter:player:stream]")) {
                    String resolved = normalize(resolve(finalUrl, el.attr("content")));
                    if (resolved != null) emit(candidates, listener, pageUrl, resolved, "social media metadata");
                }
            } catch (Exception e) {
                listener.onError(pageUrl, e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()));
            }
        }
        listener.onFinished(visited.size(), candidates.size());
    }

    private static void emit(Set<String> seen, Listener listener, String page, String media, String kind) {
        if (seen.add(media)) listener.onCandidate(page, media, kind);
    }

    private static boolean isPageLink(String url) {
        String path = URI.create(url).getPath();
        return path != null && (PAGE.matcher(path).find() || path.endsWith("/") || path.endsWith(".html") || path.endsWith(".php"));
    }

    private static String resolve(String base, String raw) {
        try { if (raw == null || raw.trim().isEmpty()) return null; return URI.create(base).resolve(raw.trim()).toString(); }
        catch (Exception ignored) { return null; }
    }

    private static String normalize(String raw) {
        try {
            URI u = URI.create(raw == null ? "" : raw.trim());
            String scheme = u.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || u.getHost() == null) return null;
            return new URI(scheme.toLowerCase(), u.getUserInfo(), u.getHost().toLowerCase(), u.getPort(), u.getPath(), u.getQuery(), null).toString();
        } catch (Exception ignored) { return null; }
    }

    private static String host(String url) {
        try { return URI.create(url).getHost().toLowerCase(); } catch (Exception e) { return ""; }
    }
}
