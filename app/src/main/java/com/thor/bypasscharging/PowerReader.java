package com.thor.bypasscharging;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

public final class PowerReader {
    private static String currentPath;
    private static String voltagePath;

    public static final class Snapshot {
        public final String battery;
        public final double currentA;
        public final double voltageV;
        public final double powerW;
        public final double temperatureC;
        public final boolean plugged;
        public final String charger;

        Snapshot(String battery, double currentA, double voltageV, double temperatureC,
                 boolean plugged, String charger) {
            this.battery = battery;
            this.currentA = currentA;
            this.voltageV = voltageV;
            this.powerW = Math.abs(currentA * voltageV);
            this.temperatureC = temperatureC;
            this.plugged = plugged;
            this.charger = charger;
        }
    }

    private PowerReader() {}

    public static String read(String path) {
        if (path == null || path.isEmpty()) return "";
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String s = r.readLine();
            return s == null ? "" : s.trim();
        } catch (Exception ignored) {
            return RootShell.read(path);
        }
    }

    private static long number(String path) {
        try {
            return Long.parseLong(read(path));
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    private static String resolve(String... names) {
        String[] roots = {
                "/sys/class/power_supply/battery/",
                "/sys/class/power_supply/bms/",
                "/sys/class/power_supply/main/",
                "/sys/class/power_supply/usb/",
                "/sys/class/power_supply/charger/"
        };

        for (String root : roots) {
            for (String name : names) {
                String path = root + name;
                if (number(path) != Long.MIN_VALUE) return path;
            }
        }

        String regex = "(" + String.join("|", names) + ")";
        return RootShell.run(
                "find /sys/class/power_supply -maxdepth 3 -type f 2>/dev/null | " +
                "grep -E '/" + regex + "$' | head -1"
        ).trim();
    }

    private static double currentFromRaw(long raw) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        double a = Math.abs(raw);
        // Most Android BatteryManager/sysfs current values are µA.
        // Vendor charger nodes may expose mA or A; use magnitude as a safe fallback.
        if (a >= 100_000) return a / 1_000_000.0;
        if (a >= 100) return a / 1_000.0;
        return a;
    }

    private static double voltageFromRaw(long raw) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        double v = Math.abs(raw);
        if (v > 100_000) return v / 1_000_000.0; // µV
        if (v > 1_000) return v / 1_000.0;       // mV
        return v;                                // already V
    }

    private static double temperatureFromRaw(long raw) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        double t = Math.abs(raw);
        if (t > 1000) return t / 10.0;
        if (t > 100) return t / 10.0;
        return t;
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
        int temperatureTenths = batteryIntent != null
                ? batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) : 0;
        int pluggedType = batteryIntent != null
                ? batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) : 0;

        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        long rawCurrent = bm != null
                ? bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                : Long.MIN_VALUE;

        if (rawCurrent == Long.MIN_VALUE || rawCurrent == 0) {
            long average = bm != null
                    ? bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                    : Long.MIN_VALUE;
            if (average != Long.MIN_VALUE && average != 0) rawCurrent = average;
        }

        if (rawCurrent == Long.MIN_VALUE || rawCurrent == 0) {
            if (currentPath == null) {
                currentPath = resolve(
                        "input_current_now", "current_now", "current_avg",
                        "batt_current", "ibus", "ibus_now", "charger_current");
            }
            rawCurrent = number(currentPath);
        }

        double currentA = currentFromRaw(rawCurrent);

        int uvFromIntent = voltageMv > 0 ? voltageMv * 1000 : 0;
        double voltageV = voltageFromRaw(uvFromIntent);

        if (voltageV <= 0) {
            if (voltagePath == null) voltagePath = resolve(
                    "voltage_now", "voltage_mv", "battery_voltage");
            voltageV = voltageFromRaw(number(voltagePath));
        }

        double temperatureC = temperatureTenths > 0
                ? temperatureTenths / 10.0
                : 0;

        if (temperatureC <= 0) {
            String tempPath = resolve("temp", "temperature");
            temperatureC = temperatureFromRaw(number(tempPath));
        }

        boolean plugged = pluggedType != 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        String charger;
        switch (pluggedType) {
            case BatteryManager.BATTERY_PLUGGED_AC: charger = "AC"; break;
            case BatteryManager.BATTERY_PLUGGED_USB: charger = "USB"; break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: charger = "Wireless"; break;
            default: charger = plugged ? "Connected" : "Battery";
        }

        return new Snapshot(
                level >= 0 ? String.format(Locale.US, "%.0f%%", (float) level) : "--",
                currentA,
                voltageV,
                temperatureC,
                plugged,
                charger
        );
    }
}
