package com.wfs.storyboost;

/** Immutable export options shared by Quick Enhance and Studio Editor. */
public final class ExportOptions {
    public final int preset,ratio,frameStyle,qualityMode;
    public final boolean fullHd;
    public final long trimStartMs,trimEndMs;
    public final float focusX,focusY,brightness,contrast,saturation;
    public final String caption;
    public final int captionColor;
    public final boolean muteAudio;
    public final int socialProfile;
    public final int customWidth,customHeight;
    public final boolean portraitBokeh,aiDenoise;

    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd){
        this(preset,ratio,frameStyle,fullHd,0,0,0.5f,0.5f);
    }
    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd,long start,long end){
        this(preset,ratio,frameStyle,fullHd,start,end,0.5f,0.5f);
    }
    public ExportOptions(int preset,int ratio,int frameStyle,boolean fullHd,long start,long end,
                         float focusX,float focusY){
        this(preset,ratio,frameStyle,fullHd?OutputProfile.FULL_HD:OutputProfile.HD,
                start,end,focusX,focusY,0f,0f,0f,"",0xFFFFFFFF,false,SocialExportProfile.CUSTOM,0,0,false,false);
    }
    private ExportOptions(int preset,int ratio,int frameStyle,int quality,long start,long end,
                          float focusX,float focusY,float brightness,float contrast,
                          float saturation,String caption,int captionColor,boolean mute,int socialProfile,int customWidth,int customHeight,boolean portraitBokeh,boolean aiDenoise){
        ClipWindow.validate(start,end);
        OutputProfile.normalized(quality);
        if(!Float.isFinite(focusX)||!Float.isFinite(focusY)||focusX<0||focusX>1||focusY<0||focusY>1)
            throw new IllegalArgumentException("Fokus crop harus antara 0–100%");
        if(!valid(brightness)||!valid(contrast)||!valid(saturation))
            throw new IllegalArgumentException("Pengaturan warna harus antara -100 dan +100");
        if(caption==null)caption="";
        if(caption.length()>80)throw new IllegalArgumentException("Teks maksimal 80 karakter");
        this.preset=preset;this.ratio=ratio;this.frameStyle=frameStyle;qualityMode=quality;
        fullHd=quality!=OutputProfile.HD;
        trimStartMs=start;trimEndMs=end;this.focusX=focusX;this.focusY=focusY;
        this.brightness=brightness;this.contrast=contrast;this.saturation=saturation;
        this.caption=caption;this.captionColor=captionColor;this.muteAudio=mute;
        SocialExportProfile.get(socialProfile);
        this.socialProfile=socialProfile;
        this.customWidth=customWidth;this.customHeight=customHeight;
        this.portraitBokeh=portraitBokeh;this.aiDenoise=aiDenoise;
    }
    private static boolean valid(float x){return Float.isFinite(x)&&x>=-1f&&x<=1f;}
    private ExportOptions copy(int ratio,int style,int quality,float brightness,float contrast,
                               float saturation,String caption,int captionColor,boolean mute){
        return new ExportOptions(preset,ratio,style,quality,trimStartMs,trimEndMs,focusX,focusY,
                brightness,contrast,saturation,caption,captionColor,mute,socialProfile,customWidth,customHeight,portraitBokeh,aiDenoise);
    }
    public ExportOptions withQuality(int quality){
        return copy(ratio,frameStyle,quality,brightness,contrast,saturation,caption,captionColor,muteAudio);
    }
    public ExportOptions forRatio(int ratio){
        return copy(ratio,frameStyle,qualityMode,brightness,contrast,saturation,caption,captionColor,muteAudio);
    }
    public ExportOptions inHd(){return withQuality(OutputProfile.HD);}
    public ExportOptions withFrameStyle(int style){
        if(style<AspectRatioProfile.FIT||style>AspectRatioProfile.BLUR)
            throw new IllegalArgumentException("Mode frame tidak dikenal");
        return copy(ratio,style,qualityMode,brightness,contrast,saturation,caption,captionColor,muteAudio);
    }
    public ExportOptions withAdjustments(float brightness,float contrast,float saturation){
        return copy(ratio,frameStyle,qualityMode,brightness,contrast,saturation,caption,captionColor,muteAudio);
    }
    public ExportOptions withCaption(String text,int color){
        return copy(ratio,frameStyle,qualityMode,brightness,contrast,saturation,text,color,muteAudio);
    }
    public ExportOptions withMute(boolean mute){
        return copy(ratio,frameStyle,qualityMode,brightness,contrast,saturation,caption,captionColor,mute);
    }
    public ExportOptions withSocialProfile(int profile){
        SocialExportProfile.get(profile);
        // Social profiles use safe, predictable vertical Full HD canvases;
        // manual profile never overrides geometry or quality.
        int nextRatio=profile==SocialExportProfile.CUSTOM?ratio:AspectRatioProfile.PORTRAIT_9_16;
        int nextQuality=profile==SocialExportProfile.CUSTOM?qualityMode:OutputProfile.FULL_HD;
        return new ExportOptions(preset,nextRatio,frameStyle,nextQuality,
                trimStartMs,trimEndMs,focusX,focusY,brightness,contrast,saturation,
                caption,captionColor,muteAudio,profile,customWidth,customHeight,portraitBokeh,aiDenoise);
    }
    /** Explicit custom width/height; copied through all derived variants. */
    public ExportOptions withCustomSize(int width,int height){
        AspectRatioProfile.validateCustom(width,height);
        return new ExportOptions(preset,AspectRatioProfile.CUSTOM,frameStyle,qualityMode,
                trimStartMs,trimEndMs,focusX,focusY,brightness,contrast,saturation,
                caption,captionColor,muteAudio,SocialExportProfile.CUSTOM,width,height,portraitBokeh,aiDenoise);
    }
    /** Machine-learning portrait cutout and background blur, for photographs only. */
    public ExportOptions withPortraitBokeh(boolean enabled){
        return new ExportOptions(preset,ratio,frameStyle,qualityMode,trimStartMs,trimEndMs,
                focusX,focusY,brightness,contrast,saturation,caption,captionColor,muteAudio,
                socialProfile,customWidth,customHeight,enabled,aiDenoise);
    }
    /** DnCNN on-device grayscale luminance denoiser for still photos only. */
    public ExportOptions withAiDenoise(boolean enabled){
        return new ExportOptions(preset,ratio,frameStyle,qualityMode,trimStartMs,trimEndMs,
                focusX,focusY,brightness,contrast,saturation,caption,captionColor,muteAudio,
                socialProfile,customWidth,customHeight,portraitBokeh,enabled);
    }
    public boolean hasColorAdjustments(){
        return brightness!=0f || contrast!=0f || saturation!=0f;
    }
    public boolean hasText(){
        return !caption.trim().isEmpty();
    }
}
