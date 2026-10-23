package bd.saifnest.player;

import android.net.Uri;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SaifNest Media Discovery Engine.
 *
 * Combines URL, MIME-type, and browser-observation hints into a normalized
 * media candidate. It deliberately does not attempt to defeat DRM or access
 * controls; "candidate" means a source worth presenting for user review.
 */
public final class MediaDetectionEngine {
    public enum Kind { VIDEO, AUDIO, HLS, DASH, UNKNOWN_MEDIA }

    public static final class Candidate {
        public final String url;
        public final String identity;
        public final Kind kind;
        public final String mimeType;
        public final String source;
        public final int confidence;

        private Candidate(String url, String identity, Kind kind, String mimeType,
                          String source, int confidence) {
            this.url = url;
            this.identity = identity;
            this.kind = kind;
            this.mimeType = mimeType;
            this.source = source;
            this.confidence = confidence;
        }

        public boolean isAdaptiveStream() {
            return kind == Kind.HLS || kind == Kind.DASH;
        }

        public boolean isDirectFile() {
            return kind == Kind.VIDEO || kind == Kind.AUDIO;
        }
    }

    private static final Pattern VIDEO_EXT = Pattern.compile(
            "(?i)\\.(mp4|m4v|webm|mov|mkv|avi|3gp|ts|m4s)(?:$|[?#])");
    private static final Pattern AUDIO_EXT = Pattern.compile(
            "(?i)\\.(mp3|m4a|aac|ogg|opus|wav|flac)(?:$|[?#])");
    private static final Pattern HLS_EXT = Pattern.compile("(?i)\\.m3u8(?:$|[?#])");
    private static final Pattern DASH_EXT = Pattern.compile("(?i)\\.mpd(?:$|[?#])");

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

        String lower = url.toLowerCase(Locale.ROOT);
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        String hint = sourceHint == null ? "" : sourceHint.toLowerCase(Locale.ROOT);
        Kind kind = null;
        int confidence = 0;

        if (HLS_EXT.matcher(lower).find() || mime.contains("mpegurl") || mime.contains("vnd.apple.mpegurl")) {
            kind = Kind.HLS; confidence = 98;
        } else if (DASH_EXT.matcher(lower).find() || mime.contains("dash+xml")) {
            kind = Kind.DASH; confidence = 98;
        } else if (VIDEO_EXT.matcher(lower).find() || mime.startsWith("video/")) {
            kind = Kind.VIDEO; confidence = 96;
        } else if (AUDIO_EXT.matcher(lower).find() || mime.startsWith("audio/")) {
            kind = Kind.AUDIO; confidence = 96;
        } else if (lower.contains("videoplayback") || lower.contains("mime=video")
                || lower.contains("contenttype=video") || lower.contains("type=video")) {
            kind = Kind.VIDEO; confidence = 82;
        } else if (lower.contains("/media/") || lower.contains("/stream/")) {
            kind = Kind.UNKNOWN_MEDIA; confidence = 58;
        }

        // DOM/player observations are strong evidence even when URLs are signed
        // or do not expose a recognizable extension.
        if (kind == null && (hint.contains("video element") || hint.contains("playing video")
                || hint.contains("video source") || hint.contains("video"))) {
            kind = Kind.VIDEO; confidence = 76;
        } else if (kind == null && (hint.contains("audio element") || hint.contains("audio source")
                || hint.contains("audio"))) {
            kind = Kind.AUDIO; confidence = 76;
        } else if (kind == null && hint.contains("media resource")) {
            kind = Kind.UNKNOWN_MEDIA; confidence = 50;
        }

        if (kind == null) return null;
        String identity = identityFor(url);
        return new Candidate(url, identity, kind, mime, sourceHint == null ? "" : sourceHint, confidence);
    }

    /**
     * Removes fragments for de-duplication, but retains the query string:
     * signed URLs and quality variants often depend on query parameters.
     */
    public static String identityFor(String url) {
        if (url == null) return "";
        int hash = url.indexOf('#');
        return hash >= 0 ? url.substring(0, hash) : url;
    }

    public static boolean looksLikeMedia(String url) {
        return inspect(url, "", "network resource") != null;
    }
}
