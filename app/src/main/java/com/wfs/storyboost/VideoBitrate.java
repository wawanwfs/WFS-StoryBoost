package com.wfs.storyboost;

/** Pure-Java AVC bitrate planner with conservative bounds for device encoders. */
public final class VideoBitrate {
    private VideoBitrate() {}

    public static int estimate(int width, int height, boolean fullHd) {
        return estimate(width, height, fullHd, 30f);
    }

    /**
     * Frame rate can be absent or inaccurate in source metadata: use 30 fps if unknown.
     * 60-fps footage receives a modest bitrate increase, not an unbounded doubling.
     */
    public static int estimate(int width, int height, boolean fullHd, float frameRate) {
        if (width < 2 || height < 2 || width > 8192 || height > 8192) {
            throw new IllegalArgumentException("Invalid video dimensions");
        }
        double fps = Float.isFinite(frameRate) && frameRate >= 1 && frameRate <= 240
                ? frameRate : 30d;
        double frameFactor = Math.max(0.9d, Math.min(1.4d, Math.sqrt(fps / 30d)));
        double pixels = (double) width * height;
        double bitsPerPixel = fullHd ? 4.1d : 4.0d;
        return (int) Math.round(Math.max(1_600_000d,
                Math.min(15_000_000d, pixels * bitsPerPixel * frameFactor)));
    }
}
