package pageqwq.rgbchat;

import java.util.Objects;

/**
 * One character to be rendered: its index in the source string (UTF-16 code
 * units), the Unicode code point, and its absolute formatting state.
 * Non-BMP characters are emitted as a single entry with the index of their
 * high surrogate, matching vanilla {@code StringDecomposer} behavior.
 */
public final class StyledChar {
    public final int index;
    public final int codePoint;
    public final RgbFormat format;

    public StyledChar(int index, int codePoint, RgbFormat format) {
        this.index = index;
        this.codePoint = codePoint;
        this.format = format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StyledChar other)) {
            return false;
        }
        return index == other.index && codePoint == other.codePoint && format.equals(other.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, codePoint, format);
    }

    @Override
    public String toString() {
        return "StyledChar{" + index + ", '" + new String(Character.toChars(codePoint)) + "', " + format + '}';
    }
}
