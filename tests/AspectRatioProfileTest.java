package com.wfs.storyboost;

/** No Android runtime needed; can run via scripts/test-ratio-geometry.sh. */
public final class AspectRatioProfileTest {
    private static int passed;
    public static void main(String[] args) {
        expect(AspectRatioProfile.PORTRAIT_9_16, true, 1920, 1080, 1080, 1920);
        expect(AspectRatioProfile.LANDSCAPE_16_9, true, 1080, 1920, 1920, 1080);
        expect(AspectRatioProfile.PORTRAIT_9_16, false, 1920, 1080, 720, 1280);
        expect(AspectRatioProfile.LANDSCAPE_16_9, false, 1080, 1920, 1280, 720);
        expect(AspectRatioProfile.SQUARE_1_1, true, 1080, 1920, 1080, 1080);
        expect(AspectRatioProfile.PORTRAIT_4_5, true, 1080, 1920, 1080, 1350);
        expect(AspectRatioProfile.PORTRAIT_4_5, false, 1080, 1920, 720, 900);
        expect(AspectRatioProfile.ORIGINAL, true, 3840, 2160, 1920, 1080);
        expect(AspectRatioProfile.ORIGINAL, true, 2160, 3840, 1080, 1920);
        expect(AspectRatioProfile.ORIGINAL, false, 1280, 720, 1280, 720);
        expect(AspectRatioProfile.ORIGINAL, true, 640, 480, 640, 480);
        expect(AspectRatioProfile.ORIGINAL, true, 1919, 1079, 1918, 1078);
        FrameMath.Bounds portraitFit = FrameMath.center(1920, 1080, 1080, 1920, AspectRatioProfile.FIT);
        check(Math.abs(portraitFit.width() - 1080) < 0.1f
                && portraitFit.top > 0 && portraitFit.bottom < 1920, "landscape → portrait fit");
        FrameMath.Bounds portraitCrop = FrameMath.center(1920, 1080, 1080, 1920, AspectRatioProfile.CROP);
        check(Math.abs(portraitCrop.height() - 1920) < 0.1f
                && portraitCrop.left < 0 && portraitCrop.right > 1080, "landscape → portrait crop");
        FrameMath.Bounds landscapeFit = FrameMath.center(1080, 1920, 1920, 1080, AspectRatioProfile.FIT);
        check(Math.abs(landscapeFit.height() - 1080) < 0.1f
                && landscapeFit.left > 0 && landscapeFit.right < 1920, "portrait → landscape fit");
        FrameMath.Bounds landscapeCrop = FrameMath.center(1080, 1920, 1920, 1080, AspectRatioProfile.CROP);
        check(Math.abs(landscapeCrop.width() - 1920) < 0.1f
                && landscapeCrop.top < 0 && landscapeCrop.bottom > 1080, "portrait → landscape crop");
        check(VideoBitrate.estimate(1080, 1920, true) > VideoBitrate.estimate(720, 1280, false),
                "Full HD AVC bitrate higher than HD");
        check(VideoBitrate.estimate(720, 1280, false) >= 1_600_000,
                "HD AVC bitrate floor");
        check(VideoBitrate.estimate(1920, 1080, true) <= 12_000_000,
                "Full HD bitrate cap");
        try {
            VideoBitrate.estimate(0, 1920, true);
            throw new AssertionError("Expected invalid video dimensions");
        } catch (IllegalArgumentException expected) { passed++; }
        try {
            AspectRatioProfile.resolve(0, true, 0, 100);
            throw new AssertionError("Expected invalid dimension rejection");
        } catch (IllegalArgumentException expected) { passed++; }
        System.out.println("PASS " + passed + " aspect geometry assertions");
    }

    private static void check(boolean valid, String description) {
        if (!valid) throw new AssertionError(description);
        passed++;
    }

    private static void expect(int ratio, boolean fullHd, int inputW, int inputH,
                               int expectedW, int expectedH) {
        AspectRatioProfile.Size size = AspectRatioProfile.resolve(ratio, fullHd, inputW, inputH);
        if (size.width != expectedW || size.height != expectedH) {
            throw new AssertionError(AspectRatioProfile.label(ratio) + " expected "
                    + expectedW + "x" + expectedH + " got " + size);
        }
        if (size.width % 2 != 0 || size.height % 2 != 0) {
            throw new AssertionError("Dimensions must be even for H.264");
        }
        passed++;
    }
}
