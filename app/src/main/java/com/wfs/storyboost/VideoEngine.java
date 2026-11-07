package com.wfs.storyboost;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.effect.Contrast;
import androidx.media3.effect.HslAdjustment;
import androidx.media3.effect.GaussianBlurWithFrameOverlaid;
import androidx.media3.effect.Crop;
import androidx.media3.effect.Presentation;
import androidx.media3.effect.RgbAdjustment;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.VideoEncoderSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Media3-based H.264/AAC video export; exports remain device-codec dependent. */
@SuppressWarnings("UnsafeOptInUsageError")
public final class VideoEngine {
    public interface Listener {
        void onSuccess(File file, AspectRatioProfile.Size dimensions);
        void onError(String message);
    }

    private Transformer transformer;
    private File temp;
    private boolean cancelled;

    /** Call on main thread; returned dimensions are the requested encoded canvas. */
    public AspectRatioProfile.Size start(Context context, Uri source,
                                          ExportOptions settings, Listener callback) {
        cancelled=false;
        return startInternal(context,source,settings,callback,true);
    }
    private AspectRatioProfile.Size startInternal(Context context,Uri source,
            ExportOptions settings,Listener callback,boolean allowFallback){
        AspectRatioProfile.Size sourceSize=readVideoDimensions(context,source);
        final MediaProbe.Info sourceInfo;
        try{
            sourceInfo=MediaProbe.inspect(context,source);
            if(!sourceInfo.video)throw new IllegalArgumentException("File tidak berisi video");
        }catch(Exception ex){throw new IllegalArgumentException("Media tidak didukung: "+ex.getMessage(),ex);}
        long expectedDuration=ClipWindow.expectedDuration(MediaProbe.duration(context,source),
                settings.trimStartMs,settings.trimEndMs);
        final AspectRatioProfile.Size target = AspectRatioProfile.resolve(settings.ratio,
                settings.qualityMode, sourceSize.width, sourceSize.height, settings.frameStyle);
        temp = new File(context.getCacheDir(), "storyboost_" + System.nanoTime() + ".mp4");
        List<Effect> effects = new ArrayList<>();
        if (settings.preset > 0) {
            effects.add(new Contrast(settings.preset == 3 ? 0.015f
                    : settings.preset == 1 ? 0.065f : 0.09f));
            effects.add(new HslAdjustment.Builder()
                    .adjustSaturation(settings.preset == 3 ? 5
                            : settings.preset == 1 ? 8 : 13).build());
            effects.add(new RgbAdjustment.Builder()
                    .setRedScale(settings.preset == 3 ? 1.035f : 1.025f)
                    .setGreenScale(settings.preset == 3 ? 1.035f : 1.008f)
                    .setBlueScale(settings.preset == 1 ? 0.99f : 1.015f).build());
        } else {
            effects.add(new Contrast(0.025f));
            effects.add(new HslAdjustment.Builder().adjustSaturation(4).build());
        }
        boolean crop=settings.frameStyle==AspectRatioProfile.CROP;
        boolean blur=settings.frameStyle==AspectRatioProfile.BLUR;
        float scale=VideoFramePlan.sharpScale(sourceSize.width,sourceSize.height,
                target.width,target.height);
        if(blur&&scale<0.999f)effects.add(new GaussianBlurWithFrameOverlaid(20f,scale,scale));
        if(crop){
            CropFocus.Bounds region=CropFocus.region(sourceSize.width,sourceSize.height,
                    target.width,target.height,settings.focusX,settings.focusY);
            effects.add(new Crop(region.left,region.right,region.bottom,region.top));
        }
        effects.add(Presentation.createForWidthAndHeight(target.width,target.height,
                crop ? Presentation.LAYOUT_SCALE_TO_FIT :
                (blur&&scale<0.999f)
                    ? Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                    : Presentation.LAYOUT_SCALE_TO_FIT));
        MediaItem.ClippingConfiguration.Builder clip=new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(settings.trimStartMs);
        if(settings.trimEndMs>0)clip.setEndPositionMs(settings.trimEndMs);
        MediaItem mediaItem=new MediaItem.Builder().setUri(source)
                .setClippingConfiguration(clip.build()).build();
        EditedMediaItem item = new EditedMediaItem.Builder(mediaItem)
                .setEffects(new Effects(Collections.emptyList(), effects)).build();
        float frameRate = sourceInfo.fps;
        int bitrate = VideoBitrate.estimate(target.width, target.height, settings.qualityMode, frameRate);
        VideoEncoderSettings requested = new VideoEncoderSettings.Builder()
                .setBitrate(bitrate).setiFrameIntervalSeconds(2f).build();
        transformer = new Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(new DefaultEncoderFactory.Builder(context)
                        .setRequestedVideoEncoderSettings(requested).build())
                .addListener(new Transformer.Listener() {
                    @Override public void onCompleted(Composition composition, ExportResult result) {
                        try {
                            validateOutput(temp,target,sourceInfo,expectedDuration);
                            if(!cancelled)callback.onSuccess(temp,target);
                        } catch (Exception invalid) {
                            if(!cancelled)callback.onError("Validasi MP4 gagal: "+invalid.getMessage());
                        }
                    }
                    @Override public void onError(Composition composition, ExportResult result,
                                                   ExportException e) {
                        if(cancelled)return;
                        if(allowFallback&&settings.qualityMode==OutputProfile.FULL_HD){
                            if(temp!=null)temp.delete();
                            try{startInternal(context,source,settings.inHd(),callback,false);return;}
                            catch(Exception problem){callback.onError("FHD/HD gagal: "+problem.getMessage());return;}
                        }
                        callback.onError(e.getMessage()==null?"Video gagal diekspor.":e.getMessage());
                    }
                })
                .build();
        transformer.start(item, temp.getAbsolutePath());
        return target;
    }

    private static AspectRatioProfile.Size readVideoDimensions(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            int width = Integer.parseInt(w);
            int height = Integer.parseInt(h);
            int rotation = rot == null ? 0 : Integer.parseInt(rot);
            if (rotation % 180 != 0) return new AspectRatioProfile.Size(height, width);
            return new AspectRatioProfile.Size(width, height);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Tidak bisa membaca dimensi video: " + ex.getMessage(), ex);
        } finally {
            try { retriever.release(); }
            catch (java.io.IOException ignored) { /* Provider cleanup failure. */ }
        }
    }

    /** Reject empty/truncated media before it reaches the public Gallery. */
    private static void validateOutput(File file, AspectRatioProfile.Size target,MediaProbe.Info sourceInfo,long expectedDuration) throws Exception {
        if (!file.isFile() || file.length() < 1024) {
            throw new java.io.IOException("File video kosong atau terpotong.");
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (w == null || h == null) throw new java.io.IOException("Track video tidak ditemukan.");
            int width = Integer.parseInt(w), height = Integer.parseInt(h);
            String rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (rot != null && Integer.parseInt(rot) % 180 != 0) {
                int swap = width; width = height; height = swap;
            }
            if (width != target.width || height != target.height) {
                throw new java.io.IOException("Resolusi aktual " + width + "×" + height
                        + " berbeda dari target " + target + ".");
            }
            String duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION);
            if(duration==null||Long.parseLong(duration)<=0)throw new java.io.IOException("Durasi MP4 tidak terbaca.");
            MediaProbe.verifyTracks(sourceInfo,MediaProbe.inspect(file),expectedDuration);
        } finally {
            try { retriever.release(); } catch (Exception ignored) { }
        }
    }

    public int progress() {
        if (transformer == null) return -1;
        ProgressHolder holder = new ProgressHolder();
        return transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE
                ? holder.progress : -1;
    }

    public void cleanup() {
        if (temp != null && temp.exists()) temp.delete();
        temp = null;
        transformer = null;
    }

    public void cancel() {
        cancelled=true;
        if (transformer != null) transformer.cancel();
        cleanup();
    }
}
