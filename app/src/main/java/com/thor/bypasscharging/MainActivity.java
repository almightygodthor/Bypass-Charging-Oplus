package com.thor.bypasscharging;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private static final int BG=Color.rgb(9,15,10);
    private static final int CARD=Color.rgb(27,33,29);
    private static final int TEXT=Color.rgb(241,242,236);
    private static final int MUTED=Color.rgb(184,192,183);
    private static final int GREEN=Color.rgb(157,228,157);
    private static final int LINE=Color.rgb(55,64,56);

    private ChargingGaugeView gauge;
    private SparklineCardView voltageCard,currentCard,powerCard,tempCard,healthCard,pluggedCard;
    private TextView stateTitle, stateSub, monitorDot, monitorTitle, monitorText, monitorAction;
    private Button toggle;
    private boolean busy;

    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView tv(String s,float sp,int color){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); return v;
    }
    private LinearLayout card(){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(CARD,24)); return l;
    }
    private android.graphics.drawable.GradientDrawable round(int color,float r){
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }
    private TextView section(String s){
        TextView v=tv(s,13,MUTED); v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        v.setLetterSpacing(.08f); v.setPadding(0,dp(4),0,dp(4));
        return v;
    }

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18),dp(14),dp(18),dp(26));
        content.setBackgroundColor(BG);

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=tv("OPLUS BYPASS",25,TEXT);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));

        TextView badge=tv("◇",25,GREEN);
        badge.setGravity(Gravity.CENTER);
        header.addView(badge,new LinearLayout.LayoutParams(dp(50),dp(48)));
        content.addView(header);

        TextView subtitle=tv("Bypass charging",14,MUTED);
        LinearLayout.LayoutParams subLp=new LinearLayout.LayoutParams(-1,dp(26));
        subLp.topMargin=-dp(8); content.addView(subtitle,subLp);

        FrameLayout gaugeWrap=new FrameLayout(this);
        gauge=new ChargingGaugeView(this);
        gaugeWrap.addView(gauge,new FrameLayout.LayoutParams(-1,dp(350)));
        LinearLayout.LayoutParams gaugeLp=new LinearLayout.LayoutParams(-1,dp(350));
        gaugeLp.topMargin=dp(2); content.addView(gaugeWrap,gaugeLp);

        LinearLayout monitor=card();
        monitor.setPadding(dp(18),dp(16),dp(18),dp(16));
        LinearLayout monRow=new LinearLayout(this); monRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot=tv("●",16,GREEN); monitorDot=dot;
        monRow.addView(dot,new LinearLayout.LayoutParams(dp(28),dp(48)));

        LinearLayout monText=new LinearLayout(this);
        monText.setOrientation(LinearLayout.VERTICAL);
        monitorTitle=tv("Live Monitoring",16,TEXT);
        monitorTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        monitorText=tv("Live charging stats and bypass state",12,MUTED);
        monitorText.setPadding(0,dp(4),0,0);
        monText.addView(monitorTitle);
        monText.addView(monitorText);
        monRow.addView(monText,new LinearLayout.LayoutParams(0,dp(56),1));

        monitorAction=tv("ACTIVE",14,GREEN);
        monitorAction.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        monitorAction.setGravity(Gravity.CENTER);
        monRow.addView(monitorAction,new LinearLayout.LayoutParams(dp(72),dp(48)));
        monitor.addView(monRow);
        content.addView(monitor,new LinearLayout.LayoutParams(-1,dp(88)));

        content.addView(section("LIVE TELEMETRY"),new LinearLayout.LayoutParams(-1,dp(34)));

        LinearLayout grid=new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row1=new LinearLayout(this);
        row1.setWeightSum(2);
        voltageCard=new SparklineCardView(this); currentCard=new SparklineCardView(this);
        row1.addView(voltageCard,new LinearLayout.LayoutParams(0,dp(150),1));
        LinearLayout.LayoutParams cGap=new LinearLayout.LayoutParams(0,dp(150),1); cGap.leftMargin=dp(12);
        row1.addView(currentCard,cGap);
        grid.addView(row1);

        LinearLayout row2=new LinearLayout(this); row2.setWeightSum(2);
        powerCard=new SparklineCardView(this); tempCard=new SparklineCardView(this);
        row2.addView(powerCard,new LinearLayout.LayoutParams(0,dp(150),1));
        cGap=new LinearLayout.LayoutParams(0,dp(150),1); cGap.leftMargin=dp(12);
        row2.addView(tempCard,cGap);
        LinearLayout.LayoutParams r2lp=new LinearLayout.LayoutParams(-1,dp(150)); r2lp.topMargin=dp(12);
        grid.addView(row2,r2lp);

        LinearLayout row3=new LinearLayout(this); row3.setWeightSum(2);
        healthCard=new SparklineCardView(this); pluggedCard=new SparklineCardView(this);
        row3.addView(healthCard,new LinearLayout.LayoutParams(0,dp(150),1));
        cGap=new LinearLayout.LayoutParams(0,dp(150),1); cGap.leftMargin=dp(12);
        row3.addView(pluggedCard,cGap);
        LinearLayout.LayoutParams r3lp=new LinearLayout.LayoutParams(-1,dp(150)); r3lp.topMargin=dp(12);
        grid.addView(row3,r3lp);
        content.addView(grid);

        TextView charging=section("BYPASS CONTROL");
        LinearLayout.LayoutParams chargeLp=new LinearLayout.LayoutParams(-1,dp(34)); chargeLp.topMargin=dp(14);
        content.addView(charging,chargeLp);

        toggle=new Button(this);
        toggle.setAllCaps(false); toggle.setTextSize(15); toggle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        toggle.setTextColor(Color.rgb(17,35,18)); toggle.setGravity(Gravity.CENTER);
        toggle.setMinHeight(0); toggle.setMinWidth(0); toggle.setPadding(0,0,0,0);
        toggle.setBackground(round(GREEN,22));
        content.addView(toggle,new LinearLayout.LayoutParams(-1,dp(58)));

        stateTitle=tv("CHARGING NORMAL",16,TEXT);
        stateTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        stateTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(-1,dp(28)); st.topMargin=dp(8);
        content.addView(stateTitle,st);

        stateSub=tv("Root access required",11,Color.rgb(105,115,106));
        stateSub.setGravity(Gravity.CENTER);
        content.addView(stateSub,new LinearLayout.LayoutParams(-1,dp(24)));

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        scroll.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            content.setPadding(
                dp(18)+insets.getSystemWindowInsetLeft(),
                dp(14)+insets.getSystemWindowInsetTop(),
                dp(18)+insets.getSystemWindowInsetRight(),
                dp(26)+insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(content); setContentView(scroll);

        toggle.setOnClickListener(v->toggleBypass());
        main.post(update);
    }

    private void toggleBypass(){
        if(busy)return;
        busy=true; toggle.setEnabled(false);
        worker.execute(()->{
            boolean root=RootShell.isRootAvailable();
            boolean current=root&&RootShell.isBypassEnabled();
            boolean target=!current;
            boolean ok=root&&RootShell.setBypass(target);
            main.post(()->{
                busy=false; toggle.setEnabled(true); refresh();
                Toast.makeText(this,ok?(target?"Bypass enabled":"Normal charging restored"):"Unable to change bypass state",Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void refresh(){
        PowerReader.Snapshot s=PowerReader.snapshot(this);
        float level=parseLevel(s.battery);
        gauge.setValues(level,s.powerW,s.plugged);

        voltageCard.setData("VOLTAGE",String.format(Locale.US,"%.0f mV",s.voltageV*1000),"ϟ",
                s.voltageV/5.0,GREEN);
        currentCard.setData("CURRENT",String.format(Locale.US,"%+.0f mA",s.currentA*1000),"≈",
                Math.min(s.currentA/5.0,.95),GREEN);
        powerCard.setData("WATTAGE",String.format(Locale.US,"%+.1f W",s.powerW),"▣",
                Math.min(s.powerW/30.0,.95),GREEN);
        tempCard.setData("TEMPERATURE",String.format(Locale.US,"%.1f°C",s.temperatureC),"♨",
                Math.min(s.temperatureC/50.0,.95),0xFFFFA7A0);
        healthCard.setData("HEALTH",healthText(), "♥", .65, 0xFFFFB29F);
        pluggedCard.setData("PLUGGED",s.charger,"▣",s.plugged?.8:.2,TEXT);

        monitorTitle.setText(s.plugged?"Monitoring Active":"Monitoring Ready");
        monitorText.setText(s.plugged?"Live charging stats and bypass state":"Connect a charger for live input telemetry");
        monitorAction.setText(s.plugged?"ACTIVE":"IDLE");
        toggle.setText(RootShell.isBypassEnabled()?"Disable bypass":"Enable bypass");
        stateTitle.setText(RootShell.isBypassEnabled()?"BYPASS ACTIVE":"CHARGING NORMAL");
        stateTitle.setTextColor(RootShell.isBypassEnabled()?GREEN:TEXT);
    }

    private float parseLevel(String s){
        try{return Float.parseFloat(s.replace("%","").trim());}catch(Exception e){return 0;}
    }

    private String healthText(){
        Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int h=i==null?-1:i.getIntExtra(BatteryManager.EXTRA_HEALTH,-1);
        switch(h){
            case BatteryManager.BATTERY_HEALTH_GOOD:return "GOOD";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:return "HOT";
            case BatteryManager.BATTERY_HEALTH_COLD:return "COLD";
            case BatteryManager.BATTERY_HEALTH_DEAD:return "BAD";
            default:return "UNKNOWN";
        }
    }

    private final Runnable update=new Runnable(){
        @Override public void run(){
            if(!isFinishing()){
                worker.execute(()->{
                    boolean bypass=RootShell.isBypassEnabled();
                    main.post(()->{refresh(); stateTitle.setText(bypass?"BYPASS ACTIVE":"CHARGING NORMAL");});
                });
                main.postDelayed(this,1000);
            }
        }
    };

    @Override protected void onDestroy(){
        main.removeCallbacks(update); worker.shutdownNow(); super.onDestroy();
    }
}
