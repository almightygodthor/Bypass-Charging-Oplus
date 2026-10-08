package com.thor.bypasscharging;
import android.content.*;
public class PowerReceiver extends BroadcastReceiver { @Override public void onReceive(Context c, Intent i) { if(Intent.ACTION_POWER_DISCONNECTED.equals(i.getAction())) RootShell.setBypass(false); } }
