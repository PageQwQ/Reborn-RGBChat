package pageqwq.rgbchat;

import java.util.Objects;

/**
 * One character to be rendered: its index in the source string,
 * the character itself, and its absolute formatting state.
 */
public final class StyledChar {
    public final int index;
    public final char character;
    public final RgbFormat format;

    public StyledChar(int index, char character, RgbFormat format) {
        this.index = index;
        this.character = character;
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
        return index == other.index && character == other.character && format.equals(other.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, character, format);
    }

    @Override
    public String toString() {
        return "StyledChar{" + index + ", '" + character + "', " + format + '}';
    }
}
