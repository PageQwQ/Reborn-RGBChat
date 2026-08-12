package pageqwq.rgbchat;

/** Multi-stop RGB gradient interpolation. */
public final class Gradient {
    private Gradient() {
    }

    /**
     * Samples a multi-stop gradient.
     *
     * @param stops RGB colors, at least one entry
     * @param t     position in [0, 1]
     */
    public static int sample(int[] stops, double t) {
        if (stops.length == 1 || t <= 0.0) {
            return stops[0];
        }
        if (t >= 1.0) {
            return stops[stops.length - 1];
        }
        double pos = t * (stops.length - 1);
        int i = (int) Math.floor(pos);
        return lerp(stops[i], stops[i + 1], pos - i);
    }

    /** Linear interpolation between two RGB colors, per channel. */
    public static int lerp(int a, int b, double f) {
        int r = lerpChannel(a >> 16 & 0xFF, b >> 16 & 0xFF, f);
        int g = lerpChannel(a >> 8 & 0xFF, b >> 8 & 0xFF, f);
        int bl = lerpChannel(a & 0xFF, b & 0xFF, f);
        return r << 16 | g << 8 | bl;
    }

    private static int lerpChannel(int a, int b, double f) {
        int v = (int) Math.round(a + (b - a) * f);
        return Math.max(0, Math.min(255, v));
    }
}
