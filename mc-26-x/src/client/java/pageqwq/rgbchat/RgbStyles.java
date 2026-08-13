package pageqwq.rgbchat;

import net.minecraft.network.chat.Style;

/** Maps the common layer's absolute {@link RgbFormat} onto a Minecraft {@link Style}. */
public final class RgbStyles {
    private RgbStyles() {
    }

    public static Style apply(Style base, RgbFormat format) {
        Style style = base;
        if (format.color != null) {
            style = style.withColor(format.color);
        }
        if (format.bold) {
            style = style.withBold(true);
        }
        if (format.italic) {
            style = style.withItalic(true);
        }
        if (format.underlined) {
            style = style.withUnderlined(true);
        }
        if (format.strikethrough) {
            style = style.withStrikethrough(true);
        }
        if (format.obfuscated) {
            style = style.withObfuscated(true);
        }
        return style;
    }
}
