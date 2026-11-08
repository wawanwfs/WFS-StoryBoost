package com.wfs.storyboost;

/**
 * Converts a finger drag in the output canvas into a normalized source crop
 * focus change. An axis with no crop remains unchanged. No Android dependencies.
 */
public final class CropGestureMath {
    private CropGestureMath(){}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
    public static float moveX(int srcW,int srcH,int canvasW,int canvasH,
                              float startingFocus,float dragPixels){
        if(srcW<=0||srcH<=0||canvasW<=0||canvasH<=0||!Float.isFinite(dragPixels))
            throw new IllegalArgumentException("Invalid crop transform");
        double visible=(double)canvasW*srcH/((double)canvasH*srcW);
        if(visible>=1d-1e-8)return clamp(startingFocus);
        return clamp((float)(startingFocus-
                dragPixels/canvasW*visible/(1d-visible)));
    }
    public static float moveY(int srcW,int srcH,int canvasW,int canvasH,
                              float startingFocus,float dragPixels){
        if(srcW<=0||srcH<=0||canvasW<=0||canvasH<=0||!Float.isFinite(dragPixels))
            throw new IllegalArgumentException("Invalid crop transform");
        double visible=(double)canvasH*srcW/((double)canvasW*srcH);
        if(visible>=1d-1e-8)return clamp(startingFocus);
        return clamp((float)(startingFocus-
                dragPixels/canvasH*visible/(1d-visible)));
    }
}