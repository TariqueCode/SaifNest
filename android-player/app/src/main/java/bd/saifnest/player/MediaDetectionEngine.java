package bd.saifnest.player;

import android.net.Uri;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SaifNest Media Detection Engine v2.
 *
 * A lightweight classifier designed for an Android WebView browser. It combines
 * URL path/query clues, MIME types, and DOM/network observation hints. It returns
 * candidates for the user to review; it does not bypass DRM, authentication, or
 * website access controls.
 */
public final class MediaDetectionEngine {
    public enum Kind { VIDEO, AUDIO, HLS, DASH, LIVE, UNKNOWN_MEDIA }

    public static final class Candidate {
        public final String url;
        public final String identity;
        public final Kind kind;
        public final String mimeType;
        public final String source;
        public final int confidence;
        public final String extension;

        private Candidate(String url, String identity, Kind kind, String mimeType,
                          String source, int confidence, String extension) {
            this.url = url;
            this.identity = identity;
            this.kind = kind;
            this.mimeType = mimeType;
            this.source = source;
            this.confidence = confidence;
            this.extension = extension;
        }

        public boolean isAdaptiveStream() {
            return kind == Kind.HLS || kind == Kind.DASH || kind == Kind.LIVE;
        }

        public boolean isDirectFile() {
            return kind == Kind.VIDEO || kind == Kind.AUDIO;
        }

        public boolean isVideo() {
            return kind == Kind.VIDEO || kind == Kind.HLS || kind == Kind.DASH
                    || kind == Kind.LIVE || kind == Kind.UNKNOWN_MEDIA;
        }

        public String displayType() {
            switch (kind) {
                case HLS: return "HLS stream";
                case DASH: return "DASH stream";
                case LIVE: return "Live/adaptive stream";
                case AUDIO: return "Audio";
                case VIDEO: return "Video";
                default: return "Media candidate";
            }
        }
    }

    private static final Pattern VIDEO_EXT = Pattern.compile(
            "(?i)\\.(mp4|m4v|webm|mov|mkv|avi|3gp|ts|m4s|mpeg|mpg|ogv|flv)(?:$|[?#])");
    private static final Pattern AUDIO_EXT = Pattern.compile(
            "(?i)\\.(mp3|m4a|aac|ogg|opus|wav|flac|oga|weba)(?:$|[?#])");
    private static final Pattern HLS_EXT = Pattern.compile("(?i)\\.m3u8(?:$|[?#])");
    private static final Pattern DASH_EXT = Pattern.compile("(?i)\\.mpd(?:$|[?#])");
    private static final Pattern EXTENSION = Pattern.compile("(?i)\\.([a-z0-9]{2,5})(?:$|[?#])");

    private MediaDetectionEngine() {}

    public static Candidate inspect(String rawUrl, String mimeType, String sourceHint) {
        if (rawUrl == null || rawUrl.trim().isEmpty() || rawUrl.length() > 8192) return null;
        String url = rawUrl.trim();
        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception ignored) {
            return null;
        }
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null) return null;

        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        String query = uri.getEncodedQuery() == null ? "" : uri.getEncodedQuery().toLowerCase(Locale.ROOT);
        String lower = (path + "?" + query).toLowerCase(Locale.ROOT);
        String mime = normalizeMime(mimeType);
        String hint = sourceHint == null ? "" : sourceHint.toLowerCase(Locale.ROOT);
        Kind kind = null;
        int confidence = 0;
        String extension = extensionOf(path);

        // Strongest evidence first: explicit streaming manifests and MIME types.
        if (HLS_EXT.matcher(path).find() || mime.contains("mpegurl") || mime.contains("vnd.apple.mpegurl")) {
            kind = Kind.HLS; confidence = 99; extension = "m3u8";
        } else if (DASH_EXT.matcher(path).find() || mime.contains("dash+xml")) {
            kind = Kind.DASH; confidence = 99; extension = "mpd";
        } else if (mime.startsWith("video/")) {
            kind = Kind.VIDEO; confidence = 97;
        } else if (mime.startsWith("audio/")) {
            kind = Kind.AUDIO; confidence = 97;
        } else if (VIDEO_EXT.matcher(path).find()) {
            kind = Kind.VIDEO; confidence = 96;
        } else if (AUDIO_EXT.matcher(path).find()) {
            kind = Kind.AUDIO; confidence = 96;
        }

        // Common media endpoints hide the file extension in a signed query URL.
        if (kind == null && (hasParam(query, "mime", "video") || hasParam(query, "type", "video")
                || hasParam(query, "contenttype", "video") || lower.contains("videoplayback")
                || lower.contains("/video/segment") || lower.contains("/videos/")
                || lower.contains("/media/video"))) {
            kind = Kind.VIDEO; confidence = 83;
        }
        if (kind == null && (lower.contains("mime=audio") || lower.contains("/audio/segment")
                || lower.contains("/media/audio"))) {
            kind = Kind.AUDIO; confidence = 80;
        }

        // Browser/player signals are valuable for signed URLs with opaque paths.
        if (kind == null && (hint.contains("playing video") || hint.contains("video element")
                || hint.contains("video source") || hint.contains("htmlvideoelement"))) {
            kind = Kind.VIDEO; confidence = 80;
        } else if (kind == null && (hint.contains("audio element") || hint.contains("audio source")
                || hint.contains("htmlaudioelement"))) {
            kind = Kind.AUDIO; confidence = 80;
        } else if (kind == null && (hint.contains("media resource") || hint.contains("network media")
                || hint.contains("fetch media"))) {
            kind = Kind.UNKNOWN_MEDIA; confidence = 55;
        }

        // Segment URLs often have no obvious extension; only accept them when the
        // browser has observed them as part of a media/player context.
        if (kind == null && (path.contains("/segment") || path.contains("/chunk")
                || path.endsWith("/manifest") || path.endsWith("/playlist"))) {
            if (hint.contains("video") || hint.contains("media") || hint.contains("stream")) {
                kind = Kind.UNKNOWN_MEDIA; confidence = 60;
            }
        }
        if (kind == null) return null;

        boolean liveHint = hint.contains("live") || query.contains("is_live=true")
                || query.contains("live=1") || path.contains("/live/");
        if (liveHint && (kind == Kind.HLS || kind == Kind.DASH || kind == Kind.UNKNOWN_MEDIA)) {
            kind = Kind.LIVE;
            confidence = Math.max(confidence, 82);
        }

        return new Candidate(url, identityFor(url), kind, mime,
                sourceHint == null ? "" : sourceHint, confidence, extension);
    }

    public static Candidate inspectResource(String url, String mimeType, String resourceType) {
        return inspect(url, mimeType, resourceType == null ? "network resource" : resourceType);
    }

    public static boolean looksLikeMedia(String url) {
        return inspect(url, "", "network resource") != null;
    }

    public static String identityFor(String rawUrl) {
        if (rawUrl == null) return "";
        try {
            Uri uri = Uri.parse(rawUrl);
            // Fragment is never sent to the server. Keep query parameters because
            // signed links and quality variants can be distinct resources.
            return new Uri.Builder().scheme(uri.getScheme()).encodedAuthority(uri.getEncodedAuthority())
                    .encodedPath(uri.getEncodedPath()).encodedQuery(uri.getEncodedQuery()).build().toString();
        } catch (Exception ignored) {
            int hash = rawUrl.indexOf('#');
            return hash >= 0 ? rawUrl.substring(0, hash) : rawUrl;
        }
    }

    private static String normalizeMime(String mimeType) {
        if (mimeType == null) return "";
        String mime = mimeType.toLowerCase(Locale.ROOT).trim();
        int semicolon = mime.indexOf(';');
        return semicolon >= 0 ? mime.substring(0, semicolon).trim() : mime;
    }

    private static String extensionOf(String path) {
        java.util.regex.Matcher matcher = EXTENSION.matcher(path == null ? "" : path);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : "";
    }

    private static boolean hasParam(String query, String name, String value) {
        if (query == null || query.isEmpty()) return false;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = decode(pair.substring(0, eq)).toLowerCase(Locale.ROOT);
            String val = decode(pair.substring(eq + 1)).toLowerCase(Locale.ROOT);
            if (key.equals(name) && (val.equals(value) || val.startsWith(value + "/"))) return true;
        }
        return false;
    }

    private static String decode(String value) {
        try { return URLDecoder.decode(value, StandardCharsets.UTF_8.name()); }
        catch (Exception ignored) { return value; }
    }
}
