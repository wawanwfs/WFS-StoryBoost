package com.wfs.storyboost;
/** Immutable export settings for one media item. */
public final class ExportOptions {
    public final int preset,ratio,frameStyle;
    public final boolean fullHd;
    public final long trimStartMs,trimEndMs;
    public final float focusX,focusY;
    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd){
        this(preset,ratio,frameStyle,fullHd,0,0,0.5f,0.5f);
    }
    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd,long start,long end){
        this(preset,ratio,frameStyle,fullHd,start,end,0.5f,0.5f);
    }
    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd,long start,long end,
                         float focusX,float focusY){
        ClipWindow.validate(start,end);
        if(!Float.isFinite(focusX)||!Float.isFinite(focusY)||focusX<0||focusX>1||focusY<0||focusY>1)
            throw new IllegalArgumentException("Fokus crop harus antara 0–100%");
        this.preset=preset;this.ratio=ratio;this.frameStyle=frameStyle;this.fullHd=fullHd;
        trimStartMs=start;trimEndMs=end;this.focusX=focusX;this.focusY=focusY;
    }
    public ExportOptions forRatio(int ratio){
        return new ExportOptions(preset,ratio,frameStyle,fullHd,trimStartMs,trimEndMs,focusX,focusY);
    }
    public ExportOptions inHd(){
        return new ExportOptions(preset,ratio,frameStyle,false,trimStartMs,trimEndMs,focusX,focusY);
    }
}
