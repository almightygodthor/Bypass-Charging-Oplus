package com.thor.bypasscharging;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public final class DebugLog {
    private static final int MAX_LINES = 400;
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    private static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private DebugLog() {}

    public static synchronized void add(String message) {
        String line = FORMAT.format(new Date()) + "  " + message;
        lines.addLast(line);
        while (lines.size() > MAX_LINES) lines.removeFirst();
    }

    public static synchronized String dump() {
        StringBuilder out = new StringBuilder();
        for (String line : lines) out.append(line).append('\n');
        return out.toString();
    }

    public static synchronized void clear() {
        lines.clear();
        add("Log cleared");
    }
}
