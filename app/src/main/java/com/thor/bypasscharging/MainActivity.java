package com.thor.bypasscharging;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.BatteryManager;
import android.os.Build;
import android.content.SharedPreferences;
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
    private TextView monitorDot, monitorTitle, monitorText, rootStatus, subtitleView;
    private GlassSwitchView bypassSwitch;
    private FrameLayout rootContainer;
    private ScrollView scroll;
    private RootAccessOverlay rootOverlay;
    private boolean busy;
    private SharedPreferences prefs;

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
        l.setBackground(roundGlass(CARD, 24));
        l.setElevation(dp(2));
        return l;
    }

    private android.graphics.drawable.GradientDrawable roundGlass(int color, float r) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(0xC91F2921);
        g.setCornerRadius(dp(r));
        g.setStroke(dp(1), 0x305B6D5F);
        return g;
    }

    private android.graphics.drawable.GradientDrawable round(int color, float r) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    private TextView section(String s) {
        TextView v = tv(s, 12, MUTED);
        v.setTypeface(AppTypography.labelMedium());
        v.setLetterSpacing(.12f);
        v.setPadding(0, dp(2), 0, dp(2));
        return v;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().getAttributes().preferredRefreshRate = 120f;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(12), dp(18), dp(22));
        content.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = tv("OPLUS BYPASS", 25, TEXT);
        title.setTypeface(AppTypography.displayMedium());
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView debug = tv("⋮", 28, MUTED);
        debug.setGravity(Gravity.CENTER);
        debug.setContentDescription("Open debug log");
        debug.setOnClickListener(v -> startActivity(new Intent(this, DebugLogActivity.class)));
        header.addView(debug, new LinearLayout.LayoutParams(dp(34), dp(48)));
        content.addView(header);

        subtitleView = tv("Bypass charging", 14, MUTED);
        TextView subtitle = subtitleView;
        subtitle.setTypeface(AppTypography.body());
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
        monitor.setPadding(dp(16), dp(14), dp(12), dp(14));
        LinearLayout monRow = new LinearLayout(this);
        monRow.setGravity(Gravity.CENTER_VERTICAL);

        monitorDot = tv("●", 17, GREEN);
        monitorDot.setGravity(Gravity.CENTER);
        monRow.addView(monitorDot, new LinearLayout.LayoutParams(dp(26), dp(56)));

        LinearLayout monText = new LinearLayout(this);
        monText.setOrientation(LinearLayout.VERTICAL);
        monText.setGravity(Gravity.CENTER_VERTICAL);
        monitorTitle = tv("BYPASS CHARGING", 15, TEXT);
        monitorTitle.setTypeface(AppTypography.labelMedium());
        monitorTitle.setLetterSpacing(.04f);
        monitorText = tv("Direct battery bypass control", 12, MUTED);
        monitorText.setPadding(0, dp(4), 0, 0);
        monText.addView(monitorTitle);
        monText.addView(monitorText);
        monRow.addView(monText, new LinearLayout.LayoutParams(0, dp(56), 1));

        bypassSwitch = new GlassSwitchView(this);
        bypassSwitch.setOnCheckedChangeListener(checked -> toggleBypass());
        monRow.addView(bypassSwitch, new LinearLayout.LayoutParams(dp(108), dp(52)));
        monitor.addView(monRow);
        content.addView(monitor, new LinearLayout.LayoutParams(-1, dp(84)));

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

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            content.setPadding(
                    dp(18) + insets.getSystemWindowInsetLeft(),
                    dp(12) + insets.getSystemWindowInsetTop(),
                    dp(18) + insets.getSystemWindowInsetRight(),
                    dp(22) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(content);

        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(BG);
        rootContainer.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        rootOverlay = new RootAccessOverlay(this);
        rootOverlay.setListener(this::checkRootFromOverlay);
        rootContainer.addView(rootOverlay, new FrameLayout.LayoutParams(-1, -1));
        setContentView(rootContainer);

        prefs = getSharedPreferences("ui_state", MODE_PRIVATE);
        if (prefs.getBoolean("root_gate_completed", false)) {
            rootOverlay.setVisibility(View.GONE);
        } else {
            showRootGate(false);
        }

        animateDashboard(content);
        DebugLog.add("MainActivity started");
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

    private void checkRootFromOverlay() {
        if (busy) return;
        busy = true;
        rootOverlay.setChecking();
        DebugLog.add("Root permission check requested");
        worker.execute(() -> {
            boolean root = RootShell.isRootAvailable();
            DebugLog.add("Root permission result=" + root);
            main.post(() -> {
                busy = false;
                rootOverlay.setResult(root);
                if (root) {
                    prefs.edit().putBoolean("root_gate_completed", true).apply();
                    clearRootBlur();
                    rootOverlay.dismissAnimated();
                    Toast.makeText(this, "Root access granted", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showRootGate(boolean animate) {
        if (rootOverlay == null || scroll == null) return;
        rootOverlay.setVisibility(View.VISIBLE);
        rootOverlay.setAlpha(0f);
        rootOverlay.setScaleX(.94f);
        rootOverlay.setScaleY(.94f);
        if (Build.VERSION.SDK_INT >= 31) {
            scroll.setRenderEffect(RenderEffect.createBlurEffect(dp(12), dp(12), Shader.TileMode.CLAMP));
        }
        if (animate) {
            rootOverlay.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(260).setInterpolator(new android.view.animation.OvershootInterpolator(1.15f)).start();
        } else {
            rootOverlay.setAlpha(1f);
            rootOverlay.setScaleX(1f);
            rootOverlay.setScaleY(1f);
        }
    }

    private void clearRootBlur() {
        if (scroll != null && Build.VERSION.SDK_INT >= 31) {
            scroll.setRenderEffect(null);
        }
    }

    private void animateDashboard(LinearLayout content) {
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(16));
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(45L * i)
                    .setDuration(480)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.9f))
                    .start();
        }
    }

    private void toggleBypass() {
        if (busy) return;
        busy = true;
        bypassSwitch.setEnabled(false);
        worker.execute(() -> {
            boolean root = RootShell.isRootAvailable();
            if (!root) {
                main.post(() -> {
                    busy = false;
                    bypassSwitch.setEnabled(true);
                    showRootGate(true);
                });
                return;
            }
            boolean current = RootShell.isBypassEnabled();
            boolean target = !current;
            boolean ok = root && RootShell.setBypass(target);
            DebugLog.add("Bypass target=" + target + " result=" + ok);
            PowerReader.Snapshot s = PowerReader.snapshot(this);
            main.post(() -> {
                busy = false;
                bypassSwitch.setEnabled(true);
                refresh(s, target, root);
                Toast.makeText(this,
                        ok ? (target ? "Bypass enabled" : "Normal charging restored") : "Unable to change bypass state",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void refresh(PowerReader.Snapshot s, boolean bypass, boolean rooted) {
        float level = parseLevel(s.battery);
        gauge.setValues(level, bypass ? 0.0 : s.powerW, s.charging, bypass);

        voltageCard.setData("VOLTAGE", String.format(Locale.US, "%.2f V", s.voltageV), "ϟ",
                s.voltageV / 10.0, GREEN, true);
        double displayCurrentA = bypass ? 0.0 : s.currentA;
        double displayPowerW = bypass ? 0.0 : s.powerW;
        currentCard.setData("CURRENT", bypass ? "0 mA" : String.format(Locale.US, "%+.0f mA", displayCurrentA * 1000), "⇆",
                bypass ? .05 : Math.min(Math.abs(displayCurrentA) / 5.0, .95), GREEN, true);
        powerCard.setData("WATTAGE", bypass ? "0.0 W" : String.format(Locale.US, "%+.1f W", displayPowerW), "ϟ",
                bypass ? .05 : Math.min(Math.abs(displayPowerW) / 30.0, .95), GREEN, true);
        tempCard.setData("TEMPERATURE", String.format(Locale.US, "%.1f°C", s.temperatureC), "♨",
                Math.min(s.temperatureC / 50.0, .95), 0xFFFFA7A0, true);

        String health = s.healthPercent > 0
                ? String.format(Locale.US, "%.0f%%", s.healthPercent)
                : healthText();
        healthCard.setData("HEALTH", health, "♥",
                s.healthPercent > 0 ? s.healthPercent / 100.0 : .65, 0xFFFFB29F, false);
        pluggedCard.setData("PLUGGED", s.charger, "⎔", s.plugged ? .8 : .2, TEXT, false);

        subtitleView.setText(GtNeo3Variant.label(s.variant));
        monitorTitle.setText("BYPASS CHARGING");
        monitorText.setText(bypass
                ? "Battery bypass is enabled"
                : (s.plugged ? "Normal charging is active" : "Connect a charger to enable bypass"));
        bypassSwitch.setChecked(bypass);
        monitorDot.setTextColor(bypass ? GREEN : (s.plugged ? GREEN : MUTED));
        rootStatus.setText(rooted ? "Root access granted" : "Root access required");
        DebugLog.add(String.format(Locale.US,
                "UI update: variant=%s design=%dmAh cells=%d rawVoltage=%.3fV voltage=%.3fV rawCurrent=%.3fA current=%.3fA power=%.3fW temp=%.1fC plugged=%s charging=%s charger=%s bypass=%s root=%s",
                GtNeo3Variant.label(s.variant), s.designCapacityMah, s.cellCount, s.rawVoltageV, s.voltageV, s.rawCurrentA, s.currentA, s.powerW, s.temperatureC,
                s.plugged, s.charging, s.charger, bypass, rooted));
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
                    DebugLog.add("Telemetry snapshot captured");
                    boolean gateCompleted = prefs != null &&
                            prefs.getBoolean("root_gate_completed", false);
                    boolean root = gateCompleted && RootShell.isRootAvailable();
                    boolean bypass = root && RootShell.isBypassEnabled();
                    main.post(() -> {
                        if (gateCompleted && !root) {
                            prefs.edit().putBoolean("root_gate_completed", false).apply();
                            showRootGate(true);
                            rootOverlay.setRevoked();
                            DebugLog.add("Root access revoked; dashboard locked");
                            return;
                        }
                        refresh(s, bypass, root);
                    });
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
