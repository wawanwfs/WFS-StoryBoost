package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Debug;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Random;

/**
 * Deterministic, fully synthetic, paired clean/noisy color fixture with 4+
 * neural tiles. No human subject; not a portrait-bokeh accuracy benchmark.
 * Real Qualcomm/KAIR DnCNN-25 weights are loaded by AiDenoiseEngine in CI.
 * Records rather than exaggerates chroma-noise quality or memory results.
 */
@RunWith(AndroidJUnit4.class)
public final class DncnnTilingQaTest {
    private static final String TAG="WfsDncnnTileQA";
    private static final int W=480,H=480;
    private static int clamp(int v){return Math.max(0,Math.min(255,v));}
    private static int gray(int rgb){
        return Math.round(.299f*Color.red(rgb)+.587f*Color.green(rgb)+.114f*Color.blue(rgb));
    }
    private static double boundaryDifference(int[] pixels,int x){
        double total=0;
        for(int y=0;y<H;y++)
            total+=Math.abs(gray(pixels[y*W+x])-gray(pixels[y*W+x-1]));
        return total/H;
    }
    private static String memoryDetail(Debug.MemoryInfo memory){
        return " pssKB="+memory.getTotalPss()+
                " javaKB="+memory.getMemoryStat("summary.java-heap")+
                " nativeKB="+memory.getMemoryStat("summary.native-heap")+
                " graphicsKB="+memory.getMemoryStat("summary.graphics")+
                " codeKB="+memory.getMemoryStat("summary.code")+
                " stackKB="+memory.getMemoryStat("summary.stack");
    }
    private static int linuxThreads(){
        try(java.io.BufferedReader reader=new java.io.BufferedReader(
                new java.io.FileReader("/proc/self/status"))){
            String line;
            while((line=reader.readLine())!=null){
                if(line.startsWith("Threads:"))return Integer.parseInt(
                        line.substring("Threads:".length()).trim());
            }
        }catch(Exception ignored){}
        return -1;
    }
    @Test(timeout=160000)
    public void colorNoiseTiledDenoiseReportsPairedQualityAndSeamContinuity() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Pinned DnCNN TFLite weights missing",AiDenoiseEngine.included(context));
        final int[] clean=new int[W*H],noisy=new int[W*H];
        Random random=new Random(3031141L);
        for(int y=0;y<H;y++)for(int x=0;x<W;x++){
            // Spatially smooth luminance and non-grey chroma, no identifiable face.
            int r=94+x/12+y/40,g=102+x/24+y/18,b=91+x/35+y/15;
            int k=y*W+x;
            clean[k]=Color.argb(255,r,g,b);
            int shared=(int)Math.round(random.nextGaussian()*24.5);
            int chroma=(int)Math.round(random.nextGaussian()*10);
            noisy[k]=Color.argb(255,clamp(r+shared+chroma),
                    clamp(g+shared-chroma),clamp(b+shared));
        }
        Bitmap input=Bitmap.createBitmap(noisy,W,H,Bitmap.Config.ARGB_8888);
        String configured=InstrumentationRegistry.getArguments()
                .getString("wfs.ai.threads");
        int cpuThreads=configured==null?Math.max(1,Math.min(3,
                Runtime.getRuntime().availableProcessors())):
                Integer.parseInt(configured);
        assertTrue(cpuThreads>=1&&cpuThreads<=3);
        String xnnpackArg=InstrumentationRegistry.getArguments()
                .getString("wfs.ai.xnnpack","default");
        assertTrue("Unknown XNNPACK QA mode: "+xnnpackArg,
                "default".equals(xnnpackArg)||"on".equals(xnnpackArg)||"off".equals(xnnpackArg));
        Boolean xnnpackOverride="default".equals(xnnpackArg)?null:
                Boolean.valueOf("on".equals(xnnpackArg));
        Bitmap processed=null;
        try {
            Debug.MemoryInfo beforeMemory=new Debug.MemoryInfo();
            Debug.getMemoryInfo(beforeMemory);
            int threadsBefore=linuxThreads();
            long nativeBytesBefore=Debug.getNativeHeapAllocatedSize();
            // In-process sampling avoids ADB/dumpsys polling latency: on
            // Android 10 a complete four-tile inference can take only 4 seconds.
            java.util.concurrent.atomic.AtomicBoolean sampleRunning=
                    new java.util.concurrent.atomic.AtomicBoolean(true);
            java.util.concurrent.atomic.AtomicInteger liveSamples=
                    new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicLong livePeakPssKb=
                    new java.util.concurrent.atomic.AtomicLong();
            java.util.concurrent.atomic.AtomicReference<String> livePeakBreakdown=
                    new java.util.concurrent.atomic.AtomicReference<>("");
            Thread sampler=new Thread(()->{
                while(sampleRunning.get()){
                    try{
                        Debug.MemoryInfo snapshot=new Debug.MemoryInfo();
                        Debug.getMemoryInfo(snapshot);
                        long value=snapshot.getTotalPss();
                        liveSamples.incrementAndGet();
                        if(value>livePeakPssKb.get()){
                            livePeakPssKb.set(value);
                            livePeakBreakdown.set(memoryDetail(snapshot));
                        }
                        Thread.sleep(150);
                    }catch(InterruptedException stop){
                        Thread.currentThread().interrupt();
                        break;
                    }catch(RuntimeException cannotSample){
                        Log.w(TAG,"Memory sample error: "+cannotSample.getMessage());
                    }
                }
            },"WfsDncnnMemorySampler");
            sampler.setDaemon(true);
            sampler.start();
            long start=android.os.SystemClock.elapsedRealtime();
            AiDenoiseEngine.Result result;
            try {
                // "default" now exercises the actual consumer entrypoint;
                // explicit "on" reproduces legacy pre-beta6 memory behavior.
                result=xnnpackOverride==null
                        ?AiDenoiseEngine.process(context,input,cpuThreads)
                        :AiDenoiseEngine.process(context,input,cpuThreads,xnnpackOverride);
            }finally{
                sampleRunning.set(false);
                sampler.interrupt();
                sampler.join(2000);
            }
            processed=result.bitmap;
            long elapsed=android.os.SystemClock.elapsedRealtime()-start;
            assertTrue("No live in-process PSS samples",liveSamples.get()>0);
            assertTrue("Invalid sampled peak PSS",livePeakPssKb.get()>0);
            assertNotNull(processed);
            assertEquals(W,processed.getWidth());
            assertEquals(H,processed.getHeight());
            assertTrue("Expected overlapping 256px tiles",result.stats.tiles>=4);
            assertTrue(result.stats.inferenceMs>=0);
            assertTrue(result.stats.totalMs>=result.stats.inferenceMs);

            int[] out=new int[W*H],a=new int[W*H],n=new int[W*H],o=new int[W*H];
            processed.getPixels(out,0,W,0,0,W,H);
            long chromaChanged=0;
            for(int k=0;k<out.length;k++){
                a[k]=gray(clean[k]);n[k]=gray(noisy[k]);o[k]=gray(out[k]);
                assertEquals("Alpha must remain unchanged",Color.alpha(noisy[k]),Color.alpha(out[k]));
                int oldRG=Color.red(noisy[k])-Color.green(noisy[k]);
                int newRG=Color.red(out[k])-Color.green(out[k]);
                if(Math.abs(oldRG-newRG)>2)chromaChanged++;
            }
            double beforePsnr=ImageQualityMetrics.psnr(a,n);
            double afterPsnr=ImageQualityMetrics.psnr(a,o);
            double beforeSsim=ImageQualityMetrics.ssim(a,n);
            double afterSsim=ImageQualityMetrics.ssim(a,o);
            // The clean/noisy pair is an actual pixel-aligned ground truth, so
            // PSNR/SSIM are justified here, but not for unpaired real photos.
            assertTrue(Double.isFinite(beforePsnr));
            assertTrue(Double.isFinite(afterPsnr));
            assertTrue(Double.isFinite(beforeSsim));
            assertTrue(Double.isFinite(afterSsim));
            assertEquals("Bounded luminance correction must keep RGB channel differences",
                    0,chromaChanged);
            // Prior validated paired synthetic reference ~28.80dB / .745 SSIM.
            // Explicit backend toggles must not reduce perceptual quality in QA.
            assertTrue("Backend degraded paired PSNR: "+afterPsnr,afterPsnr>=28.55);
            assertTrue("Backend degraded paired SSIM: "+afterSsim,afterSsim>=0.730);
            // Tile 0 starts at x=0, tile 1 at x=224. Check that edges of the
            // 32px overlap are not sharp seams on a smooth paired gradient.
            double seamLeft=boundaryDifference(out,224);
            double seamRight=boundaryDifference(out,256);
            double contextEdge=(boundaryDifference(out,206)+boundaryDifference(out,274))*.5;
            assertTrue("Visible discontinuity around x=224: "+seamLeft,
                    seamLeft<=contextEdge*4+16);
            assertTrue("Visible discontinuity around x=256: "+seamRight,
                    seamRight<=contextEdge*4+16);
            Debug.MemoryInfo afterMemory=new Debug.MemoryInfo();
            Debug.getMemoryInfo(afterMemory);
            int threadsAfter=linuxThreads();
            long nativeBytesAfter=Debug.getNativeHeapAllocatedSize();
            String measurements="syntheticRGB480x480 qaCpuThreads="+cpuThreads+
                " qaXnnpack="+xnnpackArg+
                " tiles="+result.stats.tiles+
                " inferMs="+result.stats.inferenceMs+" pipelineMs="+result.stats.totalMs+
                " elapsedMs="+elapsed+" pssBeforeKB="+beforeMemory.getTotalPss()+
                " pssAfterKB="+afterMemory.getTotalPss()+
                " pid="+android.os.Process.myPid()+
                " threadsBefore="+threadsBefore+" threadsAfter="+threadsAfter+
                " nativeHeapBeforeBytes="+nativeBytesBefore+
                " nativeHeapAfterBytes="+nativeBytesAfter+
                " liveSamples="+liveSamples.get()+
                " sampledPeakPssKB="+livePeakPssKb.get()+
                " sampledPeakMemory{"+livePeakBreakdown.get()+"}"+
                " memBefore{"+memoryDetail(beforeMemory)+"}"+
                " memAfter{"+memoryDetail(afterMemory)+"}"+
                " psnr="+beforePsnr+"->"+afterPsnr+
                " ssim="+beforeSsim+"->"+afterSsim+
                " pairedPsnr="+afterPsnr+" pairedSsim="+afterSsim+
                " seam224="+seamLeft+" seam256="+seamRight;
            Log.i(TAG,measurements);
            System.out.println("DN CNN TILE QA "+measurements);
        }finally {
            if(processed!=null)processed.recycle();
            input.recycle();
        }
    }
}
