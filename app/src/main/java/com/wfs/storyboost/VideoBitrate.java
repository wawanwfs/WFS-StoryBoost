package com.wfs.storyboost;

/** Conservative device-friendly AVC bitrate budgets based on requested pixel count. */
public final class VideoBitrate {
    private VideoBitrate() {}

    public static int estimate(int width, int height, boolean fullHd) {
        if (width < 2 || height < 2) throw new IllegalArgumentException("Invalid video dimensions");
        // Approximately 3.8 Mbps for 720p and 8.5 Mbps for 1080p, with a safe cap.
        double pixels = (double) width * height;
        double bitsPerPixel = fullHd ? 4.1d : 4.0d;
        return (int) Math.round(Math.max(1_600_000d,
                Math.min(12_000_000d, pixels * bitsPerPixel)));
    }
}
