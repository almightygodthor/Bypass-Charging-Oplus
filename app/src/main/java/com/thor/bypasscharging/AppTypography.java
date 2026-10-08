package com.thor.bypasscharging;

import android.graphics.Typeface;

/**
 * Centralized typography for the dashboard. Uses Android's polished system
 * sans family so the UI stays crisp, formal and consistent without an
 * external font dependency.
 */
public final class AppTypography {
    private AppTypography() {}

    public static Typeface body() {
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    public static Typeface display() {
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    public static Typeface displayMedium() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static Typeface labelMedium() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static Typeface value() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static Typeface mono() {
        return Typeface.create("monospace", Typeface.NORMAL);
    }
}
