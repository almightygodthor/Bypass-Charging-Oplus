package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public final class ChargingGaugeView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float level;
    private double power;
    private boolean charging;

    private final int track = 0xFF202721;
    private final int green = 0xFF9DE49D;
    private final int text = 0xFFF1F2EC;
    private final int muted = 0xFFB8C0B7;

    public ChargingGaugeView(Context c) { super(c); init(); }
    public ChargingGaugeView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setValues(float level, double power, boolean charging) {
        this.level = Math.max(0, Math.min(100, level));
        this.power = power;
        this.charging = charging;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f;
        float cy = getHeight() * .53f;
        float r = Math.min(getWidth(), getHeight()) * .34f;
        RectF oval = new RectF(cx-r, cy-r, cx+r, cy+r);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(26);
        paint.setColor(track);
        c.drawArc(oval, 135, 270, false, paint);
        paint.setColor(green);
        c.drawArc(oval, 135, 270 * (level / 100f), false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        paint.setColor(text);
        paint.setTextSize(sp(46));
        c.drawText(String.format(Locale.US, "%.1f", level), cx - 4, cy + 2, paint);

        paint.setColor(green);
        paint.setTextSize(sp(24));
        c.drawText("%", cx + 72, cy + 2, paint);

        paint.setColor(muted);
        paint.setTextSize(sp(13));
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText(charging ? "C H A R G I N G" : "O N   B A T T E R Y", cx, cy + 48, paint);

        float pillW = 154, pillH = 40, left = cx-pillW/2, top = cy+70;
        paint.setColor(0x222E6A36);
        c.drawRoundRect(left, top, left+pillW, top+pillH, 20,20,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(0x664E8156);
        c.drawRoundRect(left, top, left+pillW, top+pillH, 20,20,paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(green);
        paint.setTextSize(sp(16));
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText(String.format(Locale.US, "%+.1f W", power), cx, top+26, paint);
    }

    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
}
