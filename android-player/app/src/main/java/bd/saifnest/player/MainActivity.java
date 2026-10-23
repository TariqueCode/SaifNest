package bd.saifnest.player;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native entry screen with an early same-host site scanner and direct media playback. */
public final class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ScannerEngine scanner;
    private TextView results;
    private Button scanButton;
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(7, 12, 26));
        getWindow().setNavigationBarColor(Color.rgb(7, 12, 26));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(18));
        root.setBackgroundColor(Color.rgb(7, 12, 26));

        TextView brand = new TextView(this);
        brand.setText("S  SaifNest");
        brand.setTextColor(Color.rgb(77, 218, 255));
        brand.setTextSize(28);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(brand);

        TextView headline = new TextView(this);
        headline.setText("Find media.\nPlay what you find.");
        headline.setTextColor(Color.WHITE);
        headline.setTextSize(29);
        headline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(18);
        root.addView(headline, hp);

        TextView description = new TextView(this);
        description.setText("Scan public pages on a site for direct media URLs and video-page links.");
        description.setTextColor(Color.rgb(171, 185, 211));
        description.setTextSize(14);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
        dlp.topMargin = dp(8);
        root.addView(description, dlp);

        EditText site = new EditText(this);
        site.setSingleLine(true);
        site.setHint("Website URL, e.g. https://example.com");
        site.setHintTextColor(Color.rgb(112, 128, 155));
        site.setTextColor(Color.WHITE);
        site.setTextSize(14);
        site.setPadding(dp(12), dp(8), dp(12), dp(8));
        site.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30, 57, 88)));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = dp(20);
        root.addView(site, slp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        scanButton = new Button(this);
        scanButton.setText("Scan site");
        scanButton.setAllCaps(false);
        actions.addView(scanButton, new LinearLayout.LayoutParams(0, -2, 1f));
        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setAllCaps(false);
        actions.addView(stop, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = dp(8);
        root.addView(actions, alp);

        TextView status = new TextView(this);
        status.setText("Ready to scan");
        status.setTextColor(Color.rgb(77, 218, 255));
        status.setTextSize(12);
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(-1, -2);
        stlp.topMargin = dp(12);
        root.addView(status, stlp);

        results = new TextView(this);
        results.setText("Found media links will appear here.\n\nNote: this first scanner handles public HTML pages and common direct media URLs. JavaScript players, blob URLs, DRM and protected streams may need separate support.");
        results.setTextColor(Color.rgb(207, 219, 239));
        results.setTextSize(13);
        results.setPadding(dp(12), dp(12), dp(12), dp(12));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(results);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, 0, 1f);
        rlp.topMargin = dp(8);
        root.addView(scroll, rlp);

        scanButton.setOnClickListener(v -> {
            String value = site.getText().toString().trim();
            Uri uri = Uri.parse(value);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                site.setError("Enter a valid HTTP(S) website URL");
                return;
            }
            if (scanner != null) scanner.cancel();
            scanner = new ScannerEngine();
            ScannerEngine active = scanner;
            results.setText("");
            scanButton.setEnabled(false);
            status.setText("Scanning…");
            worker.execute(() -> active.scan(value, 100, new ScannerEngine.Listener() {
                @Override public void onPage(String url, int pagesVisited, int queuedPages) {
                    runOnUiThread(() -> status.setText("Pages checked: " + pagesVisited + "  •  queued: " + queuedPages + "\n" + url));
                }
                @Override public void onCandidate(String pageUrl, String mediaUrl, String kind) {
                    runOnUiThread(() -> {
                        results.append("\n[" + kind + "]\n" + mediaUrl + "\nSource: " + pageUrl + "\n\n");
                        Button open = new Button(MainActivity.this);
                        open.setText("Play / open detected URL");
                        open.setAllCaps(false);
                        open.setOnClickListener(v2 -> {
                            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                            intent.setData(Uri.parse("saifnest://play?url=" + Uri.encode(mediaUrl)));
                            startActivity(intent);
                        });
                        root.addView(open, new LinearLayout.LayoutParams(-1, -2));
                    });
                }
                @Override public void onError(String url, String message) {
                    runOnUiThread(() -> results.append("\nCould not inspect " + url + "\n" + message + "\n"));
                }
                @Override public void onFinished(int pagesVisited, int candidateCount) {
                    runOnUiThread(() -> {
                        status.setText("Finished • pages: " + pagesVisited + " • unique media links: " + candidateCount);
                        scanButton.setEnabled(true);
                    });
                }
            }));
        });
        stop.setOnClickListener(v -> {
            if (scanner != null) scanner.cancel();
            status.setText("Stopping scan…");
        });

        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }

    @Override protected void onDestroy() {
        if (scanner != null) scanner.cancel();
        worker.shutdownNow();
        super.onDestroy();
    }
}
