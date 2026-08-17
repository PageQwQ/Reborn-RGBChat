package pageqwq.rgbchat;

import java.util.function.BiFunction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * 聊天输入框的 Modern UI 实时预览 formatter：隐藏颜色标签，可见字符按标签着色。
 *
 * <p>输入框字符串被 {@code EditBoxMixin} 规范化为旧式 {@code §x§R§R§G§G§B§B} 色码——vanilla
 * 分解器完整吞掉色码，Modern UI 的光标/选区数学对 {@code §} 对精确补偿，因此这里只输出可见
 * 字符即可：布局 advance 数量、宽度与光标数学完全一致。本 formatter 同时兼容未规范化的原始
 * {@code #RRGGBB} 标签（如输入中途的标签会按字面显示）与粘贴进来的经典 {@code §} 码。
 *
 * <p>未启用时返回 vanilla 默认序列（{@link FormattedCharSequence#forward}）。
 */
public final class RgbInputFormatter {

    private static final BiFunction<String, Integer, FormattedCharSequence> INSTANCE =
            RgbInputFormatter::format;

    private RgbInputFormatter() {
    }

    public static BiFunction<String, Integer, FormattedCharSequence> instance() {
        return INSTANCE;
    }

    public static FormattedCharSequence format(String text, int displayPos) {
        if (!RgbCompat.isModernUiLoaded()) {
            return FormattedCharSequence.forward(text, Style.EMPTY);
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.inputPreview || text.isEmpty()) {
            return FormattedCharSequence.forward(text, Style.EMPTY);
        }
        return buildSequence(text);
    }

    private static FormattedCharSequence buildSequence(String text) {
        int n = text.length();
        int[] codePoints = new int[n];
        Style[] styles = new Style[n];
        int count = 0;
        Integer color = null;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);

            if (c == '\u00a7') {
                if (i + 1 < n && text.charAt(i + 1) == 'x') {
                    int rgb = RgbCompat.parseLegacyHex(text, i);
                    int[] stops;
                    if (rgb >= 0) {
                        i += 14;
                        stops = new int[]{rgb};
                    } else {
                        int k = RgbCompat.hexRun(text, i + 2);
                        if (k == 6) {
                            i += 8;
                            stops = new int[]{RgbCompat.parseHex(text, i - 6)};
                        } else {
                            i += 2 + k;
                            continue;
                        }
                    }
                    // 连续色码或 -RRGGBB 停靠点 → 渐变
                    int stopsCount = 1;
                    while (stopsCount < 8) {
                        if (RgbCompat.isLegacyHexCode(text, i)) {
                            if (stopsCount == stops.length) {
                                int[] bigger = new int[stops.length * 2];
                                System.arraycopy(stops, 0, bigger, 0, stops.length);
                                stops = bigger;
                            }
                            stops[stopsCount++] = RgbCompat.parseLegacyHex(text, i);
                            i += 14;
                            continue;
                        }
                        if (i + 7 <= n && text.charAt(i) == '-') {
                            int extra = RgbCompat.parseHex(text, i + 1);
                            if (extra >= 0) {
                                if (stopsCount == stops.length) {
                                    int[] bigger = new int[stops.length * 2];
                                    System.arraycopy(stops, 0, bigger, 0, stops.length);
                                    stops = bigger;
                                }
                                stops[stopsCount++] = extra;
                                i += 7;
                                continue;
                            }
                        }
                        break;
                    }
                    if (stopsCount > 1) {
                        int[] gradientStops = java.util.Arrays.copyOf(stops, stopsCount);
                        int spanStart = count;
                        int p = i;
                        while (p < n) {
                            char d = text.charAt(p);
                            if (d == '\u00a7') {
                                break;
                            }
                            if (d == '#') {
                                if (p + 1 < n && text.charAt(p + 1) == '#') {
                                    codePoints[count++] = '#';
                                    p += 2;
                                    continue;
                                }
                                if (RgbParser.tryParseTag(text, p) != null) {
                                    break;
                                }
                            }
                            if (Character.isHighSurrogate(d) && p + 1 < n
                                    && Character.isLowSurrogate(text.charAt(p + 1))) {
                                codePoints[count++] = Character.toCodePoint(d, text.charAt(p + 1));
                                p += 2;
                            } else {
                                codePoints[count++] = d;
                                p++;
                            }
                        }
                        int m = count - spanStart;
                        for (int k = 0; k < m; k++) {
                            double t = m <= 1 ? 0.0 : (double) k / (m - 1);
                            styles[spanStart + k] = makeStyle(Gradient.sample(gradientStops, t),
                                    bold, italic, underlined, strikethrough, obfuscated);
                        }
                        i = p;
                        color = null;
                    } else {
                        color = stops[0];
                    }
                    continue;
                }
                if (i + 1 < n) {
                    char code = text.charAt(i + 1);
                    Integer legacy = LegacyFormats.colorFor(code);
                    if (legacy != null) {
                        color = legacy;
                        bold = italic = underlined = strikethrough = obfuscated = false;
                    } else if (LegacyFormats.isFormatCode(code)) {
                        switch (Character.toLowerCase(code)) {
                            case 'l' -> bold = true;
                            case 'o' -> italic = true;
                            case 'n' -> underlined = true;
                            case 'm' -> strikethrough = true;
                            case 'k' -> obfuscated = true;
                            default -> { }
                        }
                    } else if (LegacyFormats.isResetCode(code)) {
                        color = null;
                        bold = italic = underlined = strikethrough = obfuscated = false;
                    }
                    i += 2;
                } else {
                    i++;
                }
                continue;
            }

            if (c == '#') {
                if (i + 1 < n && text.charAt(i + 1) == '#') {
                    codePoints[count] = '#';
                    styles[count] = makeStyle(color, bold, italic, underlined, strikethrough, obfuscated);
                    count++;
                    i += 2;
                    continue;
                }
                int[] tag = RgbParser.tryParseTag(text, i);
                if (tag != null) {
                    if (tag.length == 1) {
                        color = tag[0];
                        i += 7;
                        continue;
                    }
                    i += RgbParser.tagLength(tag);
                    int spanStart = count;
                    int p = i;
                    while (p < n) {
                        char d = text.charAt(p);
                        if (d == '\u00a7') {
                            break;
                        }
                        if (d == '#') {
                            if (p + 1 < n && text.charAt(p + 1) == '#') {
                                codePoints[count++] = '#';
                                p += 2;
                                continue;
                            }
                            if (RgbParser.tryParseTag(text, p) != null) {
                                break;
                            }
                        }
                        if (Character.isHighSurrogate(d) && p + 1 < n
                                && Character.isLowSurrogate(text.charAt(p + 1))) {
                            codePoints[count++] = Character.toCodePoint(d, text.charAt(p + 1));
                            p += 2;
                        } else {
                            codePoints[count++] = d;
                            p++;
                        }
                    }
                    int m = count - spanStart;
                    for (int k = 0; k < m; k++) {
                        double t = m <= 1 ? 0.0 : (double) k / (m - 1);
                        styles[spanStart + k] = makeStyle(Gradient.sample(tag, t),
                                bold, italic, underlined, strikethrough, obfuscated);
                    }
                    i = p;
                    color = null;
                    continue;
                }
                codePoints[count] = '#';
                styles[count] = makeStyle(color, bold, italic, underlined, strikethrough, obfuscated);
                count++;
                i++;
                continue;
            }

            if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(text.charAt(i + 1))) {
                codePoints[count] = Character.toCodePoint(c, text.charAt(i + 1));
                styles[count] = makeStyle(color, bold, italic, underlined, strikethrough, obfuscated);
                count++;
                i += 2;
            } else {
                codePoints[count] = c;
                styles[count] = makeStyle(color, bold, italic, underlined, strikethrough, obfuscated);
                count++;
                i++;
            }
        }

        final int total = count;
        return sink -> {
            for (int k = 0; k < total; k++) {
                if (!sink.accept(k, styles[k], codePoints[k])) {
                    return false;
                }
            }
            return true;
        };
    }

    private static Style makeStyle(Integer color, boolean bold, boolean italic, boolean underlined,
                                   boolean strikethrough, boolean obfuscated) {
        Style style = color != null ? Style.EMPTY.withColor(color) : Style.EMPTY;
        if (bold) {
            style = style.withBold(true);
        }
        if (italic) {
            style = style.withItalic(true);
        }
        if (underlined) {
            style = style.withUnderlined(true);
        }
        if (strikethrough) {
            style = style.withStrikethrough(true);
        }
        if (obfuscated) {
            style = style.withObfuscated(true);
        }
        return style;
    }
}
