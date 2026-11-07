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

    /**
     * MAX_DETAIL adapts canvas size to the native source. With CROP it also
     * reduces the requested canvas when covering it would enlarge source pixels.
     * Thus it avoids claiming that a 1080p landscape crop is a native 4K Story.
     */
    public static Size resolve(int ratio, int quality, int sourceW, int sourceH, int frameStyle) {
        OutputProfile.normalized(quality);
        if (sourceW <= 0 || sourceH <= 0) throw new IllegalArgumentException("Dimensi media tidak valid.");
        if (quality != OutputProfile.MAX_DETAIL) {
            return resolve(ratio, quality == OutputProfile.FULL_HD, sourceW, sourceH);
        }
        int longest = OutputProfile.targetLongEdge(quality, sourceW, sourceH);
        if (ratio == ORIGINAL) {
            double factor = Math.min(1d, 3840d / Math.max(sourceW, sourceH));
            return new Size(even(Math.max(2,(int)Math.round(sourceW*factor))),
                            even(Math.max(2,(int)Math.round(sourceH*factor))));
        }
        int width,height;
        switch (ratio) {
            case PORTRAIT_9_16: width = (int)Math.round(longest * 9d/16d); height=longest; break;
            case LANDSCAPE_16_9: width=longest; height=(int)Math.round(longest * 9d/16d); break;
            case SQUARE_1_1: width=longest; height=longest; break;
            case PORTRAIT_4_5: width=(int)Math.round(longest*4d/5d); height=longest; break;
            default: throw new IllegalArgumentException("Rasio tidak dikenal: "+ratio);
        }
        if (frameStyle == CROP) {
            double nativeLimit = Math.min(1d, Math.min((double)sourceW/width, (double)sourceH/height));
            width=Math.max(2,(int)Math.floor(width*nativeLimit));
            height=Math.max(2,(int)Math.floor(height*nativeLimit));
        }
        return new Size(even(width),even(height));
    }

    private static int even(int x) { return Math.max(2, x - x % 2); }

    /** Reverse portrait/landscape; retain other selected aspect ratios. */
    public static int reverse(int ratio) {
        if (ratio == PORTRAIT_9_16) return LANDSCAPE_16_9;
        if (ratio == LANDSCAPE_16_9) return PORTRAIT_9_16;
        return ratio;
    }

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
