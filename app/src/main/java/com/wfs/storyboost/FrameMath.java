package com.wfs.storyboost;

/** Centered frame bounds, independent from Android graphics APIs. */
public final class FrameMath {
    private FrameMath() {}

    public static final class Bounds {
        public final float left, top, right, bottom;
        public Bounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        public float width() { return right - left; }
        public float height() { return bottom - top; }
    }

    public static Bounds center(int sourceW, int sourceH, int targetW, int targetH,
                                 int frameStyle) {
        return center(sourceW,sourceH,targetW,targetH,frameStyle,0.5f,0.5f);
    }
    public static Bounds center(int sourceW,int sourceH,int targetW,int targetH,
                                int frameStyle,float focusX,float focusY) {
        if (sourceW <= 0 || sourceH <= 0 || targetW <= 0 || targetH <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        double scale = frameStyle == AspectRatioProfile.CROP
                ? Math.max((double) targetW / sourceW, (double) targetH / sourceH)
                : Math.min((double) targetW / sourceW, (double) targetH / sourceH);
        float drawnW = (float) (sourceW * scale);
        float drawnH = (float) (sourceH * scale);
        float x=(targetW-drawnW)*(frameStyle==AspectRatioProfile.CROP?
                Math.max(0f,Math.min(1f,focusX)):0.5f);
        float y=(targetH-drawnH)*(frameStyle==AspectRatioProfile.CROP?
                Math.max(0f,Math.min(1f,focusY)):0.5f);
        return new Bounds(x,y,x+drawnW,y+drawnH);
    }
}
