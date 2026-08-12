package pageqwq.rgbchat;

import java.util.Objects;

/**
 * Absolute formatting state for a single emitted character.
 * {@code color == null} means "inherit the base style's color".
 */
public final class RgbFormat {
    public Integer color;
    public boolean bold;
    public boolean italic;
    public boolean underlined;
    public boolean strikethrough;
    public boolean obfuscated;

    public RgbFormat copy() {
        RgbFormat f = new RgbFormat();
        f.color = this.color;
        f.bold = this.bold;
        f.italic = this.italic;
        f.underlined = this.underlined;
        f.strikethrough = this.strikethrough;
        f.obfuscated = this.obfuscated;
        return f;
    }

    /** Clears all formatting flags (vanilla: a color code also clears formatting). */
    public void clearFormatting() {
        this.bold = false;
        this.italic = false;
        this.underlined = false;
        this.strikethrough = false;
        this.obfuscated = false;
    }

    /** Resets everything back to the base style (vanilla {@code §r}). */
    public void reset() {
        this.color = null;
        clearFormatting();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RgbFormat other)) {
            return false;
        }
        return Objects.equals(color, other.color)
                && bold == other.bold
                && italic == other.italic
                && underlined == other.underlined
                && strikethrough == other.strikethrough
                && obfuscated == other.obfuscated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    @Override
    public String toString() {
        return "RgbFormat{color=" + (color == null ? "inherit" : String.format("#%06X", color))
                + (bold ? " bold" : "") + (italic ? " italic" : "")
                + (underlined ? " underlined" : "") + (strikethrough ? " strikethrough" : "")
                + (obfuscated ? " obfuscated" : "") + '}';
    }
}
