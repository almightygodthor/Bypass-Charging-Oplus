package com.thor.bypasscharging;

import java.io.BufferedReader;
import java.io.FileReader;

public final class PowerReader {
    private PowerReader() {}

    public static String read(String path) {
        String value = RootShell.read(path);
        if (!value.isEmpty()) return value;

        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String s = r.readLine();
            return s == null ? "" : s.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static long number(String path) {
        try {
            return Long.parseLong(read(path).trim());
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    public static String battery() {
        String s = read("/sys/class/power_supply/battery/capacity");
        return s.isEmpty() ? "--" : s + "%";
    }

    public static double currentA() {
        long ua = number("/sys/class/power_supply/battery/current_now");
        if (ua == Long.MIN_VALUE) return 0;
        return Math.abs(ua) / 1_000_000.0;
    }

    public static double voltageV() {
        long uv = number("/sys/class/power_supply/battery/voltage_now");
        if (uv == Long.MIN_VALUE) return 0;
        return uv / 1_000_000.0;
    }

    public static double powerW() {
        long ua = number("/sys/class/power_supply/battery/current_now");
        long uv = number("/sys/class/power_supply/battery/voltage_now");
        if (ua == Long.MIN_VALUE || uv == Long.MIN_VALUE) return 0;
        return Math.abs((double) ua * uv) / 1_000_000_000_000.0;
    }

    public static boolean plugged() {
        String s = read("/sys/class/power_supply/usb/online");
        if ("1".equals(s)) return true;
        s = read("/sys/class/power_supply/ac/online");
        if ("1".equals(s)) return true;

        String status = read("/sys/class/power_supply/battery/status");
        return "Charging".equalsIgnoreCase(status);
    }
}