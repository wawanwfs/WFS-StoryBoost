package com.wfs.storyboost;

/** Only MP4 without any requested edit can be copied byte-for-byte. */
public final class VideoPassthrough {
    private VideoPassthrough(){}
    public static boolean allowed(ExportOptions o,String mime) {
        return o.preset==4 && o.ratio==AspectRatioProfile.ORIGINAL
                && o.frameStyle==AspectRatioProfile.FIT
                && o.qualityMode==OutputProfile.MAX_DETAIL
                && o.trimStartMs==0L&&o.trimEndMs==0L
                && !o.muteAudio && !o.hasText() && !o.hasColorAdjustments()
                && !o.portraitBokeh && !o.aiDenoise
                &&"video/mp4".equalsIgnoreCase(mime);
    }
}
