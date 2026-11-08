package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Rights-safe synthetic MP4 fixtures run through real Android MediaExtractor.
 * A 1080p Media3 transform is a HARD gate on the Android 15 emulator only.
 */
@RunWith(AndroidJUnit4.class)
public class VideoMediaQaTest {
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private final List<Uri> sources=new ArrayList<>();
    private Context ctx;
    private Uri av,silent,rotated;
    private VideoEngine engine;
    private File result;

    @Before public void setup() throws Exception {
        ctx=inst.getTargetContext();
        av=importFixture("qa-h264-aac.mp4");
        silent=importFixture("qa-h264-silent.mp4");
        rotated=importFixture("qa-rotated.mp4");
    }
    private Uri importFixture(String name) throws Exception {
        ContentValues meta=new ContentValues();
        meta.put(MediaStore.Video.Media.DISPLAY_NAME,"wfs-qa-"+System.nanoTime()+name);
        meta.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
        meta.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/WFS-StoryBoost-QA");
        Uri uri=ctx.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,meta);
        assertNotNull(uri);
        sources.add(uri);
        try(InputStream in=inst.getContext().getAssets().open(name);
            OutputStream out=ctx.getContentResolver().openOutputStream(uri)){
            assertNotNull(out);
            byte[] bytes=new byte[8192];
            for(int n;(n=in.read(bytes))!=-1;)out.write(bytes,0,n);
        }
        return uri;
    }
    private static boolean containsCodec(File file,String mime) throws Exception {
        MediaExtractor extractor=new MediaExtractor();
        try{
            extractor.setDataSource(file.getAbsolutePath());
            for(int i=0;i<extractor.getTrackCount();i++){
                MediaFormat fmt=extractor.getTrackFormat(i);
                if(mime.equals(fmt.getString(MediaFormat.KEY_MIME)))return true;
            }
            return false;
        }finally{extractor.release();}
    }
    @After public void cleanup(){
        if(engine!=null)inst.runOnMainSync(engine::cancel);
        if(result!=null)result.delete();
        for(Uri uri:sources)try{ctx.getContentResolver().delete(uri,null,null);}catch(Exception ignored){}
    }

    @Test public void extractorReadsAacSilentAndRotatedFixtures() throws Exception {
        MediaProbe.Info video=MediaProbe.inspect(ctx,av);
        assertTrue(video.video);assertTrue(video.audio);
        assertTrue(video.videoDurationMs>=1800);
        assertTrue(video.audioDurationMs>=1800);
        MediaProbe.Info noAudio=MediaProbe.inspect(ctx,silent);
        assertTrue(noAudio.video);assertFalse(noAudio.audio);
        AspectRatioProfile.Size logical=MediaDimensions.read(ctx,rotated);
        assertEquals(568,logical.width);
        assertEquals(320,logical.height);
    }

    @Test(timeout=210000)
    @SdkSuppress(minSdkVersion=35)
    public void android15EncoderProducesActual1080pTrimmedMp4WithAac() throws Exception {
        ExportOptions config=new ExportOptions(ColorLooks.ORIGINAL,
            AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true,200,1700)
            .withQuality(OutputProfile.FULL_HD);
        CountDownLatch done=new CountDownLatch(1);
        AtomicReference<File> produced=new AtomicReference<>();
        AtomicReference<String> error=new AtomicReference<>();
        engine=new VideoEngine();
        inst.runOnMainSync(()->{
            try{
                engine.start(ctx,av,config,new VideoEngine.Listener(){
                    @Override public void onSuccess(File f,AspectRatioProfile.Size dims){
                        produced.set(f);done.countDown();
                    }
                    @Override public void onError(String msg){error.set(msg);done.countDown();}
                });
            }catch(Exception ex){error.set(ex.toString());done.countDown();}
        });
        assertTrue("Encoder timeout",done.await(170,TimeUnit.SECONDS));
        assertNull("VideoEngine error or 1080p fallback: "+error.get(),error.get());
        result=produced.get();
        assertNotNull(result);
        MediaProbe.Info actual=MediaProbe.inspect(result);
        assertTrue(actual.video);assertTrue(actual.audio);
        assertTrue(containsCodec(result,"video/avc"));
        assertTrue(containsCodec(result,"audio/mp4a-latm"));
        assertTrue("trim duration "+actual.durationMs,Math.abs(actual.durationMs-1500)<450);
        assertTrue("A/V drift",Math.abs(actual.audioDurationMs-actual.videoDurationMs)<450);
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        try{
            retriever.setDataSource(result.getAbsolutePath());
            int w=Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int h=Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            String rot=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if(rot!=null&&Integer.parseInt(rot)%180!=0){int swap=w;w=h;h=swap;}
            assertEquals("Real encoded width",1080,w);
            assertEquals("Real encoded height",1920,h);
        }finally{retriever.release();}
    }

    @Test public void rejectsMalformedMp4InsteadOfInventingVideoTrack() throws Exception {
        File bad=new File(ctx.getCacheDir(),"wfs-qa-bad-"+System.nanoTime()+".mp4");
        try{
            try(FileOutputStream out=new FileOutputStream(bad)){
                out.write("not a real mp4".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            try{
                MediaProbe.inspect(bad);
                fail("MediaExtractor must reject corrupted MP4");
            }catch(java.io.IOException|IllegalArgumentException expected){}
        }finally{bad.delete();}
    }
}
