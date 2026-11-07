package com.wfs.storyboost;

/** Immutable, per-export options; UI changes cannot modify in-progress jobs. */
public final class ExportOptions {
    public final int preset,ratio,frameStyle,qualityMode;
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
        this(preset,ratio,frameStyle,fullHd ? OutputProfile.FULL_HD : OutputProfile.HD,
             start,end,focusX,focusY);
    }
    private ExportOptions(int preset,int ratio,int frameStyle,int quality,long start,long end,
                          float focusX,float focusY){
        ClipWindow.validate(start,end);
        OutputProfile.normalized(quality);
        if(!Float.isFinite(focusX)||!Float.isFinite(focusY)||focusX<0||focusX>1||focusY<0||focusY>1)
            throw new IllegalArgumentException("Fokus crop harus antara 0–100%");
        this.preset=preset;this.ratio=ratio;this.frameStyle=frameStyle;qualityMode=quality;
        fullHd=quality!=OutputProfile.HD;
        trimStartMs=start;trimEndMs=end;this.focusX=focusX;this.focusY=focusY;
    }
    public ExportOptions withQuality(int quality){
        return new ExportOptions(preset,ratio,frameStyle,quality,trimStartMs,trimEndMs,focusX,focusY);
    }
    public ExportOptions forRatio(int ratio){
        return new ExportOptions(preset,ratio,frameStyle,qualityMode,trimStartMs,trimEndMs,focusX,focusY);
    }
    public ExportOptions inHd(){
        return withQuality(OutputProfile.HD);
    }
    public ExportOptions withFrameStyle(int style){
        if(style<AspectRatioProfile.FIT||style>AspectRatioProfile.BLUR)
            throw new IllegalArgumentException("Mode frame tidak dikenal");
        return new ExportOptions(preset,ratio,style,qualityMode,trimStartMs,trimEndMs,focusX,focusY);
    }
}
