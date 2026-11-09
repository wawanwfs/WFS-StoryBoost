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
    private VideoMergeEngine mergeEngine;
    private File result;
    private final List<File> outputFiles=new ArrayList<>();

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
        if(mergeEngine!=null)inst.runOnMainSync(mergeEngine::cancel);
        if(result!=null)result.delete();
        for(File f:outputFiles)if(f!=null)f.delete();
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

    private File realExport(Uri source, ExportOptions options) throws Exception {
        CountDownLatch completed=new CountDownLatch(1);
        AtomicReference<File> file=new AtomicReference<>();
        AtomicReference<String> fail=new AtomicReference<>();
        engine=new VideoEngine();
        inst.runOnMainSync(()->{
            try{
                engine.start(ctx,source,options,new VideoEngine.Listener(){
                    @Override public void onSuccess(File output,AspectRatioProfile.Size dimensions){
                        file.set(output);completed.countDown();
                    }
                    @Override public void onError(String reason){fail.set(reason);completed.countDown();}
                });
            }catch(Exception ex){fail.set(ex.toString());completed.countDown();}
        });
        assertTrue("Actual encoder did not finish",completed.await(170,TimeUnit.SECONDS));
        assertNull("VideoEngine failed: "+fail.get(),fail.get());
        File output=file.get();
        assertNotNull("No encoded video returned",output);
        assertTrue("Encoded MP4 unexpectedly short",output.length()>1024);
        outputFiles.add(output);
        return output;
    }

    @Test(timeout=350000)
    @SdkSuppress(minSdkVersion=35)
    public void splitExportProducesTwoIndependentTimedH264AacSegments() throws Exception {
        ExportOptions first=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true,0,1000)
                .withQuality(OutputProfile.FULL_HD);
        ExportOptions second=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true,1000,2000)
                .withQuality(OutputProfile.FULL_HD);
        File p1=realExport(av,first);
        File p2=realExport(av,second);
        for(File f:new File[]{p1,p2}){
            MediaProbe.Info info=MediaProbe.inspect(f);
            assertTrue(info.video);
            assertTrue(info.audio);
            assertTrue(containsCodec(f,"video/avc"));
            assertTrue(containsCodec(f,"audio/mp4a-latm"));
            assertTrue("Split output expected ~1000ms, actual="+info.durationMs,
                    Math.abs(info.durationMs-1000)<420);
        }
        assertNotEquals("Two split exports must be distinct files",
                p1.getAbsolutePath(),p2.getAbsolutePath());
    }

    @Test(timeout=240000)
    @SdkSuppress(minSdkVersion=35)
    public void media3MergeConcatenatesTwoClipsIntoOneMp4() throws Exception {
        CountDownLatch done=new CountDownLatch(1);
        AtomicReference<File> produced=new AtomicReference<>();
        AtomicReference<String> error=new AtomicReference<>();
        mergeEngine=new VideoMergeEngine();
        inst.runOnMainSync(()->{
            try{
                mergeEngine.start(ctx,java.util.Arrays.asList(av,av),
                        OutputProfile.FULL_HD,new VideoMergeEngine.Listener(){
                    @Override public void onCompleted(File file){
                        produced.set(file);done.countDown();
                    }
                    @Override public void onFailure(String message){
                        error.set(message);done.countDown();
                    }
                });
            }catch(Exception ex){error.set(ex.toString());done.countDown();}
        });
        assertTrue("Media3 merge did not finish",done.await(175,TimeUnit.SECONDS));
        assertNull("Merge failed: "+error.get(),error.get());
        File merged=produced.get();
        assertNotNull(merged);
        outputFiles.add(merged);
        MediaProbe.Info info=MediaProbe.inspect(merged);
        assertTrue(info.video);assertTrue(info.audio);
        assertTrue(containsCodec(merged,"video/avc"));
        assertTrue(containsCodec(merged,"audio/mp4a-latm"));
        assertTrue("Two 2-second clips should produce ~4s, got "+info.durationMs,
                Math.abs(info.durationMs-4000)<750);
        assertTrue("Merged A/V drift too large",
                Math.abs(info.audioDurationMs-info.videoDurationMs)<450);
    }

    @Test(timeout=350000)
    @SdkSuppress(minSdkVersion=35)
    public void silentAndRotatedSourcesExportWithoutInventingAudio() throws Exception {
        ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true,0,1800)
                .withQuality(OutputProfile.FULL_HD);
        File noAudio=realExport(silent,options);
        MediaProbe.Info output=MediaProbe.inspect(noAudio);
        assertTrue(output.video);
        assertFalse("Silent fixture must stay silent",output.audio);
        assertTrue(containsCodec(noAudio,"video/avc"));
        File upright=realExport(rotated,options);
        MediaProbe.Info rotatedOutput=MediaProbe.inspect(upright);
        assertTrue(rotatedOutput.video);
        assertTrue(rotatedOutput.audio);
        android.media.MediaMetadataRetriever md=new android.media.MediaMetadataRetriever();
        try{
            md.setDataSource(upright.getAbsolutePath());
            int width=Integer.parseInt(md.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height=Integer.parseInt(md.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            String angle=md.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if(angle!=null&&Integer.parseInt(angle)%180!=0){
                int swap=width;width=height;height=swap;
            }
            assertEquals(1080,width);
            assertEquals(1920,height);
        }finally{md.release();}
    }

    @Test(timeout=245000)
    @SdkSuppress(minSdkVersion=35)
    public void mixedSoundAndSilentClipMergeDoesNotLoseVideoFrames() throws Exception {
        CountDownLatch done=new CountDownLatch(1);
        AtomicReference<File> produced=new AtomicReference<>();
        AtomicReference<String> failure=new AtomicReference<>();
        mergeEngine=new VideoMergeEngine();
        inst.runOnMainSync(()->{
            try{
                mergeEngine.start(ctx,java.util.Arrays.asList(av,silent),
                        OutputProfile.FULL_HD,new VideoMergeEngine.Listener(){
                    @Override public void onCompleted(File file){
                        produced.set(file);done.countDown();
                    }
                    @Override public void onFailure(String msg){
                        failure.set(msg);done.countDown();
                    }
                });
            }catch(Exception e){failure.set(e.toString());done.countDown();}
        });
        assertTrue("Mixed audio/silent merge timeout",done.await(185,TimeUnit.SECONDS));
        assertNull("Mixed clip merge failure: "+failure.get(),failure.get());
        File merged=produced.get();
        assertNotNull("Merge callback returned no MP4",merged);
        outputFiles.add(merged);
        assertTrue("Merged output empty",merged.length()>1024);
        MediaProbe.Info info=MediaProbe.inspect(merged);
        assertTrue("Mixed composition must retain video",info.video);
        assertTrue("Mixed composition must retain first clip audio",info.audio);
        MediaProbe.SampleTimeline timeline=MediaProbe.inspectSampleTimeline(merged);
        assertTrue("Mixed merge has no encoded video samples",timeline.videoSamples>40);
        assertTrue("Mixed merge AAC samples missing",timeline.audioSamples>30);
        assertTrue("Mixed-order audio samples finish more than 250ms from video",
                timeline.lastPtsDriftMs()<=250);
        assertTrue("No silent leg should truncate the second clip: "+info.durationMs,
                Math.abs(info.durationMs-4000)<850);
        assertTrue(containsCodec(merged,"video/avc"));
        assertTrue(containsCodec(merged,"audio/mp4a-latm"));
        assertTrue("A/V track should cover sequence within 500ms: "+
                info.audioDurationMs+"/"+info.videoDurationMs,
                Math.abs(info.audioDurationMs-info.videoDurationMs)<500);
    }

    @Test(timeout=260000)
    @SdkSuppress(minSdkVersion=35)
    public void reverseMixedMergePadsSilentFirstClipAndKeepsRealSamplePts()throws Exception{
        CountDownLatch done=new CountDownLatch(1);
        AtomicReference<File> produced=new AtomicReference<>();
        AtomicReference<String> failure=new AtomicReference<>();
        mergeEngine=new VideoMergeEngine();
        inst.runOnMainSync(()->{
            try{
                mergeEngine.start(ctx,java.util.Arrays.asList(silent,av),
                        OutputProfile.FULL_HD,new VideoMergeEngine.Listener(){
                    @Override public void onCompleted(File file){
                        produced.set(file);done.countDown();
                    }
                    @Override public void onFailure(String msg){
                        failure.set(msg);done.countDown();
                    }
                });
            }catch(Exception e){failure.set(e.toString());done.countDown();}
        });
        assertTrue("Reverse mixed merge timeout",done.await(190,TimeUnit.SECONDS));
        assertNull("Reverse mixed merge error "+failure.get(),failure.get());
        File output=produced.get();
        assertNotNull(output);
        outputFiles.add(output);
        MediaProbe.Info tracks=MediaProbe.inspect(output);
        assertTrue(tracks.video);
        assertTrue("Second clip AAC must be retained",tracks.audio);
        assertTrue(containsCodec(output,"video/avc"));
        assertTrue(containsCodec(output,"audio/mp4a-latm"));
        assertTrue("Four-second video sequence was truncated: "+tracks.durationMs,
                Math.abs(tracks.durationMs-4000)<850);
        MediaProbe.SampleTimeline actual=MediaProbe.inspectSampleTimeline(output);
        assertTrue("Expected actual encoded video frames",actual.videoSamples>=40);
        assertTrue("Expected actual encoded AAC samples",actual.audioSamples>=30);
        assertTrue("Video presentation timestamps do not reach end of composition: "+actual.lastVideoUs,
                actual.lastVideoUs>=3_400_000);
        assertTrue("Audio presentation timestamps do not reach end of composition: "+actual.lastAudioUs,
                actual.lastAudioUs>=3_300_000);
        assertTrue("Real sample PTS drift exceeds 250ms, track metadata alone is insufficient: "+
                        actual.lastPtsDriftMs()+"ms",actual.lastPtsDriftMs()<=250);
        android.util.Log.i("WfsMediaPtsQA","reverseMixed videoSamples="+actual.videoSamples+
                " audioSamples="+actual.audioSamples+
                " videoLastUs="+actual.lastVideoUs+" audioLastUs="+actual.lastAudioUs+
                " driftMs="+actual.lastPtsDriftMs());
    }

    @Test(timeout=50000)
    @SdkSuppress(minSdkVersion=35)
    public void cancellationOfRunningEncoderDeletesItsTemporaryOutput() throws Exception {
        String[] before=ctx.getCacheDir().list((folder,name)->name.startsWith("storyboost_")
                &&name.endsWith(".mp4"));
        final java.util.concurrent.atomic.AtomicInteger callbacks=
                new java.util.concurrent.atomic.AtomicInteger();
        engine=new VideoEngine();
        ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true,0,2000)
                .withQuality(OutputProfile.FULL_HD);
        inst.runOnMainSync(()->engine.start(ctx,av,options,new VideoEngine.Listener(){
            @Override public void onSuccess(File file,AspectRatioProfile.Size size){
                callbacks.incrementAndGet();outputFiles.add(file);
            }
            @Override public void onError(String message){callbacks.incrementAndGet();}
        }));
        // start() has returned after Media3 begins the asynchronous transform.
        inst.runOnMainSync(engine::cancel);
        android.os.SystemClock.sleep(1100);
        assertEquals("Cancellation must not report successful export",0,callbacks.get());
        String[] after=ctx.getCacheDir().list((folder,name)->name.startsWith("storyboost_")
                &&name.endsWith(".mp4"));
        java.util.Set<String> prior=new java.util.HashSet<>(java.util.Arrays.asList(
                before==null?new String[0]:before));
        for(String name:after==null?new String[0]:after)
            assertTrue("Cancel leaked cache MP4: "+name,prior.contains(name));
    }
}
