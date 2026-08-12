package pageqwq.rgbchat;

/**
 * Runtime configuration model. Persisted as JSON by the loader layers
 * ({@code config/rgbchat.json}); the common layer only holds the data.
 */
public final class RgbConfig {
    /** Master switch: when false the mod never touches text rendering. */
    public boolean enabled = true;
    /** When false, gradient tags degrade to their first stop (solid color). */
    public boolean gradient = true;

    /** Per-context switches. */
    public boolean chat = true;
    public boolean signs = true;
    public boolean anvil = true;
    public boolean itemNames = true;
    public boolean inputPreview = true;

    private static volatile RgbConfig instance = new RgbConfig();

    private RgbConfig() {
    }

    public static RgbConfig get() {
        return instance;
    }

    /** Replaces the active config (called by the loader layer after (re)loading the file). */
    public static void set(RgbConfig config) {
        instance = config == null ? new RgbConfig() : config;
    }

    /** Whether parsing is allowed in the given render context. */
    public boolean allows(RenderContext context) {
        if (!enabled) {
            return false;
        }
        return switch (context) {
            case CHAT -> chat;
            case SIGN -> signs;
            case ANVIL -> anvil;
            case ITEM_NAME -> itemNames;
            case INPUT -> inputPreview;
            case OTHER -> true;
        };
    }
}
