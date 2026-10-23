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
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/** Dedicated page showing media candidates found during the current browser session. */
public final class MediaLibraryActivity extends Activity {
    private static final int BG = Color.rgb(7, 12, 26);
    private static final int PANEL = Color.rgb(16, 27, 48);
    private static final int CYAN = Color.rgb(77, 218, 255);
    private static final int WHITE = Color.rgb(245, 248, 255);
    private static final int MUTED = Color.rgb(164, 180, 207);
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); return t;
    }
    private Button button(String s, boolean primary) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(primary ? BG : WHITE); b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(bg(primary ? CYAN : PANEL, 12)); b.setMinHeight(dp(42)); return b;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12)); root.setBackgroundColor(BG);
        LinearLayout header = new LinearLayout(this); header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button back = button("‹ Back", false); header.addView(back);
        TextView title = text("Detected videos", 21, CYAN, true);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(0, -2, 1); titleP.leftMargin = dp(12);
        header.addView(title, titleP);
        Button refresh = button("Refresh", false); header.addView(refresh);
        root.addView(header);
        TextView subtitle = text("Media sources discovered in this browser session", 12, MUTED, false);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1, -2); subP.topMargin = dp(8); root.addView(subtitle, subP);
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list); LinearLayout.LayoutParams scrollP = new LinearLayout.LayoutParams(-1, 0, 1); scrollP.topMargin = dp(12); root.addView(scroll, scrollP);
        setContentView(root);
        back.setOnClickListener(v -> finish());
        refresh.setOnClickListener(v -> render(list));
        render(list);
    }
    private void render(LinearLayout list) {
        list.removeAllViews();
        List<SharedMediaStore.MediaItem> items = SharedMediaStore.all();
        if (items.isEmpty()) {
            TextView empty = text("No video sources detected yet. Return to the browser and open a page or start playing a video.", 15, MUTED, false);
            empty.setPadding(dp(16), dp(20), dp(16), dp(20)); empty.setBackground(bg(PANEL, 16)); list.addView(empty);
            return;
        }
        int index = 0;
        for (SharedMediaStore.MediaItem item : items) {
            index++;
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(12), dp(12), dp(12)); card.setBackground(bg(PANEL, 14));
            LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, -2); cardP.bottomMargin = dp(10); list.addView(card, cardP);
            card.addView(text(String.format(Locale.US, "%02d · %s", index, item.source), 13, CYAN, true));
            TextView link = text(item.url, 12, WHITE, false); link.setTextIsSelectable(true); link.setMaxLines(3);
            LinearLayout.LayoutParams linkP = new LinearLayout.LayoutParams(-1, -2); linkP.topMargin = dp(6); card.addView(link, linkP);
            LinearLayout actions = new LinearLayout(this); LinearLayout.LayoutParams actionsP = new LinearLayout.LayoutParams(-1, -2); actionsP.topMargin = dp(10); card.addView(actions, actionsP);
            Button play = button("▶ Play", true), download = button("↓ Download", false);
            actions.addView(play, new LinearLayout.LayoutParams(0, dp(44), 1));
            LinearLayout.LayoutParams downloadP = new LinearLayout.LayoutParams(0, dp(44), 1); downloadP.leftMargin = dp(8); actions.addView(download, downloadP);
            play.setOnClickListener(v -> {
                Intent i = new Intent(this, PlayerActivity.class);
                i.setData(Uri.parse("saifnest://play?url=" + Uri.encode(item.url))); startActivity(i);
            });
            download.setOnClickListener(v -> enqueue(item));
        }
    }
    private void enqueue(SharedMediaStore.MediaItem item) {
        String lower = item.url.toLowerCase(Locale.ROOT);
        if (lower.contains(".m3u8") || lower.contains(".mpd")) {
            Toast.makeText(this, "Adaptive-stream file downloading is not supported yet. Try Play.", Toast.LENGTH_LONG).show(); return;
        }
        try {
            String file = Uri.parse(item.url).getLastPathSegment();
            if (file == null || file.trim().isEmpty()) file = "saifnest-" + System.currentTimeMillis() + ".mp4";
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(item.url));
            file = file.replaceAll("[\\\\/:*?\"<>|]", "_");
            req.setTitle(file); req.setDescription("SaifNest browser download");
            String userAgent = System.getProperty("http.agent");
            if (userAgent != null && !userAgent.isEmpty()) req.addRequestHeader("User-Agent", userAgent);
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setAllowedOverMetered(true); req.setAllowedOverRoaming(false);
            String cookie = CookieManager.getInstance().getCookie(item.url);
            if (cookie != null && !cookie.isEmpty()) req.addRequestHeader("Cookie", cookie);
            if (item.pageUrl != null && !item.pageUrl.isEmpty()) req.addRequestHeader("Referer", item.pageUrl);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SaifNest/" + file);
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
            Toast.makeText(this, "Download queued.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }
}
