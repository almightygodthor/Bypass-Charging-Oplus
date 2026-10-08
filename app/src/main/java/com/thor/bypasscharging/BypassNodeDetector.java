package com.thor.bypasscharging;

import java.util.LinkedHashSet;

public final class BypassNodeDetector {
    private static final String[] EXACT = {
        "/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable",
        "/sys/devices/virtual/oplus_chg/battery/charging_enable",
        "/sys/devices/virtual/oplus_chg/battery/charge_enable",
        "/sys/devices/virtual/oplus_chg/battery/charge_enabled",
        "/sys/devices/virtual/oplus_chg/battery/charging_enabled"
    };

    private static final String[] NAMES = {
        "mmi_charging_enable",
        "charging_enable",
        "charge_enable",
        "charging_enabled",
        "charge_enabled",
        "battery_charging_enabled",
        "charging_switch",
        "charger_enable"
    };

    private static volatile String cached;

    private BypassNodeDetector() {}

    public static String find() {
        String current = cached;
        if (current != null && RootShell.exists(current)) return current;

        for (String path : EXACT) {
            if (isBinaryNode(path)) {
                cached = path;
                return path;
            }
        }

        StringBuilder names = new StringBuilder();
        for (String name : NAMES) {
            if (names.length() > 0) names.append(" -o ");
            names.append("-name '").append(name).append("'");
        }

        String output = RootShell.run(
                "find /sys/devices/virtual/oplus_chg /sys/class/power_supply " +
                "/sys/devices/platform -type f \\(" + names +
                "\\) 2>/dev/null | head -100");

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (String line : output.split("\\n")) {
            String path = line.trim();
            if (!path.isEmpty()) candidates.add(path);
        }

        for (String path : candidates) {
            if (isBinaryNode(path)) {
                cached = path;
                return path;
            }
        }
        return "";
    }

    private static boolean isBinaryNode(String path) {
        String value = RootShell.read(path);
        return "0".equals(value) || "1".equals(value);
    }

    public static String displayName(String path) {
        if (path == null || path.isEmpty()) return "Not detected";
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}