package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Invoked once per NEW Android instrumentation process by
 * scripts/qa-dncnn-memory-v4.sh (5 independent invocations per API).
 * This measures process PSS snapshots/peak samples, Java/native/graphics,
 * post-GC retention, and CPU time for one real 480x480 four-tile inference.
 * Emulator data alone cannot certify any physical-device RAM target.
 */
@RunWith(AndroidJUnit4.class)
public final class DncnnMemoryV4Test {
    private static final String TAG="WfsDnnMemV4";
    private static final int W=480,H=480;
    private static String stat(Debug.MemoryInfo info,String name){
        String value=info.getMemoryStat(name);
        return value==null?"NA":value;
    }
    private static Debug.MemoryInfo memory(){
        Debug.MemoryInfo info=new Debug.MemoryInfo();
        Debug.getMemoryInfo(info);
        return info;
    }
    private static int cap(int x){return Math.max(0,Math.min(255,x));}
    @Test(timeout=170000)
    public void freshProcessFourTileInferenceMemoryAndCpu()throws Exception{
        Bundle args=InstrumentationRegistry.getArguments();
        Assume.assumeTrue("Run five independent processes via QA shell",
                "memory-single".equals(args.getString("wfs.phase")));
        int threads=Integer.parseInt(args.getString("wfs.threads","3"));
        assertTrue("Requested QA worker count unsupported",threads==1||threads==3);
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Pinned model missing",AiDenoiseEngine.included(ctx));
        int[] pixels=new int[W*H];
        Random rand=new Random(82491L);
        for(int y=0;y<H;y++)for(int x=0;x<W;x++){
            int noise=(int)Math.round(rand.nextGaussian()*22);
            pixels[y*W+x]=Color.rgb(cap(75+x/8+noise),cap(80+y/9+noise),
                    cap(100+x/15+y/18+noise));
        }
        Bitmap input=Bitmap.createBitmap(pixels,W,H,Bitmap.Config.ARGB_8888);
        Bitmap output=null;
        final AtomicBoolean measuring=new AtomicBoolean(true);
        final AtomicLong peak=new AtomicLong(0);
        final AtomicLong nativePeak=new AtomicLong(0);
        Debug.MemoryInfo before=memory();
        long cpuStart=Process.getElapsedCpuTime();
        long nativeStart=Debug.getNativeHeapAllocatedSize();
        Thread monitor=new Thread(()->{
            while(measuring.get()){
                Debug.MemoryInfo now=memory();
                peak.accumulateAndGet(now.getTotalPss(),Math::max);
                nativePeak.accumulateAndGet(Debug.getNativeHeapAllocatedSize(),Math::max);
                SystemClock.sleep(75);
            }
        },"wfs-dncnn-pss-sampler");
        monitor.setDaemon(true);
        monitor.start();
        long start=SystemClock.elapsedRealtime();
        AiDenoiseEngine.Result result;
        try{
            result=AiDenoiseEngine.processWithThreads(ctx,input,threads);
            output=result.bitmap;
            assertNotNull(output);
            assertEquals(W,output.getWidth());
            assertEquals(H,output.getHeight());
            assertEquals("Must execute 4 real neural tiles",4,result.stats.tiles);
        }finally{
            measuring.set(false);
            monitor.join(1000);
        }
        long wall=SystemClock.elapsedRealtime()-start;
        long cpu=Process.getElapsedCpuTime()-cpuStart;
        Debug.MemoryInfo after=memory();
        long nativeAfter=Debug.getNativeHeapAllocatedSize();
        if(output!=null)output.recycle();
        input.recycle();
        pixels=null;
        System.gc();
        SystemClock.sleep(150);
        Debug.MemoryInfo postGc=memory();
        long nativePostGc=Debug.getNativeHeapAllocatedSize();
        String log="WFS_DNCNN_MEMORY_V4 pid="+Process.myPid()+
          " sdk="+android.os.Build.VERSION.SDK_INT+" threads="+threads+
          " tiles="+result.stats.tiles+" inferenceMs="+result.stats.inferenceMs+
          " pipelineMs="+result.stats.totalMs+" wallMs="+wall+" cpuMs="+cpu+
          " pssBeforeKB="+before.getTotalPss()+
          " pssAfterKB="+after.getTotalPss()+
          " pssSamplePeakKB="+peak.get()+
          " pssPostGcKB="+postGc.getTotalPss()+
          " javaBeforeKB="+stat(before,"summary.java-heap")+
          " javaAfterKB="+stat(after,"summary.java-heap")+
          " javaPostGcKB="+stat(postGc,"summary.java-heap")+
          " nativeBeforeKB="+stat(before,"summary.native-heap")+
          " nativeAfterKB="+stat(after,"summary.native-heap")+
          " nativePostGcKB="+stat(postGc,"summary.native-heap")+
          " graphicsBeforeKB="+stat(before,"summary.graphics")+
          " graphicsAfterKB="+stat(after,"summary.graphics")+
          " nativeAllocatedBeforeBytes="+nativeStart+
          " nativeAllocatedAfterBytes="+nativeAfter+
          " nativeAllocatedPeakSampleBytes="+nativePeak.get()+
          " nativeAllocatedPostGcBytes="+nativePostGc;
        Log.i(TAG,log);
        System.out.println(log);
        assertTrue("No PSS samples collected",peak.get()>0);
    }
}
