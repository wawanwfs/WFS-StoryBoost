package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Actual DnCNN / TFLite neural inference, MIT-licensed model from Qualcomm/KAIR.
 * The model is trained for sigma=25 grayscale Gaussian noise. For RGB photos
 * only Y luminance is denoised; color channels are preserved via a common delta.
 * No generative content, no interpolation passed off as neural restoration.
 *
 * Sources exceeding a bounded amount of RAM/work must fail with an actionable
 * error instead of silently resampling and replacing the original resolution.
 */
public final class AiDenoiseEngine {
    public static final int TILE=256, OVERLAP=32, STEP=TILE-OVERLAP;
    private static final int MAX_TILES=120;
    private static final long MAX_PIXELS=2_600_000L;
    private static final String MODEL="models/dncnn_25.tflite";
    private AiDenoiseEngine(){}

    public static final class Stats{
        public final int tiles;
        public final long inferenceMs;
        public final long totalMs;
        Stats(int n,long inference,long total){
            tiles=n;inferenceMs=inference;totalMs=total;
        }
    }
    public static final class Result{
        public final Bitmap bitmap;
        public final Stats stats;
        Result(Bitmap b,Stats stats){bitmap=b;this.stats=stats;}
    }
    public static boolean included(Context context){
        try(InputStream input=context.getAssets().open(MODEL)){return input.read()>=0;}
        catch(IOException ignored){return false;}
    }

    private static ByteBuffer loadModel(Context context)throws IOException{
        try(InputStream input=context.getAssets().open(MODEL);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] block=new byte[65536];int n;
            while((n=input.read(block))!=-1){
                if(bytes.size()+n>10*1024*1024)throw new IOException("Model AI melebihi batas ukuran");
                bytes.write(block,0,n);
            }
            byte[] content=bytes.toByteArray();
            if(content.length<500000)throw new IOException("Model DnCNN tidak lengkap");
            ByteBuffer buffer=ByteBuffer.allocateDirect(content.length).order(ByteOrder.nativeOrder());
            buffer.put(content);buffer.rewind();
            return buffer;
        }
    }
    private static void validate(Interpreter tf)throws IOException {
        if(tf.getInputTensorCount()!=1||tf.getOutputTensorCount()!=1)
            throw new IOException("DnCNN harus memiliki satu input dan output");
        int[] input=tf.getInputTensor(0).shape(),out=tf.getOutputTensor(0).shape();
        if(tf.getInputTensor(0).dataType()!=DataType.FLOAT32||
                tf.getOutputTensor(0).dataType()!=DataType.FLOAT32||
                !isGrayTile(input)||!isGrayTile(out))
            throw new IOException("Model DnCNN tidak cocok dengan tensor float32 256x256 grayscale: "+
                    Arrays.toString(input)+" / "+Arrays.toString(out));
    }
    private static boolean isGrayTile(int[] dimensions){
        return dimensions.length==4&&dimensions[0]==1&&
                ((dimensions[1]==256&&dimensions[2]==256&&dimensions[3]==1)||
                 (dimensions[1]==1&&dimensions[2]==256&&dimensions[3]==256));
    }
    private static int clamp(int x){return Math.min(255,Math.max(0,x));}
    private static int sample(int x,int bound){
        return Math.min(bound-1,Math.max(0,x));
    }
    private static float y(int rgb){
        return (0.299f*Color.red(rgb)+.587f*Color.green(rgb)+.114f*Color.blue(rgb))/255f;
    }
    private static float edgeWeight(int p){
        // Blends 32-pixel overlap across neighboring neural tiles.
        return .05f+.95f*Math.min(1f,Math.min(p,TILE-1-p)/30f);
    }
    private static int[] starts(int length){
        if(length<=TILE)return new int[]{0};
        int count=1+(length-TILE+STEP-1)/STEP;
        int[] starts=new int[count];
        for(int i=0;i<count;i++)starts[i]=Math.min(i*STEP,length-TILE);
        return starts;
    }

    public static Result process(Context context,Bitmap source)throws IOException {
        return process(context,source,Math.max(1,Math.min(3,
                Runtime.getRuntime().availableProcessors())));
    }

    /** Exact CPU thread-count override for controlled QA comparisons.
     * Normal user flow still uses the original automatic 1..3 thread policy.
     */
    static Result process(Context context,Bitmap source,int cpuThreads)throws IOException {
        if(cpuThreads<1||cpuThreads>3)
            throw new IllegalArgumentException("DnCNN CPU threads must be 1..3");
        if(source==null||source.isRecycled())throw new IOException("Foto AI tidak tersedia");
        int width=source.getWidth(),height=source.getHeight();
        long pixels=(long)width*height;
        if(pixels>MAX_PIXELS)
            throw new IOException("AI Denoise maksimal 2,6 MP. Pilih foto lebih kecil dahulu; tidak akan di-upscale diam-diam.");
        int[] xs=starts(width),ys=starts(height);
        int tiles=xs.length*ys.length;
        if(tiles>MAX_TILES)throw new IOException("Jumlah tile AI melebihi kapasitas HP");
        long startTime=android.os.SystemClock.elapsedRealtime(),inference=0;
        try{
            int[] src=new int[(int)pixels];
            source.getPixels(src,0,width,0,0,width,height);
            float[] sum=new float[(int)pixels],weight=new float[(int)pixels];
            ByteBuffer model=loadModel(context);
            Interpreter.Options opts=new Interpreter.Options();
            opts.setNumThreads(cpuThreads);
            try(Interpreter tf=new Interpreter(model,opts)){
                validate(tf);
                tf.allocateTensors();
                ByteBuffer input=ByteBuffer.allocateDirect(TILE*TILE*4).order(ByteOrder.nativeOrder());
                ByteBuffer output=ByteBuffer.allocateDirect(TILE*TILE*4).order(ByteOrder.nativeOrder());
                for(int oy:ys)for(int ox:xs){
                    if(Thread.currentThread().isInterrupted())throw new IOException("AI Denoise dibatalkan");
                    input.clear();
                    for(int ty=0;ty<TILE;ty++){
                        int sy=sample(oy+ty,height);
                        for(int tx=0;tx<TILE;tx++){
                            int sx=sample(ox+tx,width);
                            input.putFloat(y(src[sy*width+sx]));
                        }
                    }
                    input.rewind();output.clear();
                    long invokeStart=android.os.SystemClock.elapsedRealtime();
                    tf.run(input,output);
                    inference+=android.os.SystemClock.elapsedRealtime()-invokeStart;
                    output.rewind();
                    for(int ty=0;ty<TILE;ty++){
                        int sy=oy+ty;
                        for(int tx=0;tx<TILE;tx++){
                            float modelY=output.getFloat();
                            int sx=ox+tx;
                            if(sx>=width||sy>=height)continue;
                            if(!Float.isFinite(modelY))throw new IOException("Model AI mengembalikan nilai tidak valid");
                            float newY=Math.min(1f,Math.max(0f,modelY));
                            float w=edgeWeight(tx)*edgeWeight(ty);
                            int idx=sy*width+sx;
                            sum[idx]+=newY*w;weight[idx]+=w;
                        }
                    }
                }
            }
            int[] rendered=new int[(int)pixels];
            for(int i=0;i<rendered.length;i++){
                if(weight[i]<=0)throw new IOException("Hasil tile AI tidak lengkap");
                int old=src[i];
                float originalY=y(old);
                float enhancedY=sum[i]/weight[i];
                // Guard highlights & identity: cap luminance shift and retain chroma.
                int delta=Math.round(Math.max(-.10f,Math.min(.10f,enhancedY-originalY))*255f);
                rendered[i]=Color.argb(Color.alpha(old),
                        clamp(Color.red(old)+delta),clamp(Color.green(old)+delta),
                        clamp(Color.blue(old)+delta));
            }
            Bitmap bitmap=Bitmap.createBitmap(rendered,width,height,Bitmap.Config.ARGB_8888);
            return new Result(bitmap,new Stats(tiles,inference,
                    android.os.SystemClock.elapsedRealtime()-startTime));
        }catch(IllegalArgumentException|IllegalStateException e){
            throw new IOException("Model AI gagal menjalankan inference: "+e.getMessage(),e);
        }catch(OutOfMemoryError oom){
            throw new IOException("RAM tidak cukup untuk AI Denoise; gunakan gambar lebih kecil",oom);
        }
    }
}
