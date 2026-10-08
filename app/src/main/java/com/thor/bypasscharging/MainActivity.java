package com.thor.bypasscharging;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
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

    private static final int BG = Color.rgb(16, 21, 15);
    private static final int CARD = Color.rgb(28, 33, 27);
    private static final int TEXT = Color.rgb(240, 238, 234);
    private static final int MUTED = Color.rgb(184, 192, 185);
    private static final int GREEN = Color.rgb(157, 212, 157);

    private ChargingGaugeView gauge;
    private SparklineCardView voltageCard, currentCard, powerCard, tempCard, healthCard, pluggedCard;
    private TextView monitorDot, monitorTitle, monitorText, monitorAction, rootStatus;
    private boolean busy;

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView tv(String s, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(CARD, 24));
        return l;
    }

    private android.graphics.drawable.GradientDrawable round(int color, float r) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    private TextView section(String s) {
        TextView v = tv(s, 12, MUTED);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(.12f);
        v.setPadding(0, dp(2), 0, dp(2));
        return v;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(12), dp(18), dp(22));
        content.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = tv("OPLUS BYPASS", 25, TEXT);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView badge = tv("♢", 34, GREEN);
        badge.setGravity(Gravity.CENTER);
        header.addView(badge, new LinearLayout.LayoutParams(dp(54), dp(48)));
        content.addView(header);

        TextView subtitle = tv("Bypass charging", 14, MUTED);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, dp(24));
        subLp.topMargin = -dp(7);
        content.addView(subtitle, subLp);

        FrameLayout gaugeWrap = new FrameLayout(this);
        gauge = new ChargingGaugeView(this);
        gaugeWrap.addView(gauge, new FrameLayout.LayoutParams(-1, dp(344)));
        LinearLayout.LayoutParams gaugeLp = new LinearLayout.LayoutParams(-1, dp(344));
        gaugeLp.topMargin = dp(3);
        content.addView(gaugeWrap, gaugeLp);

        LinearLayout monitor = card();
        monitor.setPadding(dp(18), dp(13), dp(14), dp(13));
        LinearLayout monRow = new LinearLayout(this);
        monRow.setGravity(Gravity.CENTER_VERTICAL);

        monitorDot = tv("●", 17, GREEN);
        monRow.addView(monitorDot, new LinearLayout.LayoutParams(dp(30), dp(62)));

        LinearLayout monText = new LinearLayout(this);
        monText.setOrientation(LinearLayout.VERTICAL);
        monitorTitle = tv("Monitoring Active", 16, TEXT);
        monitorTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        monitorText = tv("Live charging stats and bypass state", 12, MUTED);
        monitorText.setPadding(0, dp(3), 0, 0);
        monText.addView(monitorTitle);
        monText.addView(monitorText);
        monRow.addView(monText, new LinearLayout.LayoutParams(0, dp(62), 1));

        monitorAction = tv("Disable", 14, GREEN);
        monitorAction.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        monitorAction.setGravity(Gravity.CENTER);
        monitorAction.setOnClickListener(v -> toggleBypass());
        monRow.addView(monitorAction, new LinearLayout.LayoutParams(dp(86), dp(62)));
        monitor.addView(monRow);
        content.addView(monitor, new LinearLayout.LayoutParams(-1, dp(88)));

        content.addView(section("LIVE READINGS"), new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        addRow(grid, true);
        addRow(grid, false);
        addRow(grid, false);
        content.addView(grid);

        rootStatus = tv("Root access required", 11, Color.rgb(106, 115, 108));
        rootStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rootLp = new LinearLayout.LayoutParams(-1, dp(28));
        rootLp.topMargin = dp(8);
        content.addView(rootStatus, rootLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            content.setPadding(
                    dp(18) + insets.getSystemWindowInsetLeft(),
                    dp(12) + insets.getSystemWindowInsetTop(),
                    dp(18) + insets.getSystemWindowInsetRight(),
                    dp(22) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(content);
        setContentView(scroll);

        main.post(update);
    }

    private void addRow(LinearLayout grid, boolean first) {
        LinearLayout row = new LinearLayout(this);
        row.setWeightSum(2);
        row.setGravity(Gravity.CENTER_VERTICAL);

        if (first) {
            voltageCard = new SparklineCardView(this);
            currentCard = new SparklineCardView(this);
            row.addView(voltageCard, new LinearLayout.LayoutParams(0, dp(150), 1));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(0, dp(150), 1);
            gap.leftMargin = dp(12);
            row.addView(currentCard, gap);
        } else if (powerCard == null) {
            powerCard = new SparklineCardView(this);
            tempCard = new SparklineCardView(this);
            row.addView(powerCard, new LinearLayout.LayoutParams(0, dp(150), 1));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(0, dp(150), 1);
            gap.leftMargin = dp(12);
            row.addView(tempCard, gap);
        } else {
            healthCard = new SparklineCardView(this);
            pluggedCard = new SparklineCardView(this);
            row.addView(healthCard, new LinearLayout.LayoutParams(0, dp(150), 1));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(0, dp(150), 1);
            gap.leftMargin = dp(12);
            row.addView(pluggedCard, gap);
        }

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(150));
        if (!first) rowLp.topMargin = dp(12);
        grid.addView(row, rowLp);
    }

    private void toggleBypass() {
        if (busy) return;
        busy = true;
        monitorAction.setEnabled(false);
        worker.execute(() -> {
            boolean root = RootShell.isRootAvailable();
            boolean current = root && RootShell.isBypassEnabled();
            boolean target = !current;
            boolean ok = root && RootShell.setBypass(target);
            PowerReader.Snapshot s = PowerReader.snapshot(this);
            main.post(() -> {
                busy = false;
                monitorAction.setEnabled(true);
                refresh(s, target, root);
                Toast.makeText(this,
                        ok ? (target ? "Bypass enabled" : "Normal charging restored") : "Unable to change bypass state",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void refresh(PowerReader.Snapshot s, boolean bypass, boolean rooted) {
        float level = parseLevel(s.battery);
        gauge.setValues(level, s.powerW, s.plugged);

        voltageCard.setData("VOLTAGE", String.format(Locale.US, "%.0f mV", s.voltageV * 1000), "ϟ",
                s.voltageV / 5.0, GREEN, true);
        currentCard.setData("CURRENT", String.format(Locale.US, "%+.0f mA", s.currentA * 1000), "≈",
                Math.min(s.currentA / 5.0, .95), GREEN, true);
        powerCard.setData("WATTAGE", String.format(Locale.US, "%+.1f W", s.powerW), "▣",
                Math.min(s.powerW / 30.0, .95), GREEN, true);
        tempCard.setData("TEMPERATURE", String.format(Locale.US, "%.1f°C", s.temperatureC), "♨",
                Math.min(s.temperatureC / 50.0, .95), 0xFFFFA7A0, true);

        String health = s.healthPercent > 0
                ? String.format(Locale.US, "%.0f%%", s.healthPercent)
                : healthText();
        healthCard.setData("HEALTH", health, "♥",
                s.healthPercent > 0 ? s.healthPercent / 100.0 : .65, 0xFFFFB29F, false);
        pluggedCard.setData("PLUGGED", s.charger, "▣", s.plugged ? .8 : .2, TEXT, false);

        monitorTitle.setText(bypass ? "Bypass Active" : "Monitoring Active");
        monitorText.setText(bypass
                ? "Battery is isolated from normal charging"
                : (s.plugged ? "Live charging stats and bypass state" : "Connect a charger for live telemetry"));
        monitorAction.setText(bypass ? "Disable" : "Enable");
        monitorDot.setTextColor(bypass ? GREEN : (s.plugged ? GREEN : MUTED));
        rootStatus.setText(rooted ? "Root access granted" : "Root access required");
    }

    private float parseLevel(String s) {
        try { return Float.parseFloat(s.replace("%", "").trim()); }
        catch (Exception e) { return 0; }
    }

    private String healthText() {
        Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int h = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        switch (h) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "GOOD";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "HOT";
            case BatteryManager.BATTERY_HEALTH_COLD: return "COLD";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "BAD";
            default: return "UNKNOWN";
        }
    }

    private final Runnable update = new Runnable() {
        @Override public void run() {
            if (!isFinishing()) {
                worker.execute(() -> {
                    PowerReader.Snapshot s = PowerReader.snapshot(MainActivity.this);
                    boolean root = RootShell.isRootAvailable();
                    boolean bypass = root && RootShell.isBypassEnabled();
                    main.post(() -> refresh(s, bypass, root));
                });
                main.postDelayed(this, 1000);
            }
        }
    };

    @Override protected void onDestroy() {
        main.removeCallbacks(update);
        worker.shutdownNow();
        super.onDestroy();
    }
}
