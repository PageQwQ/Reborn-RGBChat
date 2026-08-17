package pageqwq.rgbchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class RgbCompatTest {

    private static final String FF0000 = "\u00a7x\u00a7F\u00a7F\u00a70\u00a70\u00a70\u00a70";
    private static final String OOFF00 = "\u00a7x\u00a70\u00a70\u00a7F\u00a7F\u00a70\u00a70";

    @Test
    void encodeSolidTag() {
        assertEquals(FF0000 + "hi", RgbCompat.encodeInputTags("#FF0000hi"));
    }

    @Test
    void encodeLowercaseTagUppercased() {
        assertEquals("\u00a7x\u00a70\u00a70\u00a7F\u00a7F\u00a70\u00a70hi",
                RgbCompat.encodeInputTags("#00ff00hi"));
    }

    @Test
    void encodeGradientExpandsPerChar() {
        assertEquals(FF0000 + "h" + OOFF00 + "i",
                RgbCompat.encodeInputTags("#FF0000-00FF00hi"));
    }

    @Test
    void encodeKeepsLegacyHexCodes() {
        String s = FF0000 + "hi";
        assertSame(s, RgbCompat.encodeInputTags(s));
    }

    @Test
    void encodeConvertsModernHexCode() {
        assertEquals(FF0000 + "hi", RgbCompat.encodeInputTags("\u00a7xFF0000hi"));
    }

    @Test
    void encodeDropsBrokenHexCode() {
        assertEquals("hi", RgbCompat.encodeInputTags("\u00a7x\u00a7F\u00a7F\u00a70\u00a70\u00a70hi"));
    }

    @Test
    void encodeCollapsesEscape() {
        assertEquals("#FF0000hi", RgbCompat.encodeInputTags("##FF0000hi"));
    }

    @Test
    void encodeKeepsVanillaCodes() {
        String s = "\u00a7cRED";
        assertSame(s, RgbCompat.encodeInputTags(s));
    }

    @Test
    void encodeDropsLoneTrailingSection() {
        assertEquals("hi", RgbCompat.encodeInputTags("hi\u00a7"));
    }

    @Test
    void encodeWithoutChangesReturnsSameInstance() {
        String s = "plain text";
        assertSame(s, RgbCompat.encodeInputTags(s));
    }

    @Test
    void encodeMappingSolidTag() {
        String s = "#FF0000hi";
        int[] map = new int[s.length() + 1];
        RgbCompat.encodeInputTags(s, map);
        assertEquals(16, map[s.length()]);
        assertEquals(0, map[0]);
        assertEquals(14, map[7]);
        assertEquals(15, map[8]);
        assertEquals(16, map[9]);
    }

    @Test
    void encodeMappingGradient() {
        String s = "#FF0000-00FF00hi";
        int[] map = new int[s.length() + 1];
        RgbCompat.encodeInputTags(s, map);
        assertEquals(30, map[s.length()]);
        assertEquals(0, map[14]);
        assertEquals(15, map[15]);
        assertEquals(30, map[16]);
        // 标签内部边界折叠到标签起始
        assertEquals(0, map[7]);
        assertEquals(0, map[13]);
    }

    @Test
    void encodeMappingEscape() {
        String s = "a##b";
        int[] map = new int[s.length() + 1];
        RgbCompat.encodeInputTags(s, map);
        assertEquals("a#b", RgbCompat.encodeInputTags(s));
        assertEquals(3, map[s.length()]);
        assertEquals(2, map[2]);
        assertEquals(2, map[3]);
    }

    @Test
    void decodeLegacyCode() {
        assertEquals("#FF0000hi", RgbCompat.decodeInputHex(FF0000 + "hi"));
    }

    @Test
    void decodeModernCode() {
        assertEquals("#FF0000hi", RgbCompat.decodeInputHex("\u00a7xFF0000hi"));
    }

    @Test
    void decodeGradientCodes() {
        assertEquals("#FF0000h#00FF00i", RgbCompat.decodeInputHex(FF0000 + "h" + OOFF00 + "i"));
    }

    @Test
    void decodeDropsBrokenCode() {
        assertEquals("hi", RgbCompat.decodeInputHex("\u00a7x\u00a7F\u00a7Fhi"));
    }

    @Test
    void decodeKeepsVanillaCodes() {
        String s = "\u00a7chi";
        assertSame(s, RgbCompat.decodeInputHex(s));
    }

    @Test
    void decodeCollapsesEscape() {
        assertEquals("#FF0000hi", RgbCompat.decodeInputHex("##FF0000hi"));
    }

    @Test
    void decodeIsIdempotent() {
        String decoded = RgbCompat.decodeInputHex(FF0000 + "hi");
        assertSame(decoded, RgbCompat.decodeInputHex(decoded));
    }

    @Test
    void encodeDecodeRoundTripSolid() {
        String s = "#FF0000hi";
        assertEquals(s, RgbCompat.decodeInputHex(RgbCompat.encodeInputTags(s)));
    }

    @Test
    void encodeDecodeRoundTripGradient() {
        String s = "#FF0000-00FF00hi";
        assertEquals("#FF0000h#00FF00i", RgbCompat.decodeInputHex(RgbCompat.encodeInputTags(s)));
    }

    @Test
    void encodeIdempotent() {
        String once = RgbCompat.encodeInputTags("#FF0000hi");
        assertSame(once, RgbCompat.encodeInputTags(once));
        String gradientOnce = RgbCompat.encodeInputTags("#FF0000-00FF00hi");
        assertSame(gradientOnce, RgbCompat.encodeInputTags(gradientOnce));
    }

    @Test
    void legacyCodeParsing() {
        assertEquals(0xFF0000, RgbCompat.parseLegacyHex(FF0000 + "hi", 0));
        assertEquals(-1, RgbCompat.parseLegacyHex("\u00a7xFF0000hi", 0));
        assertEquals(-1, RgbCompat.parseLegacyHex(FF0000, 1));
    }

    @Test
    void stripTags() {
        assertEquals("hi", RgbCompat.stripTags("#FF0000hi"));
        assertEquals("hi", RgbCompat.stripTags("#FF0000-00FF00hi"));
        assertEquals("#hi", RgbCompat.stripTags("##hi"));
        String plain = "plain";
        assertSame(plain, RgbCompat.stripTags(plain));
        assertEquals("hello", RgbCompat.stripTags("he#123456llo"));
    }

    @Test
    void encodeGradientWithoutSpanBecomesChain() {
        assertEquals(FF0000 + OOFF00, RgbCompat.encodeInputTags("#FF0000-00FF00"));
    }

    @Test
    void encodeMultiStopGradientWithoutSpanBecomesChain() {
        String blue = "\u00a7x\u00a70\u00a70\u00a70\u00a70\u00a7F\u00a7F";
        assertEquals(FF0000 + OOFF00 + blue,
                RgbCompat.encodeInputTags("#FF0000-00FF00-0000FF"));
    }

    @Test
    void encodeCodeWithStopsBecomesChain() {
        // 已规范化的色码后继续输入 -RRGGBB 停靠点 → 渐变链
        assertEquals(FF0000 + OOFF00, RgbCompat.encodeInputTags(FF0000 + "-00FF00"));
    }

    @Test
    void encodeCodeWithStopsAndSpanExpandsPerChar() {
        assertEquals(FF0000 + "h" + OOFF00 + "i",
                RgbCompat.encodeInputTags(FF0000 + "-00FF00hi"));
    }

    @Test
    void encodeChainIsIdempotent() {
        String chain = RgbCompat.encodeInputTags("#FF0000-00FF00");
        assertSame(chain, RgbCompat.encodeInputTags(chain));
    }

    @Test
    void decodeChainRestoresGradient() {
        assertEquals("#FF0000-00FF00", RgbCompat.decodeInputHex(FF0000 + OOFF00));
    }

    @Test
    void decodeChainWithSpanRestoresGradient() {
        assertEquals("#FF0000-00FF00hi", RgbCompat.decodeInputHex(FF0000 + OOFF00 + "hi"));
    }

    @Test
    void encodeDecodeRoundTripGradientWithoutSpan() {
        String s = "#FF0000-00FF00";
        assertEquals(s, RgbCompat.decodeInputHex(RgbCompat.encodeInputTags(s)));
    }

    @Test
    void encodeDecodeRoundTripMultiStopGradient() {
        // 有可见字符时展开为逐字符色码，解码回逐字符标签（每字符颜色精确保持）
        String s = "#FF0000-00FF00-0000FFhi";
        assertEquals("#FF0000h#0000FFi", RgbCompat.decodeInputHex(RgbCompat.encodeInputTags(s)));
    }

    @Test
    void alignScrollPosition() {
        String s = "ab" + FF0000 + "cd";
        assertEquals(0, RgbCompat.alignScrollPosition(s, 0));
        assertEquals(2, RgbCompat.alignScrollPosition(s, 2));
        assertEquals(2, RgbCompat.alignScrollPosition(s, 5));
        assertEquals(2, RgbCompat.alignScrollPosition(s, 15));
        assertEquals(16, RgbCompat.alignScrollPosition(s, 16));
        assertEquals(17, RgbCompat.alignScrollPosition(s, 17));
        assertEquals(s.length(), RgbCompat.alignScrollPosition(s, s.length()));
    }

    @Test
    void extractGradientStops() {
        assertEquals(0xFF0000, RgbCompat.extractGradientStops("#FF0000-00FF00hi")[0]);
        assertEquals(0x0000FF, RgbCompat.extractGradientStops("#FF0000-00FF00-0000FFhi")[2]);
        assertEquals(2, RgbCompat.extractGradientStops("x#FF0000-00FF00y").length);
        assertNull(RgbCompat.extractGradientStops("#FF0000hi"));
        assertNull(RgbCompat.extractGradientStops("##FF0000-00FF00"));
        assertNull(RgbCompat.extractGradientStops("plain"));
    }
}
