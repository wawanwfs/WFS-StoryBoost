package com.wfs.storyboost;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.effect.Contrast;
import androidx.media3.effect.HslAdjustment;
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

    /** Call on main thread; returned dimensions are the requested encoded canvas. */
    public AspectRatioProfile.Size start(Context context, Uri source,
                                          ExportOptions settings, Listener callback) {
        AspectRatioProfile.Size sourceSize = readVideoDimensions(context, source);
        final AspectRatioProfile.Size target = AspectRatioProfile.resolve(settings.ratio,
                settings.fullHd, sourceSize.width, sourceSize.height);
        temp = new File(context.getCacheDir(), "storyboost_" + System.nanoTime() + ".mp4");
        List<Effect> effects = new ArrayList<>();
        if (settings.preset > 0) {
            effects.add(new Contrast(settings.preset == 1 ? 0.065f : 0.09f));
            effects.add(new HslAdjustment.Builder()
                    .adjustSaturation(settings.preset == 1 ? 8 : 13).build());
            effects.add(new RgbAdjustment.Builder()
                    .setRedScale(1.025f).setGreenScale(1.008f)
                    .setBlueScale(settings.preset == 1 ? 0.99f : 1.015f).build());
        } else {
            effects.add(new Contrast(0.025f));
            effects.add(new HslAdjustment.Builder().adjustSaturation(4).build());
        }
        // BLUR is photo-only in v1.3: for video, preserve the entire original frame
        // with letter/pillarboxing rather than silently cropping it.
        boolean crop = settings.frameStyle == AspectRatioProfile.CROP;
        effects.add(Presentation.createForWidthAndHeight(target.width, target.height,
                crop ? Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                        : Presentation.LAYOUT_SCALE_TO_FIT));
        EditedMediaItem item = new EditedMediaItem.Builder(MediaItem.fromUri(source))
                .setEffects(new Effects(Collections.emptyList(), effects)).build();
        int bitrate = VideoBitrate.estimate(target.width, target.height, settings.fullHd);
        VideoEncoderSettings requested = new VideoEncoderSettings.Builder()
                .setBitrate(bitrate).setiFrameIntervalSeconds(2f).build();
        transformer = new Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(new DefaultEncoderFactory.Builder(context)
                        .setRequestedVideoEncoderSettings(requested).build())
                .addListener(new Transformer.Listener() {
                    @Override public void onCompleted(Composition composition, ExportResult result) {
                        callback.onSuccess(temp, target);
                    }
                    @Override public void onError(Composition composition, ExportResult result,
                                                   ExportException e) {
                        callback.onError(e.getMessage() == null
                                ? "Video gagal diekspor." : e.getMessage());
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
        if (transformer != null) transformer.cancel();
        cleanup();
    }
}
