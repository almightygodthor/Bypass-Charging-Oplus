package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.animation.ValueAnimator;
import android.view.animation.PathInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class GlassSwitchView extends View {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean checked;
    private float progress;
    private OnCheckedChangeListener listener;
    private ValueAnimator animator;

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
        if (this.checked == checked && Math.abs(progress - (checked ? 1f : 0f)) < .01f) return;
        this.checked = checked;
        animateProgress(checked ? 1f : 0f);
    }

    private void animateProgress(float target) {
        if (animator != null) animator.cancel();
        float start = progress;
        animator = ValueAnimator.ofFloat(start, target);
        animator.setDuration(360);
        animator.setInterpolator(new PathInterpolator(.18f, 1.25f, .3f, 1f));
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        animator.start();
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
        paint.setColor(blend(TRACK, TRACK_ON, progress));
        canvas.drawRoundRect(track, trackH / 2f, trackH / 2f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setColor(blend(BORDER, BORDER_ON, progress));
        canvas.drawRoundRect(track, trackH / 2f, trackH / 2f, paint);

        paint.setStyle(Paint.Style.FILL);
        float thumb = dp(34);
        float padding = dp(6);
        float offX = left + padding;
        float onX = left + trackW - padding - thumb;
        float thumbX = offX + (onX - offX) * progress;
        float thumbTop = cy - thumb / 2f;

        paint.setColor(blend(0xFFDCE1DC, 0xFFF2F8F0, progress));
        canvas.drawRoundRect(
                new RectF(thumbX, thumbTop, thumbX + thumb, thumbTop + thumb),
                thumb / 2f, thumb / 2f, paint);

        paint.setColor(blend(0x22FFFFFF, 0x449DD49D, progress));
        canvas.drawCircle(thumbX + thumb / 2f, thumbTop + thumb / 2f, thumb / 2.6f, paint);

        paint.setTypeface(AppTypography.labelMedium());
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(blend(TEXT, TEXT_ON, progress));
        canvas.drawText(progress > .5f ? "ON" : "OFF", left + trackW / 2f, top + dp(30), paint);
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
        setChecked(!checked);
        if (listener != null) listener.onCheckedChanged(checked);
        return true;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private int blend(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t);
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t);
        return Color.argb(a, r, g, b);
    }
}
