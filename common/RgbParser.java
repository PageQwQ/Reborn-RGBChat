package pageqwq.rgbchat;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses RGB Chat Reborn text tags into styled characters.
 *
 * <p>Supported syntax (compatible with the original 1.12.2 RGB Chat mod):
 * <ul>
 *   <li>{@code #RRGGBBtext} — solid RGB color</li>
 *   <li>{@code #RRGGBB-RRGGBB[-RRGGBB...]text} — gradient with 2+ stops, per-character interpolation</li>
 *   <li>{@code ##} — a literal {@code #}</li>
 *   <li>a {@code #} that does not start a valid tag is emitted literally</li>
 * </ul>
 *
 * <p>Vanilla {@code §} codes keep their vanilla semantics: color codes set the
 * color and clear formatting, formatting codes set a flag, {@code §r} resets
 * everything. Any {@code §} color code, {@code §r} or new {@code #} tag ends
 * an active gradient; formatting codes do not. A trailing {@code §} and the
 * character after an invalid {@code §} are dropped, matching vanilla.
 *
 * <p>Tag characters themselves are never emitted.
 */
public final class RgbParser {
    private static final char SECTION = '§';
    private static final int REPLACEMENT_CHAR = 0xFFFD;

    private RgbParser() {
    }

    /** Fast check used by the mixin: strings without {@code #} never need parsing. */
    public static boolean containsTag(String s) {
        return s.indexOf('#') >= 0;
    }

    /** Parses with gradients enabled. */
    public static List<StyledChar> parse(String s) {
        return parse(s, true);
    }

    /**
     * Parses the given string into styled characters.
     *
     * @param gradientEnabled when false, gradient tags degrade to their first stop (solid)
     */
    public static List<StyledChar> parse(String s, boolean gradientEnabled) {
        List<StyledChar> out = new ArrayList<>(s.length());
        RgbFormat fmt = new RgbFormat();
        int[] stops = null;              // active gradient stops
        List<StyledChar> buffer = null;  // visible chars of the active gradient segment

        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);

            if (c == '#') {
                if (i + 1 < n && s.charAt(i + 1) == '#') {
                    // escaped "##" -> literal '#'
                    emit(out, buffer, fmt, i, '#');
                    i += 2;
                    continue;
                }
                int[] tag = tryParseTag(s, i);
                if (tag != null) {
                    // end any active gradient
                    flushGradient(out, buffer, stops);
                    buffer = null;
                    stops = null;
                    if (tag.length == 1 || !gradientEnabled) {
                        fmt.color = tag[0];
                    } else {
                        stops = tag;
                        buffer = new ArrayList<>();
                    }
                    i += tagLength(tag);
                    continue;
                }
                // not a valid tag -> literal '#'
                emit(out, buffer, fmt, i, '#');
                i++;
                continue;
            }

            if (c == SECTION) {
                if (i + 1 >= n) {
                    // trailing '§' is dropped (vanilla behavior)
                    i++;
                    continue;
                }
                char code = s.charAt(i + 1);
                Integer color = LegacyFormats.colorFor(code);
                if (color != null) {
                    flushGradient(out, buffer, stops);
                    buffer = null;
                    stops = null;
                    fmt.color = color;
                    fmt.clearFormatting();
                    i += 2;
                    continue;
                }
                if (LegacyFormats.isFormatCode(code)) {
                    switch (Character.toLowerCase(code)) {
                        case 'l' -> fmt.bold = true;
                        case 'o' -> fmt.italic = true;
                        case 'n' -> fmt.underlined = true;
                        case 'm' -> fmt.strikethrough = true;
                        case 'k' -> fmt.obfuscated = true;
                        default -> throw new IllegalStateException("unreachable");
                    }
                    i += 2;
                    continue;
                }
                if (LegacyFormats.isResetCode(code)) {
                    flushGradient(out, buffer, stops);
                    buffer = null;
                    stops = null;
                    fmt.reset();
                    i += 2;
                    continue;
                }
                // invalid code: vanilla consumes the next character too
                i += 2;
                continue;
            }

            // regular character, with vanilla-style surrogate handling
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= n) {
                    // unpaired high surrogate at the end -> replacement char (vanilla)
                    emit(out, buffer, fmt, i, REPLACEMENT_CHAR);
                    i++;
                } else {
                    char low = s.charAt(i + 1);
                    if (Character.isLowSurrogate(low)) {
                        emit(out, buffer, fmt, i, Character.toCodePoint(c, low));
                        i += 2;
                    } else {
                        emit(out, buffer, fmt, i, REPLACEMENT_CHAR);
                        i++;
                    }
                }
                continue;
            }
            emit(out, buffer, fmt, i, c);
            i++;
        }

        flushGradient(out, buffer, stops);
        return out;
    }

    /** Emits one code point either into the gradient buffer or directly to the output. */
    private static void emit(List<StyledChar> out, List<StyledChar> buffer,
                             RgbFormat fmt, int index, int codePoint) {
        if (buffer != null) {
            buffer.add(new StyledChar(index, codePoint, fmt.copy()));
        } else {
            out.add(new StyledChar(index, codePoint, fmt.copy()));
        }
    }

    /** Applies gradient colors to buffered chars and appends them to the output. */
    private static void flushGradient(List<StyledChar> out, List<StyledChar> buffer, int[] stops) {
        if (buffer == null || stops == null) {
            return;
        }
        int m = buffer.size();
        for (int k = 0; k < m; k++) {
            double t = m <= 1 ? 0.0 : (double) k / (m - 1);
            buffer.get(k).format.color = Gradient.sample(stops, t);
        }
        out.addAll(buffer);
    }

    /**
     * Tries to parse a tag at {@code s.charAt(i) == '#'}:
     * {@code #RRGGBB} optionally followed by more {@code -RRGGBB} stops.
     *
     * <p>Package-private so {@link RgbComponents}/{@link RgbInputFormatter} can scan for tags.
     *
     * @return the stop colors, or {@code null} if there is no valid tag at {@code i}
     */
    static int[] tryParseTag(String s, int i) {
        int n = s.length();
        if (i + 7 > n) {
            return null;
        }
        int first = parseHex(s, i + 1);
        if (first < 0) {
            return null;
        }
        int[] stops = new int[8];
        stops[0] = first;
        int count = 1;
        int p = i + 7;
        while (p + 7 <= n && s.charAt(p) == '-') {
            int color = parseHex(s, p + 1);
            if (color < 0) {
                break;
            }
            if (count == stops.length) {
                int[] bigger = new int[stops.length * 2];
                System.arraycopy(stops, 0, bigger, 0, stops.length);
                stops = bigger;
            }
            stops[count++] = color;
            p += 7;
        }
        int[] result = new int[count];
        System.arraycopy(stops, 0, result, 0, count);
        return result;
    }

    /** Total length of a tag with the given number of stops: {@code #RRGGBB} plus {@code -RRGGBB} per extra stop. */
    static int tagLength(int[] stops) {
        return 1 + 6 + (stops.length - 1) * 7;
    }

    /** Parses exactly 6 hex digits starting at {@code i}, or returns -1. */
    private static int parseHex(String s, int i) {
        if (i + 6 > s.length()) {
            return -1;
        }
        int value = 0;
        for (int k = 0; k < 6; k++) {
            int digit = Character.digit(s.charAt(i + k), 16);
            if (digit < 0) {
                return -1;
            }
            value = value << 4 | digit;
        }
        return value;
    }
}
