package bd.saifnest.player;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Browser-first media discovery. It observes ordinary WebView resource requests
 * and DOM video sources; it does not defeat DRM, authentication, or site controls.
 */
public final class BrowserActivity extends Activity {
    private static final int BG = Color.rgb(7, 12, 26), PANEL = Color.rgb(16, 27, 48);
    private static final int CYAN = Color.rgb(77, 218, 255), WHITE = Color.rgb(245, 248, 255);
    private static final int MUTED = Color.rgb(164, 180, 207);
    private static final Pattern MEDIA = Pattern.compile("(?i)\\.(mp4|m4v|webm|mov|mkv|mp3|m4a|aac|ogg|opus|m3u8|mpd)(?:$|[?#])");
    private final Set<String> found = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final String[] AD_HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "googletagservices.com", "adservice.google.com", "adnxs.com",
        "adform.net", "adsrvr.org", "advertising.com", "adcolony.com",
        "unityads.unity3d.com", "applovin.com", "mopub.com", "taboola.com",
        "outbrain.com", "criteo.com", "pubmatic.com", "openx.net",
        "rubiconproject.com", "lijit.com", "smartadserver.com", "zedo.com",
        "popads.net", "popcash.net", "propellerads.com", "exoclick.com",
        "trafficjunky.net", "juicyads.com", "mgid.com"
    };
    private static final String[] AD_PATH_MARKERS = {
        "/ads/", "/adserver/", "/advertising/", "/banner-ad", "/popunder",
        "/pop-up", "/popup.php", "/tracking/", "/track.gif", "/pixel.gif"
    };
    private EditText address;
    private WebView web;
    private LinearLayout mediaList;
    private TextView pageStatus;
    private int seq = 0;

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); return t;
    }
    private Button button(String s, boolean primary) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(primary ? BG : WHITE); b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(bg(primary ? CYAN : PANEL, 12, primary ? 0 : Color.rgb(40,60,88)));
        b.setMinHeight(dp(44)); return b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG); root.setPadding(dp(14), dp(8), dp(14), dp(6));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()); top = bars.top; bottom = bars.bottom;
            } else { top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom(); }
            v.setPadding(dp(14), top + dp(8), dp(14), bottom + dp(6)); return insets;
        });
        LinearLayout head = new LinearLayout(this); head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button back = button("‹", false); head.addView(back, new LinearLayout.LayoutParams(dp(46), dp(44)));
        TextView title = label("SaifNest Browser", 20, CYAN, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1); tp.leftMargin = dp(10); head.addView(title, tp);
        Button refresh = button("↻", false); head.addView(refresh, new LinearLayout.LayoutParams(dp(46), dp(44)));
        root.addView(head);
        LinearLayout bar = new LinearLayout(this); bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        address = new EditText(this); address.setSingleLine(true); address.setTextSize(14); address.setTextColor(WHITE);
        address.setHintTextColor(MUTED); address.setHint("Enter website URL"); address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setPadding(dp(12), 0, dp(8), 0); address.setBackground(bg(PANEL, 12, Color.rgb(40,60,88)));
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button go = button("Go", true); LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(60), dp(48)); gp.leftMargin = dp(7); bar.addView(go, gp);
        root.addView(bar, lp(-1, -2, 10));
        pageStatus = label("Open a page; detected media sources appear below.", 12, MUTED, false);
        root.addView(pageStatus, lp(-1, -2, 8));

        web = new WebView(this); web.setBackgroundColor(BG);
        web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(true); web.getSettings().setLoadsImagesAutomatically(true);
        web.getSettings().setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.isForMainFrame()) address.setText(request.getUrl().toString());
                return false;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                address.setText(url); pageStatus.setText("Loading page…"); 
            }
            @Override public void onPageFinished(WebView view, String url) {
                address.setText(url); pageStatus.setText("Watching page resources for supported media…");
                // DOM discovery complements network request observation; this cannot see every blob/DRM stream.
                String js = "(function(){var a=[];function add(u){if(u&&a.indexOf(u)<0)a.push(u)};" +
                        "document.querySelectorAll('video,audio,video source,audio source,source').forEach(function(e){add(e.currentSrc||e.src||e.getAttribute('src'))});" +
                        "performance.getEntriesByType('resource').forEach(function(e){if(/\\.(mp4|m4v|webm|mov|mkv|mp3|m4a|aac|ogg|opus|m3u8|mpd)(?:$|[?#])/i.test(e.name)||/videoplayback|\\/media\\//i.test(e.name))add(e.name)});" +
                        "document.querySelectorAll(\"meta[property='og:video'],meta[name='twitter:player:stream'],link[rel='preload'][as='video']\").forEach(function(e){add(e.content||e.href)});" +
                        "return JSON.stringify(a)})()";
                view.evaluateJavascript("(function(){try{window.open=function(){return null};" +
                        "var css='iframe[src*=\\\"ad\\\"],iframe[id*=\\\"ad\\\"],[class*=\\\"advertisement\\\"],[id*=\\\"advertisement\\\"],[class*=\\\"ad-container\\\"],[id*=\\\"ad-container\\\"],[class*=\\\"popunder\\\"],[id*=\\\"popunder\\\"]{display:none!important;visibility:hidden!important;pointer-events:none!important}';" +
                        "var s=document.getElementById('saifnest-ad-style');if(!s){s=document.createElement('style');s.id='saifnest-ad-style';s.textContent=css;(document.head||document.documentElement).appendChild(s)}" +
                        "}catch(e){};return true})()", ignored -> {});
                view.evaluateJavascript(js, value -> {
                    try {
                        if (value == null || "null".equals(value)) return;
                        JSONArray array = new JSONArray(value);
                        for (int i=0; i<array.length(); i++) addMedia(array.optString(i), "Page video/audio element");
                    } catch (Exception ignored) { }
                });
            }
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isHttp(url) && isAdRequest(url)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(new byte[0]));
                }
                if (isHttp(url) && looksLikeMedia(url)) addMedia(url, "Browser network request");
                return super.shouldInterceptRequest(view, request);
            }
        });

        // Browser occupies the main area; detected sources are in a compact, independently scrollable drawer.
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView mediaHeader = label("Detected media", 16, WHITE, true);
        root.addView(mediaHeader, lp(-1, -2, 10));
        ScrollView results = new ScrollView(this); results.setFillViewport(false);
        mediaList = new LinearLayout(this); mediaList.setOrientation(LinearLayout.VERTICAL);
        results.addView(mediaList); root.addView(results, lp(-1, dp(150), 6));
        setContentView(root, new ViewGroup.LayoutParams(-1, -1)); root.requestApplyInsets();

        back.setOnClickListener(v -> finish());
        refresh.setOnClickListener(v -> web.reload());
        go.setOnClickListener(v -> loadAddress());
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { loadAddress(); return true; }
            return false;
        });
        String initial = getIntent().getStringExtra("url");
        if (initial != null && isHttp(initial)) { address.setText(initial); web.loadUrl(initial); }
    }

    private LinearLayout.LayoutParams lp(int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w,h); p.topMargin=dp(top); return p;
    }
    private void loadAddress() {
        String value = address.getText().toString().trim();
        if (!value.matches("(?i)^https?://.*")) value = "https://" + value;
        if (!isHttp(value)) { address.setError("Enter a valid HTTP(S) URL"); return; }
        web.loadUrl(value);
    }
    private boolean isHttp(String s) {
        try { Uri u=Uri.parse(s); return u.getHost()!=null && ("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())); }
        catch (Exception e) { return false; }
    }

    private boolean isAdRequest(String rawUrl) {
        try {
            Uri uri = Uri.parse(rawUrl);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            for (String domain : AD_HOSTS) {
                if (host.equals(domain) || host.endsWith("." + domain)) return true;
            }
            String path = (uri.getPath() == null ? "" : uri.getPath()).toLowerCase(Locale.ROOT);
            String query = (uri.getEncodedQuery() == null ? "" : uri.getEncodedQuery()).toLowerCase(Locale.ROOT);
            String full = path + "?" + query;
            for (String marker : AD_PATH_MARKERS) if (full.contains(marker)) return true;
            return full.matches(".*[?&](ad|ads|ad_id|adunit|advert|campaign_id|click_id|doubleclick)=.*");
        } catch (Exception ignored) { return false; }
    }

    private boolean looksLikeMedia(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return MEDIA.matcher(lower).find()
                || lower.contains("videoplayback")
                || lower.contains("/media/")
                || lower.contains("mime=video")
                || lower.contains("contenttype=video");
    }

    private void addMedia(String url, String source) {
        if (!isHttp(url) || !found.add(url)) return;
        runOnUiThread(() -> {
            seq++;
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(8), dp(10), dp(8)); card.setBackground(bg(PANEL, 12, Color.rgb(35,52,78)));
            TextView name = label(String.format(Locale.US, "%02d  ·  %s", seq, source), 11, CYAN, true); card.addView(name);
            TextView link = label(url, 11, WHITE, false); link.setMaxLines(2); link.setTextIsSelectable(true); card.addView(link, lp(-1,-2,4));
            LinearLayout actions = new LinearLayout(this);
            Button play = button("▶ Play", true), download = button("↓ Download", false);
            actions.addView(play, new LinearLayout.LayoutParams(0,dp(40),1));
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0,dp(40),1); dp.leftMargin=dp(6); actions.addView(download,dp);
            card.addView(actions, lp(-1,-2,5)); mediaList.addView(card,0,lp(-1,-2,5));
            pageStatus.setText(found.size() + " media candidate(s) detected on this page");
            play.setOnClickListener(v -> {
                Intent i = new Intent(this, PlayerActivity.class);
                i.setData(Uri.parse("saifnest://play?url=" + Uri.encode(url))); startActivity(i);
            });
            download.setOnClickListener(v -> {
                String lower = url.toLowerCase(Locale.ROOT);
                if (lower.contains(".m3u8") || lower.contains(".mpd")) {
                    Toast.makeText(this, "This is an HLS/DASH stream. Stream downloading is not enabled yet; try Play.", Toast.LENGTH_LONG).show(); return;
                }
                try {
                    String file = Uri.parse(url).getLastPathSegment();
                    if (file == null || file.trim().isEmpty()) file = "saifnest-" + System.currentTimeMillis() + ".mp4";
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.setTitle(file); req.setDescription("SaifNest browser download");
                    req.setMimeType("video/*");
                    req.addRequestHeader("User-Agent", web.getSettings().getUserAgentString());
                    String cookie = CookieManager.getInstance().getCookie(url);
                    if (cookie != null && !cookie.isEmpty()) req.addRequestHeader("Cookie", cookie);
                    String referer = web.getUrl();
                    if (referer != null && isHttp(referer)) req.addRequestHeader("Referer", referer);
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setAllowedOverMetered(true); req.setAllowedOverRoaming(false);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SaifNest/" + file);
                    ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
                    Toast.makeText(this, "Download queued in Android Download Manager.", Toast.LENGTH_LONG).show();
                } catch (Exception e) { Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
            });
        });
    }
}