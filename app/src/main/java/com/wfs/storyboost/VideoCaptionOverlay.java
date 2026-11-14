package com.wfs.storyboost;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.SpannableString;

import androidx.media3.common.OverlaySettings;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.Size;
import androidx.media3.effect.TextOverlay;
import androidx.media3.effect.StaticOverlaySettings;

/** Rasterizes only the caption strip, after framing, in Media3's actual display coordinates. */
@SuppressWarnings("UnsafeOptInUsageError")
final class VideoCaptionOverlay extends TextOverlay {
    private final ExportOptions options;
    private final SpannableString text;
    private Bitmap strip;
    private OverlaySettings settings;

    VideoCaptionOverlay(ExportOptions options){
        this.options=options;
        text=new SpannableString(options.caption);
    }

    // Preserve Media3's HDR text treatment. A generic bitmap overlay
    // requires a gain map; this is caption text, not an Ultra HDR image.
    @Override public SpannableString getText(long presentationTimeUs){return text;}

    @Override public void configure(Size videoSize) {
        super.configure(videoSize);
        int width=videoSize.getWidth(),height=videoSize.getHeight();
        // At most two lines, each at most 4% of the frame height.
        int stripHeight=Math.max(2,(int)Math.ceil(height*.20));
        int top=Math.round(height*SocialExportProfile.captionCenterY(options.socialProfile)-stripHeight/2f);
        if(strip!=null)strip.recycle();
        strip=Bitmap.createBitmap(width,stripHeight,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(strip);
        canvas.translate(0,-top);
        new CaptionRenderer().draw(canvas,options.caption,options.captionColor,options.socialProfile,width,height);
        // Android counts from top to bottom; Media3 anchors run from +1 to -1.
        settings=new StaticOverlaySettings.Builder()
                .setBackgroundFrameAnchor(0f,1f-2f*(top+stripHeight/2f)/height)
                .setOverlayFrameAnchor(0f,0f)
                .build();
    }

    @Override public Bitmap getBitmap(long presentationTimeUs){return strip;}

    @Override public OverlaySettings getOverlaySettings(long presentationTimeUs){return settings;}

    @Override public void release() throws VideoFrameProcessingException {
        try{super.release();}
        finally{if(strip!=null){strip.recycle();strip=null;}}
    }
}
