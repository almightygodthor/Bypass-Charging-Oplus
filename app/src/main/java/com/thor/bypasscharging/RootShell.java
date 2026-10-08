package com.thor.bypasscharging;

import java.io.*;
public final class RootShell {
    private RootShell() {}
    public static String run(String command) {
        try {
            Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = p.getInputStream()) { byte[] b=new byte[4096]; int n; while((n=in.read(b))!=-1) out.write(b,0,n); }
            p.waitFor(); return out.toString().trim();
        } catch (Exception e) { return ""; }
    }
    public static boolean setBypass(boolean enabled) {
        String value = enabled ? "0" : "1";
        String out = run("echo " + value + " > /sys/devices/virtual/oplus_chg/battery/mmi_charging_enable");
        String now = run("cat /sys/devices/virtual/oplus_chg/battery/mmi_charging_enable");
        return (enabled && "0".equals(now)) || (!enabled && "1".equals(now));
    }
    public static boolean isRootAvailable() { return !run("id").isEmpty(); }
    public static boolean isBypassEnabled() { return "0".equals(run("cat /sys/devices/virtual/oplus_chg/battery/mmi_charging_enable")); }
}
