package pageqwq.rgbchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RgbParserTest {

    private static String chars(List<StyledChar> parsed) {
        StringBuilder sb = new StringBuilder();
        for (StyledChar sc : parsed) {
            sb.append(sc.character);
        }
        return sb.toString();
    }

    @Test
    void containsTagFastCheck() {
        assertFalse(RgbParser.containsTag("hello world"));
        assertFalse(RgbParser.containsTag(""));
        assertTrue(RgbParser.containsTag("#FF0000hi"));
    }

    @Test
    void plainTextPassesThrough() {
        List<StyledChar> parsed = RgbParser.parse("hello");
        assertEquals("hello", chars(parsed));
        for (int i = 0; i < 5; i++) {
            assertEquals(i, parsed.get(i).index);
            assertNull(parsed.get(i).format.color);
        }
    }

    @Test
    void solidColorTag() {
        List<StyledChar> parsed = RgbParser.parse("#B0E0E6ABC");
        assertEquals("ABC", chars(parsed));
        for (StyledChar sc : parsed) {
            assertEquals(0xB0E0E6, sc.format.color);
        }
        // tag characters are not emitted, indices stay source-relative
        assertEquals(7, parsed.get(0).index);
        assertEquals(9, parsed.get(2).index);
    }

    @Test
    void solidColorAppliesUntilNextTag() {
        List<StyledChar> parsed = RgbParser.parse("#FF0000AB#00FF00CD");
        assertEquals("ABCD", chars(parsed));
        assertEquals(0xFF0000, parsed.get(0).format.color);
        assertEquals(0xFF0000, parsed.get(1).format.color);
        assertEquals(0x00FF00, parsed.get(2).format.color);
        assertEquals(0x00FF00, parsed.get(3).format.color);
    }

    @Test
    void lowercaseHexAccepted() {
        List<StyledChar> parsed = RgbParser.parse("#ff69b4X");
        assertEquals("X", chars(parsed));
        assertEquals(0xFF69B4, parsed.get(0).format.color);
    }

    @Test
    void invalidTagsAreLiteral() {
        // not enough hex digits
        assertEquals("#12345", chars(RgbParser.parse("#12345")));
        // non-hex characters
        assertEquals("#GGGGGGx", chars(RgbParser.parse("#GGGGGGx")));
        // lone '#' at end
        assertEquals("a#", chars(RgbParser.parse("a#")));
    }

    @Test
    void escapedHash() {
        List<StyledChar> parsed = RgbParser.parse("##FF0000hi");
        assertEquals("#FF0000hi", chars(parsed));
        assertNull(parsed.get(0).format.color);
    }

    @Test
    void gradientTwoStops() {
        List<StyledChar> parsed = RgbParser.parse("#000000-FFFFFFABC");
        assertEquals("ABC", chars(parsed));
        assertEquals(0x000000, parsed.get(0).format.color);
        assertEquals(0x808080, parsed.get(1).format.color);
        assertEquals(0xFFFFFF, parsed.get(2).format.color);
    }

    @Test
    void gradientMultiStop() {
        List<StyledChar> parsed = RgbParser.parse("#FF0000-00FF00-0000FFABCDE");
        assertEquals("ABCDE", chars(parsed));
        assertEquals(0xFF0000, parsed.get(0).format.color);
        // t=0.25 -> halfway between red and green
        assertEquals(0x808000, parsed.get(1).format.color);
        assertEquals(0x00FF00, parsed.get(2).format.color);
        // t=0.75 -> halfway between green and blue
        assertEquals(0x008080, parsed.get(3).format.color);
        assertEquals(0x0000FF, parsed.get(4).format.color);
    }

    @Test
    void gradientSingleCharUsesFirstStop() {
        List<StyledChar> parsed = RgbParser.parse("#FF0000-0000FFX");
        assertEquals("X", chars(parsed));
        assertEquals(0xFF0000, parsed.get(0).format.color);
    }

    @Test
    void gradientEndsAtSectionColor() {
        List<StyledChar> parsed = RgbParser.parse("#000000-FFFFFFAB§cCD");
        assertEquals("ABCD", chars(parsed));
        assertEquals(0x000000, parsed.get(0).format.color);
        assertEquals(0xFFFFFF, parsed.get(1).format.color);
        assertEquals(0xFF5555, parsed.get(2).format.color);
        assertEquals(0xFF5555, parsed.get(3).format.color);
    }

    @Test
    void gradientEndsAtNextHashTag() {
        List<StyledChar> parsed = RgbParser.parse("#000000-FFFFFFAB#123456C");
        assertEquals("ABC", chars(parsed));
        assertEquals(0x000000, parsed.get(0).format.color);
        assertEquals(0xFFFFFF, parsed.get(1).format.color);
        assertEquals(0x123456, parsed.get(2).format.color);
    }

    @Test
    void gradientDisabledDegradesToFirstStop() {
        List<StyledChar> parsed = RgbParser.parse("#FF0000-0000FFABC", false);
        assertEquals("ABC", chars(parsed));
        for (StyledChar sc : parsed) {
            assertEquals(0xFF0000, sc.format.color);
        }
    }

    @Test
    void sectionColorClearsFormatting() {
        List<StyledChar> parsed = RgbParser.parse("§l§cX");
        assertEquals("X", chars(parsed));
        assertEquals(0xFF5555, parsed.get(0).format.color);
        assertFalse(parsed.get(0).format.bold);
    }

    @Test
    void sectionFormattingPreservedInsideGradient() {
        List<StyledChar> parsed = RgbParser.parse("#000000-FFFFFFA§lBC");
        assertEquals("ABC", chars(parsed));
        assertFalse(parsed.get(0).format.bold);
        assertTrue(parsed.get(1).format.bold);
        assertTrue(parsed.get(2).format.bold);
        // bold char still takes its gradient slot
        assertEquals(0x808080, parsed.get(1).format.color);
        assertEquals(0xFFFFFF, parsed.get(2).format.color);
    }

    @Test
    void sectionResetClearsEverything() {
        List<StyledChar> parsed = RgbParser.parse("#FF0000§lA§rB");
        assertEquals("AB", chars(parsed));
        assertEquals(0xFF0000, parsed.get(0).format.color);
        assertTrue(parsed.get(0).format.bold);
        assertNull(parsed.get(1).format.color);
        assertFalse(parsed.get(1).format.bold);
    }

    @Test
    void invalidSectionCodeSkipsNextChar() {
        // vanilla consumes the character after '§' even if it is not a valid code
        assertEquals("ABC", chars(RgbParser.parse("A§zBC")));
        // trailing '§' is dropped
        assertEquals("AB", chars(RgbParser.parse("AB§")));
    }

    @Test
    void gradientWithEscapedHashInside() {
        List<StyledChar> parsed = RgbParser.parse("#000000-FFFFFFA##B");
        assertEquals("A#B", chars(parsed));
        assertEquals(0x000000, parsed.get(0).format.color);
        assertEquals(0x808080, parsed.get(1).format.color);
        assertEquals(0xFFFFFF, parsed.get(2).format.color);
    }

    @Test
    void hashTagKeepsExistingFormatting() {
        List<StyledChar> parsed = RgbParser.parse("§l#FF0000X");
        assertEquals("X", chars(parsed));
        assertEquals(0xFF0000, parsed.get(0).format.color);
        assertTrue(parsed.get(0).format.bold);
    }

    @Test
    void emptyGradientSegmentEmitsNothing() {
        List<StyledChar> parsed = RgbParser.parse("#000000-FFFFFF#FF0000X");
        assertEquals("X", chars(parsed));
        assertEquals(0xFF0000, parsed.get(0).format.color);
    }
}
