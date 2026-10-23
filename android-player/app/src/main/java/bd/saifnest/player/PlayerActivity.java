package bd.saifnest.player;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Plays verified direct media with Media3. If the supplied URL is an HTML page,
 * opens that page in a WebView instead of incorrectly feeding HTML to ExoPlayer.
 */
@UnstableApi
public final class PlayerActivity extends Activity {
    private static final int BG = Color.rgb(7, 12, 26);
    private static final int CYAN = Color.rgb(77, 218, 255);
    private ExoPlayer player;
    private PlayerView playerView;
    private WebView webView;
    private ProgressBar progress;
    private TextView message;
    private final ExecutorService probeExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        Uri intentUri = getIntent().getData();
        String stream = intentUri == null ? null : intentUri.getQueryParameter("url");
        if (stream == null || !isHttpUrl(stream)) {
            Toast.makeText(this, "Invalid media URL", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        setContentView(root);
        progress = new ProgressBar(this);
        FrameLayout.LayoutParams progressP = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER);
        root.addView(progress, progressP);
        message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(14);
        message.setText("Checking media type…");
        message.setPadding(24, 24, 24, 24);
        message.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams messageP = new FrameLayout.LayoutParams(-1, -2,
                android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.BOTTOM);
        root.addView(message, messageP);

        final String url = stream;
        probeExecutor.execute(() -> {
            ProbeResult result = probe(url);
            runOnUiThread(() -> {
                if (destroyed || isFinishing()) return;
                progress.setVisibility(View.GONE);
                if (result.directMedia) startNativePlayer(root, url, result.contentType);
                else openPage(root, url, result.reason);
            });
        });
    }

    private static final class ProbeResult {
        final boolean directMedia;
        final String contentType;
        final String reason;
        ProbeResult(boolean directMedia, String contentType, String reason) {
            this.directMedia = directMedia;
            this.contentType = contentType == null ? "" : contentType;
            this.reason = reason == null ? "" : reason;
        }
    }

    private ProbeResult probe(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\.(mp4|m4v|webm|mov|mkv|mp3|m4a|aac|ogg|opus|m3u8|mpd)(?:[?#].*)?$")) {
            return new ProbeResult(true, "", "URL extension indicates media");
        }
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .followRedirects(true).followSslRedirects(true)
                    .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .callTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder().url(url)
                    .header("Range", "bytes=0-0")
                    .header("User-Agent", "SaifNest/0.3 Android")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String type = response.header("Content-Type", "");
                String t = type.toLowerCase(Locale.ROOT);
                boolean media = t.startsWith("video/") || t.startsWith("audio/")
                        || t.contains("application/vnd.apple.mpegurl")
                        || t.contains("application/x-mpegurl")
                        || t.contains("application/dash+xml");
                return new ProbeResult(media, type, media ? "Media content type" : "The URL responds with a webpage or unknown content");
            }
        } catch (Exception e) {
            return new ProbeResult(false, "", "Could not verify direct media; opening as a webpage");
        }
    }

    private void startNativePlayer(FrameLayout root, String url, String contentType) {
        if (message != null) message.setVisibility(View.GONE);
        playerView = new PlayerView(this);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        playerView.setUseController(true);
        playerView.setKeepContentOnPlayerReset(true);
        root.addView(playerView, 0);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this,
                        "Playback failed. The stream may require cookies, headers, or a supported format.",
                        Toast.LENGTH_LONG).show();
            }
        });
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        if (message != null) message.setText("Opening direct media…");
    }

    private void openPage(FrameLayout root, String url, String reason) {
        if (message != null) {
            message.setVisibility(View.VISIBLE);
            message.setText("This URL is a webpage, not a confirmed direct video file. Opening the page so its own player can run.");
        }
        webView = new WebView(this);
        webView.setBackgroundColor(BG);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLoadsImagesAutomatically(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(url);
        root.addView(webView, 0, new FrameLayout.LayoutParams(-1, -1));
    }

    private boolean isHttpUrl(String value) {
        Uri uri = Uri.parse(value);
        return uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()));
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void releasePlayer() {
        if (playerView != null) playerView.setPlayer(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        probeExecutor.shutdownNow();
        releasePlayer();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}