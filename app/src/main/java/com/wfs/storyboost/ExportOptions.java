package com.wfs.storyboost;
/** Settings snapshot for a single export. */
public final class ExportOptions {
    public final int preset,ratio,frameStyle;
    public final boolean fullHd;
    public final long trimStartMs,trimEndMs;
    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd){
        this(preset,ratio,frameStyle,fullHd,0,0);
    }
    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd,long start,long end){
        ClipWindow.validate(start,end);
        this.preset=preset;this.ratio=ratio;this.frameStyle=frameStyle;
        this.fullHd=fullHd;this.trimStartMs=start;this.trimEndMs=end;
    }
    public ExportOptions forRatio(int ratio){
        return new ExportOptions(preset,ratio,frameStyle,fullHd,trimStartMs,trimEndMs);
    }
    public ExportOptions inHd(){
        return new ExportOptions(preset,ratio,frameStyle,false,trimStartMs,trimEndMs);
    }
}
