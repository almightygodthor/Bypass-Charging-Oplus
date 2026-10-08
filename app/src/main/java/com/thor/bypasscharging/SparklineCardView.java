package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

public final class SparklineCardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final float[] samples = new float[32];
    private String label = "", value = "--", icon = "";
    private int accent = 0xFF9DD49D;
    private boolean showGraph = true;
    private boolean initialized;

    public SparklineCardView(Context c) { super(c); init(); }
    public SparklineCardView(Context c, AttributeSet a) { super(c); init(); }

    private void init() {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setData(String label, String value, String icon, double normalized, int accent) {
        setData(label, value, icon, normalized, accent, true);
    }

    public void setData(String label, String value, String icon, double normalized, int accent, boolean showGraph) {
        this.label = label;
        this.value = value;
        this.icon = icon;
        this.accent = accent;
        this.showGraph = showGraph;

        float sample = Math.max(.08f, Math.min(.92f, (float) normalized));
        if (!initialized) {
            Arrays.fill(samples, sample);
            initialized = true;
        } else {
            System.arraycopy(samples, 1, samples, 0, samples.length - 1);
            samples[samples.length - 1] = sample;
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF1C211B);
        c.drawRoundRect(new RectF(0, 0, w, h), dp(24), dp(24), paint);

        if (showGraph) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.4f));
            paint.setColor(0xFF505B63);
            line.reset();
            float base = h * .46f;
            float amp = h * .20f;
            for (int i = 0; i < samples.length; i++) {
                float x = w * i / (samples.length - 1);
                float y = base - (samples[i] - .5f) * amp * 2f;
                if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            }
            c.drawPath(line, paint);

            paint.setStrokeWidth(dp(1));
            paint.setColor(0x553D4540);
            c.drawLine(0, base, w, base, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(0xFFE3E5E0);
        paint.setTextSize(sp(20));
        c.drawText(icon, dp(18), dp(29), paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setLetterSpacing(.10f);
        paint.setColor(0xFFD1D5D0);
        paint.setTextSize(sp(11));
        c.drawText(label, w - dp(18), dp(28), paint);
        paint.setLetterSpacing(0);

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(sp(23));
        paint.setColor(value.startsWith("+") ? accent : 0xFFF0F0EA);
        c.drawText(value, dp(18), h - dp(18), paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(sp(33));
        paint.setColor(0xFF747B75);
        c.drawText("›", w - dp(16), h - dp(14), paint);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
}
