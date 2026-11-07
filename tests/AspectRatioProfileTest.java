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
        check(VideoBitrate.estimate(1920, 1080, true) <= 22_000_000,
                "Full HD bitrate cap");
        try {
            VideoBitrate.estimate(0, 1920, true);
            throw new AssertionError("Expected invalid video dimensions");
        } catch (IllegalArgumentException expected) { passed++; }
        try {
            AspectRatioProfile.resolve(0, true, 0, 100);
            throw new AssertionError("Expected invalid dimension rejection");
        } catch (IllegalArgumentException expected) { passed++; }
        check(AspectRatioProfile.reverse(AspectRatioProfile.PORTRAIT_9_16)
                == AspectRatioProfile.LANDSCAPE_16_9, "9:16 -> 16:9 reverse");
        check(AspectRatioProfile.reverse(AspectRatioProfile.LANDSCAPE_16_9)
                == AspectRatioProfile.PORTRAIT_9_16, "16:9 -> 9:16 reverse");
        check(AspectRatioProfile.reverse(AspectRatioProfile.ORIGINAL)
                == AspectRatioProfile.ORIGINAL, "original ratio reverse stays original");
        check(VideoBitrate.estimate(1080, 1920, true, 60f)
                > VideoBitrate.estimate(1080, 1920, true, 30f), "60fps gets more budget");
        check(VideoBitrate.estimate(1080, 1920, true, 0f)
                == VideoBitrate.estimate(1080, 1920, true), "unknown fps falls back to 30");
        check(VideoBitrate.estimate(1920, 1080, true, 240f) <= 22_000_000,
                "FPS adaptive bitrate is capped");
        check(Math.abs(VideoFramePlan.sharpScale(1920,1080,1080,1920)-0.31640625f)<0.001,"blur landscape portrait");
        check(Math.abs(VideoFramePlan.sharpScale(1080,1920,1920,1080)-0.31640625f)<0.001,"blur portrait landscape");
        check(VideoFramePlan.sharpScale(1080,1920,720,1280)==1f,"same aspect");
        check(VideoFramePlan.sharpScale(1080,1080,1080,1920)<1f,"square blur");
        check(ClipWindow.expectedDuration(120000,10000,30000)==20000,"clip time range");
        check(ClipWindow.expectedDuration(120000,30000,0)==90000,"clip open end");
        try{ClipWindow.validate(5000,1000);throw new AssertionError("invalid clip accepted");}
        catch(IllegalArgumentException expected){passed++;}
        try{ClipWindow.expectedDuration(4000,5000,0);throw new AssertionError("out of bounds accepted");}
        catch(IllegalArgumentException expected){passed++;}
        check(new ExportOptions(1,0,0,true,1000,3000).forRatio(1).trimStartMs==1000,"multi ratio retains clip");
        check(!new ExportOptions(1,0,0,true).inHd().fullHd,"fallback HD");
        FrameMath.Bounds left=FrameMath.center(1920,1080,1080,1920,
                AspectRatioProfile.CROP,0f,0.5f);
        FrameMath.Bounds right=FrameMath.center(1920,1080,1080,1920,
                AspectRatioProfile.CROP,1f,0.5f);
        check(left.left==0f,"left anchored crop");
        check(Math.abs(right.right-1080)<0.1f,"right anchored crop");
        CropFocus.Bounds l=CropFocus.region(1920,1080,1080,1920,0,0.5f);
        CropFocus.Bounds r=CropFocus.region(1920,1080,1080,1920,1,0.5f);
        check(Math.abs(l.left+1f)<0.001f,"NDC left focus");
        check(Math.abs(r.right-1f)<0.001f,"NDC right focus");
        CropFocus.Bounds top=CropFocus.region(1080,1920,1920,1080,0.5f,0f);
        check(Math.abs(top.top-1f)<0.001f,"NDC top focus");
        check(new ExportOptions(1,0,1,true,0,0,0f,1f).inHd().focusY==1f,
                "focus preserved through HD fallback");
        check(OutputProfile.targetLongEdge(OutputProfile.MAX_DETAIL,2160,3840)==3840,"4K source target");
        check(OutputProfile.targetLongEdge(OutputProfile.MAX_DETAIL,1080,1920)==1920,"1080 source no artificial 4K");
        AspectRatioProfile.Size q4k=AspectRatioProfile.resolve(AspectRatioProfile.PORTRAIT_9_16,OutputProfile.MAX_DETAIL,2160,3840,AspectRatioProfile.FIT);
        check(q4k.width==2160&&q4k.height==3840,"native 4K portrait");
        AspectRatioProfile.Size qcrop=AspectRatioProfile.resolve(AspectRatioProfile.PORTRAIT_9_16,OutputProfile.MAX_DETAIL,1920,1080,AspectRatioProfile.CROP);
        check(qcrop.height<=1080&&qcrop.width<=1920,"native landscape crop never enlarged");
        AspectRatioProfile.Size qoriginal=AspectRatioProfile.resolve(AspectRatioProfile.ORIGINAL,OutputProfile.MAX_DETAIL,640,480,AspectRatioProfile.FIT);
        check(qoriginal.width==640&&qoriginal.height==480,"small original unchanged");
        check(VideoBitrate.estimate(2160,3840,OutputProfile.MAX_DETAIL,30f)>=35_000_000,"4K bitrate");
        check(VideoBitrate.estimate(1080,1920,OutputProfile.MAX_DETAIL,60f)>VideoBitrate.estimate(1080,1920,OutputProfile.MAX_DETAIL,30f),"MAX 60fps");
        check(new ExportOptions(1,0,0,true).withQuality(OutputProfile.MAX_DETAIL).forRatio(1).qualityMode==OutputProfile.MAX_DETAIL,"dual aspect keeps max quality");
        check(new ExportOptions(1,0,0,true).withQuality(OutputProfile.MAX_DETAIL).inHd().qualityMode==OutputProfile.HD,"fallback HD mode");
        try{OutputProfile.normalized(3);throw new AssertionError("invalid quality");}
        catch(IllegalArgumentException expected){passed++;}
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
