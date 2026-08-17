package pageqwq.rgbchat;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * 把 {@code #RRGGBB} 标签改写为逐字符 {@code TextColor} 样式组件（Modern UI 兼容）。
 *
 * <p>Modern UI 的布局引擎按组件样式逐字形渲染 RGB（{@code TextLayout.drawText} 读取每个
 * glyph 的颜色位），因此逐字符样式是与其布局缓存、行拆分与光标数学兼容的全彩方案。
 *
 * <p>转换保留组件树结构与全部样式（含悬停/点击事件），仅重写各文本片段中的标签；
 * 无任何有效标签时返回原实例（引用相等），调用方据此避免无限递归。
 */
public final class RgbComponents {
    private RgbComponents() {
    }

    /** 无任何有效标签时返回原实例（引用相等）。 */
    public static Component convert(Component message) {
        if (!RgbParser.containsTag(message.getString())) {
            return message;
        }
        boolean[] changed = {false};
        MutableComponent root = Component.empty();
        message.visit((style, content) -> {
            if (RgbParser.containsTag(content)) {
                List<StyledChar> chars = RgbParser.parse(content, RgbConfig.get().gradient);
                if (pieceChanged(content, chars)) {
                    changed[0] = true;
                    appendStyled(root, style, chars);
                    return Optional.empty();
                }
            }
            root.append(Component.literal(content).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        if (!changed[0]) {
            return message;
        }
        root.setStyle(message.getStyle());
        return root.getString().isEmpty() ? message : root;
    }

    /** True when parsing changed the piece's text or added any color. */
    private static boolean pieceChanged(String content, List<StyledChar> chars) {
        StringBuilder sb = new StringBuilder(chars.size());
        boolean anyColor = false;
        for (StyledChar sc : chars) {
            sb.appendCodePoint(sc.codePoint);
            if (sc.format.color != null) {
                anyColor = true;
            }
        }
        return anyColor || !sb.toString().equals(content);
    }

    private static void appendStyled(MutableComponent root, Style base, List<StyledChar> chars) {
        StringBuilder buf = new StringBuilder();
        Style lastStyle = null;
        for (StyledChar sc : chars) {
            Style s = RgbStyles.apply(base, sc.format);
            if (lastStyle != null && !s.equals(lastStyle)) {
                root.append(Component.literal(buf.toString()).withStyle(lastStyle));
                buf.setLength(0);
            }
            buf.appendCodePoint(sc.codePoint);
            lastStyle = s;
        }
        if (buf.length() > 0) {
            root.append(Component.literal(buf.toString()).withStyle(lastStyle));
        }
    }
}
