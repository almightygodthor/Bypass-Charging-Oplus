package com.thor.bypasscharging;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class RootShell {
    private RootShell() {}

    public static String run(String command) {
        try {
            Process p = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = p.getInputStream()) {
                byte[] b = new byte[4096];
                int n;
                while ((n = in.read(b)) != -1) out.write(b, 0, n);
            }
            p.waitFor();
            return out.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static String read(String path) {
        if (path == null || path.isEmpty()) return "";
        return run("cat '" + path.replace("'", "'\\''") + "' 2>/dev/null");
    }

    public static boolean exists(String path) {
        if (path == null || path.isEmpty()) return false;
        return "1".equals(run("test -f '" + path.replace("'", "'\\''") + "' && echo 1 || echo 0"));
    }

    public static boolean setBypass(boolean enabled) {
        String path = BypassNodeDetector.find();
        if (path.isEmpty()) return false;

        String value = enabled ? "0" : "1";
        String safe = path.replace("'", "'\\''");
        run("printf '%s' '" + value + "' > '" + safe + "' 2>/dev/null");
        String now = read(path);
        return (enabled && "0".equals(now)) || (!enabled && "1".equals(now));
    }

    public static boolean isRootAvailable() {
        return "0".equals(run("id -u"));
    }

    public static boolean isBypassEnabled() {
        String path = BypassNodeDetector.find();
        return !path.isEmpty() && "0".equals(read(path));
    }
}