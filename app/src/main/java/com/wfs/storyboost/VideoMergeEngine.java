package com.wfs.storyboost;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.Transformer;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Concatenate video clips in order using Media3 Composition. Not a crossfade editor. */
@SuppressWarnings("UnsafeOptInUsageError")
public final class VideoMergeEngine {
    public interface Listener {
        void onCompleted(File output);
        void onFailure(String message);
    }
    private Transformer transformer;
    private File intermediate;
    private boolean cancelled;
    public void start(Context context,List<Uri> videos,int quality,Listener listener){
        if(videos==null||videos.isEmpty()||videos.size()>10)
            throw new IllegalArgumentException("Pilih 1 sampai 10 video");
        cancelled=false;
        ArrayList<EditedMediaItem> items=new ArrayList<>();
        AspectRatioProfile.Size first;
        try{first=MediaDimensions.read(context,videos.get(0));}
        catch(Exception e){throw new IllegalArgumentException("Dimensi video pertama tidak tersedia",e);}
        int longest=quality==OutputProfile.HD?1280:
                quality==OutputProfile.FULL_HD?1920:3840;
        float factor=Math.min(1f,longest/(float)Math.max(first.width,first.height));
        int width=Math.max(2,(int)(Math.round(first.width*factor)/2)*2);
        int height=Math.max(2,(int)(Math.round(first.height*factor)/2)*2);
        for(Uri video:videos){
            try{
                if(!MediaProbe.inspect(context,video).video)
                    throw new IllegalArgumentException("Video tidak memiliki track yang valid");
            }catch(Exception ex){throw new IllegalArgumentException("Sumber tidak didukung: "+ex.getMessage(),ex);}
            items.add(new EditedMediaItem.Builder(MediaItem.fromUri(video)).build());
        }
        EditedMediaItemSequence sequence=EditedMediaItemSequence.withAudioAndVideoFrom(items);
        Composition composition=new Composition.Builder(sequence)
                .setEffects(new Effects(Collections.emptyList(),Collections.singletonList(
                        Presentation.createForWidthAndHeight(width,height,
                                Presentation.LAYOUT_SCALE_TO_FIT))))
                .build();
        intermediate=new File(context.getCacheDir(),"wfs_join_"+System.nanoTime()+".mp4");
        transformer=new Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(new Transformer.Listener(){
                    @Override public void onCompleted(Composition result,ExportResult export){
                        if(cancelled)return;
                        try{
                            if(!intermediate.isFile()||intermediate.length()<1024)
                                throw new java.io.IOException("Gabungan video kosong");
                            MediaProbe.Info info=MediaProbe.inspect(intermediate);
                            if(!info.video||info.durationMs<=0)
                                throw new java.io.IOException("Output video tidak terbaca");
                            listener.onCompleted(intermediate);
                        }catch(Exception ex){
                            clean();
                            listener.onFailure(ex.getMessage());
                        }
                    }
                    @Override public void onError(Composition result,ExportResult export,
                                                   ExportException ex){
                        if(cancelled)return;
                        String message=ex.getMessage()==null?
                                "Perangkat tidak mendukung komposisi ini":ex.getMessage();
                        clean();
                        listener.onFailure(message);
                    }
                }).build();
        transformer.start(composition,intermediate.getAbsolutePath());
    }
    public int progress(){
        if(transformer==null)return -1;
        ProgressHolder h=new ProgressHolder();
        return transformer.getProgress(h)==Transformer.PROGRESS_STATE_AVAILABLE?h.progress:-1;
    }
    public void cancel(){
        cancelled=true;
        if(transformer!=null)transformer.cancel();
        clean();
    }
    public void clean(){
        if(intermediate!=null&&intermediate.exists())intermediate.delete();
        intermediate=null;transformer=null;
    }
}
