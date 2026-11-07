package com.wfs.storyboost;
import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
/** Inspects video/audio tracks and frame-rate metadata. */
public final class MediaProbe {
    private MediaProbe(){}
    public static final class Info {
        public final boolean video,audio;
        public final float fps;
        public final long durationMs,audioDurationMs,videoDurationMs;
        Info(boolean v,boolean a,float fps,long d,long ad,long vd){
            video=v;audio=a;this.fps=fps;durationMs=d;audioDurationMs=ad;videoDurationMs=vd;
        }
    }
    public static Info inspect(Context c,Uri u)throws IOException{
        MediaExtractor e=new MediaExtractor();
        try{e.setDataSource(c,u,null);return read(e);}finally{e.release();}
    }
    public static Info inspect(File f)throws IOException{
        MediaExtractor e=new MediaExtractor();
        try{e.setDataSource(f.getAbsolutePath());return read(e);}finally{e.release();}
    }
    private static Info read(MediaExtractor e){
        boolean v=false,a=false;float fps=0f;long vd=0,ad=0;
        for(int i=0;i<e.getTrackCount();i++){
            MediaFormat f=e.getTrackFormat(i);
            if(!f.containsKey(MediaFormat.KEY_MIME))continue;
            String mime=f.getString(MediaFormat.KEY_MIME);
            long d=f.containsKey(MediaFormat.KEY_DURATION)?f.getLong(MediaFormat.KEY_DURATION):0;
            if(mime!=null&&mime.startsWith("video/")){
                v=true;vd=Math.max(vd,d);
                if(f.containsKey(MediaFormat.KEY_FRAME_RATE)){
                    try{fps=f.getNumber(MediaFormat.KEY_FRAME_RATE).floatValue();}
                    catch(Exception ignored){}
                }
            }else if(mime!=null&&mime.startsWith("audio/")){a=true;ad=Math.max(ad,d);}
        }
        return new Info(v,a,fps>0f&&fps<=240f?fps:30f,
            Math.max(vd,ad)/1000,ad/1000,vd/1000);
    }
    public static long duration(Context c,Uri u){
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{
            r.setDataSource(c,u);
            String d=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return d==null?0:Long.parseLong(d);
        }catch(Exception ex){return 0;}
        finally{try{r.release();}catch(Exception ignored){}}
    }
    public static void verifyTracks(Info source,Info output,long wantedMs)throws IOException{
        if(!output.video)throw new IOException("Track video hilang");
        if(source.audio&&!output.audio)throw new IOException("Audio hasil hilang");
        if(wantedMs>0&&output.durationMs>0&&Math.abs(output.durationMs-wantedMs)>Math.max(1200,wantedMs/10))
            throw new IOException("Durasi video meleset jauh");
        if(output.audio&&output.audioDurationMs>0&&output.videoDurationMs>0&&
           Math.abs(output.audioDurationMs-output.videoDurationMs)>Math.max(1500,output.videoDurationMs/10))
            throw new IOException("Durasi audio dan video tidak cocok");
    }
}
