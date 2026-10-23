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
import android.widget.TextView;

/** First native launch screen; scanner and queue are added in later phases. */
public final class MainActivity extends Activity {
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(7, 12, 26));
        getWindow().setNavigationBarColor(Color.rgb(7, 12, 26));
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(34), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(7, 12, 26));

        TextView brand = new TextView(this);
        brand.setText("S  SaifNest");
        brand.setTextColor(Color.rgb(77, 218, 255));
        brand.setTextSize(30);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(brand);

        TextView headline = new TextView(this);
        headline.setText("Your media,\nyour space.");
        headline.setTextColor(Color.WHITE);
        headline.setTextSize(32);
        headline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(28);
        root.addView(headline, hp);

        TextView description = new TextView(this);
        description.setText("Discover media, manage downloads, and play supported streams — all from one native app.");
        description.setTextColor(Color.rgb(171, 185, 211));
        description.setTextSize(15);
        LinearLayout.LayoutParams dpms = new LinearLayout.LayoutParams(-1, -2);
        dpms.topMargin = dp(12);
        root.addView(description, dpms);

        TextView section = new TextView(this);
        section.setText("OPEN A MEDIA LINK");
        section.setTextColor(Color.rgb(77, 218, 255));
        section.setTextSize(12);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(34);
        root.addView(section, sp);

        EditText link = new EditText(this);
        link.setSingleLine(true);
        link.setHint("https://example.com/video.mp4");
        link.setHintTextColor(Color.rgb(112, 128, 155));
        link.setTextColor(Color.WHITE);
        link.setTextSize(14);
        link.setPadding(dp(14), dp(12), dp(14), dp(12));
        link.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30, 57, 88)));
        root.addView(link, new LinearLayout.LayoutParams(-1, -2));

        Button play = new Button(this);
        play.setText("Play supported link");
        play.setAllCaps(false);
        play.setOnClickListener(v -> {
            String value = link.getText().toString().trim();
            Uri uri = Uri.parse(value);
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null) {
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.setData(Uri.parse("saifnest://play?url=" + Uri.encode(value)));
                startActivity(intent);
            } else {
                link.setError("Enter a valid HTTP(S) media URL");
            }
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(14);
        root.addView(play, bp);

        TextView status = new TextView(this);
        status.setText("NATIVE FOUNDATION  •  SCANNER IN DEVELOPMENT");
        status.setTextColor(Color.rgb(255, 184, 67));
        status.setTextSize(11);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(-1, -2);
        stp.topMargin = dp(26);
        root.addView(status, stp);

        TextView footer = new TextView(this);
        footer.setText("SaifNest is being built in phases. Website scanning and the persistent download manager are not implemented yet.");
        footer.setTextColor(Color.rgb(125, 143, 171));
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(10);
        root.addView(footer, fp);

        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }
}
