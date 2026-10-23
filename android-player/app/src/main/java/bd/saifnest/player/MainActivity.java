package bd.saifnest.player;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Early native URL resolver/site scanner. Results are rendered incrementally in one scrollable list. */
public final class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ScannerEngine scanner;
    private TextView results;
    private TextView status;
    private Button scanButton;
    private EditText urlInput;
    private boolean singleLinkMode = true;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(7, 12, 26));
        getWindow().setNavigationBarColor(Color.rgb(7, 12, 26));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(12));
        root.setBackgroundColor(Color.rgb(7, 12, 26));

        root.addView(text("S  SaifNest", 27, Color.rgb(77, 218, 255), true));
        TextView headline = text("Find media.\nPlay what you find.", 27, Color.WHITE, true);
        LinearLayout.LayoutParams headlineParams = new LinearLayout.LayoutParams(-1, -2);
        headlineParams.topMargin = dp(12);
        root.addView(headline, headlineParams);
        TextView description = text(
                "Paste a video page or scan a public website. Results appear as they are discovered.",
                14, Color.rgb(171, 185, 211), false);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.topMargin = dp(7);
        root.addView(description, descriptionParams);

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        Button linkMode = button("Single link");
        Button siteMode = button("Scan website");
        modes.addView(linkMode, new LinearLayout.LayoutParams(0, -2, 1f));
        modes.addView(siteMode, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams modesParams = new LinearLayout.LayoutParams(-1, -2);
        modesParams.topMargin = dp(12);
        root.addView(modes, modesParams);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://example.com/video");
        urlInput.setHintTextColor(Color.rgb(112, 128, 155));
        urlInput.setTextColor(Color.WHITE);
        urlInput.setTextSize(14);
        urlInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        urlInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30, 57, 88)));
        root.addView(urlInput, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        scanButton = button("Find videos");
        Button stop = button("Stop");
        actions.addView(scanButton, new LinearLayout.LayoutParams(0, -2, 1f));
        actions.addView(stop, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        status = text("Ready", 12, Color.rgb(77, 218, 255), false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(7);
        root.addView(status, statusParams);

        results = text("Detected videos and media candidates will appear here.\n\nThis prototype scans public HTML and common direct media URLs. Some JavaScript players and protected streams may not expose a downloadable URL.", 13, Color.rgb(207, 219, 239), false);
        results.setPadding(dp(4), dp(8), dp(4), dp(18));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root, new ViewGroup.LayoutParams(-1, -1));

        linkMode.setOnClickListener(v -> {
            singleLinkMode = true;
            scanButton.setText("Find videos");
            urlInput.setHint("Paste a video page or direct media URL");
            status.setText("Single-link mode • one URL");
        });
        siteMode.setOnClickListener(v -> {
            singleLinkMode = false;
            scanButton.setText("Scan site");
            urlInput.setHint("Website URL, e.g. https://example.com");
            status.setText("Website mode • limited prototype crawl");
        });

        scanButton.setOnClickListener(v -> startScan());
        stop.setOnClickListener(v -> {
            if (scanner != null) scanner.cancel();
            status.setText("Stopping…");
        });
    }

    private void startScan() {
        String value = urlInput.getText().toString().trim();
        Uri uri = Uri.parse(value);
        if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))) {
            urlInput.setError("Enter a valid HTTP(S) URL");
            return;
        }
        if (scanner != null) scanner.cancel();
        scanner = new ScannerEngine();
        ScannerEngine active = scanner;
        results.setText("");
        scanButton.setEnabled(false);
        status.setText(singleLinkMode ? "Resolving link…" : "Scanning website…");
        int pageLimit = singleLinkMode ? 1 : 100;

        worker.execute(() -> active.scan(value, pageLimit, new ScannerEngine.Listener() {
            @Override public void onPage(String url, int pagesVisited, int queuedPages) {
                runOnUiThread(() -> status.setText(
                        (singleLinkMode ? "Resolving" : "Pages checked: " + pagesVisited + " • queued: " + queuedPages)
                                + "\n" + url));
            }

            @Override public void onCandidate(String pageUrl, String mediaUrl, String kind) {
                runOnUiThread(() -> {
                    String item = "\n━━━━━━━━━━━━━━━━━━━━\n" + kind + "\n" + mediaUrl
                            + "\nSource: " + pageUrl + "\n";
                    results.append(item);
                    Button play = button("▶  Play in SaifNest");
                    play.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                        intent.setData(Uri.parse("saifnest://play?url=" + Uri.encode(mediaUrl)));
                        startActivity(intent);
                    });
                    // Keep result actions inside the scrollable results area instead of
                    // adding unbounded buttons to the outer screen layout.
                    ViewGroup parent = (ViewGroup) results.getParent();
                    if (parent instanceof ScrollView) {
                        // TextView remains the compact live list in this milestone.
                        results.append("\nAction: tap Play is being added to the full library UI.\n");
                    }
                });
            }

            @Override public void onError(String url, String message) {
                runOnUiThread(() -> results.append("\nCould not inspect " + url + "\n" + message + "\n"));
            }

            @Override public void onFinished(int pagesVisited, int candidateCount) {
                runOnUiThread(() -> {
                    status.setText("Finished • pages: " + pagesVisited + " • media candidates: " + candidateCount);
                    scanButton.setEnabled(true);
                });
            }
        }));
    }

    @Override protected void onDestroy() {
        if (scanner != null) scanner.cancel();
        worker.shutdownNow();
        super.onDestroy();
    }
}
