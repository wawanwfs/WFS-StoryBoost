package com.wfs.storyboost;

/** Pure-Java guard against accidentally copying edited JPEGs without rendering edits. */
public final class PhotoPassthrough {
    private PhotoPassthrough(){}
    public static boolean allowed(ExportOptions options,String mime){
        return options.preset==ColorLooks.ORIGINAL
                && options.ratio==AspectRatioProfile.ORIGINAL
                && options.frameStyle==AspectRatioProfile.FIT
                && options.qualityMode==OutputProfile.MAX_DETAIL
                && !options.hasText() && !options.hasColorAdjustments()
                && !options.portraitBokeh && !options.aiDenoise
                && "image/jpeg".equalsIgnoreCase(mime);
    }
}
