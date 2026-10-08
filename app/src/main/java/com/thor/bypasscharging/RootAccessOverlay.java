package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class RootAccessOverlay extends FrameLayout {
    public interface Listener {
        void onCheckRoot();
    }

    private final TextView status;
    private final TextView check;
    private Listener listener;

    private static final int BG = 0xCC08100B;
    private static final int CARD = 0xF21B241D;
    private static final int TEXT = 0xFFF1F4EF;
    private static final int MUTED = 0xFFADB8AE;
    private static final int GREEN = 0xFF9DD49D;

    public RootAccessOverlay(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(BG);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(28), dp(28), dp(28), dp(28));
        card.setBackground(round(CARD, 30));

        TextView icon = text("◆", 26, GREEN);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(-1, dp(38)));

        TextView title = text("ROOT ACCESS", 22, TEXT);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, dp(34));
        titleLp.topMargin = dp(8);
        card.addView(title, titleLp);

        TextView body = text(
                "Bypass charging needs superuser access to control the OPLUS charging node.\n\n"
                        + "Tap below to check your root permission. If your root manager shows a prompt, allow access and check again.",
                13, MUTED);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(dp(2), 1.08f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, dp(108));
        bodyLp.topMargin = dp(8);
        card.addView(body, bodyLp);

        status = text("Root status: not checked", 12, MUTED);
        status.setGravity(Gravity.CENTER);
        card.addView(status, new LinearLayout.LayoutParams(-1, dp(28)));

        check = text("CHECK ROOT PERMISSION", 13, Color.rgb(12, 25, 14));
        check.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        check.setGravity(Gravity.CENTER);
        check.setLetterSpacing(.05f);
        check.setBackground(round(GREEN, 18));
        check.setOnClickListener(v -> {
            if (listener != null) listener.onCheckRoot();
        });
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(-1, dp(52));
        checkLp.topMargin = dp(12);
        card.addView(check, checkLp);

        // Let the card size itself from its content. The previous fixed 342dp
        // height was 4dp shorter than the child stack, clipping the lower edge
        // of the action button on some densities.
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                Math.min(dp(360), getResources().getDisplayMetrics().widthPixels - dp(36)),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        cardLp.gravity = Gravity.CENTER;
        addView(card, cardLp);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setChecking() {
        status.setText("Root status: requesting permission…");
        status.setTextColor(GREEN);
        check.setText("CHECKING…");
        check.setEnabled(false);
        check.setAlpha(.65f);
    }

    public void setResult(boolean granted) {
        check.setEnabled(true);
        check.setAlpha(1f);
        if (granted) {
            status.setText("Root status: granted");
            status.setTextColor(GREEN);
            check.setText("CONTINUE");
        } else {
            status.setText("Root status: not granted");
            status.setTextColor(Color.rgb(255, 174, 164));
            check.setText("CHECK ROOT PERMISSION");
        }
    }

    public void dismissAnimated() {
        animate().alpha(0f).scaleX(.94f).scaleY(.94f).setDuration(220).withEndAction(() -> {
            setVisibility(GONE);
            setAlpha(1f);
            setScaleX(1f);
            setScaleY(1f);
        }).start();
    }

    private TextView text(String s, float size, int color) {
        TextView v = new TextView(getContext());
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), 0x335E765F);
        return g;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
