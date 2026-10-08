package com.thor.bypasscharging;

public final class GtNeo3Variant {
    private static final String DESIGN_CAPACITY =
            "/sys/class/oplus_chg/battery/design_capacity";

    public enum Type {
        GT_NEO_3_80W,
        GT_NEO_3_150W,
        UNKNOWN
    }

    private GtNeo3Variant() {}

    public static int readDesignCapacityMah() {
        String value = RootShell.read(DESIGN_CAPACITY);
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static Type detect() {
        switch (readDesignCapacityMah()) {
            case 5000:
                return Type.GT_NEO_3_80W;
            case 4500:
                return Type.GT_NEO_3_150W;
            default:
                return Type.UNKNOWN;
        }
    }

    public static String label(Type type) {
        switch (type) {
            case GT_NEO_3_80W:
                return "GT NEO 3 • 80W";
            case GT_NEO_3_150W:
                return "GT NEO 3 • 150W";
            default:
                return "GT NEO 3 • Variant unknown";
        }
    }
}
