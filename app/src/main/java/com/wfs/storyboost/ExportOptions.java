package com.wfs.storyboost;

/** Immutable settings snapshot for a batch. */
public final class ExportOptions {
    public final int preset;
    public final int ratio;
    public final int frameStyle;
    public final boolean fullHd;

    public ExportOptions(int preset, int ratio, int frameStyle, boolean fullHd) {
        this.preset = preset;
        this.ratio = ratio;
        this.frameStyle = frameStyle;
        this.fullHd = fullHd;
    }
}
