package com.wfs.storyboost;

/**
 * Export profiles. MAX keeps native detail where possible; it is not AI upscaling.
 * MAX may select a non-standard size to avoid enlarging a cropped source.
 */
public final class OutputProfile {
    private OutputProfile() {}
    public static final int HD = 0;
    public static final int FULL_HD = 1;
    public static final int MAX_DETAIL = 2;

    public static int normalized(int quality) {
        if (quality < HD || quality > MAX_DETAIL) throw new IllegalArgumentException("Profil kualitas tidak dikenal");
        return quality;
    }

    public static int targetLongEdge(int quality, int sourceW, int sourceH) {
        normalized(quality);
        if (quality == HD) return 1280;
        if (quality == FULL_HD) return 1920;
        int longest = Math.max(sourceW, sourceH);
        return longest >= 3840 ? 3840 : longest >= 2560 ? 2560 : longest >= 1920 ? 1920 : longest;
    }
}
