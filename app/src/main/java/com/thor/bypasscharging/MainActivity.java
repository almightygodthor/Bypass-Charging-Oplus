package com.thor.bypasscharging;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView state, battery, current, voltage, power, root, plug, node;
    private Button toggle;

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float sp) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(Color.WHITE);
        v.setTextSize(sp);
        v.setFontFeatureSettings("kern");
        return v;
    }

    private GradientDrawable bg(int fill, int stroke, float radius, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (strokeWidth > 0) g.setStroke(dp(strokeWidth), stroke);
        return g;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(bg(Color.rgb(18, 29, 51), Color.rgb(55, 145, 255), 24, 1));
        return c;
    }

    private TextView caption(String s) {
        TextView v = text(s, 11);
        v.setTextColor(Color.rgb(135, 184, 235));
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(0.08f);
        return v;
    }

    private LinearLayout metric(String label, TextView[] holder) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        c.setBackground(bg(Color.rgb(14, 25, 45), Color.rgb(31, 71, 125), 22, 1));

        TextView l = caption(label);
        l.setGravity(Gravity.CENTER);
        TextView value = text("--", 22);
        value.setGravity(Gravity.CENTER);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(l, new LinearLayout.LayoutParams(-1, dp(22)));
        c.addView(value, new LinearLayout.LayoutParams(-1, dp(40)));
        holder[0] = value;
        return c;
    }

    @Override
    public void onCreate(Bundle stateBundle) {
        super.onCreate(stateBundle);

        getWindow().setStatusBarColor(Color.rgb(5, 9, 17));
        getWindow().setNavigationBarColor(Color.rgb(5, 9, 17));
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(28));
        content.setBackgroundColor(Color.rgb(5, 9, 17));

        TextView title = text("OPLUS BYPASS", 28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(236, 247, 255));
        title.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(title, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView subtitle = text("Vivid Glass • live charging telemetry", 13);
        subtitle.setTextColor(Color.rgb(123, 177, 229));
        content.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(30)));

        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView heroLabel = caption("CHARGING CONTROL");
        heroLabel.setGravity(Gravity.CENTER);
        hero.addView(heroLabel, new LinearLayout.LayoutParams(-1, dp(24)));

        this.state = text("CHECKING…", 25);
        this.state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        this.state.setGravity(Gravity.CENTER);
        hero.addView(this.state, new LinearLayout.LayoutParams(-1, dp(42)));

        this.battery = text("Battery --", 14);
        this.battery.setTextColor(Color.rgb(177, 208, 238));
        this.battery.setGravity(Gravity.CENTER);
        hero.addView(this.battery, new LinearLayout.LayoutParams(-1, dp(28)));

        content.addView(hero, new LinearLayout.LayoutParams(-1, dp(145)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView[] a = new TextView[1];
        TextView[] b = new TextView[1];
        row.addView(metric("CURRENT", a), new LinearLayout.LayoutParams(0, dp(92), 1));
        LinearLayout spacer = new LinearLayout(this);
        row.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));
        row.addView(metric("VOLTAGE", b), new LinearLayout.LayoutParams(0, dp(92), 1));
        current = a[0];
        voltage = b[0];
        content.addView(row);

        LinearLayout powerCard = card();
        powerCard.setGravity(Gravity.CENTER);
        TextView powerLabel = caption("LIVE POWER");
        powerLabel.setGravity(Gravity.CENTER);
        powerCard.addView(powerLabel, new LinearLayout.LayoutParams(-1, dp(22)));
        power = text("0.00 W", 30);
        power.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        power.setGravity(Gravity.CENTER);
        power.setTextColor(Color.rgb(83, 183, 255));
        powerCard.addView(power, new LinearLayout.LayoutParams(-1, dp(48)));
        content.addView(powerCard, new LinearLayout.LayoutParams(-1, dp(98)));

        LinearLayout info = card();
        info.setPadding(dp(18), dp(12), dp(18), dp(12));

        root = text("Root: checking…", 13);
        root.setTextColor(Color.rgb(205, 225, 245));
        plug = text("Charger: checking…", 13);
        plug.setTextColor(Color.rgb(205, 225, 245));
        node = text("OPLUS node: checking…", 13);
        node.setTextColor(Color.rgb(135, 184, 235));

        info.addView(root, new LinearLayout.LayoutParams(-1, dp(28)));
        info.addView(plug, new LinearLayout.LayoutParams(-1, dp(28)));
        info.addView(node, new LinearLayout.LayoutParams(-1, dp(28)));
        content.addView(info, new LinearLayout.LayoutParams(-1, dp(100)));

        toggle = new Button(this);
        toggle.setText("ENABLE BYPASS");
        toggle.setTextSize(15);
        toggle.setTextColor(Color.WHITE);
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setAllCaps(false);
        toggle.setGravity(Gravity.CENTER);
        toggle.setBackground(bg(Color.rgb(28, 126, 224), Color.rgb(75, 176, 255), 20, 1));
        toggle.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(-1, dp(58));
        toggleLp.topMargin = dp(14);
        content.addView(toggle, toggleLp);

        TextView note = text("Requires root • OPLUS charging node control", 11);
        note.setTextColor(Color.rgb(94, 121, 151));
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, dp(30));
        noteLp.topMargin = dp(6);
        content.addView(note, noteLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(5, 9, 17));
        scroll.addView(content);
        setContentView(scroll);

        toggle.setOnClickListener(v -> {
            boolean target = !RootShell.isBypassEnabled();
            boolean ok = RootShell.setBypass(target);
            refresh();
            Toast.makeText(this,
                    ok ? (target ? "Bypass charging enabled" : "Normal charging restored")
                       : "Root or OPLUS charging node unavailable",
                    Toast.LENGTH_SHORT).show();
        });

        handler.post(update);
    }

    private void refresh() {
        boolean bypass = RootShell.isBypassEnabled();
        boolean rooted = RootShell.isRootAvailable();
        boolean plugged = PowerReader.plugged();

        state.setText(bypass ? "BYPASS ACTIVE" : "CHARGING NORMAL");
        state.setTextColor(bypass ? Color.rgb(75, 190, 255) : Color.WHITE);
        toggle.setText(bypass ? "DISABLE BYPASS" : "ENABLE BYPASS");

        root.setText("Root: " + (rooted ? "Granted ✓" : "Unavailable ✕"));
        plug.setText("Charger: " + (plugged ? "Connected ✓" : "Disconnected"));
        node.setText("OPLUS node: " + (bypass ? "Bypass enabled" : "Normal charging"));

        battery.setText("Battery " + PowerReader.battery());
        current.setText(String.format(Locale.US, "%.2f A", PowerReader.currentA()));
        voltage.setText(String.format(Locale.US, "%.2f V", PowerReader.voltageV()));
        power.setText(String.format(Locale.US, "%.2f W", PowerReader.powerW()));
    }

    private final Runnable update = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onDestroy() {
        handler.removeCallbacks(update);
        super.onDestroy();
    }
}
