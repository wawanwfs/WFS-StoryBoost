package com.wfs.storyboost;
public final class VideoFramePlan {
    private VideoFramePlan(){}
    public static float sharpScale(int sw,int sh,int tw,int th){
        if(sw<=0||sh<=0||tw<=0||th<=0)throw new IllegalArgumentException("Dimensi tidak valid");
        double sr=(double)sw/sh,tr=(double)tw/th;
        return (float)Math.min(1.0,Math.min(sr/tr,tr/sr));
    }
}
