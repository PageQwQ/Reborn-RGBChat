package pageqwq.rgbchat;

/**
 * Where a piece of text is being rendered. Loader-layer mixins mark the
 * current context around the relevant render calls so the core parser hook
 * can apply per-context config switches.
 */
public enum RenderContext {
    CHAT,
    SIGN,
    ANVIL,
    ITEM_NAME,
    INPUT,
    /** Anything not explicitly marked: always allowed when the mod is enabled. */
    OTHER;

    private static final ThreadLocal<RenderContext> CURRENT = ThreadLocal.withInitial(() -> OTHER);

    public static RenderContext current() {
        return CURRENT.get();
    }

    public static void set(RenderContext context) {
        CURRENT.set(context == null ? OTHER : context);
    }

    public static void clear() {
        CURRENT.set(OTHER);
    }
}
