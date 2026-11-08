package com.wfs.storyboost;

/** Unit-tested, pure-Java alpha policy for the ML Kit selfie mask. */
public final class PortraitMaskMath {
    private PortraitMaskMath(){}
    public static int alpha(float confidence){
        if(!Float.isFinite(confidence))return 0;
        float x=Math.max(0f,Math.min(1f,(confidence-.12f)/.76f));
        // Cubic smoothstep makes low-resolution hair/garment edges less abrupt
        // without changing the original foreground RGB pixels.
        float feather=x*x*(3f-2f*x);
        return Math.min(255,Math.max(0,Math.round(feather*255f)));
    }
    public static boolean usable(float foregroundFraction){
        return Float.isFinite(foregroundFraction)
                && foregroundFraction>=.015f && foregroundFraction<=.975f;
    }
}
