package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.animation.ValueAnimator;
import android.view.animation.PathInterpolator;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public final class ChargingGaugeView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float level;
    private double power;
    private boolean charging;
    private ValueAnimator animator;

    private static final int TRACK = 0xFF292E2A;
    private static final int GREEN = 0xFF9DD49D;
    private static final int TEXT = 0xFFF0EEEA;
    private static final int MUTED = 0xFFB9C0BA;

    public ChargingGaugeView(Context c) { super(c); init(); }
    public ChargingGaugeView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() { paint.setStrokeCap(Paint.Cap.ROUND); }

    public void setValues(float level, double power, boolean charging) {
        float targetLevel = Math.max(0, Math.min(100, level));
        double targetPower = power;
        float startLevel = this.level;
        double startPower = this.power;

        if (animator != null) animator.cancel();

        this.charging = charging;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(620);
        animator.setInterpolator(new PathInterpolator(.16f, 1f, .3f, 1f));
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float eased = 1f - (1f - t) * (1f - t);
            this.level = startLevel + (targetLevel - startLevel) * eased;
            this.power = startPower + (targetPower - startPower) * eased;
            postInvalidateOnAnimation();
        });
        animator.start();
    }

    @Override protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f;
        float cy = getHeight() * .49f;
        float r = Math.min(getWidth(), getHeight()) * .35f;
        RectF oval = new RectF(cx-r, cy-r, cx+r, cy+r);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(25));
        paint.setColor(TRACK);
        c.drawArc(oval, 135, 270, false, paint);
        paint.setColor(GREEN);
        c.drawArc(oval, 135, 270 * level / 100f, false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setColor(TEXT);
        paint.setTextSize(sp(46));
        c.drawText(String.format(Locale.US, "%.1f", level), cx - dp(4), cy + dp(5), paint);

        paint.setColor(GREEN);
        paint.setTextSize(sp(22));
        c.drawText("%", cx + dp(70), cy + dp(5), paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setLetterSpacing(.30f);
        paint.setColor(MUTED);
        paint.setTextSize(sp(12));
        c.drawText(charging ? "CHARGING" : "ON BATTERY", cx, cy + dp(49), paint);
        paint.setLetterSpacing(0);

        float pillW = dp(154), pillH = dp(40), left = cx-pillW/2, top = cy+dp(68);
        paint.setColor(0x263B533C);
        c.drawRoundRect(left, top, left+pillW, top+pillH, pillH/2, pillH/2, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(0x665E7B61);
        c.drawRoundRect(left, top, left+pillW, top+pillH, pillH/2, pillH/2, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(sp(16));
        paint.setColor(GREEN);
        c.drawText(String.format(Locale.US, "%+.1f W", power), cx, top+dp(26), paint);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
}
