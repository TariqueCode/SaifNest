package bd.saifnest.player;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** Native SaifNest discovery screen. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 12, 26);
    private static final int PANEL = Color.rgb(16, 27, 48);
    private static final int PANEL_ALT = Color.rgb(20, 34, 59);
    private static final int CYAN = Color.rgb(77, 218, 255);
    private static final int WHITE = Color.rgb(245, 248, 255);
    private static final int MUTED = Color.rgb(164, 180, 207);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ScannerEngine scanner;
    private LinearLayout resultsList;
    private TextView status;
    private TextView countView;
    private Button scanButton;
    private EditText urlInput;
    private ProgressBar progress;
    private boolean singleLinkMode = true;
    private int candidateCount = 0;
    private int scanGeneration = 0;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getStatusBarInset() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int getNavigationInset() {
        int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(0);
    }

    private GradientDrawable shape(int color, int radiusDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(true);
        if (bold) v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        else v.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return v;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setTextColor(primary ? BG : WHITE);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setMinHeight(dp(48));
        b.setBackground(shape(primary ? CYAN : PANEL_ALT, 14, primary ? 0 : Color.rgb(40, 60, 88)));
        if (android.os.Build.VERSION.SDK_INT >= 21) b.setElevation(dp(1));
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams params(int width, int height, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.topMargin = dp(top);
        return p;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(20), getStatusBarInset() + dp(10), dp(20), getNavigationInset() + dp(8));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.saifnest_logo);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setContentDescription("SaifNest logo");
        brandRow.addView(mark, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView brand = text("SaifNest", 25, CYAN, true);
        LinearLayout.LayoutParams brandP = new LinearLayout.LayoutParams(-2, -2);
        brandP.leftMargin = dp(10);
        brandRow.addView(brand, brandP);
        TextView nativeLabel = text("NATIVE MEDIA", 10, MUTED, true);
        nativeLabel.setGravity(android.view.Gravity.CENTER);
        nativeLabel.setPadding(dp(9), dp(6), dp(9), dp(6));
        nativeLabel.setBackground(shape(PANEL, 20, Color.rgb(38, 55, 82)));
        LinearLayout.LayoutParams badgeP = new LinearLayout.LayoutParams(-2, -2);
        badgeP.leftMargin = dp(8);
        brandRow.addView(nativeLabel, badgeP);
        root.addView(brandRow);

        TextView headline = text("Your media,\none place.", 30, WHITE, true);
        root.addView(headline, params(-1, -2, 20));
        TextView subtitle = text("Find public media, play compatible streams and manage your downloads.", 14, MUTED, false);
        root.addView(subtitle, params(-1, -2, 7));

        LinearLayout searchPanel = column();
        searchPanel.setPadding(dp(14), dp(14), dp(14), dp(14));
        searchPanel.setBackground(shape(PANEL, 20, Color.rgb(30, 47, 73)));
        root.addView(searchPanel, params(-1, -2, 20));

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        Button linkMode = button("Single link", true);
        Button siteMode = button("Scan website", false);
        modes.addView(linkMode, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams siteP = new LinearLayout.LayoutParams(0, dp(46), 1);
        siteP.leftMargin = dp(8);
        modes.addView(siteMode, siteP);
        searchPanel.addView(modes);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(14);
        urlInput.setTextColor(WHITE);
        urlInput.setHintTextColor(Color.rgb(112, 132, 163));
        urlInput.setHint("Paste a video or webpage URL");
        urlInput.setSelectAllOnFocus(false);
        urlInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        urlInput.setBackground(shape(BG, 13, Color.rgb(43, 64, 94)));
        searchPanel.addView(urlInput, params(-1, dp(52), 12));

        scanButton = button("⌕   Find media", true);
        searchPanel.addView(scanButton, params(-1, dp(50), 10));
        Button browseButton = button("Open in browser · detect media", false);
        searchPanel.addView(browseButton, params(-1, dp(44), 8));
        Button stop = button("Stop scan", false);
        searchPanel.addView(stop, params(-1, dp(44), 8));

        LinearLayout statusPanel = new LinearLayout(this);
        statusPanel.setOrientation(LinearLayout.VERTICAL);
        statusPanel.setPadding(dp(2), dp(12), dp(2), dp(4));
        root.addView(statusPanel, params(-1, -2, 12));
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        statusRow.addView(text("DISCOVERY", 11, CYAN, true));
        countView = text("  •  Ready", 12, MUTED, false);
        statusRow.addView(countView);
        statusPanel.addView(statusRow);
        status = text("Paste a link to get started.", 12, MUTED, false);
        statusPanel.addView(status, params(-1, -2, 5));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        if (android.os.Build.VERSION.SDK_INT >= 21) progress.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        statusPanel.addView(progress, params(-1, dp(3), 8));

        LinearLayout resultsHeader = new LinearLayout(this);
        resultsHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView resultsTitle = text("Media results", 18, WHITE, true);
        resultsHeader.addView(resultsTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView resultsHint = text("DISCOVERED", 10, CYAN, true);
        resultsHeader.addView(resultsHint);
        root.addView(resultsHeader, params(-1, -2, 12));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        resultsList = column();
        resultsList.setPadding(0, dp(10), 0, dp(20));
        showEmptyState("Nothing here yet", "Your scan results will appear here as they are discovered.");
        scroll.addView(resultsList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
        root.requestApplyInsets();

        linkMode.setOnClickListener(v -> {
            singleLinkMode = true;
            linkMode.setBackground(shape(CYAN, 14, 0));
            linkMode.setTextColor(BG);
            siteMode.setBackground(shape(PANEL_ALT, 14, Color.rgb(40, 60, 88)));
            siteMode.setTextColor(WHITE);
            scanButton.setText("⌕   Find media");
            urlInput.setHint("Paste a video or webpage URL");
            countView.setText("  •  Single link");
        });
        siteMode.setOnClickListener(v -> {
            singleLinkMode = false;
            siteMode.setBackground(shape(CYAN, 14, 0));
            siteMode.setTextColor(BG);
            linkMode.setBackground(shape(PANEL_ALT, 14, Color.rgb(40, 60, 88)));
            linkMode.setTextColor(WHITE);
            scanButton.setText("⌕   Scan website");
            urlInput.setHint("Website URL, e.g. https://example.com");
            countView.setText("  •  Website scan");
        });
        scanButton.setOnClickListener(v -> startScan());
        browseButton.setOnClickListener(v -> {
            String value = urlInput.getText().toString().trim();
            Uri uri = Uri.parse(value);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                urlInput.setError("Enter a valid HTTP(S) URL");
                return;
            }
            Intent browser = new Intent(MainActivity.this, BrowserActivity.class);
            browser.putExtra("url", value);
            startActivity(browser);
        });
        stop.setOnClickListener(v -> {
            scanGeneration++;
            if (scanner != null) scanner.cancel();
            progress.setVisibility(View.GONE);
            scanButton.setEnabled(true);
            status.setText("Scan stopped.");
            countView.setText("  •  Stopped");
        });
    }

    private void showEmptyState(String title, String message) {
        LinearLayout empty = column();
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(dp(18), dp(28), dp(18), dp(28));
        empty.setBackground(shape(PANEL, 18, Color.rgb(30, 47, 73)));
        TextView icon = text("◈", 34, CYAN, true);
        icon.setGravity(android.view.Gravity.CENTER);
        empty.addView(icon);
        TextView heading = text(title, 16, WHITE, true);
        heading.setGravity(android.view.Gravity.CENTER);
        empty.addView(heading, params(-1, -2, 8));
        TextView desc = text(message, 13, MUTED, false);
        desc.setGravity(android.view.Gravity.CENTER);
        empty.addView(desc, params(-1, -2, 5));
        resultsList.addView(empty);
    }

    private void startScan() {
        String value = urlInput.getText().toString().trim();
        Uri uri = Uri.parse(value);
        if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))) {
            urlInput.setError("Enter a valid HTTP(S) URL");
            return;
        }
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        if (scanner != null) scanner.cancel();
        final int generation = ++scanGeneration;
        scanner = new ScannerEngine();
        ScannerEngine active = scanner;
        resultsList.removeAllViews();
        candidateCount = 0;
        scanButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        countView.setText("  •  Scanning");
        status.setText(singleLinkMode ? "Checking the link…" : "Exploring public pages…");
        int pageLimit = singleLinkMode ? 1 : 100;

        worker.execute(() -> active.scan(value, pageLimit, new ScannerEngine.Listener() {
            @Override public void onPage(String url, int pagesVisited, int queuedPages) {
                runOnUiThread(() -> {
                    if (generation != scanGeneration) return;
                    status.setText((singleLinkMode ? "Resolving link" : "Pages checked: " + pagesVisited + " • queued: " + queuedPages) + "\n" + url);
                });
            }
            @Override public void onCandidate(String pageUrl, String mediaUrl, String kind) {
                runOnUiThread(() -> { if (generation == scanGeneration) addCandidate(pageUrl, mediaUrl, kind); });
            }
            @Override public void onError(String url, String message) {
                runOnUiThread(() -> {
                    if (generation != scanGeneration) return;
                    TextView error = text("Could not inspect this page\n" + message, 12, Color.rgb(255, 165, 150), false);
                    error.setPadding(dp(10), dp(10), dp(10), dp(10));
                    resultsList.addView(error);
                });
            }
            @Override public void onFinished(int pagesVisited, int count) {
                runOnUiThread(() -> {
                    if (generation != scanGeneration) return;
                    progress.setVisibility(View.GONE);
                    scanButton.setEnabled(true);
                    countView.setText("  •  " + count + " found");
                    status.setText("Finished · pages checked: " + pagesVisited + " · candidates: " + count);
                    if (count == 0) showEmptyState("No media found", "This site may use a JavaScript player, require access, or use a format that is not detected yet.");
                });
            }
        }));
    }

    private void addCandidate(String pageUrl, String mediaUrl, String kind) {
        candidateCount++;
        LinearLayout card = column();
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(shape(PANEL, 18, Color.rgb(31, 49, 76)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(10);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView number = text(String.format(Locale.US, "%02d", candidateCount), 12, CYAN, true);
        number.setGravity(android.view.Gravity.CENTER);
        number.setBackground(shape(BG, 10, Color.rgb(37, 67, 96)));
        top.addView(number, new LinearLayout.LayoutParams(dp(38), dp(34)));
        TextView kindView = text(kind == null ? "Media candidate" : kind, 12, CYAN, true);
        LinearLayout.LayoutParams kindP = new LinearLayout.LayoutParams(0, -2, 1);
        kindP.leftMargin = dp(10);
        top.addView(kindView, kindP);
        card.addView(top);

        TextView url = text(mediaUrl, 13, WHITE, false);
        url.setTextIsSelectable(true);
        url.setMaxLines(4);
        card.addView(url, params(-1, -2, 10));

        TextView source = text("SOURCE PAGE\n" + pageUrl, 11, MUTED, false);
        source.setMaxLines(3);
        card.addView(source, params(-1, -2, 8));

        boolean direct = isDirectMedia(mediaUrl, kind);
        TextView tag = text(direct ? "DIRECT MEDIA URL" : "WEBPAGE / UNVERIFIED STREAM", 10,
                direct ? Color.rgb(104, 230, 174) : Color.rgb(255, 204, 120), true);
        card.addView(tag, params(-1, -2, 9));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button play = button(direct ? "▶  Play" : "▶  Open page", true);
        Button download = button("↓  Download", false);
        actions.addView(play, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams downP = new LinearLayout.LayoutParams(0, dp(46), 1);
        downP.leftMargin = dp(8);
        actions.addView(download, downP);
        card.addView(actions, params(-1, -2, 12));

        play.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.setData(Uri.parse("saifnest://play?url=" + Uri.encode(mediaUrl)));
            startActivity(intent);
        });
        download.setOnClickListener(v -> {
            if (!isDirectMedia(mediaUrl, kind)) {
                Toast.makeText(this, "This is a webpage, not a verified media file. Open it first and resolve the actual video URL.", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                String fileName = Uri.parse(mediaUrl).getLastPathSegment();
                if (fileName == null || fileName.trim().isEmpty()) fileName = "saifnest-media-" + System.currentTimeMillis() + ".mp4";
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mediaUrl));
                request.setTitle(fileName);
                request.setDescription("Downloading with SaifNest");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(false);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SaifNest/" + fileName);
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);
                Toast.makeText(this, "Download queued. Progress appears in Android notifications.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not start download: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        resultsList.addView(card, cardParams);
    }

    private boolean isDirectMedia(String value, String kind) {
        if (value == null) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        boolean extension = Pattern.compile("\\.(mp4|m4v|webm|mov|mkv|mp3|m4a|aac|ogg|opus|m3u8|mpd)(?:$|[?#])").matcher(lower).find();
        boolean mime = kind != null && (kind.toLowerCase(Locale.ROOT).startsWith("video/") || kind.toLowerCase(Locale.ROOT).startsWith("audio/"));
        return extension || mime;
    }

    @Override protected void onDestroy() {
        scanGeneration++;
        if (scanner != null) scanner.cancel();
        worker.shutdownNow();
        super.onDestroy();
    }
}