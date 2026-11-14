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

    @Test(timeout=150000) @SdkSuppress(minSdkVersion=29,maxSdkVersion=29)
    public void android10ReportsRealHdAndDistinguishesFullHdFromFallback() throws Exception {
        AspectRatioProfile.Size hd=new AspectRatioProfile.Size(720,1280);
        AspectRatioProfile.Size fhd=new AspectRatioProfile.Size(1080,1920);
        for(int quality:new int[]{OutputProfile.HD,OutputProfile.FULL_HD}){
            ExportOptions config=new ExportOptions(ColorLooks.ORIGINAL,
                    AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true,200,1700)
                    .withQuality(quality);
            CountDownLatch finished=new CountDownLatch(1);
            AtomicReference<String> error=new AtomicReference<>();
            AtomicReference<File> actual=new AtomicReference<>();
            AtomicReference<AspectRatioProfile.Size> resultSize=new AtomicReference<>();
            engine=new VideoEngine();
            inst.runOnMainSync(()->engine.start(ctx,av,config,new VideoEngine.Listener(){
                @Override public void onSuccess(File file,AspectRatioProfile.Size dimensions){
                    actual.set(file);resultSize.set(dimensions);finished.countDown();
                }
                @Override public void onError(String message){error.set(message);finished.countDown();}
            }));
            assertTrue("Android 10 encoder did not finish",finished.await(55,TimeUnit.SECONDS));
            File encoded=actual.get();
            if(encoded==null){
                String reason=error.get();
                assertNotNull("No classified outcome from encoder",reason);
                assertTrue("Unexpected failure for valid MP4: "+reason,
                        reason.startsWith("ERROR_CODE_ENCODER_")||reason.startsWith("ERROR_CODE_ENCODING_")
                        ||reason.startsWith("ERROR_CODE_DECODER_")||reason.startsWith("ERROR_CODE_DECODING_"));
                android.util.Log.i("WfsEncoderQa","API29 quality="+quality+" CODEC_FAILURE "+reason);
            }else{
                outputFiles.add(encoded);
                assertNull(error.get());
                assertTrue(containsCodec(encoded,"video/avc"));
                assertTrue(containsCodec(encoded,"audio/mp4a-latm"));
                MediaProbe.SampleTimeline pts=MediaProbe.inspectSampleTimeline(encoded);
                assertTrue("Unexpected sample drift: "+pts.lastPtsDriftMs(),pts.lastPtsDriftMs()>=0&&pts.lastPtsDriftMs()<=250);
                AspectRatioProfile.Size dimensions=resultSize.get();
                assertTrue("Unexpected actual canvas "+dimensions,hd.toString().equals(dimensions.toString())
                        ||(quality==OutputProfile.FULL_HD&&fhd.toString().equals(dimensions.toString())));
                android.util.Log.i("WfsEncoderQa","API29 quality="+quality+" actual="+dimensions+
                        " A/V last PTS drift="+pts.lastPtsDriftMs()+"ms result="+
                        (quality==OutputProfile.HD?"HD":fhd.toString().equals(dimensions.toString())?"FHD":"HD_FALLBACK"));
            }
            inst.runOnMainSync(engine::cleanup);
        }
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

    @Test public void videoEngineRejectsMalformedSourceBeforeAnyEncode() throws Exception {
        Uri malformed=importFixture("qa-invalid.mp4");
        ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.ORIGINAL,AspectRatioProfile.FIT,true,0,0);
        AtomicReference<RuntimeException> rejected=new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger callbacks=new java.util.concurrent.atomic.AtomicInteger();
        engine=new VideoEngine();
        String[] before=ctx.getCacheDir().list((dir,name)->name.startsWith("storyboost_"));
        inst.runOnMainSync(()->{
            try{
                engine.start(ctx,malformed,options,new VideoEngine.Listener(){
                    @Override public void onSuccess(File file,AspectRatioProfile.Size dims){
                        outputFiles.add(file);
                        callbacks.incrementAndGet();
                    }
                    @Override public void onError(String error){callbacks.incrementAndGet();}
                });
            }catch(IllegalArgumentException expected){rejected.set(expected);}
        });
        assertNotNull("Malformed source passed engine preflight",rejected.get());
        assertEquals(0,callbacks.get());
        String[] after=ctx.getCacheDir().list((dir,name)->name.startsWith("storyboost_"));
        assertNotNull(before);
        assertNotNull(after);
        java.util.Arrays.sort(before);
        java.util.Arrays.sort(after);
        assertArrayEquals("Malformed source leaked encoder cache",before,after);
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

    @Test(timeout=450000)
    public void captionSafeZonesMatchEditorOverlayOnDecodedVideo() throws Exception {
        Uri portrait=importFixture("qa-caption.mp4"), landscape=importFixture("qa-caption-rotated.mp4");
        final int gold=0xffffd786;
        for(int profile:new int[]{SocialExportProfile.CUSTOM,SocialExportProfile.INSTAGRAM_REELS}){
            for(Uri source:new Uri[]{portrait,landscape}){
                boolean rotated=landscape.equals(source);
                int ratio=profile==SocialExportProfile.CUSTOM&&rotated
                        ?AspectRatioProfile.LANDSCAPE_16_9:AspectRatioProfile.PORTRAIT_9_16;
                String title="CAPTION PLACEMENT FROM PREVIEW TO VIDEO";
                ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,ratio,AspectRatioProfile.FIT,
                        false,0,0).withSocialProfile(profile).withCaption(title,gold);
                byte[] before=hashSource(source);
                File encoded=realExport(source,options);
                try{
                    assertTrue(containsCodec(encoded,"video/avc"));
                    assertTrue(containsCodec(encoded,"audio/mp4a-latm"));
                    MediaProbe.SampleTimeline pts=MediaProbe.inspectSampleTimeline(encoded);
                    assertTrue("Video needs actual A/V samples",pts.videoSamples>10&&pts.audioSamples>15);
                    assertTrue("Caption export changed A/V timing",pts.lastPtsDriftMs()>=0&&pts.lastPtsDriftMs()<=250);
                    android.media.MediaMetadataRetriever reader=new android.media.MediaMetadataRetriever();
                    try{
                        reader.setDataSource(encoded.getAbsolutePath());
                        android.graphics.Rect reference=null;
                        int w=profile==SocialExportProfile.CUSTOM&&rotated?1280:profile==SocialExportProfile.CUSTOM?720:1080;
                        int h=profile==SocialExportProfile.CUSTOM&&rotated?720:profile==SocialExportProfile.CUSTOM?1280:1920;
                        for(long time:new long[]{200000L,850000L}){
                            android.graphics.Bitmap frame=reader.getFrameAtTime(time,MediaMetadataRetriever.OPTION_CLOSEST);
                            assertNotNull("No decoded frame at "+time,frame);
                            try{
                                assertEquals("Decoded width",w,frame.getWidth());
                                assertEquals("Decoded height",h,frame.getHeight());
                                android.graphics.Rect actual=captionPixels(frame);
                                if(reference==null)reference=drawPreviewCaption(w,h,title,gold,profile);
                                assertEquals("Caption left",reference.left/(float)w,actual.left/(float)w,.04f);
                                assertEquals("Caption right",reference.right/(float)w,actual.right/(float)w,.04f);
                                assertEquals("Caption top",reference.top/(float)h,actual.top/(float)h,.025f);
                                assertEquals("Caption bottom",reference.bottom/(float)h,actual.bottom/(float)h,.025f);
                                SocialExportProfile.Profile zone=SocialExportProfile.get(profile);
                                assertTrue("Caption exceeds profile safe zone "+actual,
                                        actual.top>=zone.safeTop*h&&actual.bottom<=(1f-zone.safeBottom)*h);
                                assertTrue("Caption crosses horizontal safe margins "+actual,
                                        actual.left>=w*.035f&&actual.right<=w*.965f);
                                android.util.Log.i("WfsBeta11CaptionQA","profile="+profile+" rotated="+rotated+
                                        " canvas="+w+"x"+h+" frameUs="+time+" preview="+reference+
                                        " encoded="+actual+" sampleDriftMs="+pts.lastPtsDriftMs());
                            }finally{frame.recycle();}
                        }
                    }finally{reader.release();}
                    assertArrayEquals("Source video changed",before,hashSource(source));
                }finally{inst.runOnMainSync(engine::cleanup);}
            }
        }
    }

    private android.graphics.Rect drawPreviewCaption(int w,int h,String text,int color,int profile) {
        AtomicReference<android.graphics.Rect> result=new AtomicReference<>();
        inst.runOnMainSync(()->{
            CaptionPreviewView preview=new CaptionPreviewView(ctx);
            preview.update(text,color,profile,false,true);
            preview.layout(0,0,w,h);
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(w,h,
                    android.graphics.Bitmap.Config.ARGB_8888);
            try{
                preview.draw(new android.graphics.Canvas(bitmap));
                result.set(captionPixels(bitmap));
            }finally{bitmap.recycle();}
        });
        return result.get();
    }

    private static android.graphics.Rect captionPixels(android.graphics.Bitmap bitmap) {
        android.graphics.Rect result=new android.graphics.Rect();
        int[] row=new int[bitmap.getWidth()];
        for(int y=0;y<bitmap.getHeight();y++){
            bitmap.getPixels(row,0,row.length,0,y,row.length,1);
            for(int x=0;x<row.length;x++){
                int pixel=row[x],r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
                if(r>165&&g>125&&r-b>58&&g-b>34){
                    if(result.isEmpty())result.set(x,y,x+1,y+1);
                    else result.union(x,y,x+1,y+1);
                }
            }
        }
        assertFalse("No encoded caption pixels found",result.isEmpty());
        return result;
    }

    private byte[] hashSource(Uri source) throws Exception {
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        try(InputStream in=ctx.getContentResolver().openInputStream(source)){
            assertNotNull(in);
            byte[] bytes=new byte[8192];
            for(int n;(n=in.read(bytes))!=-1;)digest.update(bytes,0,n);
        }
        return digest.digest();
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

    @Test(timeout=160000)
    @SdkSuppress(minSdkVersion=35)
    public void cancelAfterRealMedia3ProgressPreservesOriginalAndCache() throws Exception {
        cancelAtActualProgress(1);
    }

    @Test(timeout=160000) @SdkSuppress(minSdkVersion=35)
    public void cancelLateInRealMedia3EncodePreservesOriginalAndCache() throws Exception {
        cancelAtActualProgress(85);
    }

    private void cancelAtActualProgress(int minimumProgress) throws Exception {
        // A longer RIGHTS-SAFE actual H264/AAC MP4 forces real async encoding.
        Uri extended=importFixture("qa-h264-extended.mp4");
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        byte[] beforeHash;
        try(InputStream in=ctx.getContentResolver().openInputStream(extended)){
            assertNotNull(in);
            byte[] buffer=new byte[32768];
            for(int n;(n=in.read(buffer))>=0;){
                if(n>0)digest.update(buffer,0,n);
            }
            beforeHash=digest.digest();
        }
        String[] initial=ctx.getCacheDir().list((dir,name)->
                name.startsWith("storyboost_")&&name.endsWith(".mp4"));
        java.util.Set<String> previous=new java.util.HashSet<>(java.util.Arrays.asList(
                initial==null?new String[0]:initial));
        java.util.concurrent.atomic.AtomicInteger callbacks=
                new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger encodedProgress=
                new java.util.concurrent.atomic.AtomicInteger(-1);
        ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true,0,0)
                .withQuality(OutputProfile.FULL_HD);
        engine=new VideoEngine();
        inst.runOnMainSync(()->engine.start(ctx,extended,options,new VideoEngine.Listener(){
            @Override public void onSuccess(File file,AspectRatioProfile.Size dimensions){
                callbacks.incrementAndGet();
                outputFiles.add(file);
            }
            @Override public void onError(String error){callbacks.incrementAndGet();}
        }));
        long deadline=android.os.SystemClock.uptimeMillis()+65000L;
        java.util.concurrent.atomic.AtomicBoolean cancelled=new java.util.concurrent.atomic.AtomicBoolean();
        while(android.os.SystemClock.uptimeMillis()<deadline&&callbacks.get()==0&&!cancelled.get()){
            inst.runOnMainSync(()->{
                int pct=engine.progress();
                if(pct>=minimumProgress&&pct<100&&callbacks.get()==0){
                    encodedProgress.set(pct);
                    engine.cancel();
                    cancelled.set(true);
                }
            });
            if(!cancelled.get())android.os.SystemClock.sleep(40);
        }
        assertTrue("No in-flight cancellation at "+minimumProgress+"% was proven",cancelled.get());
        android.os.SystemClock.sleep(1600);
        assertEquals("Cancelled export emitted a success/error callback",0,callbacks.get());
        String[] after=ctx.getCacheDir().list((dir,name)->
                name.startsWith("storyboost_")&&name.endsWith(".mp4"));
        for(String file:after==null?new String[0]:after)
            assertTrue("In-progress cancellation leaked partial encoded MP4 "+file,
                    previous.contains(file));
        digest.reset();
        try(InputStream in=ctx.getContentResolver().openInputStream(extended)){
            assertNotNull(in);
            byte[] buffer=new byte[32768];
            for(int n;(n=in.read(buffer))>=0;){
                if(n>0)digest.update(buffer,0,n);
            }
        }
        assertArrayEquals("Cancellation mutated original MediaStore fixture",
                beforeHash,digest.digest());
        android.util.Log.i("WfsMediaCancelQA","PASS delayed Media3 cancellation at progress="+
                encodedProgress.get()+"%, threshold="+minimumProgress+"%, no callback/cache leak, unchanged source bytes");
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
