package com.thor.bypasscharging;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PowerReader {
    private static String currentPath;
    private static String voltagePath;
    private static String healthPath;
    private static String healthDesignPath;
    private static String healthFullPath;

    private static final double[] currentSamples = new double[50];
    private static int currentSampleCount;
    private static int currentSampleIndex;
    private static String currentSampleSource = "";
    private static Boolean lastPluggedState;
    private static double lastRawCurrent;

    private static final String[] CURRENT_NAMES = {
            "BatteryAverageCurrent",
            "batt_current_now",
            "batt_current",
            "current_now",
            "current_avg",
            "charger_current",
            "charge_rate",
            "input_current_now"
    };

    private static final String[] VOLTAGE_NAMES = {
            "BatterySenseVoltage",
            "voltage_now",
            "voltage_mv",
            "battery_voltage"
    };

    private static final String[] TEMP_NAMES = {
            "temp",
            "temperature",
            "battery_temp",
            "batt_temp",
            "temperature_now"
    };

    private static final String[] HEALTH_NAMES = {
            "battery_soh",
            "batt_soh",
            "soh",
            "health_percent",
            "health_index",
            "health_capacity_index"
    };

    public static final class Snapshot {
        public final String battery;
        public final double currentA;
        public final double voltageV;
        public final double powerW;
        public final double temperatureC;
        public final boolean plugged;
        public final String charger;
        public final double healthPercent;

        Snapshot(String battery, double currentA, double voltageV, double temperatureC,
                 boolean plugged, String charger, double healthPercent) {
            this.battery = battery;
            this.currentA = currentA;
            this.voltageV = voltageV;
            this.powerW = currentA * voltageV;
            this.temperatureC = temperatureC;
            this.plugged = plugged;
            this.charger = charger;
            this.healthPercent = healthPercent;
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
        if (path == null || path.isEmpty()) return Long.MIN_VALUE;
        try {
            File f = new File(path);
            if (!f.isFile()) return Long.MIN_VALUE;
            return Long.parseLong(read(path));
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    private static String resolveNonZero(String... names) {
        String[] roots = {
                "/sys/class/power_supply/battery/",
                "/sys/class/power_supply/Battery/",
                "/sys/class/power_supply/bms/",
                "/sys/class/power_supply/main/",
                "/sys/class/power_supply/usb/",
                "/sys/class/power_supply/ac/",
                "/sys/class/power_supply/charger/"
        };

        for (String root : roots) {
            for (String name : names) {
                String path = root + name;
                long value = number(path);
                if (value != Long.MIN_VALUE && value != 0) return path;
            }
        }

        File base = new File("/sys/class/power_supply");
        File[] supplies = base.listFiles();
        if (supplies != null) {
            for (String name : names) {
                for (File supply : supplies) {
                    if (!supply.isDirectory()) continue;
                    String path = new File(supply, name).getPath();
                    long value = number(path);
                    if (value != Long.MIN_VALUE && value != 0) return path;
                }
            }
        }
        return "";
    }

    private static double currentFromRaw(long raw, String path) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        String name = new File(path).getName();
        double magnitude = Math.abs((double) raw);

        // Legacy MediaTek battery-current interfaces report signed mA.
        if ("BatteryAverageCurrent".equals(name) ||
                "batt_current".equals(name) ||
                "batt_current_now".equals(name)) {
            return raw / 1000.0;
        }

        // Standard power_supply current interfaces report signed uA.
        if ("current_now".equals(name) || "current_avg".equals(name) ||
                "current_max".equals(name) || "input_current_now".equals(name)) {
            return raw / 1_000_000.0;
        }

        // Vendor nodes vary; preserve the sign while inferring a likely scale.
        if (magnitude >= 100_000) return raw / 1_000_000.0;
        if (magnitude >= 100) return raw / 1_000.0;
        return raw;
    }

    private static double voltageFromRaw(long raw, String path) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        double v = Math.abs((double) raw);
        String name = new File(path == null ? "" : path).getName();
        if ("BatterySenseVoltage".equals(name) || "voltage_mv".equals(name)) return v / 1000.0;
        if (v > 100_000) return v / 1_000_000.0;
        if (v > 1_000) return v / 1_000.0;
        return v;
    }

    private static double temperatureFromRaw(long raw) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        double t = Math.abs((double) raw);
        if (t >= 1000) return t / 10.0;
        if (t >= 100) return t / 10.0;
        return t;
    }

    private static void resetCurrentSamples() {
        currentSampleCount = 0;
        currentSampleIndex = 0;
        currentSampleSource = "";
        lastRawCurrent = 0;
    }

    private static void addCurrentSample(double current, String source, boolean plugged) {
        if (current == 0 || Double.isNaN(current) || Double.isInfinite(current)) return;

        if (lastPluggedState == null || lastPluggedState != plugged ||
                (lastRawCurrent != 0 && Math.signum(lastRawCurrent) != Math.signum(current))) {
            resetCurrentSamples();
            lastPluggedState = plugged;
        }

        if (source == null) source = "";
        if (!source.equals(currentSampleSource)) {
            currentSampleSource = source;
            currentSampleCount = 0;
            currentSampleIndex = 0;
        }

        currentSamples[currentSampleIndex] = current;
        currentSampleIndex = (currentSampleIndex + 1) % currentSamples.length;
        if (currentSampleCount < currentSamples.length) currentSampleCount++;
        lastRawCurrent = current;
    }

    private static double averagedCurrent(double fallback) {
        if (currentSampleCount < 5) return fallback;

        List<Double> values = new ArrayList<>(currentSampleCount);
        for (int i = 0; i < currentSampleCount; i++) values.add(currentSamples[i]);
        Collections.sort(values);

        int trim = currentSampleCount >= 20
                ? Math.min(10, currentSampleCount / 5)
                : 0;
        int from = trim;
        int to = values.size() - trim;
        if (from >= to) return fallback;

        double sum = 0;
        for (int i = from; i < to; i++) sum += values.get(i);
        return sum / (to - from);
    }

    private static double readBatteryManagerCurrent(BatteryManager bm) {
        if (bm == null) return 0;

        long raw = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        if (raw != Long.MIN_VALUE && raw != 0) return raw / 1_000_000.0;

        raw = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        if (raw != Long.MIN_VALUE && raw != 0) return raw / 1_000_000.0;

        return 0;
    }

    private static long readUeventValue(String key) {
        File base = new File("/sys/class/power_supply");
        File[] supplies = base.listFiles();
        if (supplies == null) return Long.MIN_VALUE;

        for (File supply : supplies) {
            String n = supply.getName().toLowerCase(Locale.US);
            if (supply.isDirectory() &&
                    (n.contains("battery") || n.equals("bms") || n.equals("main"))) {
                long value = parseUevent(new File(supply, "uevent"), key);
                if (value != Long.MIN_VALUE && value != 0) return value;
            }
        }
        return Long.MIN_VALUE;
    }

    private static long parseUevent(File file, String key) {
        if (!file.isFile()) return Long.MIN_VALUE;

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            String prefix = key + "=";
            while ((line = r.readLine()) != null) {
                if (line.startsWith(prefix)) {
                    return Long.parseLong(line.substring(prefix.length()).trim());
                }
            }
        } catch (Exception ignored) {
        }
        return Long.MIN_VALUE;
    }

    private static double readSysfsCurrent() {
        if (currentPath != null && !currentPath.isEmpty()) {
            long raw = number(currentPath);
            if (raw != Long.MIN_VALUE && raw != 0) {
                return currentFromRaw(raw, currentPath);
            }
        }

        // Prefer legacy interfaces first when BatteryManager reports zero.
        currentPath = resolveNonZero(CURRENT_NAMES);
        if (!currentPath.isEmpty()) {
            return currentFromRaw(number(currentPath), currentPath);
        }

        // Some kernels expose the same value only through power_supply/uevent.
        long raw = readUeventValue("POWER_SUPPLY_CURRENT_NOW");
        if (raw == Long.MIN_VALUE || raw == 0) {
            raw = readUeventValue("POWER_SUPPLY_CURRENT_AVG");
        }
        if (raw != Long.MIN_VALUE && raw != 0) {
            currentPath = "uevent:POWER_SUPPLY_CURRENT";
            return raw / 1_000_000.0;
        }

        return 0;
    }

    private static double readHealthPercent() {
        if (healthPath == null || healthPath.isEmpty()) {
            healthPath = resolveNonZero(HEALTH_NAMES);
        }

        if (!healthPath.isEmpty()) {
            long raw = number(healthPath);
            if (raw != Long.MIN_VALUE) {
                double value = Math.abs((double) raw);
                if (value > 0 && value <= 100) return value;
                if (value > 100 && value <= 10000) return value / 100.0;
            }
        }

        if (healthFullPath == null || healthFullPath.isEmpty()) {
            healthFullPath = resolveNonZero("charge_full");
        }
        if (healthDesignPath == null || healthDesignPath.isEmpty()) {
            healthDesignPath = resolveNonZero("charge_full_design");
        }

        long full = number(healthFullPath);
        long design = number(healthDesignPath);
        if (full > 0 && design > 0 && full <= design * 2L) {
            return Math.max(0, Math.min(100, full * 100.0 / design));
        }

        long ueventFull = readUeventValue("POWER_SUPPLY_CHARGE_FULL");
        long ueventDesign = readUeventValue("POWER_SUPPLY_CHARGE_FULL_DESIGN");
        if (ueventFull > 0 && ueventDesign > 0 && ueventFull <= ueventDesign * 2L) {
            return Math.max(0, Math.min(100, ueventFull * 100.0 / ueventDesign));
        }

        return 0;
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

        boolean plugged = pluggedType != 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        double currentA = readBatteryManagerCurrent(bm);
        boolean frameworkCurrent = currentA != 0;
        if (!frameworkCurrent) currentA = readSysfsCurrent();

        String source = frameworkCurrent ? "BatteryManager" : currentPath;
        addCurrentSample(currentA, source, plugged);
        currentA = averagedCurrent(currentA);

        double voltageV = voltageMv > 0 ? voltageMv / 1000.0 : 0;
        if (voltageV <= 0) {
            if (voltagePath == null || voltagePath.isEmpty()) {
                voltagePath = resolveNonZero(VOLTAGE_NAMES);
            }
            if (!voltagePath.isEmpty()) {
                voltageV = voltageFromRaw(number(voltagePath), voltagePath);
            }
        }

        double temperatureC = temperatureTenths > 0
                ? temperatureTenths / 10.0
                : 0;

        if (temperatureC <= 0) {
            String tempPath = resolveNonZero(TEMP_NAMES);
            if (!tempPath.isEmpty()) {
                temperatureC = temperatureFromRaw(number(tempPath));
            }
        }

        String charger;
        switch (pluggedType) {
            case BatteryManager.BATTERY_PLUGGED_AC: charger = "AC"; break;
            case BatteryManager.BATTERY_PLUGGED_USB: charger = "USB"; break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: charger = "Wireless"; break;
            default: charger = plugged ? "Connected" : "Battery";
        }

        return new Snapshot(
                level >= 0 ? String.format(Locale.US, "%d%%", level) : "--",
                currentA,
                voltageV,
                temperatureC,
                plugged,
                charger,
                readHealthPercent()
        );
    }
}
