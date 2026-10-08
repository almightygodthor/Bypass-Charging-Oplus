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
    private static final String BATTERY = "/sys/class/power_supply/battery/";
    private static final String MASTER_CHARGER = "/sys/class/power_supply/mtk-master-charger/";
    private static final String CHARGER_IC = "/sys/class/power_supply/11280000.i2c:mt6375@34:chg/";
    private static final String AC = "/sys/class/power_supply/ac/";

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
            "current_now",
            "current_avg",
            "BatteryAverageCurrent",
            "batt_current_now",
            "batt_current",
            "charger_current",
            "charge_rate",
            "input_current_now"
    };

    private static final String[] VOLTAGE_NAMES = {
            "voltage_now",
            "BatterySenseVoltage",
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
        public final double rawVoltageV;
        public final int cellCount;
        public final double powerW;
        public final double temperatureC;
        public final boolean plugged;
        public final String charger;
        public final double healthPercent;
        public final int designCapacityMah;
        public final GtNeo3Variant.Type variant;

        Snapshot(String battery, double currentA, double rawVoltageV, double temperatureC,
                 boolean plugged, String charger, double healthPercent,
                 int designCapacityMah, GtNeo3Variant.Type variant) {
            this.battery = battery;
            this.currentA = currentA;
            this.rawVoltageV = rawVoltageV;
            this.cellCount = variant == GtNeo3Variant.Type.UNKNOWN ? 1 : 2;
            this.voltageV = rawVoltageV * this.cellCount;
            this.powerW = currentA * this.voltageV;
            this.temperatureC = temperatureC;
            this.plugged = plugged;
            this.charger = charger;
            this.healthPercent = healthPercent;
            this.designCapacityMah = designCapacityMah;
            this.variant = variant;
        }
    }

    private PowerReader() {}

    public static String read(String path) {
        if (path == null || path.isEmpty()) return "";
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String s = r.readLine();
            if (s != null && !s.trim().isEmpty()) return s.trim();
        } catch (Exception ignored) {
        }
        return RootShell.read(path);
    }

    private static long number(String path) {
        if (path == null || path.isEmpty()) return Long.MIN_VALUE;
        try {
            String value = read(path);
            if (value.isEmpty()) return Long.MIN_VALUE;
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    private static boolean isOnline(String path) {
        long value = number(path);
        return value > 0;
    }

    private static String firstReadable(String... paths) {
        for (String path : paths) {
            if (number(path) != Long.MIN_VALUE) return path;
        }
        return "";
    }

    private static String resolveNonZero(String... names) {
        String[] roots = {
                BATTERY,
                "/sys/class/power_supply/Battery/",
                "/sys/class/power_supply/bms/",
                "/sys/class/power_supply/main/",
                MASTER_CHARGER,
                CHARGER_IC,
                "/sys/class/power_supply/usb/",
                AC,
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
        String name = new File(path == null ? "" : path).getName();
        double magnitude = Math.abs((double) raw);

        if ("BatteryAverageCurrent".equals(name) ||
                "batt_current".equals(name) ||
                "batt_current_now".equals(name)) {
            return raw / 1000.0;
        }

        if ("current_now".equals(name) || "current_avg".equals(name) ||
                "input_current_now".equals(name)) {
            return raw / 1_000_000.0;
        }

        if (magnitude >= 100_000) return raw / 1_000_000.0;
        if (magnitude >= 100) return raw / 1_000.0;
        return raw;
    }

    private static double voltageFromRaw(long raw, String path) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        double v = Math.abs((double) raw);
        String name = new File(path == null ? "" : path).getName();

        if ("voltage_now".equals(name) || "voltage_mv".equals(name) ||
                "BatterySenseVoltage".equals(name) || "battery_voltage".equals(name)) {
            if (v >= 100_000) return v / 1_000_000.0;
            if (v >= 1_000) return v / 1_000.0;
            return v;
        }

        if (v > 100_000) return v / 1_000_000.0;
        if (v > 1_000) return v / 1_000.0;
        return v;
    }

    private static double temperatureFromRaw(long raw) {
        if (raw == Long.MIN_VALUE || raw == 0) return 0;
        double t = Math.abs((double) raw);
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
        File base = new File("/sys/class/power_supply/battery/uevent");
        if (!base.isFile()) return Long.MIN_VALUE;
        return parseUevent(base, key);
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

    private static double readBatteryCurrent() {
        // This device exposes the real battery current here. It is signed uA.
        // On the supplied dump it is negative while status=Charging, so normalize
        // the charging direction below rather than treating it as -0 mA.
        currentPath = firstReadable(
                BATTERY + "current_now",
                BATTERY + "uevent"
        );

        if (currentPath.equals(BATTERY + "uevent")) {
            long raw = readUeventValue("POWER_SUPPLY_CURRENT_NOW");
            // This MT6895/OPLUS battery driver exposes POWER_SUPPLY_CURRENT_NOW
            // in mA. The supplied dump contains values such as -476/-513.
            return raw == Long.MIN_VALUE ? 0 : raw / 1000.0;
        }

        if (!currentPath.isEmpty()) {
            long raw = number(currentPath);
            // The same vendor battery driver exposes battery/current_now in mA,
            // unlike the generic power_supply convention of microamps.
            if (currentPath.equals(BATTERY + "current_now")) {
                return raw == Long.MIN_VALUE ? 0 : raw / 1000.0;
            }
            return currentFromRaw(raw, currentPath);
        }

        currentPath = resolveNonZero(CURRENT_NAMES);
        return currentPath.isEmpty() ? 0 : currentFromRaw(number(currentPath), currentPath);
    }

    private static double readBatteryVoltage() {
        // Prefer the actual battery voltage node. The supplied dump shows
        // ~4059-4090 mV; Android framework voltage was the source of the
        // incorrect single-digit mV readings seen in the UI.
        voltagePath = firstReadable(
                BATTERY + "voltage_now",
                BATTERY + "uevent"
        );

        if (voltagePath.equals(BATTERY + "uevent")) {
            long raw = readUeventValue("POWER_SUPPLY_VOLTAGE_NOW");
            return raw == Long.MIN_VALUE ? 0 : voltageFromRaw(raw, "voltage_now");
        }

        if (!voltagePath.isEmpty()) {
            return voltageFromRaw(number(voltagePath), voltagePath);
        }

        voltagePath = resolveNonZero(VOLTAGE_NAMES);
        return voltagePath.isEmpty() ? 0 : voltageFromRaw(number(voltagePath), voltagePath);
    }

    private static double readTemperature(Intent batteryIntent) {
        int framework = batteryIntent == null
                ? 0 : batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

        if (framework > 0) return framework / 10.0;

        String path = firstReadable(BATTERY + "temp", BATTERY + "uevent");
        if (path.equals(BATTERY + "uevent")) {
            long raw = readUeventValue("POWER_SUPPLY_TEMP");
            return raw == Long.MIN_VALUE ? 0 : temperatureFromRaw(raw);
        }

        if (!path.isEmpty()) return temperatureFromRaw(number(path));

        path = resolveNonZero(TEMP_NAMES);
        return path.isEmpty() ? 0 : temperatureFromRaw(number(path));
    }

    private static boolean readChargerOnline() {
        return isOnline(MASTER_CHARGER + "online") ||
                isOnline(AC + "online") ||
                number(CHARGER_IC + "online") == 2;
    }

    private static String readChargerType() {
        String type = read(CHARGER_IC + "type");
        if ("USB_DCP".equalsIgnoreCase(type) || "DCP".equalsIgnoreCase(type)) return "AC";
        if ("USB_SDP".equalsIgnoreCase(type) || "SDP".equalsIgnoreCase(type)) return "USB";
        if (!type.isEmpty() && !"Unknown".equalsIgnoreCase(type)) return type;

        if (isOnline(AC + "online")) return "AC";
        if (readChargerOnline()) return "AC";
        return "Battery";
    }

    private static double readHealthPercent() {
        if (healthPath == null || healthPath.isEmpty()) {
            healthPath = firstReadable(BATTERY + "health");
        }

        // The device exposes charge_full == charge_full_design in the dump.
        // Prefer the capacity/design ratio over the textual "Good" health state.
        healthFullPath = firstReadable(BATTERY + "charge_full");
        healthDesignPath = firstReadable(BATTERY + "charge_full_design");

        long full = number(healthFullPath);
        long design = number(healthDesignPath);
        if (full > 0 && design > 0) {
            return Math.max(0, Math.min(100, full * 100.0 / design));
        }

        if (!healthPath.isEmpty()) {
            long raw = number(healthPath);
            if (raw > 0 && raw <= 100) return raw;
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

        boolean plugged = readChargerOnline() ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        double currentA = readBatteryCurrent();

        // This MT6895/MT6375 battery node reports negative current during
        // charging. Normalize only for a genuinely plugged + charging state.
        boolean charging = plugged &&
                (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                 "Charging".equalsIgnoreCase(read(BATTERY + "status")));
        if (charging && currentA < 0) currentA = -currentA;

        String source = currentPath;
        addCurrentSample(currentA, source, plugged);
        currentA = averagedCurrent(currentA);

        double voltageV = readBatteryVoltage();
        double temperatureC = readTemperature(batteryIntent);

        String charger = readChargerType();
        if (!plugged) charger = "Battery";

        String battery = level >= 0
                ? String.format(Locale.US, "%d%%", level)
                : "--";

        int designCapacityMah = GtNeo3Variant.readDesignCapacityMah();
        GtNeo3Variant.Type variant = GtNeo3Variant.detect();

        return new Snapshot(
                battery,
                currentA,
                voltageV,
                temperatureC,
                plugged,
                charger,
                readHealthPercent(),
                designCapacityMah,
                variant
        );
    }
}
