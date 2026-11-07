package com.wfs.storyboost;
/** Normalized crop region used by the Media3 video crop effect. */
public final class CropFocus {
    private CropFocus(){}
    public static final class Bounds {
        public final float left,right,bottom,top;
        public Bounds(float l,float r,float b,float t){
            left=l;right=r;bottom=b;top=t;
        }
    }
    public static Bounds region(int sw,int sh,int tw,int th,float focusX,float focusY){
        if(sw<=0||sh<=0||tw<=0||th<=0)
            throw new IllegalArgumentException("Dimensi crop tidak valid");
        float fx=Math.max(0f,Math.min(1f,focusX));
        float fy=Math.max(0f,Math.min(1f,focusY));
        double sr=(double)sw/sh,tr=(double)tw/th;
        if(sr>tr){
            float half=(float)(tr/sr);
            float center=(fx*2f-1f)*(1f-half);
            return new Bounds(center-half,center+half,-1f,1f);
        }
        if(sr<tr){
            float half=(float)(sr/tr);
            float center=(1f-2f*fy)*(1f-half);
            return new Bounds(-1f,1f,center-half,center+half);
        }
        return new Bounds(-1f,1f,-1f,1f);
    }
}
