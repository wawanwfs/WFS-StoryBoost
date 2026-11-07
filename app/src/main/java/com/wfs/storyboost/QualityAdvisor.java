package com.wfs.storyboost;

/**
 * Reports actual source pixels surviving a ratio conversion.
 * Large target dimensions or high bitrate do not create missing pixels.
 */
public final class QualityAdvisor {
    private QualityAdvisor(){}

    public static final class Report {
        public final int usableWidth, usableHeight;
        public final double retainedFraction, enlargement;
        public final boolean crop;
        Report(int w,int h,double retained,double enlarges,boolean crop){
            usableWidth=w;usableHeight=h;retainedFraction=retained;enlargement=enlarges;this.crop=crop;
        }
        public boolean severeLoss(){
            return crop && (retainedFraction < .5d || enlargement > 1.35d);
        }
        public String description(){
            if(!crop)return "Fit/Blur mempertahankan seluruh gambar sumber.";
            int pct=(int)Math.round(retainedFraction*100d);
            return "Crop hanya menggunakan sekitar "+pct+"% piksel sumber ("
                    +usableWidth+"×"+usableHeight+"). Bitrate tinggi tidak bisa mengembalikan detail.";
        }
    }

    public static Report analyze(int sw,int sh,int tw,int th,int framing){
        if(sw<=0||sh<=0||tw<=0||th<=0)
            throw new IllegalArgumentException("Dimensi sumber/target tidak valid");
        boolean crop=framing==AspectRatioProfile.CROP;
        double scale=crop?Math.max((double)tw/sw,(double)th/sh):
                Math.min((double)tw/sw,(double)th/sh);
        double usableW=crop?Math.min(sw,tw/scale):sw;
        double usableH=crop?Math.min(sh,th/scale):sh;
        double fraction=Math.min(1d,(usableW*usableH)/((double)sw*sh));
        return new Report(Math.max(1,(int)Math.round(usableW)),
                Math.max(1,(int)Math.round(usableH)),fraction,
                Math.max(1d,scale),crop);
    }
}
