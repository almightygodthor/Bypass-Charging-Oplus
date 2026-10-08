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
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(8), dp(18), dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = text("‹", 38, TEXT);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(54)));

        TextView title = text("DEBUG LOG", 21, TEXT);
        title.setTypeface(AppTypography.displayMedium());
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));

        TextView clear = actionButton("CLEAR");
        clear.setOnClickListener(v -> {
            DebugLog.clear();
            refresh();
        });
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(dp(76), dp(38));
        actionLp.leftMargin = dp(6);
        header.addView(clear, actionLp);

        TextView copy = actionButton("COPY");
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Oplus Bypass debug log", DebugLog.dump()));
            Toast.makeText(this, "Debug log copied", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(dp(76), dp(38));
        copyLp.leftMargin = dp(8);
        header.addView(copy, copyLp);

        root.addView(header);

        TextView subtitle = text("Telemetry sources, root state and bypass operations", 12, MUTED);
        subtitle.setPadding(dp(54), dp(1), 0, dp(12));
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToOutline(true);
        scroll.setBackground(round(CARD, 24));

        logView = text("", 11, Color.rgb(210, 218, 211));
        logView.setTypeface(AppTypography.mono());
        logView.setGravity(Gravity.TOP | Gravity.START);
        logView.setIncludeFontPadding(true);
        logView.setLineSpacing(0f, 1.12f);
        logView.setPadding(dp(16), dp(16), dp(16), dp(22));
        scroll.addView(logView, new ScrollView.LayoutParams(-1, -2));

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollLp.topMargin = dp(2);
        root.addView(scroll, scrollLp);

        setContentView(root);

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            root.setPadding(
                    dp(18) + insets.getSystemWindowInsetLeft(),
                    dp(8) + insets.getSystemWindowInsetTop(),
                    dp(18) + insets.getSystemWindowInsetRight(),
                    dp(10) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        DebugLog.add("Debug log opened");
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (logView != null) refresh();
    }

    private void refresh() {
        logView.setText(DebugLog.dump());
        logView.post(() -> {
            logView.requestLayout();
            ScrollView parent = (ScrollView) logView.getParent();
            parent.fullScroll(View.FOCUS_DOWN);
        });
    }

    private TextView text(String s, float size, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private TextView actionButton(String s) {
        TextView v = text(s, 10, GREEN);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(AppTypography.labelMedium());
        v.setBackground(round(0x332E4631, 10));
        v.setAllCaps(false);
        return v;
    }

    private android.graphics.drawable.GradientDrawable round(int color, float radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), 0x66566E59);
        return d;
    }
}
