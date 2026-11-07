package com.wfs.storyboost;

/** Source-aware AVC bitrate budgets (bits per second), not an output quality guarantee. */
public final class VideoBitrate {
    private VideoBitrate() {}
    public static int estimate(int width,int height,boolean fullHd){
        return estimate(width,height,fullHd,30f);
    }
    public static int estimate(int width,int height,boolean fullHd,float fps){
        return estimate(width,height,fullHd?OutputProfile.FULL_HD:OutputProfile.HD,fps);
    }
    public static int estimate(int width,int height,int quality,float fps){
        if(width<2||height<2||width>8192||height>8192)throw new IllegalArgumentException("Invalid video dimensions");
        OutputProfile.normalized(quality);
        double realFps = Float.isFinite(fps)&&fps>=1f&&fps<=240f?fps:30d;
        double cadence = Math.max(0.85, Math.min(1.7, Math.sqrt(realFps/30d)));
        double pixels=(double)width*height;
        double rate = pixels * (quality==OutputProfile.HD ? 0.19d : 0.23d) * 30d * cadence;
        double min=quality==OutputProfile.HD ? 4_500_000d : 9_000_000d;
        double max=quality==OutputProfile.HD ? 12_000_000d : quality==OutputProfile.FULL_HD ? 22_000_000d : 60_000_000d;
        return (int)Math.round(Math.max(min,Math.min(max,rate)));
    }
}
