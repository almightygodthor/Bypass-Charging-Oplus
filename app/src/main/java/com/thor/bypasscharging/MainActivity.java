package com.thor.bypasscharging;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private TextView state, battery, current, voltage, power, root, plug, node;
    private Button toggle;
    private boolean refreshing;

    private static final int BG = Color.rgb(8, 13, 14);
    private static final int CARD = Color.rgb(18, 26, 27);
    private static final int CARD_ALT = Color.rgb(15, 34, 34);
    private static final int TEAL = Color.rgb(70, 210, 201);
    private static final int TEXT = Color.rgb(239, 244, 243);
    private static final int MUTED = Color.rgb(151, 169, 168);
    private static final int LINE = Color.rgb(42, 57, 57);

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float sp) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(TEXT);
        v.setTextSize(sp);
        return v;
    }

    private GradientDrawable bg(int fill, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable pill(int fill, float radius) {
        return bg(fill, radius);
    }

    private TextView section(String value) {
        TextView v = text(value, 12);
        v.setTextColor(MUTED);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(28));
        lp.topMargin = dp(18);
        v.setLayoutParams(lp);
        return v;
    }

    private void setText(TextView view, String value) {
        if (!value.contentEquals(view.getText())) view.setText(value);
    }

    private LinearLayout row(String label, String value, TextView[] holder) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(4), dp(16), dp(4));

        TextView l = text(label, 14);
        l.setTextColor(MUTED);

        TextView val = text(value, 18);
        val.setTextColor(TEXT);
        val.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        val.setGravity(Gravity.END);

        r.addView(l, new LinearLayout.LayoutParams(0, -1, 1));
        r.addView(val, new LinearLayout.LayoutParams(dp(125), -1));
        holder[0] = val;
        return r;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(bg(CARD, 26));
        return c;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(18), dp(30));
        content.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = text("OPLUS BYPASS", 25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleBox.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView subtitle = text("Bypass charging", 13);
        subtitle.setTextColor(MUTED);
        titleBox.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(24)));

        header.addView(titleBox, new LinearLayout.LayoutParams(0, dp(60), 1));

        TextView badge = text("OPLUS", 11);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setTextColor(TEAL);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(pill(Color.rgb(18, 55, 54), 18));
        header.addView(badge, new LinearLayout.LayoutParams(dp(72), dp(34)));

        content.addView(header);

        content.addView(section("STATUS"));

        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout statusPill = new LinearLayout(this);
        statusPill.setGravity(Gravity.CENTER_VERTICAL);
        statusPill.setPadding(dp(12), 0, dp(12), 0);
        statusPill.setBackground(pill(Color.rgb(19, 48, 47), 18));

        TextView dot = text("●", 12);
        dot.setTextColor(TEAL);
        statusPill.addView(dot, new LinearLayout.LayoutParams(dp(24), dp(32)));

        TextView statusLabel = text("BYPASS", 11);
        statusLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusLabel.setTextColor(TEAL);
        statusPill.addView(statusLabel, new LinearLayout.LayoutParams(-2, dp(32)));

        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(dp(112), dp(32));
        hero.addView(statusPill, pillLp);

        state = text("CHARGING NORMAL", 25);
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        state.setTextColor(TEXT);
        state.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams stateLp = new LinearLayout.LayoutParams(-1, dp(48));
        stateLp.topMargin = dp(8);
        hero.addView(state, stateLp);

        battery = text("Battery  --", 14);
        battery.setTextColor(MUTED);
        hero.addView(battery, new LinearLayout.LayoutParams(-1, dp(26)));

        content.addView(hero, new LinearLayout.LayoutParams(-1, dp(154)));

        content.addView(section("LIVE READINGS"));

        LinearLayout readings = card();
        readings.setPadding(0, dp(6), 0, dp(6));

        TextView[] c = new TextView[1];
        TextView[] v = new TextView[1];
        TextView[] p = new TextView[1];

        readings.addView(row("Current", "0.00 A", c), new LinearLayout.LayoutParams(-1, dp(50)));
        readings.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        readings.addView(row("Voltage", "0.00 V", v), new LinearLayout.LayoutParams(-1, dp(50)));
        readings.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        readings.addView(row("Power", "0.00 W", p), new LinearLayout.LayoutParams(-1, dp(50)));

        current = c[0];
        voltage = v[0];
        power = p[0];

        content.addView(readings, new LinearLayout.LayoutParams(-1, dp(158)));

        content.addView(section("CHARGING"));

        LinearLayout info = card();
        info.setPadding(0, dp(6), 0, dp(6));

        TextView[] r = new TextView[1];
        TextView[] ch = new TextView[1];
        TextView[] n = new TextView[1];

        info.addView(row("Root access", "Checking…", r), new LinearLayout.LayoutParams(-1, dp(48)));
        info.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        info.addView(row("Charger", "Checking…", ch), new LinearLayout.LayoutParams(-1, dp(48)));
        info.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        info.addView(row("Control node", "Checking…", n), new LinearLayout.LayoutParams(-1, dp(48)));

        root = r[0];
        plug = ch[0];
        node = n[0];

        content.addView(info, new LinearLayout.LayoutParams(-1, dp(158)));

        toggle = new Button(this);
        toggle.setText("Enable bypass");
        toggle.setTextSize(15);
        toggle.setTextColor(Color.rgb(7, 25, 25));
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setAllCaps(false);
        toggle.setGravity(Gravity.CENTER);
        toggle.setBackground(bg(TEAL, 20));
        toggle.setMinHeight(0);
        toggle.setMinWidth(0);
        toggle.setPadding(0, 0, 0, 0);

        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(-1, dp(56));
        buttonLp.topMargin = dp(18);
        content.addView(toggle, buttonLp);

        TextView note = text("Root access required", 11);
        note.setTextColor(Color.rgb(92, 108, 108));
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, dp(28));
        noteLp.topMargin = dp(4);
        content.addView(note, noteLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);

        // Android 15+ enforces edge-to-edge for targetSdk 35+.
        // Keep the actual app content out of the status/navigation bars.
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            content.setPadding(
                    dp(18) + insets.getSystemWindowInsetLeft(),
                    dp(14) + insets.getSystemWindowInsetTop(),
                    dp(18) + insets.getSystemWindowInsetRight(),
                    dp(30) + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });

        scroll.addView(content);
        setContentView(scroll);

        toggle.setOnClickListener(vw -> toggleBypass());

        main.post(update);
    }

    private ViewDivider divider() {
        return new ViewDivider();
    }

    private final class ViewDivider extends android.view.View {
        ViewDivider() {
            super(MainActivity.this);
            setBackgroundColor(LINE);
        }
    }

    private void toggleBypass() {
        if (refreshing) return;
        refreshing = true;
        toggle.setEnabled(false);

        worker.execute(() -> {
            boolean rootAvailable = RootShell.isRootAvailable();
            boolean currentState = rootAvailable && RootShell.isBypassEnabled();
            boolean target = !currentState;
            boolean ok = rootAvailable && RootShell.setBypass(target);

            main.post(() -> {
                refreshing = false;
                toggle.setEnabled(true);
                refresh();
                Toast.makeText(this,
                        ok ? (target ? "Bypass enabled" : "Normal charging restored")
                           : "Unable to change bypass state",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void refresh() {
        PowerReader.Snapshot telemetry = PowerReader.snapshot(this);

        setText(battery, "Battery  " + telemetry.battery);
        setText(current, String.format(Locale.US, "%.2f A", telemetry.currentA));
        setText(voltage, String.format(Locale.US, "%.2f V", telemetry.voltageV));
        setText(power, String.format(Locale.US, "%.2f W", telemetry.powerW));
        setText(plug, telemetry.plugged ? "Connected ✓" : "Disconnected");

        worker.execute(() -> {
            boolean rooted = RootShell.isRootAvailable();
            String path = BypassNodeDetector.find();
            boolean bypass = !path.isEmpty() && RootShell.isBypassEnabled();

            main.post(() -> {
                setText(root, rooted ? "Granted ✓" : "Unavailable ✕");
                setText(node, path.isEmpty() ? "Not detected" : BypassNodeDetector.displayName(path));
                setText(state, bypass ? "BYPASS ACTIVE" : "CHARGING NORMAL");
                state.setTextColor(bypass ? TEAL : TEXT);
                setText(toggle, bypass ? "Disable bypass" : "Enable bypass");
            });
        });
    }

    private final Runnable update = new Runnable() {
        @Override public void run() {
            if (!isFinishing()) {
                refresh();
                main.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onDestroy() {
        main.removeCallbacks(update);
        worker.shutdownNow();
        super.onDestroy();
    }
}