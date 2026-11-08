package com.wfs.storyboost;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.AbsoluteSizeSpan;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.effect.Contrast;
import androidx.media3.effect.TextOverlay;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.effect.StaticOverlaySettings;
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
                settings.qualityMode, sourceSize.width, sourceSize.height, settings.frameStyle,
                settings.customWidth,settings.customHeight);
        temp = new File(context.getCacheDir(), "storyboost_" + System.nanoTime() + ".mp4");
        List<Effect> effects = new ArrayList<>();
        ColorLooks.Look look=ColorLooks.get(settings.preset);
        if(!ColorLooks.isOriginal(settings.preset)){
            float c=Math.max(-.5f,Math.min(.5f,look.contrast-1f));
            if(Math.abs(c)>.001f)effects.add(new Contrast(c));
            float sat=(look.saturation-1f)*65f;
            if(Math.abs(sat)>.1f)effects.add(
                    new HslAdjustment.Builder().adjustSaturation(sat).build());
            float lift=look.lift/255f;
            float r=Math.max(.6f,Math.min(1.5f,look.red+lift));
            float g=Math.max(.6f,Math.min(1.5f,look.green+lift));
            float b=Math.max(.6f,Math.min(1.5f,look.blue+lift));
            effects.add(new RgbAdjustment.Builder()
                    .setRedScale(r).setGreenScale(g).setBlueScale(b).build());
        }
        if(settings.hasColorAdjustments()){
            if(settings.contrast!=0f)effects.add(new Contrast(settings.contrast*.25f));
            if(settings.saturation!=0f)effects.add(new HslAdjustment.Builder()
                    .adjustSaturation(settings.saturation*35f).build());
            if(settings.brightness!=0f){
                float scale=Math.max(.7f,Math.min(1.3f,1f+settings.brightness*.25f));
                effects.add(new RgbAdjustment.Builder().setRedScale(scale)
                        .setGreenScale(scale).setBlueScale(scale).build());
            }
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
        if(settings.hasText()){
            SpannableString text=new SpannableString(settings.caption.trim());
            text.setSpan(new ForegroundColorSpan(settings.captionColor),0,text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new StyleSpan(Typeface.BOLD),0,text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new AbsoluteSizeSpan(100),0,text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            StaticOverlaySettings titlePosition=new StaticOverlaySettings.Builder()
                    .setBackgroundFrameAnchor(0f,settings.socialProfile==SocialExportProfile.CUSTOM
                            ? -.72f : 0f)
                    .setOverlayFrameAnchor(0f,0f)
                    .setScale(.72f,.72f).build();
            effects.add(new OverlayEffect(Collections.singletonList(
                    TextOverlay.createStaticTextOverlay(text,titlePosition))));
        }
        MediaItem.ClippingConfiguration.Builder clip=new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(settings.trimStartMs);
        if(settings.trimEndMs>0)clip.setEndPositionMs(settings.trimEndMs);
        MediaItem mediaItem=new MediaItem.Builder().setUri(source)
                .setClippingConfiguration(clip.build()).build();
        EditedMediaItem item = new EditedMediaItem.Builder(mediaItem)
                .setEffects(new Effects(Collections.emptyList(), effects))
                .setRemoveAudio(settings.muteAudio).build();
        float frameRate = sourceInfo.fps;
        int bitrate = SocialExportProfile.cappedBitrate(settings.socialProfile,
                target.width,target.height,settings.qualityMode,frameRate);
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
                            validateOutput(temp,target,sourceInfo,expectedDuration,settings.muteAudio);
                            if(!cancelled)callback.onSuccess(temp,target);
                        } catch (Exception invalid) {
                            // A failed validation must never leak a partially encoded MP4.
                            cleanup();
                            if(!cancelled)callback.onError("Validasi MP4 gagal: "+invalid.getMessage());
                        }
                    }
                    @Override public void onError(Composition composition, ExportResult result,
                                                   ExportException e) {
                        if(cancelled)return;
                        if(allowFallback&&settings.qualityMode==OutputProfile.FULL_HD){
                            if(temp!=null)temp.delete();
                            try{startInternal(context,source,settings.inHd(),callback,false);return;}
                            catch(Exception problem){
                                cleanup();
                                callback.onError("FHD/HD gagal: "+problem.getMessage());return;
                            }
                        }
                        String reason=e.getMessage()==null?"Video gagal diekspor.":e.getMessage();
                        cleanup();
                        callback.onError(reason);
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
    private static void validateOutput(File file, AspectRatioProfile.Size target,MediaProbe.Info sourceInfo,long expectedDuration,boolean muteAudio) throws Exception {
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
            MediaProbe.Info exported=MediaProbe.inspect(file);
            if(muteAudio){
                if(exported.audio)throw new java.io.IOException("Audio masih ada meskipun muted");
                MediaProbe.Info expectedSilent=new MediaProbe.Info(sourceInfo.video,false,
                        sourceInfo.fps,sourceInfo.durationMs,0,sourceInfo.videoDurationMs);
                MediaProbe.verifyTracks(expectedSilent,exported,expectedDuration);
            }else MediaProbe.verifyTracks(sourceInfo,exported,expectedDuration);
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
