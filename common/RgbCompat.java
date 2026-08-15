package pageqwq.rgbchat;

/**
 * Compatibility switch detected by the loader layer.
 *
 * When another text engine owns the render pipeline (currently Modern UI), this
 * mod must not rewrite the code point stream consumed by {@code StringDecomposer}:
 * that engine caches per-string glyph layouts keyed on the raw characters, and any
 * sequence we synthesise (e.g. consuming the {@code #RRGGBB} tag characters) would
 * desync its advance arrays and crash with an {@link ArrayIndexOutOfBoundsException}.
 * In that case RGB chat is simply disabled and vanilla formatting is left untouched.
 */
public final class RgbCompat {

    private static volatile boolean modernUiLoaded;

    private RgbCompat() {
    }

    public static boolean isModernUiLoaded() {
        return modernUiLoaded;
    }

    public static void setModernUiLoaded(boolean loaded) {
        modernUiLoaded = loaded;
    }
}