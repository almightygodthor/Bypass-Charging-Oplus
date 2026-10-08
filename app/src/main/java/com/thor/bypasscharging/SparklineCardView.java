package com.thor.bypasscharging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public final class SparklineCardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final float[] samples = new float[28];
    private String label = "", value = "--", icon = "";
    private int accent = 0xFF9DE49D;

    public SparklineCardView(Context c) { super(c); init(); }
    public SparklineCardView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        for (int i=0;i<samples.length;i++) samples[i] = .45f + (float)Math.sin(i*.65)*.08f;
    }

    public void setData(String label, String value, String icon, double normalized, int accent) {
        this.label=label; this.value=value; this.icon=icon; this.accent=accent;
        System.arraycopy(samples,1,samples,0,samples.length-1);
        samples[samples.length-1] = Math.max(.15f,Math.min(.85f,(float)normalized));
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float w=getWidth(), h=getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF1B211D);
        c.drawRoundRect(new RectF(0,0,w,h),24,24,paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(0xFF5C6270);
        line.reset();
        for(int i=0;i<samples.length;i++){
            float x=w*i/(samples.length-1);
            float y=h*(.38f+samples[i]*.34f);
            if(i==0) line.moveTo(x,y); else line.lineTo(x,y);
        }
        c.drawPath(line,paint);

        paint.setStrokeWidth(2);
        paint.setColor(0x556E7279);
        c.drawLine(0,h*.38f,w,h*.38f,paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(0xFFD9DED8);
        paint.setTextSize(sp(11));
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText(label,w-18,24,paint);

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(sp(22));
        paint.setColor(value.startsWith("+")?accent:0xFFF1F2EC);
        c.drawText(value,18,h-18,paint);

        paint.setTextSize(sp(20));
        paint.setColor(0xFFE1E4DF);
        c.drawText(icon,18,25,paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(0xFF727870);
        paint.setTextSize(sp(34));
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText("›",w-18,h-16,paint);
    }

    private float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}
}
