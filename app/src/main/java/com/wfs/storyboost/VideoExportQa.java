package com.wfs.storyboost;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** QA-only real H.264/AAC on-device transcoding using production VideoEngine.
 * Runs in response to an explicit debug-emulator launch extra; no sample is
 * collected or uploaded. The ffmpeg-generated input is staged in app-local
 * external files by GitHub Actions before this test begins.
 */
public final class VideoExportQa {
    private VideoExportQa(){}
    private static final String TAG="StoryBoostVideoQA";

    public static void start(Context context){
        if((context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
        new Session(context.getApplicationContext()).run();
    }
    private static final class Session {
        final Context context;
        final File report,fixture,silent;
        final StringBuilder log=new StringBuilder();
        long totalStart;
        volatile String stage="preflight";
        private final java.util.concurrent.atomic.AtomicBoolean finished =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        int checks=0;
        VideoEngine engine;
        VideoMergeEngine merge;
        Session(Context c){
            context=c;
            report=new File(c.getFilesDir(),"qa-video-export.txt");
            fixture=new File(c.getExternalFilesDir(null),"qa-video.mp4");
            silent=new File(c.getExternalFilesDir(null),"qa-silent.mp4");
        }
        void check(boolean condition,String detail){
            if(!condition)throw new AssertionError(detail);
            checks++;
        }
        void run(){
            report.delete();
            totalStart=SystemClock.elapsedRealtime();
            // Emulator encoders may stall indefinitely without a Transformer error
            // callback. Fail CLOSED with stage diagnosis; never claim an MP4 passed.
            Thread watchdog=new Thread(()->{
                SystemClock.sleep(135_000L);
                if(!finished.get()){
                    Log.e(TAG,"QA timed out in stage: "+stage);
                    fail(new java.util.concurrent.TimeoutException(
                            "No completion/callback after 135s in "+stage));
                }
            },"wfs-mp4-qa-watchdog");
            watchdog.setDaemon(true);watchdog.start();
            try{
                check(fixture.isFile()&&fixture.length()>10000,"real synthetic MP4 staged");
                check(silent.isFile()&&silent.length()>10000,"silent MP4 staged");
                MediaProbe.Info input=MediaProbe.inspect(fixture);
                MediaProbe.Info silentInfo=MediaProbe.inspect(silent);
                check(input.video&&input.audio&&input.durationMs>=1500,
                        "source AVC/AAC with real tracks/duration");
                check(silentInfo.video&&!silentInfo.audio,"silent source has no audio track");
                check(hasMime(fixture,"video/avc"),"source video codec AVC");
                check(hasMime(fixture,"audio/mp4a-latm"),"source audio codec AAC");
                File invalid=new File(context.getCacheDir(),"qa-invalid-video.mp4");
                try(FileOutputStream out=new FileOutputStream(invalid)){
                    out.write("not a valid media file".getBytes(StandardCharsets.UTF_8));
                }
                boolean rejected=false;
                try{
                    new VideoEngine().start(context,Uri.fromFile(invalid),
                            new ExportOptions(ColorLooks.ORIGINAL,AspectRatioProfile.LANDSCAPE_16_9,
                                    AspectRatioProfile.FIT,true),
                            new VideoEngine.Listener(){
                                @Override public void onSuccess(File f,AspectRatioProfile.Size s){}
                                @Override public void onError(String message){}
                            });
                }catch(RuntimeException expected){rejected=true;}
                invalid.delete();
                check(rejected,"unsupported/corrupt MP4 rejected before export");
                stage="FHD 1920x1080 encode";
                Log.i(TAG,"Begin "+stage);
                fhd();
            }catch(Throwable t){fail(t);}
        }
        boolean hasMime(File f,String mime)throws Exception{
            MediaExtractor ex=new MediaExtractor();
            try{
                ex.setDataSource(f.getAbsolutePath());
                for(int i=0;i<ex.getTrackCount();i++){
                    MediaFormat fmt=ex.getTrackFormat(i);
                    if(mime.equals(fmt.getString(MediaFormat.KEY_MIME)))return true;
                }
                return false;
            }finally{ex.release();}
        }
        void verify(File output,int w,int h,boolean audio,long expected,long tolerance)
                throws Exception{
            check(output.isFile()&&output.length()>1024,"encoded MP4 non-empty");
            MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            int actualW,actualH;
            try{
                retriever.setDataSource(output.getAbsolutePath());
                actualW=Integer.parseInt(retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                actualH=Integer.parseInt(retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            }finally{retriever.release();}
            check(actualW==w&&actualH==h,"encoded dimensions "+w+"x"+h);
            MediaProbe.Info info=MediaProbe.inspect(output);
            check(info.video&&info.audio==audio,"export audio/video track match");
            check(hasMime(output,"video/avc"),"encoded output H.264");
            if(audio)check(hasMime(output,"audio/mp4a-latm"),"encoded output AAC");
            check(Math.abs(info.durationMs-expected)<=tolerance,
                    "export duration matches requested edit");
            if(audio)check(Math.abs(info.audioDurationMs-info.videoDurationMs)<=1200,
                    "AAC and AVC tracks synchronized within tolerance");
            log.append("outputBytes=").append(output.length()).append('\n');
        }
        void fhd(){
            engine=new VideoEngine();
            long start=SystemClock.elapsedRealtime();
            ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                    AspectRatioProfile.LANDSCAPE_16_9,AspectRatioProfile.FIT,true)
                    .withQuality(OutputProfile.FULL_HD);
            try{
                engine.start(context,Uri.fromFile(fixture),options,new VideoEngine.Listener(){
                    @Override public void onSuccess(File output,AspectRatioProfile.Size d){
                        try{
                            verify(output,1920,1080,true,2000,900);
                            log.append("fhdMp4Ms=").append(SystemClock.elapsedRealtime()-start).append('\n');
                            engine.cleanup();
                            trim();
                        }catch(Throwable ex){fail(ex);}
                    }
                    @Override public void onError(String m){fail(new Exception("FHD export: "+m));}
                });
            }catch(Throwable e){fail(e);}
        }
        void trim(){
            stage="trim";Log.i(TAG,"Begin "+stage);
            engine=new VideoEngine();
            long start=SystemClock.elapsedRealtime();
            ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                    AspectRatioProfile.LANDSCAPE_16_9,AspectRatioProfile.FIT,false,500,1500)
                    .withQuality(OutputProfile.HD);
            try{
                engine.start(context,Uri.fromFile(fixture),options,new VideoEngine.Listener(){
                    @Override public void onSuccess(File output,AspectRatioProfile.Size d){
                        try{
                            verify(output,1280,720,true,1000,950);
                            log.append("trimMp4Ms=").append(SystemClock.elapsedRealtime()-start).append('\n');
                            engine.cleanup();
                            noAudio();
                        }catch(Throwable ex){fail(ex);}
                    }
                    @Override public void onError(String m){fail(new Exception("Trim export: "+m));}
                });
            }catch(Throwable e){fail(e);}
        }
        void noAudio(){
            stage="mute";Log.i(TAG,"Begin "+stage);
            engine=new VideoEngine();
            ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                    AspectRatioProfile.LANDSCAPE_16_9,AspectRatioProfile.FIT,false)
                    .withQuality(OutputProfile.HD).withMute(true);
            try{
                engine.start(context,Uri.fromFile(fixture),options,new VideoEngine.Listener(){
                    @Override public void onSuccess(File output,AspectRatioProfile.Size d){
                        try{
                            verify(output,1280,720,false,2000,900);
                            engine.cleanup();merge();
                        }catch(Throwable ex){fail(ex);}
                    }
                    @Override public void onError(String m){fail(new Exception("Mute export: "+m));}
                });
            }catch(Throwable e){fail(e);}
        }
        void merge(){
            stage="merge";Log.i(TAG,"Begin "+stage);
            merge=new VideoMergeEngine();
            long start=SystemClock.elapsedRealtime();
            try{
                merge.start(context,Arrays.asList(Uri.fromFile(fixture),
                        Uri.fromFile(fixture)),OutputProfile.HD,new VideoMergeEngine.Listener(){
                    @Override public void onCompleted(File output){
                        try{
                            verify(output,1280,720,true,4000,1100);
                            log.append("merge2ClipsMs=").append(SystemClock.elapsedRealtime()-start).append('\n');
                            merge.clean();
                            cancellation();
                        }catch(Throwable ex){fail(ex);}
                    }
                    @Override public void onFailure(String m){fail(new Exception("Merge: "+m));}
                });
            }catch(Throwable e){fail(e);}
        }
        void cancellation(){
            stage="cancel";Log.i(TAG,"Begin "+stage);
            engine=new VideoEngine();
            try{
                engine.start(context,Uri.fromFile(fixture),new ExportOptions(
                        ColorLooks.ORIGINAL,AspectRatioProfile.LANDSCAPE_16_9,
                        AspectRatioProfile.FIT,true).withQuality(OutputProfile.FULL_HD),
                        new VideoEngine.Listener(){
                            @Override public void onSuccess(File output,AspectRatioProfile.Size d){}
                            @Override public void onError(String message){}
                        });
                engine.cancel();
                File[] temp=context.getCacheDir().listFiles((dir,name)->
                        name.startsWith("storyboost_")&&name.endsWith(".mp4"));
                check(temp==null||temp.length==0,"cancelled output removed from cache");
                log.append("cancelTempCleaned=true\n");
                success();
            }catch(Throwable e){fail(e);}
        }
        void fail(Throwable e){
            if(!finished.compareAndSet(false,true))return;
            Log.e(TAG,"QA failed",e);
            log.insert(0,"STATUS=FAIL\nerror="+e.getClass().getSimpleName()+": "+
                    String.valueOf(e.getMessage()).replace('\n',' ')+"\n");
            finish();
        }
        void success(){
            if(!finished.compareAndSet(false,true))return;
            log.insert(0,"STATUS=PASS\n");
            finish();
        }
        void finish(){
            if(engine!=null)engine.cancel();
            if(merge!=null)merge.cancel();
            log.append("stage=").append(stage).append('\n');
            log.append("checks=").append(checks).append('\n');
            log.append("totalElapsedMs=").append(SystemClock.elapsedRealtime()-totalStart).append('\n');
            try(FileOutputStream out=new FileOutputStream(report)){
                out.write(log.toString().getBytes(StandardCharsets.UTF_8));
            }catch(Exception e){Log.e(TAG,"Cannot write QA report",e);}
            Log.i(TAG,log.toString().replace('\n',';'));
        }
    }
}
