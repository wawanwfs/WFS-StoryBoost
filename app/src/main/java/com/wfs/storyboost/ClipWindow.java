package com.wfs.storyboost;
public final class ClipWindow {
    private ClipWindow(){}
    public static void validate(long startMs,long endMs){
        if(startMs<0||endMs<0||(endMs>0&&endMs<=startMs))
            throw new IllegalArgumentException("Rentang trim video tidak valid.");
    }
    public static long expectedDuration(long durationMs,long startMs,long endMs){
        validate(startMs,endMs);
        if(durationMs<=0)return 0;
        if(startMs>=durationMs||(endMs>0&&endMs>durationMs+1000))
            throw new IllegalArgumentException("Trim di luar durasi video.");
        return (endMs>0?Math.min(endMs,durationMs):durationMs)-startMs;
    }
}
