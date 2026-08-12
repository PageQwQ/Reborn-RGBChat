package pageqwq.rgbchat;

/**
 * Vanilla legacy formatting codes (the {@code §} table).
 * Colors carry their classic vanilla RGB values so the common layer
 * stays free of Minecraft dependencies.
 */
public final class LegacyFormats {
    private LegacyFormats() {
    }

    /** RGB values for codes 0-9a-f, indexed by nibble value. */
    private static final int[] COLOR_RGB = {
            0x000000, // 0 black
            0x0000AA, // 1 dark_blue
            0x00AA00, // 2 dark_green
            0x00AAAA, // 3 dark_aqua
            0xAA0000, // 4 dark_red
            0xAA00AA, // 5 dark_purple
            0xFFAA00, // 6 gold
            0xAAAAAA, // 7 gray
            0x555555, // 8 dark_gray
            0x5555FF, // 9 blue
            0x55FF55, // a green
            0x55FFFF, // b aqua
            0xFF5555, // c red
            0xFF55FF, // d light_purple
            0xFFFF55, // e yellow
            0xFFFFFF, // f white
    };

    /**
     * Returns the RGB value for a color code (0-9, a-f, case-insensitive),
     * or {@code null} if the code is not a color code.
     */
    public static Integer colorFor(char code) {
        char c = Character.toLowerCase(code);
        if (c >= '0' && c <= '9') {
            return COLOR_RGB[c - '0'];
        }
        if (c >= 'a' && c <= 'f') {
            return COLOR_RGB[c - 'a' + 10];
        }
        return null;
    }

    /** Returns true for formatting codes l, o, n, m, k (case-insensitive). */
    public static boolean isFormatCode(char code) {
        char c = Character.toLowerCase(code);
        return c == 'l' || c == 'o' || c == 'n' || c == 'm' || c == 'k';
    }

    /** Returns true for the reset code r (case-insensitive). */
    public static boolean isResetCode(char code) {
        return Character.toLowerCase(code) == 'r';
    }
}
