package bd.saifnest.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * SaifNest Browser — redesigned browser shell.
 * UI is deliberately built from native Android views to keep the app lightweight.
 */
public final class BrowserActivity extends Activity {
    private static final int BG = Color.rgb(7, 11, 23);
    private static final int SURFACE = Color.rgb(15, 23, 42);
    private static final int CARD = Color.rgb(20, 31, 54);
    private static final int BORDER = Color.rgb(40, 56, 83);
    private static final int CYAN = Color.rgb(78, 218, 255);
    private static final int WHITE = Color.rgb(241, 246, 255);
    private static final int MUTED = Color.rgb(150, 169, 199);

    private final Set<String> found = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final String[] AD_HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "googletagservices.com", "adnxs.com", "adform.net", "adsrvr.org",
        "advertising.com", "taboola.com", "outbrain.com", "criteo.com",
        "pubmatic.com", "openx.net", "rubiconproject.com", "popads.net",
        "popcash.net", "propellerads.com", "exoclick.com", "mgid.com",
        "amazon-adsystem.com", "adroll.com", "bidswitch.net", "casalemedia.com",
        "flashtalking.com", "hotjar.com", "scorecardresearch.com", "quantserve.com"
    };
    private static final String[] AD_PATHS = {
        "/ads/", "/adserver/", "/advertising/", "/banner-ad", "/popunder",
        "/popup.php", "/clicktracker", "/adchoices", "/sponsor/", "/sponsored/",
        "/promoted/", "/retargeting/"
    };

    private LinearLayout root, topBar, bottomBar, mediaStrip;
    private FrameLayout browserFrame;
    private WebView web;
    private EditText address;
    private ProgressBar progress;
    private TextView status, shieldBadge, pageTitle, mediaCount;
    private View homePanel;
    private boolean adBlockEnabled = true;
    private boolean desktopMode = false;
    private int blockedCount = 0;
    private int tabCount = 1;
    private int mediaCountValue = 0;
    private int pageNumber = 0;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private Button btn(String value, boolean primary) {
        Button b = new Button(this);
        b.setText(value); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(primary ? BG : WHITE);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setBackground(shape(primary ? CYAN : CARD, 13, primary ? 0 : BORDER));
        b.setMinHeight(dp(42)); b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private LinearLayout.LayoutParams margin(int w, int h, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return p;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        root = column(); root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(8), dp(14), dp(5));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0, bottom = 0;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top; bottom = bars.bottom;
            } else { top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom(); }
            v.setPadding(dp(14), top + dp(6), dp(14), bottom + dp(4));
            return insets;
        });

        buildTopBar();
        buildAddressBar();
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(SURFACE));
        root.addView(progress, lp(-1, dp(2)));

        browserFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameP = new LinearLayout.LayoutParams(-1, 0, 1);
        frameP.topMargin = dp(8); frameP.bottomMargin = dp(8);
        root.addView(browserFrame, frameP);
        buildBottomBar();
        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
        root.requestApplyInsets();
        showHome();

        String initial = getIntent().getStringExtra("url");
        if (initial != null && isHttp(initial)) loadUrl(initial);
    }

    private void buildTopBar() {
        topBar = row();
        LinearLayout brand = column();
        TextView logo = text("S", 23, CYAN, true);
        logo.setGravity(Gravity.CENTER); logo.setBackground(shape(CARD, 13, BORDER));
        brand.addView(logo, lp(dp(42), dp(42)));
        topBar.addView(brand, margin(dp(42), dp(42), 0, 0, 10, 0));

        LinearLayout titleStack = column();
        pageTitle = text("SaifNest", 18, WHITE, true);
        titleStack.addView(pageTitle);
        TextView subtitle = text("PRIVATE • FAST • MEDIA", 9, MUTED, true);
        LinearLayout.LayoutParams subP = lp(-2, -2); subP.topMargin = dp(1); titleStack.addView(subtitle, subP);
        topBar.addView(titleStack, new LinearLayout.LayoutParams(0, -2, 1));

        Button tabs = btn(String.valueOf(tabCount), false);
        tabs.setMinWidth(dp(42));
        topBar.addView(tabs, margin(dp(42), dp(40), 5, 0, 5, 0));
        tabs.setOnClickListener(v -> showTabs());

        Button more = btn("⋮", false);
        topBar.addView(more, lp(dp(42), dp(40)));
        more.setOnClickListener(v -> showTools());
        root.addView(topBar);
    }

    private void buildAddressBar() {
        LinearLayout bar = row();
        bar.setPadding(dp(8), dp(5), dp(5), dp(5));
        bar.setBackground(shape(SURFACE, 17, BORDER));

        TextView lock = text("⌕", 20, CYAN, true);
        lock.setGravity(Gravity.CENTER);
        bar.addView(lock, lp(dp(30), dp(38)));

        address = new EditText(this);
        address.setSingleLine(true); address.setTextSize(14); address.setTextColor(WHITE);
        address.setHintTextColor(MUTED); address.setHint("Search or enter address");
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setSelectAllOnFocus(false); address.setPadding(dp(3), 0, dp(3), 0);
        address.setBackgroundColor(Color.TRANSPARENT);
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(38), 1));

        Button reload = btn("↻", false);
        bar.addView(reload, lp(dp(40), dp(38)));
        reload.setOnClickListener(v -> { if (web != null && web.getVisibility() == View.VISIBLE) web.reload(); else showHome(); });
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { loadAddress(); return true; }
            return false;
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(12); root.addView(bar, p);
    }

    private void buildBottomBar() {
        bottomBar = row();
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(dp(3), dp(5), dp(3), dp(3));
        bottomBar.setBackground(shape(SURFACE, 18, BORDER));
        String[] labels = {"⌂\nHome", "◉\nTabs", "↓\nMedia", "✦\nTools"};
        Runnable[] actions = {
            this::showHome, this::showTabs,
            () -> startActivity(new Intent(this, MediaLibraryActivity.class)),
            this::showTools
        };
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 11, i == 0 ? CYAN : MUTED, true);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(5), dp(4), dp(5), dp(4));
            bottomBar.addView(item, new LinearLayout.LayoutParams(0, dp(50), 1));
            item.setOnClickListener(v -> actions[index].run());
        }
        root.addView(bottomBar);
    }

    private void ensureWebView() {
        if (web != null) return;
        web = new WebView(this); web.setBackgroundColor(Color.WHITE);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(true);
        web.getSettings().setLoadsImagesAutomatically(true);
        web.getSettings().setSupportMultipleWindows(false);
        web.getSettings().setBuiltInZoomControls(true); web.getSettings().setDisplayZoomControls(false);
        web.addJavascriptInterface(new MediaBridge(), "SaifNestBridge");
        CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value); progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public void onReceivedTitle(WebView view, String title) {
                if (title != null && !title.trim().isEmpty()) pageTitle.setText(title);
            }
            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {
                if (adBlockEnabled) { toast("Pop-up blocked"); return false; }
                return false;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String target = request.getUrl().toString();
                if (request.isForMainFrame() && adBlockEnabled && isHttp(target) && isAdRequest(target)) {
                    blockedCount++; updateShield(); return true;
                }
                if (request.isForMainFrame()) {
                    address.setText(target); status.setText("Loading website…");
                }
                return false;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                address.setText(url); status.setText("Loading website…"); pageNumber++;
                pageTitle.setText("Loading…");
                if (homePanel != null) homePanel.setVisibility(View.GONE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                address.setText(url); status.setText("Ready · " + blockedCount + " ads blocked");
                BrowserDataStore.addHistory(BrowserActivity.this, view.getTitle(), url);
                injectPageTools(view);
                String scan = "(function(){try{var a=[];function add(u){if(u&&/^https?:\\/\\//i.test(u)&&a.indexOf(u)<0)a.push(u)};" +
                    "document.querySelectorAll('video,audio,video source,audio source,source').forEach(function(e){add(e.currentSrc||e.src||e.getAttribute('src'))});" +
                    "performance.getEntriesByType('resource').forEach(function(e){if(/\\.(mp4|m4v|webm|mov|mkv|avi|mp3|m4a|aac|ogg|opus|m3u8|mpd)(?:$|[?#])/i.test(e.name)||/videoplayback|\\/media\\//i.test(e.name))add(e.name)});" +
                    "return JSON.stringify(a)}catch(e){return '[]'}})()";
                view.evaluateJavascript(scan, value -> {
                    try {
                        if (value == null || "null".equals(value)) return;
                        JSONArray arr = new JSONArray(value);
                        for (int i=0; i<arr.length(); i++) addMedia(arr.optString(i), "Browser resource");
                    } catch (Exception ignored) {}
                });
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (adBlockEnabled && isHttp(url) && isAdRequest(url)) {
                    synchronized (BrowserActivity.this) { blockedCount++; }
                    runOnUiThread(() -> updateShield());
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                if (isHttp(url)) addMedia(url, "Network resource");
                return super.shouldInterceptRequest(view, request);
            }
        });
    }

    private void injectPageTools(WebView view) {
        String script = "(function(){try{window.open=function(){return null};" +
            "var css='iframe[src*=\\\"ad\\\"],iframe[id*=\\\"ad\\\"],[id*=\\\"advertisement\\\"],[class*=\\\"advertisement\\\"],[id*=\\\"ad-container\\\"],[class*=\\\"ad-container\\\"],[id*=\\\"ad-slot\\\"],[class*=\\\"ad-slot\\\"],[class*=\\\"popunder\\\"]{display:none!important;visibility:hidden!important;pointer-events:none!important;max-height:0!important;overflow:hidden!important}';" +
            "var s=document.getElementById('saifnest-style');if(!s){s=document.createElement('style');s.id='saifnest-style';s.textContent=css;(document.head||document.documentElement).appendChild(s)}" +
            "var anti=/disable your ad blocker|ad blocker detected|turn off your ad blocker|adblock detected/i;" +
            "function clean(){document.querySelectorAll('div,section,aside,dialog').forEach(function(e){if(e.closest('main,article,video'))return;var t=(e.innerText||'').slice(0,400);if(anti.test(t)&&e.children.length<10)e.style.setProperty('display','none','important')})}" +
            "clean();new MutationObserver(clean).observe(document.documentElement,{subtree:true,childList:true});}catch(e){}})()";
        view.evaluateJavascript(script, null);
    }

    private void showHome() {
        ensureWebView();
        browserFrame.removeAllViews();
        if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
        homePanel = column(); homePanel.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout content = column(); content.setPadding(dp(3), dp(5), dp(3), dp(12));
        LinearLayout hero = column(); hero.setPadding(dp(20), dp(22), dp(20), dp(20));
        hero.setBackground(shape(CARD, 22, BORDER));
        TextView eyebrow = text("YOUR WEB, YOUR WAY", 10, CYAN, true);
        hero.addView(eyebrow);
        TextView heading = text("Browse beyond.", 28, WHITE, true);
        LinearLayout.LayoutParams hp = lp(-1, -2); hp.topMargin = dp(8); hero.addView(heading, hp);
        TextView desc = text("A cleaner browser with built-in media discovery and quick tools.", 13, MUTED, false);
        desc.setLineSpacing(dp(3), 1f); LinearLayout.LayoutParams descP = lp(-1, -2); descP.topMargin = dp(5); hero.addView(desc, descP);
        LinearLayout chips = row(); chips.setGravity(Gravity.LEFT);
        chips.addView(chip("🛡  Ad shield"));
        chips.addView(chip("▶  Media finder"), margin(-2, dp(30), 7, 12, 0, 0));
        hero.addView(chips);
        content.addView(hero, margin(-1, -2, 0, 6, 0, 0));

        TextView quickTitle = text("QUICK ACCESS", 11, MUTED, true);
        content.addView(quickTitle, margin(-1, -2, 4, 22, 0, 8));
        LinearLayout grid1 = row(), grid2 = row();
        addShortcut(grid1, "G", "Google", "https://www.google.com", Color.rgb(66,133,244));
        addShortcut(grid1, "▶", "YouTube", "https://m.youtube.com", Color.rgb(255,75,75));
        addShortcut(grid1, "W", "Wikipedia", "https://www.wikipedia.org", Color.rgb(100,116,139));
        addShortcut(grid2, "GH", "GitHub", "https://github.com", Color.rgb(100,116,139));
        addShortcut(grid2, "N", "News", "https://news.google.com", Color.rgb(77,218,255));
        addShortcut(grid2, "＋", "Add site", "", CYAN);
        content.addView(grid1, lp(-1, dp(94)));
        content.addView(grid2, margin(-1, dp(94), 0, 8, 0, 0));

        LinearLayout utility = column(); utility.setPadding(dp(15), dp(14), dp(15), dp(14));
        utility.setBackground(shape(SURFACE, 18, BORDER));
        TextView utilityTitle = text("BROWSER STATUS", 11, CYAN, true); utility.addView(utilityTitle);
        status = text("Ready to browse · Ad shield on", 13, WHITE, false);
        utility.addView(status, margin(-1, -2, 0, 7, 0, 0));
        mediaCount = text(mediaCountValue + " media sources found this session", 12, MUTED, false);
        utility.addView(mediaCount, margin(-1, -2, 0, 4, 0, 0));
        content.addView(utility, margin(-1, -2, 0, 22, 0, 0));
        TextView footer = text("SaifNest Browser  •  Built for Android", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer, margin(-1, dp(40), 0, 12, 0, 0));
        scroll.addView(content); homePanel.addView(scroll);
        browserFrame.addView(homePanel, new FrameLayout.LayoutParams(-1, -1));
        web.setVisibility(View.GONE);
        address.setText(""); address.setHint("Search or enter address");
        progress.setVisibility(View.GONE);
    }

    private View chip(String label) {
        TextView t = text(label, 10, WHITE, true); t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(6), dp(10), dp(6)); t.setBackground(shape(SURFACE, 30, BORDER));
        return t;
    }

    private void addShortcut(LinearLayout parent, String iconText, String name, String url, int color) {
        LinearLayout tile = column(); tile.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        p.setMargins(dp(3), dp(3), dp(3), dp(3)); parent.addView(tile, p);
        TextView icon = text(iconText, 18, color, true); icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(CARD, 18, BORDER)); tile.addView(icon, lp(dp(48), dp(48)));
        TextView title = text(name, 11, WHITE, false); title.setGravity(Gravity.CENTER);
        tile.addView(title, margin(-1, -2, 0, 5, 0, 0));
        tile.setOnClickListener(v -> {
            if (url == null || url.isEmpty()) {
                EditText input = new EditText(this); input.setSingleLine(true); input.setHint("https://example.com");
                new AlertDialog.Builder(this).setTitle("Add quick site").setView(input)
                    .setPositiveButton("Open", (d,w) -> { String s=input.getText().toString().trim(); if(!s.isEmpty()) loadUrl(s); })
                    .setNegativeButton("Cancel", null).show();
            } else loadUrl(url);
        });
    }

    private void loadUrl(String value) {
        if (value == null) return;
        value = value.trim();
        if (value.isEmpty()) return;
        if (!value.matches("(?i)^https?://.*")) {
            if (value.contains(" ") || !value.contains(".")) value = "https://www.google.com/search?q=" + Uri.encode(value);
            else value = "https://" + value;
        }
        if (!isHttp(value)) { toast("Enter a valid website or search term"); return; }
        ensureWebView();
        browserFrame.removeAllViews();
        if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
        homePanel = null;
        web.setVisibility(View.VISIBLE);
        browserFrame.addView(web, new FrameLayout.LayoutParams(-1, -1));
        address.setText(value); progress.setVisibility(View.VISIBLE); progress.setProgress(5);
        status.setText("Loading website…"); web.loadUrl(value);
    }

    private void showTabs() {
        String current = web == null ? "" : web.getUrl();
        String title = web != null && web.getTitle() != null ? web.getTitle() : "SaifNest Home";
        new AlertDialog.Builder(this).setTitle("Tabs · " + tabCount)
            .setMessage(title + (current == null ? "" : "\n" + current) + "\n\nSingle-tab preview. Multi-tab session manager is a future upgrade.")
            .setPositiveButton("New tab", (d,w) -> { tabCount++; showHome(); updateTabCount(); })
            .setNeutralButton("Reload", (d,w) -> { if(web!=null) web.reload(); })
            .setNegativeButton("Close tab", (d,w) -> { tabCount=Math.max(1,tabCount-1); showHome(); updateTabCount(); }).show();
    }

    private void updateTabCount() {
        if (topBar != null && topBar.getChildCount() > 2 && topBar.getChildAt(2) instanceof Button)
            ((Button)topBar.getChildAt(2)).setText(String.valueOf(tabCount));
    }

    private void showTools() {
        String[] tools = {
            adBlockEnabled ? "🛡  Ad shield: ON" : "🛡  Ad shield: OFF",
            "▶  Detected media & downloads",
            "⌕  Find in page",
            "▣  Share current page",
            "⧉  Copy page link",
            desktopMode ? "▤  Desktop site: ON" : "▤  Request desktop site",
            BrowserDataStore.isBookmarked(this, web == null ? "" : String.valueOf(web.getUrl())) ? "★  Remove bookmark" : "☆  Bookmark this page",
            "▣  Bookmarks",
            "◷  Browsing history",
            "⌫  Clear browsing cache",
            "↻  Reload page",
            "⌂  Home"
        };
        new AlertDialog.Builder(this).setTitle("SaifNest tools").setItems(tools, (d, which) -> {
            switch (which) {
                case 0: adBlockEnabled=!adBlockEnabled; updateShield(); if(web!=null) web.reload(); toast(adBlockEnabled?"Ad shield enabled":"Ad shield paused"); break;
                case 1: startActivity(new Intent(this, MediaLibraryActivity.class)); break;
                case 2: if(web!=null) { EditText q=new EditText(this); q.setSingleLine(true); q.setHint("Find text"); new AlertDialog.Builder(this).setTitle("Find in page").setView(q).setPositiveButton("Find",(a,b)->web.findAllAsync(q.getText().toString())).setNegativeButton("Cancel",null).show(); } break;
                case 3: sharePage(); break;
                case 4: copyPage(); break;
                case 5: desktopMode=!desktopMode; ensureWebView(); web.getSettings().setUserAgentString(desktopMode ? "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/125.0.0.0 Safari/537.36" : null); if(web!=null) web.reload(); toast(desktopMode?"Desktop mode requested":"Mobile mode requested"); break;
                case 6:
                    if (web == null || web.getUrl() == null) { toast("Open a page first"); break; }
                    boolean added = BrowserDataStore.toggleBookmark(this, web.getTitle(), web.getUrl());
                    toast(added ? "Bookmark saved" : "Bookmark removed");
                    break;
                case 7: showBookmarks(); break;
                case 8: showHistory(); break;
                case 9: if(web!=null) web.clearCache(true); CookieManager.getInstance().flush(); toast("Browser cache cleared"); break;
                case 10: if(web!=null) web.reload(); break;
                case 11: showHome(); break;
            }
        }).show();
    }


    private void showBookmarks() {
        java.util.List<BrowserDataStore.Entry> entries = BrowserDataStore.getBookmarks(this);
        if (entries.isEmpty()) { new AlertDialog.Builder(this).setTitle("Bookmarks").setMessage("Your saved pages will appear here. Open a page and choose Tools → Bookmark this page.").setPositiveButton("OK", null).show(); return; }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) { BrowserDataStore.Entry e = entries.get(i); labels[i] = e.title + "\n" + e.url; }
        new AlertDialog.Builder(this).setTitle("Bookmarks · " + entries.size()).setItems(labels, (dialog, which) -> { if (which >= 0 && which < entries.size()) loadUrl(entries.get(which).url); }).setNegativeButton("Close", null).show();
    }

    private void showHistory() {
        java.util.List<BrowserDataStore.Entry> entries = BrowserDataStore.getHistory(this);
        if (entries.isEmpty()) { new AlertDialog.Builder(this).setTitle("Browsing history").setMessage("Pages you visit will appear here.").setPositiveButton("OK", null).show(); return; }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) { BrowserDataStore.Entry e = entries.get(i); labels[i] = e.title + "\n" + e.url; }
        new AlertDialog.Builder(this).setTitle("History · " + entries.size()).setItems(labels, (dialog, which) -> { if (which >= 0 && which < entries.size()) loadUrl(entries.get(which).url); }).setNeutralButton("Clear history", (dialog, which) -> { new AlertDialog.Builder(this).setTitle("Clear browsing history?").setMessage("This removes locally stored page visit records.").setNegativeButton("Cancel", null).setPositiveButton("Clear", (d, w) -> { BrowserDataStore.clearHistory(this); toast("Browsing history cleared"); }).show(); }).setNegativeButton("Close", null).show();
    }

    private void sharePage() {
        String url = web == null ? "" : web.getUrl();
        if (url == null || url.isEmpty()) { toast("Open a page first"); return; }
        Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, url); startActivity(Intent.createChooser(i, "Share page"));
    }

    private void copyPage() {
        String url = web == null ? "" : web.getUrl();
        if (url == null || url.isEmpty()) { toast("Open a page first"); return; }
        ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("SaifNest page", url)); toast("Page link copied");
    }

    private void updateShield() {
        if (shieldBadge != null) shieldBadge.setText((adBlockEnabled ? "Shield ON" : "Shield OFF") + " · " + blockedCount);
        if (status != null) status.setText((adBlockEnabled ? "Ad shield active" : "Ad shield paused") + " · " + blockedCount + " blocked");
    }

    private boolean isAdRequest(String raw) {
        try {
            Uri u=Uri.parse(raw); String host=u.getHost();
            if(host==null)return false; host=host.toLowerCase(Locale.ROOT);
            for(String domain:AD_HOSTS) if(host.equals(domain)||host.endsWith("."+domain)) return true;
            String path=(u.getPath()==null?"":u.getPath()).toLowerCase(Locale.ROOT);
            String query=(u.getEncodedQuery()==null?"":u.getEncodedQuery()).toLowerCase(Locale.ROOT);
            for(String marker:AD_PATHS) if(path.contains(marker)) return true;
            return query.matches(".*[?&](ad|ads|ad_id|adunit|advert|campaign_id|click_id|sponsored)=.*");
        } catch(Exception e){return false;}
    }

    private boolean isHttp(String value) {
        try { Uri u=Uri.parse(value); return u.getHost()!=null && ("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme())); }
        catch(Exception e){return false;}
    }

    private void addMedia(String url, String source) {
        if(!isHttp(url)) return;
        MediaDetectionEngine.Candidate candidate=MediaDetectionEngine.inspect(url,"",source);
        if(candidate==null || !found.add(candidate.identity)) return;
        SharedMediaStore.add(candidate.url,source,web==null?"":String.valueOf(web.getUrl()));
        runOnUiThread(() -> {
            mediaCountValue++;
            if(mediaCount!=null) mediaCount.setText(mediaCountValue+" media sources found this session");
            if(status!=null) status.setText(candidate.displayType()+" detected · "+candidate.confidence+"% confidence");
        });
    }

    private final class MediaBridge {
        @JavascriptInterface public void found(String url,String source) {
            if(url!=null && url.length()<8192) addMedia(url,source==null?"Page media element":source);
        }
    }

    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }

    @Override public void onBackPressed() {
        if(web!=null && web.getVisibility()==View.VISIBLE && web.canGoBack()) { web.goBack(); return; }
        if(web!=null && web.getVisibility()==View.VISIBLE) { showHome(); return; }
        super.onBackPressed();
    }
}
