package com.thor.bypasscharging;

import java.io.*;
public final class PowerReader {
    public static String read(String path) {
        try { BufferedReader r=new BufferedReader(new FileReader(path)); String s=r.readLine(); r.close(); return s==null?"":s.trim(); } catch(Exception e){ return ""; }
    }
    public static String battery() { String s=read("/sys/class/power_supply/battery/capacity"); return s.isEmpty()?"--":s+"%"; }
    public static double currentA() { try { return Math.abs(Long.parseLong(read("/sys/class/power_supply/battery/current_now")))/1_000_000.0; } catch(Exception e){ return 0; } }
    public static double voltageV() { try { return Long.parseLong(read("/sys/class/power_supply/battery/voltage_now"))/1_000_000.0; } catch(Exception e){ return 0; } }
    public static double powerW() { try { long ua=Long.parseLong(read("/sys/class/power_supply/battery/current_now")); long uv=Long.parseLong(read("/sys/class/power_supply/battery/voltage_now")); return Math.abs(ua*uv)/1_000_000_000_000.0; } catch(Exception e){ return 0; } }
    public static boolean plugged() { String s=read("/sys/class/power_supply/usb/online"); if("1".equals(s)) return true; s=read("/sys/class/power_supply/ac/online"); return "1".equals(s); }
}
