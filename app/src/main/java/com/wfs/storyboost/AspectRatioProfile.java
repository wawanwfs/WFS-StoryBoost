package com.wfs.storyboost;

/** Pure-Java export geometry shared by photo and video processing. */
public final class AspectRatioProfile {
    private AspectRatioProfile() {}

    public static final int PORTRAIT_9_16 = 0;
    public static final int LANDSCAPE_16_9 = 1;
    public static final int SQUARE_1_1 = 2;
    public static final int PORTRAIT_4_5 = 3;
    public static final int ORIGINAL = 4;

    public static final int FIT = 0;
    public static final int CROP = 1;
    public static final int BLUR = 2;

    public static final class Size {
        public final int width;
        public final int height;
        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
        @Override public String toString() { return width + " × " + height; }
    }

    public static Size resolve(int ratio, boolean fullHd, int sourceWidth, int sourceHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("Dimensi media tidak valid.");
        }
        int shortSide = fullHd ? 1080 : 720;
        switch (ratio) {
            case PORTRAIT_9_16: return new Size(shortSide, fullHd ? 1920 : 1280);
            case LANDSCAPE_16_9: return new Size(fullHd ? 1920 : 1280, shortSide);
            case SQUARE_1_1: return new Size(shortSide, shortSide);
            case PORTRAIT_4_5: return new Size(shortSide, fullHd ? 1350 : 900);
            case ORIGINAL:
                // No upscaling when maintaining the input's original aspect ratio.
                double scale = Math.min(1.0, (fullHd ? 1920d : 1280d)
                        / Math.max(sourceWidth, sourceHeight));
                return new Size(even(Math.max(2, (int) Math.round(sourceWidth * scale))),
                        even(Math.max(2, (int) Math.round(sourceHeight * scale))));
            default: throw new IllegalArgumentException("Rasio tidak dikenal: " + ratio);
        }
    }

    private static int even(int x) { return Math.max(2, x - x % 2); }

    public static String label(int ratio) {
        switch (ratio) {
            case PORTRAIT_9_16: return "9:16";
            case LANDSCAPE_16_9: return "16:9";
            case SQUARE_1_1: return "1:1";
            case PORTRAIT_4_5: return "4:5";
            default: return "Original";
        }
    }
}
