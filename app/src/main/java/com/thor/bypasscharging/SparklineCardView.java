package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.animation.ValueAnimator;
import android.view.animation.PathInterpolator;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

public final class SparklineCardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final Path fill = new Path();
    private final float[] samples = new float[36];
    private String label = "", value = "--", icon = "";
    private int accent = 0xFF9DD49D;
    private boolean showGraph = true;
    private boolean initialized;
    private int sampleCursor;
    private ValueAnimator sampleAnimator;

    public SparklineCardView(Context c) { super(c); init(); }
    public SparklineCardView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
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

        float sample = Math.max(.05f, Math.min(.95f, (float) normalized));
        if (!initialized) {
            Arrays.fill(samples, sample);
            initialized = true;
        } else {
            float previous = samples[samples.length - 1];
            System.arraycopy(samples, 1, samples, 0, samples.length - 1);
            samples[samples.length - 1] = previous;
            sampleCursor++;

            if (sampleAnimator != null) sampleAnimator.cancel();
            sampleAnimator = ValueAnimator.ofFloat(previous, sample);
            sampleAnimator.setDuration(760);
            sampleAnimator.setInterpolator(new PathInterpolator(.16f, 1f, .3f, 1f));
            sampleAnimator.addUpdateListener(a -> {
                samples[samples.length - 1] = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            sampleAnimator.start();
        }
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float radius = dp(24);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF1B211B);
        c.drawRoundRect(new RectF(0, 0, w, h), radius, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0x263E4940);
        c.drawRoundRect(new RectF(dp(.5f), dp(.5f), w - dp(.5f), h - dp(.5f)),
                radius - dp(.5f), radius - dp(.5f), paint);

        if (showGraph) {
            float top = dp(48);
            float bottom = h - dp(50);
            float range = Math.max(dp(24), bottom - top);
            float base = top + range * .58f;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0x243E4940);
            for (int i = 1; i < 4; i++) {
                float y = top + range * i / 4f;
                c.drawLine(dp(14), y, w - dp(14), y, paint);
            }
            for (int i = 1; i < 6; i++) {
                float x = dp(14) + (w - dp(28)) * i / 6f;
                c.drawLine(x, top, x, bottom, paint);
            }

            line.reset();
            fill.reset();
            for (int i = 0; i < samples.length; i++) {
                float x = dp(14) + (w - dp(28)) * i / (samples.length - 1);
                float y = base - (samples[i] - .5f) * range * .82f;
                if (i == 0) {
                    line.moveTo(x, y);
                    fill.moveTo(x, bottom);
                    fill.lineTo(x, y);
                } else {
                    line.lineTo(x, y);
                    fill.lineTo(x, y);
                }
            }
            fill.lineTo(w - dp(14), bottom);
            fill.close();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor((accent & 0x00FFFFFF) | 0x12000000);
            c.drawPath(fill, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.2f));
            paint.setColor((accent & 0x00FFFFFF) | 0xB0000000);
            c.drawPath(line, paint);

            int last = samples.length - 1;
            float dotX = dp(14) + (w - dp(28)) * last / (samples.length - 1);
            float dotY = base - (samples[last] - .5f) * range * .82f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(accent);
            c.drawCircle(dotX, dotY, dp(3.5f), paint);

            if (sampleCursor > 0) {
                paint.setColor(0x339DD49D);
                c.drawCircle(dotX, dotY, dp(7), paint);
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(0xFFE7E9E4);
        paint.setTextSize(sp(19));
        c.drawText(icon, dp(18), dp(30), paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setLetterSpacing(.11f);
        paint.setColor(0xFFBFC6BF);
        paint.setTextSize(sp(10));
        c.drawText(label, w - dp(18), dp(29), paint);
        paint.setLetterSpacing(0);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(sp(22));
        paint.setColor(value.startsWith("+") ? accent : 0xFFF0F0EA);
        c.drawText(value, w / 2f, h - dp(17), paint);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
}
