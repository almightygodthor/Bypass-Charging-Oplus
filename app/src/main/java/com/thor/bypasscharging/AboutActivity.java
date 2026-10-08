package com.thor.bypasscharging;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AboutActivity extends Activity {
    private static final int BG = Color.rgb(16, 21, 15);
    private static final int CARD = Color.rgb(28, 33, 27);
    private static final int TEXT = Color.rgb(240, 238, 234);
    private static final int MUTED = Color.rgb(184, 192, 185);
    private static final int GREEN = Color.rgb(157, 212, 157);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView tv(String text, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    private android.graphics.drawable.GradientDrawable glass(int color, float radius) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), 0x305B6D5F);
        return g;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(28));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = tv("‹", 38, MUTED);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));

        TextView title = tv("ABOUT", 24, TEXT);
        title.setTypeface(AppTypography.displayMedium());
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        page.addView(header);

        TextView subtitle = tv("OPLUS BYPASS", 13, MUTED);
        subtitle.setTypeface(AppTypography.labelMedium());
        subtitle.setLetterSpacing(.12f);
        subtitle.setPadding(dp(42), 0, 0, dp(14));
        page.addView(subtitle);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(22), dp(24), dp(22), dp(24));
        hero.setBackground(glass(0xC91F2921, 26));

        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackground(glass(0xFF253128, 100));
        hero.addView(avatar, new LinearLayout.LayoutParams(dp(92), dp(92)));

        TextView name = tv("Thor", 21, TEXT);
        name.setTypeface(AppTypography.displayMedium());
        name.setPadding(0, dp(14), 0, dp(2));
        hero.addView(name);

        TextView handle = tv("@almightygodthor", 13, MUTED);
        hero.addView(handle);

        TextView role = tv("Original concept • Project owner", 12, GREEN);
        role.setPadding(0, dp(8), 0, 0);
        hero.addView(role);

        TextView github = tv("github.com/almightygodthor", 13, TEXT);
        github.setGravity(Gravity.CENTER);
        github.setPadding(0, dp(14), 0, 0);
        github.setOnClickListener(v -> openUrl("https://github.com/almightygodthor"));
        hero.addView(github);

        page.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        TextView creditsTitle = section("CREDITS");
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(-1, dp(34));
        sectionLp.topMargin = dp(14);
        page.addView(creditsTitle, sectionLp);

        LinearLayout credits = card();
        addCredit(credits, "Thor", "Original concept / project owner");
        addCredit(credits, "Robinop", "Lead app developer");
        page.addView(credits);

        TextView supportTitle = section("SUPPORT");
        LinearLayout.LayoutParams supportLp = new LinearLayout.LayoutParams(-1, dp(34));
        supportLp.topMargin = dp(14);
        page.addView(supportTitle, supportLp);

        LinearLayout support = card();
        TextView supportText = tv(
                "Need help, device support, or GT Neo 3 discussion? Join the community.",
                13, MUTED);
        supportText.setPadding(dp(16), dp(16), dp(16), dp(12));
        support.addView(supportText);

        TextView telegram = action("GT NEO 3 TELEGRAM COMMUNITY");
        telegram.setOnClickListener(v ->
                openUrl("https://t.me/RealmeGTNeo3discussion"));
        support.addView(telegram, new LinearLayout.LayoutParams(-1, dp(50)));
        page.addView(support);

        TextView version = tv("Oplus Bypass Charging • Beta", 11, Color.rgb(112, 121, 114));
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(22), 0, 0);
        page.addView(version);

        scroll.addView(page);
        setContentView(scroll);

        loadAvatar(avatar);
    }

    private TextView section(String text) {
        TextView v = tv(text, 12, MUTED);
        v.setTypeface(AppTypography.labelMedium());
        v.setLetterSpacing(.12f);
        return v;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(glass(0xC91F2921, 22));
        return l;
    }

    private void addCredit(LinearLayout parent, String person, String role) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(11), dp(16), dp(11));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);

        TextView n = tv(person, 14, TEXT);
        n.setTypeface(AppTypography.labelMedium());
        TextView r = tv(role, 12, MUTED);
        r.setPadding(0, dp(3), 0, 0);

        text.addView(n);
        text.addView(r);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row);
    }

    private TextView action(String text) {
        TextView v = tv(text, 12, GREEN);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setTypeface(AppTypography.labelMedium());
        v.setLetterSpacing(.06f);
        v.setPadding(dp(16), 0, dp(16), 0);
        return v;
    }

    private void loadAvatar(ImageView target) {
        worker.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://avatars.githubusercontent.com/u/81786979?s=256&v=4");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(6000);
                connection.setUseCaches(true);
                connection.connect();
                try (InputStream in = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(in);
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }

            Bitmap result = bitmap;
            runOnUiThread(() -> {
                if (result != null) target.setImageBitmap(result);
                else target.setImageResource(android.R.drawable.sym_def_app_icon);
            });
        });
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
