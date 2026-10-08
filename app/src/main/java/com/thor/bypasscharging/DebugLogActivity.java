package com.thor.bypasscharging;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class DebugLogActivity extends Activity {
    private static final int BG = Color.rgb(16, 21, 15);
    private static final int CARD = Color.rgb(28, 33, 27);
    private static final int TEXT = Color.rgb(240, 238, 234);
    private static final int MUTED = Color.rgb(184, 192, 185);
    private static final int GREEN = Color.rgb(157, 212, 157);

    private TextView logView;

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(10), dp(18), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = text("‹", 38, TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(52)));

        TextView title = text("DEBUG LOG", 20, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button clear = button("CLEAR");
        clear.setOnClickListener(v -> {
            DebugLog.clear();
            refresh();
        });
        header.addView(clear, new LinearLayout.LayoutParams(dp(76), dp(44)));

        Button copy = button("COPY");
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Oplus Bypass debug log", DebugLog.dump()));
            Toast.makeText(this, "Debug log copied", Toast.LENGTH_SHORT).show();
        });
        header.addView(copy, new LinearLayout.LayoutParams(dp(76), dp(44)));

        root.addView(header);

        TextView subtitle = text("Telemetry sources, root state and bypass operations", 12, MUTED);
        subtitle.setPadding(dp(48), 0, 0, dp(12));
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(round(CARD, 22));

        logView = text("", 11, Color.rgb(210, 218, 211));
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(dp(14), dp(14), dp(14), dp(18));
        scroll.addView(logView);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (logView != null) refresh();
    }

    private void refresh() {
        logView.setText(DebugLog.dump());
        logView.post(() -> logView.getParent().requestLayout());
    }

    private TextView text(String s, float size, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(10);
        b.setTextColor(GREEN);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private android.graphics.drawable.GradientDrawable round(int color, float radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
}
