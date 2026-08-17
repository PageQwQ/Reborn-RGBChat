package pageqwq.rgbchat;

import java.util.Arrays;

/**
 * Modern UI 兼容层。
 *
 * <p>Modern UI 用自己的文本引擎（{@code TextLayoutEngine} / {@code TextLayoutProcessor} /
 * {@code ModernStringSplitter}）接管 vanilla 的字体渲染，其换行与光标数学只在原始字符串与布局
 * 之间补偿 {@code §}<i>code</i> 字符对。因此本模组不能在 {@code StringDecomposer} 层改写文本：
 * 任何长度变化都会破坏基于原始字符串的下标映射，导致 AIOOBE 崩溃或光标/换行错位。
 *
 * <p>兼容策略（仅当 Modern UI 加载时启用）：
 * <ul>
 *   <li>聊天 HUD：{@code ChatComponentMixin} 在消息进入行拆分前，把 {@code #RRGGBB} 标签改写为
 *       逐字符 {@code TextColor} 样式组件（Modern UI 布局引擎原生渲染逐字形 RGB）；</li>
 *   <li>输入框：{@code EditBoxMixin} 把标签规范化为旧式 {@code §x§R§R§G§G§B§B} 色码——vanilla
 *       分解器会完整吞掉这 14 个字符（7 个 {@code §} 对），Modern UI 的光标/选区数学也对
 *       {@code §} 对做精确补偿，因此标签被隐藏且光标精确；{@code RgbInputFormatter} 负责把
 *       可见字符按色码着色；发送前 {@code ChatScreenMixin} 用 {@link #decodeInputHex(String)}
 *       转回 {@code #RRGGBB}（1.19+ 服务端会剥离玩家消息中的 {@code §}）；</li>
 *   <li>告示牌 / 物品名：在渲染入口把组件改写为逐字符样式组件（见 {@code SignTextMixin} /
 *       {@code ItemStackMixin}）；</li>
 *   <li>其余上下文：{@code StringDecomposerMixin} 直接放行，标签按字面渲染。</li>
 * </ul>
 */
public final class RgbCompat {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static volatile boolean modernUiLoaded;

    private RgbCompat() {
    }

    public static boolean isModernUiLoaded() {
        return modernUiLoaded;
    }

    public static void setModernUiLoaded(boolean loaded) {
        modernUiLoaded = loaded;
    }

    /**
     * 输入框规范化：把 {@code #RRGGBB} 标签编码为旧式 {@code §x§R§R§G§G§B§B} 色码（14 字符，
     * 全部由 {@code §} 对组成，vanilla 分解器完整吞掉、Modern UI 光标数学精确补偿），渐变标签
     * 展开为逐字符色码。已存在的完整色码原样保留；残缺色码被丢弃；{@code ##} 转义折叠为单个
     * {@code #}；其余字符（含经典 {@code §} 码）原样保留。
     *
     * @param s 输入字符串
     * @return 规范化后的字符串；无标签且无残缺色码时返回原实例
     */
    public static String encodeInputTags(String s) {
        return encodeInputTags(s, null);
    }

    /**
     * 同 {@link #encodeInputTags(String)}，并写入位置映射表。
     *
     * @param s   输入字符串
     * @param map 长度为 {@code s.length() + 1} 的映射表（初始内容被覆盖）：{@code map[i]} 为原始
     *            第 {@code i} 个字符边界在规范化后字符串中的位置；标签内部边界折叠到标签两端
     * @return 规范化后的字符串
     */
    public static String encodeInputTags(String s, int[] map) {
        int n = s.length();
        if (map != null) {
            Arrays.fill(map, -1);
        }
        if (n == 0) {
            if (map != null) {
                map[0] = 0;
            }
            return s;
        }
        StringBuilder out = new StringBuilder(n + 8);
        boolean changed = false;
        int i = 0;
        while (i < n) {
            mapBoundary(map, i, out.length());
            char c = s.charAt(i);

            if (c == '\u00a7') {
                if (i + 1 < n && s.charAt(i + 1) == 'x') {
                    if (isLegacyHexCode(s, i)) {
                        int[] stops = parseStopStops(s, i + 14);
                        if (stops == null) {
                            // 完整旧式色码原样保留
                            out.append(s, i, i + 14);
                            i += 14;
                        } else {
                            // 色码后紧跟 -RRGGBB 停靠点 → 渐变标签
                            int[] tag = new int[stops.length + 1];
                            tag[0] = parseLegacyHex(s, i);
                            System.arraycopy(stops, 0, tag, 1, stops.length);
                            changed = true;
                            SpanResult span = collectSpan(s, i + 14 + stops.length * 7, n, map);
                            emitGradient(out, tag, span, map);
                            i = span.end;
                        }
                    } else {
                        int pairs = sectionHexPairs(s, i + 2);
                        if (pairs > 0) {
                            // 残缺旧式色码（部分 §+hex 对）整体丢弃
                            changed = true;
                            i += 2 + pairs * 2;
                        } else {
                            int k = hexRun(s, i + 2);
                            if (k == 6) {
                                // 现代式 §xRRGGBB → 转成旧式
                                changed = true;
                                appendHexCode(out, parseHex(s, i + 2));
                                i += 8;
                            } else {
                                changed = true;
                                i += 2 + k;
                            }
                        }
                    }
                } else if (i + 1 < n) {
                    out.append(s, i, i + 2);
                    i += 2;
                } else {
                    changed = true;
                    i++;
                }
                continue;
            }

            if (c == '#') {
                if (i + 1 < n && s.charAt(i + 1) == '#') {
                    out.append('#');
                    changed = true;
                    i += 2;
                    continue;
                }
                int[] tag = RgbParser.tryParseTag(s, i);
                if (tag != null) {
                    changed = true;
                    if (tag.length == 1) {
                        appendHexCode(out, tag[0]);
                        i += 7;
                        continue;
                    }
                    SpanResult span = collectSpan(s, i + RgbParser.tagLength(tag), n, map);
                    emitGradient(out, tag, span, map);
                    i = span.end;
                    continue;
                }
                out.append('#');
                i++;
                continue;
            }

            if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(s.charAt(i + 1))) {
                mapBoundary(map, i + 1, -2);
                out.append(c).append(s.charAt(i + 1));
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        if (map != null) {
            map[n] = out.length();
            for (int j = n - 1; j >= 0; j--) {
                if (map[j] == -2) {
                    map[j] = j > 0 && map[j - 1] >= 0 ? map[j - 1] : 0;
                } else if (map[j] == -1) {
                    map[j] = map[j + 1];
                }
            }
        }
        return changed ? out.toString() : s;
    }

    /**
     * 发送前解码：把旧式 {@code §x§R§R§G§G§B§B} 色码转回 {@code #RRGGBB} 标签（逐字符色码
     * 展开为逐字符标签），现代式 {@code §xRRGGBB} 同样解码。残缺 {@code §x} 序列被丢弃。
     *
     * @param s 规范化后的输入字符串
     * @return 解码后的字符串；无 {@code §} 且无需转义时返回原实例
     */
    public static String decodeInputHex(String s) {
        if (s.indexOf('\u00a7') < 0 && s.indexOf('#') < 0) {
            return s;
        }
        int n = s.length();
        StringBuilder out = new StringBuilder(n);
        boolean changed = false;
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\u00a7') {
                if (i + 1 < n && s.charAt(i + 1) == 'x') {
                    if (isLegacyHexCode(s, i)) {
                        out.append('#');
                        for (int j = 0; j < 6; j++) {
                            out.append(s.charAt(i + 3 + j * 2));
                        }
                        changed = true;
                        i += 14;
                        // 连续色码 → 渐变标签补充停靠点
                        while (isLegacyHexCode(s, i)) {
                            out.append('-');
                            for (int j = 0; j < 6; j++) {
                                out.append(s.charAt(i + 3 + j * 2));
                            }
                            i += 14;
                        }
                    } else {
                        int pairs = sectionHexPairs(s, i + 2);
                        if (pairs > 0) {
                            changed = true;
                            i += 2 + pairs * 2;
                        } else {
                            int k = hexRun(s, i + 2);
                            if (k == 6) {
                                out.append('#');
                                for (int j = i + 2; j < i + 8; j++) {
                                    out.append(s.charAt(j));
                                }
                                changed = true;
                                i += 8;
                            } else {
                                changed = true;
                                i += 2 + k;
                            }
                        }
                    }
                } else if (i + 1 < n) {
                    out.append(s, i, i + 2);
                    i += 2;
                } else {
                    changed = true;
                    i++;
                }
                continue;
            }
            if (c == '#') {
                if (i + 1 < n && s.charAt(i + 1) == '#') {
                    out.append('#');
                    changed = true;
                    i += 2;
                    continue;
                }
                out.append('#');
                i++;
                continue;
            }
            if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(s.charAt(i + 1))) {
                out.append(c).append(s.charAt(i + 1));
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return changed ? out.toString() : s;
    }

    /** True when {@code s[i..i+14)} is a complete legacy {@code §x§R§R§G§G§B§B} code. */
    public static boolean isLegacyHexCode(String s, int i) {
        if (i + 14 > s.length()) {
            return false;
        }
        for (int k = 0; k < 6; k++) {
            int p = i + 2 + k * 2;
            if (s.charAt(p) != '\u00a7' || Character.digit(s.charAt(p + 1), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Parses a legacy {@code §x§R§R§G§G§B§B} code at {@code i}, or returns -1. */
    public static int parseLegacyHex(String s, int i) {
        if (!isLegacyHexCode(s, i)) {
            return -1;
        }
        int value = 0;
        for (int k = 0; k < 6; k++) {
            value = value << 4 | Character.digit(s.charAt(i + 3 + k * 2), 16);
        }
        return value;
    }

    /** Counts consecutive {@code §}+hex-digit pairs at {@code i}, at most 6. */
    private static int sectionHexPairs(String s, int i) {
        int pairs = 0;
        while (pairs < 6 && i + 1 < s.length()
                && s.charAt(i) == '\u00a7' && Character.digit(s.charAt(i + 1), 16) >= 0) {
            pairs++;
            i += 2;
        }
        return pairs;
    }

    /** Records a boundary unless the position is a low surrogate (marker -2 is ignored). */
    private static void mapBoundary(int[] map, int pos, int value) {
        if (map != null && pos < map.length) {
            map[pos] = value;
        }
    }

    /** Appends a legacy {@code §x§R§R§G§G§B§B} code for the given RGB value. */
    private static void appendHexCode(StringBuilder out, int rgb) {
        out.append('\u00a7').append('x');
        out.append('\u00a7').append(HEX[rgb >> 20 & 0xF]);
        out.append('\u00a7').append(HEX[rgb >> 16 & 0xF]);
        out.append('\u00a7').append(HEX[rgb >> 12 & 0xF]);
        out.append('\u00a7').append(HEX[rgb >> 8 & 0xF]);
        out.append('\u00a7').append(HEX[rgb >> 4 & 0xF]);
        out.append('\u00a7').append(HEX[rgb & 0xF]);
    }

    /** Counts consecutive hex digits at {@code i}, at most 6. */
    public static int hexRun(String s, int i) {
        int k = 0;
        while (k < 6 && i + k < s.length() && Character.digit(s.charAt(i + k), 16) >= 0) {
            k++;
        }
        return k;
    }

    /**
     * 去除 {@code #RRGGBB} 标签后的可见文本（告示牌宽度上限检查用）。
     * 无标签时返回原实例。
     */
    public static String stripTags(String s) {
        if (!RgbParser.containsTag(s)) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (StyledChar sc : RgbParser.parse(s, true)) {
            sb.appendCodePoint(sc.codePoint);
        }
        return sb.toString();
    }

    /**
     * 返回首个（多重）渐变标签的停靠点颜色；无渐变标签时返回 {@code null}。
     * 用于 Modern UI 提示框边框取色。
     */
    public static int[] extractGradientStops(String s) {
        if (!RgbParser.containsTag(s)) {
            return null;
        }
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (s.charAt(i) != '#') {
                continue;
            }
            if (i > 0 && s.charAt(i - 1) == '#') {
                continue;
            }
            int[] tag = RgbParser.tryParseTag(s, i);
            if (tag != null && tag.length > 1) {
                return tag;
            }
            if (tag != null) {
                i += RgbParser.tagLength(tag) - 1;
            }
        }
        return null;
    }

    /**
     * 把滚动位置对齐到色码边界：若 {@code pos} 落在某个旧式色码内部，返回该色码的起始位置，
     * 避免滚动时把色码截断成乱码字符。
     */
    public static int alignScrollPosition(String s, int pos) {
        int n = s.length();
        int i = 0;
        while (i < pos) {
            if (s.charAt(i) == '\u00a7' && i + 1 < n && s.charAt(i + 1) == 'x'
                    && isLegacyHexCode(s, i)) {
                if (pos < i + 14) {
                    return i;
                }
                i += 14;
                continue;
            }
            i++;
        }
        return pos;
    }

    /** 渐变标签的可见字符段（到 {@code §} 或下一个有效标签为止），{@code ##} 折叠为 {@code #}。 */
    private static SpanResult collectSpan(String s, int start, int n, int[] map) {
        StringBuilder span = new StringBuilder();
        int[] rawPos = new int[n - start];
        int m = 0;
        int p = start;
        while (p < n) {
            char d = s.charAt(p);
            if (d == '\u00a7') {
                break;
            }
            if (d == '#') {
                if (p + 1 < n && s.charAt(p + 1) == '#') {
                    rawPos[m++] = p;
                    span.append('#');
                    mapBoundary(map, p + 1, -2);
                    p += 2;
                    continue;
                }
                if (RgbParser.tryParseTag(s, p) != null) {
                    break;
                }
            }
            rawPos[m++] = p;
            if (Character.isHighSurrogate(d) && p + 1 < n
                    && Character.isLowSurrogate(s.charAt(p + 1))) {
                span.append(d).append(s.charAt(p + 1));
                mapBoundary(map, p + 1, -2);
                p += 2;
            } else {
                span.append(d);
                p++;
            }
        }
        int[] pos = new int[m];
        System.arraycopy(rawPos, 0, pos, 0, m);
        return new SpanResult(p, pos, span.toString());
    }

    /**
     * 输出渐变：有可见字符时逐字符插值（每字符前一个色码）；无可见字符时输出连续色码链
     * （隐藏且保留全部停靠点，供后续输入继续展开）。
     */
    private static void emitGradient(StringBuilder out, int[] tag, SpanResult span, int[] map) {
        int m = span.text.length();
        if (m == 0) {
            for (int stop : tag) {
                appendHexCode(out, stop);
            }
            return;
        }
        int spanOut = out.length();
        for (int k = 0; k < m; k++) {
            mapBoundary(map, span.rawPos[k], spanOut + k * 15);
            double t = m <= 1 ? 0.0 : (double) k / (m - 1);
            appendHexCode(out, Gradient.sample(tag, t));
            out.append(span.text.charAt(k));
        }
        mapBoundary(map, span.end, out.length());
    }

    /** 解析 {@code -RRGGBB} 形式的补充停靠点（从 {@code i} 开始），无则返回 {@code null}。 */
    private static int[] parseStopStops(String s, int i) {
        int n = s.length();
        int count = 0;
        int p = i;
        int[] stops = new int[8];
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
        if (count == 0) {
            return null;
        }
        int[] result = new int[count];
        System.arraycopy(stops, 0, result, 0, count);
        return result;
    }

    /** 渐变标签可见字符段。 */
    private static final class SpanResult {
        final int end;
        final int[] rawPos;
        final String text;

        SpanResult(int end, int[] rawPos, String text) {
            this.end = end;
            this.rawPos = rawPos;
            this.text = text;
        }
    }

    /** Parses exactly 6 hex digits starting at {@code i}, or returns -1. */
    public static int parseHex(String s, int i) {
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
