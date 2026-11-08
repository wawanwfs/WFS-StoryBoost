package com.wfs.storyboost;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Matrix;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/**
 * Genuine on-device ML segmentation for still photographs. Keeps subject pixels
 * from the ORIGINAL bitmap; the model contributes only an alpha mask.
 * No face reconstruction, skin smoothing, or fake upscaling.
 */
public final class AiPortraitCompositor {
    private AiPortraitCompositor(){}

    /**
     * Maps model confidence into a soft alpha. Portable Java policy,
     * covered by test-ratio-geometry.sh.
     */
    public static int alpha(float confidence){
        return PortraitMaskMath.alpha(confidence);
    }

    /** Caller owns bitmap. Must be invoked on a background worker, never UI thread. */
    private static Bitmap maskFor(Bitmap source) throws IOException {
        final int bound=768;
        final float scale=Math.min(1f,(float)bound/Math.max(source.getWidth(),source.getHeight()));
        final Bitmap analysis=scale==1f?source:Bitmap.createScaledBitmap(source,
                Math.max(2,Math.round(source.getWidth()*scale)),
                Math.max(2,Math.round(source.getHeight()*scale)),true);
        Segmenter segmenter=null;
        try {
            segmenter=Segmentation.getClient(new SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .build());
            SegmentationMask segmentation=Tasks.await(segmenter.process(
                    InputImage.fromBitmap(analysis,0)),25,TimeUnit.SECONDS);
            int mw=segmentation.getWidth(),mh=segmentation.getHeight();
            if(mw<2||mh<2||(long)mw*mh>2_000_000L)
                throw new IOException("Ukuran mask AI tidak valid");
            ByteBuffer buffer=segmentation.getBuffer().duplicate().order(ByteOrder.nativeOrder());
            buffer.rewind();
            if(buffer.remaining()<(long)mw*mh*4L)
                throw new IOException("Data mask AI tidak lengkap");
            int[] pixels=new int[mw*mh];int confident=0;
            for(int i=0;i<pixels.length;i++){
                int a=alpha(buffer.getFloat());
                pixels[i]=(a<<24)|0x00FFFFFF;
                if(a>=110)confident++;
            }
            float coverage=(float)confident/pixels.length;
            if(!PortraitMaskMath.usable(coverage))
                throw new IOException(coverage<.015f
                        ?"Subjek manusia tidak terdeteksi untuk Portrait Bokeh":
                        "AI tidak dapat memisahkan latar dan subjek pada foto ini");
            Bitmap mask=Bitmap.createBitmap(mw,mh,Bitmap.Config.ARGB_8888);
            mask.setPixels(pixels,0,mw,0,0,mw,mh);
            return mask;
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IOException("Analisis portrait terhenti",e);
        }catch(java.util.concurrent.TimeoutException e){
            throw new IOException("Model AI terlalu lama merespons",e);
        }catch(java.util.concurrent.ExecutionException e){
            throw new IOException("Model segmentasi gagal: "+e.getCause(),e);
        }finally{
            if(segmenter!=null)segmenter.close();
            if(analysis!=source)analysis.recycle();
        }
    }

    /**
     * Renders a low-frequency background and restores the unmodified person
     * through the ML-generated alpha, matching the same fit/crop focus bounds.
     */
    public static void draw(Canvas canvas,Bitmap original,int width,int height,
                             FrameMath.Bounds bounds,Paint foregroundPaint)throws IOException{
        Bitmap mask=maskFor(original);
        try{
            PhotoEngine.drawBlurredBackground(canvas,original,width,height);
            RectF destination=new RectF(bounds.left,bounds.top,bounds.right,bounds.bottom);
            int save=canvas.saveLayer(0f,0f,(float)width,(float)height,null);
            try{
                canvas.drawBitmap(original,null,destination,foregroundPaint);
                Paint alphaPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
                alphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                canvas.drawBitmap(mask,null,destination,alphaPaint);
                alphaPaint.setXfermode(null);
            }finally{canvas.restoreToCount(save);}
        }finally{mask.recycle();}
    }
}
