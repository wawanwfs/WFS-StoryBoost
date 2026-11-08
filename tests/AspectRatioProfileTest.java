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
        QualityAdvisor.Report severe=QualityAdvisor.analyze(
                1920,1080,1080,1920,AspectRatioProfile.CROP);
        check(severe.severeLoss(),"landscape to portrait crop warns severe source loss");
        check(severe.usableWidth>=600&&severe.usableWidth<=612,
                "landscape to portrait crop has about 608 native width");
        check(severe.retainedFraction<.35d,
                "crop from landscape removes more than 65% source pixels");
        QualityAdvisor.Report safe=QualityAdvisor.analyze(1920,1080,1080,1920,AspectRatioProfile.BLUR);
        check(!safe.severeLoss()&&safe.retainedFraction==1d,
                "Blur preserves source pixels");
        check(QualityAdvisor.analyze(1080,1920,1080,1920,AspectRatioProfile.CROP)
                .retainedFraction==1d,"matching aspect crop retains whole image");
        check(QualityAdvisor.analyze(1080,1920,1920,1080,AspectRatioProfile.CROP)
                .severeLoss(),"portrait to landscape crop warns");
        check(new ExportOptions(1,0,1,true).withFrameStyle(2).frameStyle==2,
                "export options preserve quality while choosing Blur");
        ExportOptions unchanged=new ExportOptions(4,AspectRatioProfile.ORIGINAL,
                AspectRatioProfile.FIT,true).withQuality(OutputProfile.MAX_DETAIL);
        check(VideoPassthrough.allowed(unchanged,"video/mp4"),
                "original mp4 can bypass lossy encoder");
        check(!VideoPassthrough.allowed(unchanged,"video/quicktime"),
                "quicktime source may not be relabeled as mp4");
        check(!VideoPassthrough.allowed(unchanged.forRatio(0),"video/mp4"),
                "ratio conversion must transcode video");
        check(!VideoPassthrough.allowed(new ExportOptions(4,4,0,true,500,0)
                .withQuality(OutputProfile.MAX_DETAIL),"video/mp4"),
                "trimmed video must not be copied blindly");
        check(!VideoPassthrough.allowed(new ExportOptions(1,4,0,true)
                .withQuality(OutputProfile.MAX_DETAIL),"video/mp4"),
                "enhanced video must not bypass processing");
        ExportOptions advanced=new ExportOptions(4,4,0,true)
                .withQuality(OutputProfile.MAX_DETAIL)
                .withAdjustments(.15f,-.1f,.25f)
                .withCaption("My Story",0xFFFFD786)
                .withMute(true);
        check(advanced.hasColorAdjustments(),"advanced filter settings are retained");
        check(advanced.hasText(),"caption stored for rendering");
        check(advanced.muteAudio,"mute audio stored");
        check(advanced.forRatio(0).withQuality(OutputProfile.FULL_HD).hasText(),
                "dual-aspect quality update preserves caption");
        check(advanced.forRatio(0).muteAudio,"dual aspect preserves mute audio");
        check(advanced.withFrameStyle(2).brightness==.15f,
                "framing preserves custom brightness");
        check(!VideoPassthrough.allowed(advanced,"video/mp4"),
                "muted edited video cannot bypass transcoding");
        check(!VideoPassthrough.allowed(new ExportOptions(4,4,0,true)
                .withQuality(OutputProfile.MAX_DETAIL).withCaption("Caption",0xFFFFFFFF),"video/mp4"),
                "text overlay cannot bypass video encoder");
        check(!VideoPassthrough.allowed(new ExportOptions(4,4,0,true)
                .withQuality(OutputProfile.MAX_DETAIL).withAdjustments(.2f,0f,0f),"video/mp4"),
                "color adjustments cannot bypass video encoder");
        try{
            new ExportOptions(1,0,0,true).withAdjustments(1.1f,0,0);
            throw new AssertionError("invalid adjustment accepted");
        }catch(IllegalArgumentException expected){passed++;}
        try{
            new ExportOptions(1,0,0,true).withCaption("X".repeat(81),0xFFFFFFFF);
            throw new AssertionError("overlong caption accepted");
        }catch(IllegalArgumentException expected){passed++;}
        check(ColorLooks.count()==24,"24 color looks available");
        check(ColorLooks.get(ColorLooks.ORIGINAL).name.equals("Original"),"original preset index preserved");
        check(ColorLooks.get(6).name.equals("Golden Hour"),"golden-hour look available");
        check(ColorLooks.get(12).saturation==0f,"monochrome preset desaturates");
        try{ColorLooks.get(24);throw new AssertionError("Invalid look accepted");}
        catch(IllegalArgumentException expected){passed++;}
        check(SocialExportProfile.count()==5,"four social presets plus manual");
        ExportOptions ig=new ExportOptions(6,AspectRatioProfile.LANDSCAPE_16_9,
                AspectRatioProfile.FIT,true).withQuality(OutputProfile.MAX_DETAIL)
                .withSocialProfile(SocialExportProfile.INSTAGRAM_REELS);
        check(ig.ratio==AspectRatioProfile.PORTRAIT_9_16,"Reels forces 9:16");
        check(ig.qualityMode==OutputProfile.FULL_HD,"Reels outputs Full HD");
        check(ig.preset==6&&ig.socialProfile==SocialExportProfile.INSTAGRAM_REELS,
                "social export retains look");
        check(ig.withQuality(OutputProfile.HD).socialProfile==ig.socialProfile,
                "social profile survives chained quality setting");
        check(ig.forRatio(AspectRatioProfile.LANDSCAPE_16_9).socialProfile==ig.socialProfile,
                "social recipe survives per-ratio options");
        check(SocialExportProfile.cappedBitrate(SocialExportProfile.WHATSAPP_STATUS,
                1080,1920,OutputProfile.FULL_HD,60f)<=8_000_000,
                "WhatsApp caps 1080p bitrate");
        check(SocialExportProfile.cappedBitrate(SocialExportProfile.TIKTOK,
                1080,1920,OutputProfile.FULL_HD,60f)<=18_000_000,
                "TikTok caps 1080p bitrate");
        check(SocialExportProfile.cappedBitrate(SocialExportProfile.CUSTOM,
                1080,1920,OutputProfile.FULL_HD,30f)
                ==VideoBitrate.estimate(1080,1920,OutputProfile.FULL_HD,30f),
                "manual bitrate unchanged");
        check(SocialExportProfile.captionCenterY(SocialExportProfile.INSTAGRAM_REELS)>.14f
                &&SocialExportProfile.captionCenterY(SocialExportProfile.INSTAGRAM_REELS)<.65f,
                "Reels caption respects vertical safe zone");
        check(SocialExportProfile.captionCenterY(SocialExportProfile.CUSTOM)==.83f,
                "manual caption position preserved");
        try{SocialExportProfile.get(7);throw new AssertionError("Invalid profile accepted");}
        catch(IllegalArgumentException expected){passed++;}
        check(AspectRatioProfile.count()==12,"11 presets plus custom available");
        check(AspectRatioProfile.labels().length==12,"ratio labels count");
        expect(AspectRatioProfile.PORTRAIT_3_4,true,1920,1080,1080,1440);
        expect(AspectRatioProfile.LANDSCAPE_4_3,true,1080,1920,1920,1440);
        expect(AspectRatioProfile.LANDSCAPE_3_2,true,1080,1920,1920,1280);
        expect(AspectRatioProfile.PORTRAIT_2_3,true,1920,1080,1080,1620);
        expect(AspectRatioProfile.ULTRAWIDE_21_9,true,1920,1080,1920,822);
        expect(AspectRatioProfile.ULTRATALL_9_21,true,1080,1920,822,1920);
        expect(AspectRatioProfile.PORTRAIT_3_4,false,1920,1080,720,960);
        expect(AspectRatioProfile.LANDSCAPE_4_3,false,1080,1920,1280,960);
        expect(AspectRatioProfile.LANDSCAPE_3_2,false,1080,1920,1280,852);
        expect(AspectRatioProfile.PORTRAIT_2_3,false,1920,1080,720,1080);
        check(AspectRatioProfile.reverse(AspectRatioProfile.PORTRAIT_3_4)
                ==AspectRatioProfile.LANDSCAPE_4_3,"3:4 reverse to 4:3");
        check(AspectRatioProfile.reverse(AspectRatioProfile.LANDSCAPE_3_2)
                ==AspectRatioProfile.PORTRAIT_2_3,"3:2 reverse to 2:3");
        check(AspectRatioProfile.reverse(AspectRatioProfile.ULTRATALL_9_21)
                ==AspectRatioProfile.ULTRAWIDE_21_9,"9:21 reverse");
        check(AspectRatioProfile.aspect(AspectRatioProfile.CUSTOM,1200,800,1350,1080)==1.25d,
                "custom ratio aspect");
        AspectRatioProfile.Size custom=AspectRatioProfile.resolve(
                AspectRatioProfile.CUSTOM,true,1500,1200,1350,1080);
        check(custom.width==1350&&custom.height==1080,"custom photo dimensions");
        AspectRatioProfile.Size customCrop=AspectRatioProfile.resolve(
                AspectRatioProfile.CUSTOM,OutputProfile.MAX_DETAIL,1200,800,
                AspectRatioProfile.CROP,1350,1080);
        check(customCrop.width<=1200&&customCrop.height<=800,"custom crop does not upscale pixels");
        ExportOptions customSettings=new ExportOptions(1,AspectRatioProfile.ORIGINAL,
                AspectRatioProfile.FIT,true).withCustomSize(1350,1080);
        check(customSettings.ratio==AspectRatioProfile.CUSTOM
                &&customSettings.customWidth==1350&&customSettings.customHeight==1080,
                "custom options retained");
        check(customSettings.withQuality(OutputProfile.MAX_DETAIL).customWidth==1350,
                "custom resolution retained across quality settings");
        check(customSettings.withFrameStyle(AspectRatioProfile.CROP).customHeight==1080,
                "custom height retained across framing");
        try{AspectRatioProfile.resolve(AspectRatioProfile.CUSTOM,true,1080,1920);
            throw new AssertionError("Missing custom width accepted");
        }catch(IllegalArgumentException expected){passed++;}
        try{AspectRatioProfile.validateCustom(4000,4000);
            throw new AssertionError("Too large custom size accepted");
        }catch(IllegalArgumentException expected){passed++;}
        check(AiPhotoPolicy.suggest("Beach",.90f,0,.5f,.5f).colorLook==19,
                "ML scene beach maps to Beach Light");
        check(AiPhotoPolicy.suggest("Sunset",.91f,0,.5f,.5f).colorLook==20,
                "ML scene sunset maps to Sunset Glow");
        check(AiPhotoPolicy.suggest("Forest",.90f,0,.5f,.5f).colorLook==16,
                "ML scene forest maps to Forest Green");
        check(AiPhotoPolicy.suggest("Selfie",.91f,1,.75f,.25f).colorLook==21,
                "face scene maps to Portrait Soft");
        check(AiPhotoPolicy.suggest(null,0f,1,2f,-1f).focusX==1f,
                "face focal x clamped");
        check(AiPhotoPolicy.suggest(null,0f,1,2f,-1f).focusY==0f,
                "face focal y clamped");
        check(AiPhotoPolicy.suggest(null,0f,0,.5f,.5f).colorLook==0,
                "unknown scene uses safe Natural look");
        // v2.7: bundled ML Kit portrait bokeh is real segmentation, not upscale.
        check(PortraitMaskMath.alpha(0f)==0,"mask confidence zero is transparent");
        check(PortraitMaskMath.alpha(.12f)==0,"low confidence is background");
        check(PortraitMaskMath.alpha(.50f)>=120&&PortraitMaskMath.alpha(.50f)<=136,
                "medium mask confidence yields feathered subject edge");
        check(PortraitMaskMath.alpha(1f)==255,"high confidence keeps original subject");
        check(PortraitMaskMath.alpha(Float.NaN)==0,"NaN confidence never creates solid mask");
        check(!PortraitMaskMath.usable(.005f),"empty mask is rejected");
        check(PortraitMaskMath.usable(.25f),"realistic subject coverage accepted");
        check(!PortraitMaskMath.usable(.995f),"near-full mask is rejected");
        ExportOptions naturalOriginal = new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.ORIGINAL,AspectRatioProfile.FIT,true)
                .withQuality(OutputProfile.MAX_DETAIL);
        check(PhotoPassthrough.allowed(naturalOriginal,"image/jpeg"),
                "true original JPEG is eligible for byte-for-byte copy");
        ExportOptions portraitEdit=naturalOriginal.withPortraitBokeh(true);
        check(portraitEdit.portraitBokeh,"portrait effect persists in export options");
        check(!PhotoPassthrough.allowed(portraitEdit,"image/jpeg"),
                "AI portrait edit must not use unchanged JPEG copy");
        check(!VideoPassthrough.allowed(portraitEdit,"video/mp4"),
                "AI portrait edit must not silently pass through a video");
        check(portraitEdit.withQuality(OutputProfile.HD).portraitBokeh,
                "AI portrait effect survives quality setting");
        check(portraitEdit.withSocialProfile(SocialExportProfile.WHATSAPP_STATUS).portraitBokeh,
                "AI portrait effect survives social platform selection");
        check(portraitEdit.forRatio(AspectRatioProfile.PORTRAIT_9_16).portraitBokeh,
                "AI portrait effect survives ratio change");
        check(portraitEdit.withCustomSize(1350,1080).portraitBokeh,
                "AI portrait effect survives custom canvas");
        check(portraitEdit.withAdjustments(.2f,0f,0f).portraitBokeh,
                "AI portrait effect survives manual color adjustments");
        check(PhotoPassthrough.allowed(portraitEdit.withPortraitBokeh(false),"image/jpeg"),
                "disabling portrait effect restores original copy eligibility");
        // v2.8 mask feather shape/threshold regression.
        check(PortraitMaskMath.alpha(.31f)>25 && PortraitMaskMath.alpha(.31f)<65,
                "feathered alpha lower shoulder remains semi-transparent");
        check(PortraitMaskMath.alpha(.69f)>190 && PortraitMaskMath.alpha(.69f)<236,
                "feathered alpha upper shoulder remains semi-transparent");
        check(PortraitMaskMath.alpha(.31f)<PortraitMaskMath.alpha(.50f),
                "alpha increases monotonically below foreground");
        check(PortraitMaskMath.alpha(.50f)<PortraitMaskMath.alpha(.69f),
                "alpha increases monotonically toward foreground");
        check(PortraitMaskMath.usable(.015f),"minimum foreground coverage allowed");
        check(!PortraitMaskMath.usable(Float.NaN),"invalid foreground coverage rejected");
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
