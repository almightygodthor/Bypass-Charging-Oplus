package com.thor.bypasscharging;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.FileReader;

public final class PowerReader {
    public static final class Snapshot {
        public final String battery;
        public final double currentA;
        public final double voltageV;
        public final double powerW;
        public final boolean plugged;

        Snapshot(String battery, double currentA, double voltageV, boolean plugged) {
            this.battery = battery;
            this.currentA = currentA;
            this.voltageV = voltageV;
            this.powerW = Math.abs(currentA * voltageV);
            this.plugged = plugged;
        }
    }

    private PowerReader() {}

    public static String read(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String s = r.readLine();
            return s == null ? "" : s.trim();
        } catch (Exception ignored) {
            return RootShell.read(path);
        }
    }

    private static long sysfsNumber(String path) {
        try {
            return Long.parseLong(read(path));
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    public static Snapshot snapshot(Context context) {
        Intent batteryIntent = context.registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        int level = batteryIntent != null
                ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int status = batteryIntent != null
                ? batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        int voltageMv = batteryIntent != null
                ? batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) : 0;

        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        long ua = bm != null
                ? bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                : Long.MIN_VALUE;

        if (ua == Long.MIN_VALUE || ua == 0) {
            ua = sysfsNumber("/sys/class/power_supply/battery/current_now");
        }

        long uv = voltageMv > 0 ? voltageMv * 1000L
                : sysfsNumber("/sys/class/power_supply/battery/voltage_now");

        double currentA = ua == Long.MIN_VALUE ? 0 : Math.abs(ua) / 1_000_000.0;
        double voltageV = uv == Long.MIN_VALUE ? 0 : uv / 1_000_000.0;

        boolean plugged = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;

        return new Snapshot(
                level >= 0 ? level + "%" : "--",
                currentA,
                voltageV,
                plugged
        );
    }
}