package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class GlassSwitchView extends View {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean checked;
    private OnCheckedChangeListener listener;

    private static final int TRACK = 0xCC253028;
    private static final int TRACK_ON = 0xCC405B45;
    private static final int BORDER = 0x665D6C61;
    private static final int BORDER_ON = 0x889DD49D;
    private static final int THUMB = 0xFFF0F2ED;
    private static final int TEXT = 0xFF8F9890;
    private static final int TEXT_ON = 0xFFBFE8BF;

    public GlassSwitchView(Context context) {
        super(context);
        init();
    }

    public GlassSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        setContentDescription("Bypass charging switch");
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) return;
        this.checked = checked;
        invalidate();
    }

    public boolean isChecked() {
        return checked;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cy = h / 2f;

        float trackW = Math.min(w, dp(116));
        float trackH = dp(46);
        float left = w - trackW;
        float top = cy - trackH / 2f;
        RectF track = new RectF(left, top, left + trackW, top + trackH);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(checked ? TRACK_ON : TRACK);
        canvas.drawRoundRect(track, trackH / 2f, trackH / 2f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setColor(checked ? BORDER_ON : BORDER);
        canvas.drawRoundRect(track, trackH / 2f, trackH / 2f, paint);

        paint.setStyle(Paint.Style.FILL);
        float thumb = dp(34);
        float padding = dp(6);
        float thumbX = checked
                ? left + trackW - padding - thumb
                : left + padding;
        float thumbTop = cy - thumb / 2f;

        paint.setColor(checked ? 0xFFF2F8F0 : 0xFFDCE1DC);
        canvas.drawRoundRect(
                new RectF(thumbX, thumbTop, thumbX + thumb, thumbTop + thumb),
                thumb / 2f, thumb / 2f, paint);

        paint.setColor(checked ? 0x449DD49D : 0x22FFFFFF);
        canvas.drawCircle(thumbX + thumb / 2f, thumbTop + thumb / 2f, thumb / 2.6f, paint);

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(checked ? TEXT_ON : TEXT);
        canvas.drawText(checked ? "ON" : "OFF", left + trackW / 2f, top + dp(30), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        checked = !checked;
        invalidate();
        if (listener != null) listener.onCheckedChanged(checked);
        return true;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
