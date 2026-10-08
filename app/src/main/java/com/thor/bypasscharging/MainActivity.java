package com.thor.bypasscharging;

import android.app.*;import android.os.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import android.content.*;import java.util.Locale;

public class MainActivity extends Activity {
 private final Handler h=new Handler(Looper.getMainLooper()); TextView state,battery,current,voltage,power,root,plug; Button toggle;
 private TextView tv(String s,float sp){ TextView v=new TextView(this); v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(22,12,22,12);return v; }
 private LinearLayout card(){ LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(18,14,18,14);GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(24,35,59));g.setCornerRadius(34);g.setStroke(2,Color.rgb(45,130,235));l.setBackground(g);return l; }
 @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(7,11,20));getWindow().setNavigationBarColor(Color.rgb(7,11,20));
  LinearLayout rootLayout=new LinearLayout(this);rootLayout.setOrientation(LinearLayout.VERTICAL);rootLayout.setPadding(22,20,22,22);rootLayout.setBackgroundColor(Color.rgb(7,11,20));
  TextView title=tv("⚡  OPLUS BYPASS CHARGING",24);title.setTypeface(null,1);rootLayout.addView(title,new LinearLayout.LayoutParams(-1,70));
  TextView sub=tv("Direct power-path control • Root required",14);sub.setTextColor(Color.rgb(150,190,235));rootLayout.addView(sub,new LinearLayout.LayoutParams(-1,45));
  LinearLayout status=card(); state=tv("Checking…",30);state.setGravity(Gravity.CENTER);status.addView(state,new LinearLayout.LayoutParams(-1,75)); battery=tv("Battery --",16);battery.setGravity(Gravity.CENTER);status.addView(battery,new LinearLayout.LayoutParams(-1,45)); rootLayout.addView(status,new LinearLayout.LayoutParams(-1,145));
  LinearLayout row=new LinearLayout(this);row.setPadding(0,18,0,18);row.setWeightSum(2); current=metric("CURRENT");voltage=metric("VOLTAGE");row.addView(current,new LinearLayout.LayoutParams(0,105,1));row.addView(voltage,new LinearLayout.LayoutParams(0,105,1));rootLayout.addView(row);
  LinearLayout p=card(); power=tv("0.00 W",28);power.setGravity(Gravity.CENTER);p.addView(power,new LinearLayout.LayoutParams(-1,65));TextView pl=tv("LIVE POWER",12);pl.setGravity(Gravity.CENTER);pl.setTextColor(Color.rgb(120,175,230));p.addView(pl,new LinearLayout.LayoutParams(-1,30));rootLayout.addView(p,new LinearLayout.LayoutParams(-1,105));
  root=tv("Root: checking…",14);plug=tv("Charger: checking…",14);rootLayout.addView(root,new LinearLayout.LayoutParams(-1,45));rootLayout.addView(plug,new LinearLayout.LayoutParams(-1,40));
  toggle=new Button(this);toggle.setText("TOGGLE BYPASS");toggle.setTextColor(Color.WHITE);toggle.setTextSize(15);toggle.setAllCaps(false);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(25,118,210));bg.setCornerRadius(50);toggle.setBackground(bg);rootLayout.addView(toggle,new LinearLayout.LayoutParams(-1,62));
  toggle.setOnClickListener(v->{boolean ok=RootShell.setBypass(!RootShell.isBypassEnabled());refresh();Toast.makeText(this,ok?"Bypass state changed":"Root or OPLUS charging node unavailable",Toast.LENGTH_SHORT).show();});
  setContentView(rootLayout); h.post(update); }
 private TextView metric(String label){TextView v=tv(label+"\n--",16);v.setGravity(Gravity.CENTER);v.setTextColor(Color.WHITE);GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(18,29,49));g.setCornerRadius(28);v.setBackground(g);return v;}
 private void refresh(){boolean on=RootShell.isBypassEnabled();state.setText(on?"BYPASS ACTIVE":"CHARGING NORMAL");state.setTextColor(on?Color.rgb(75,180,255):Color.WHITE);toggle.setText(on?"DISABLE BYPASS":"ENABLE BYPASS");root.setText("Root: "+(RootShell.isRootAvailable()?"Granted ✓":"Unavailable ✕"));plug.setText("Charger: "+(PowerReader.plugged()?"Connected ✓":"Disconnected"));battery.setText("Battery "+PowerReader.battery());current.setText(String.format(Locale.US,"CURRENT\n%.2f A",PowerReader.currentA()));voltage.setText(String.format(Locale.US,"VOLTAGE\n%.2f V",PowerReader.voltageV()));power.setText(String.format(Locale.US,"%.2f W",PowerReader.powerW()));}
 private final Runnable update= new Runnable(){public void run(){refresh();h.postDelayed(this,1000);}};
 @Override protected void onDestroy(){h.removeCallbacks(update);super.onDestroy();}
}
