package com.wfs.storyboost;

import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Real Activities, MediaStore and Media3, with only picker results and timing controlled. */
@RunWith(AndroidJUnit4.class)
public final class ActivityGalleryCancellationQaTest {
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private final Context ctx=inst.getTargetContext();
    private final UiDevice device=UiDevice.getInstance(inst);
    private final Set<Uri> ownedFixtures=new HashSet<>();
    private boolean intents;

    private static final class Pause implements AutoCloseable {
        final CountDownLatch reached=new CountDownLatch(1), release=new CountDownLatch(1);
        final AtomicReference<Uri> row=new AtomicReference<>();
        final AtomicReference<String> name=new AtomicReference<>();
        Pause(MediaSaver.Stage stage,String prefix) {
            ExportQaCheckpoint.observer=(current,filename,uri)->{
                if(current!=stage||!filename.startsWith(prefix)||!name.compareAndSet(null,filename))return;
                row.set(uri);
                reached.countDown();
                try {
                    if(!release.await(30,TimeUnit.SECONDS))
                        throw new IllegalStateException("UI did not release the Gallery checkpoint");
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            };
        }
        void await() throws Exception {
            assertTrue("Gallery checkpoint was not reached",reached.await(105,TimeUnit.SECONDS));
        }
        @Override public void close(){
            ExportQaCheckpoint.observer=null;
            release.countDown();
        }
    }

    @After public void clean(){
        ExportQaCheckpoint.observer=null;
        if(intents)Intents.release();
        for(Uri fixture:ownedFixtures)ctx.getContentResolver().delete(fixture,null,null);
    }

    private Uri fixture(String mp4) throws Exception {
        boolean video=mp4!=null;
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,"wfs-beta8-source-"+System.nanoTime()+
                (video?".mp4":".jpg"));
        values.put(MediaStore.MediaColumns.MIME_TYPE,video?"video/mp4":"image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,(video?"Movies":"Pictures")+"/WFS-StoryBoost-QA");
        values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=ctx.getContentResolver().insert(video?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(uri);
        ownedFixtures.add(uri);
        try(OutputStream out=ctx.getContentResolver().openOutputStream(uri)){
            assertNotNull(out);
            if(video){
                try(InputStream in=inst.getContext().getAssets().open(mp4)){
                    byte[] chunk=new byte[8192];
                    for(int n;(n=in.read(chunk))!=-1;)out.write(chunk,0,n);
                }
            }else{
                Bitmap photo=Bitmap.createBitmap(360,640,Bitmap.Config.ARGB_8888);
                try{
                    photo.eraseColor(0xff5588cc);
                    assertTrue(photo.compress(Bitmap.CompressFormat.JPEG,94,out));
                }finally{photo.recycle();}
            }
        }
        ContentValues publish=new ContentValues();
        publish.put(MediaStore.MediaColumns.IS_PENDING,0);
        assertEquals(1,ctx.getContentResolver().update(uri,publish,null,null));
        return uri;
    }

    private byte[] sha(Uri uri) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=ctx.getContentResolver().openInputStream(uri)){
            assertNotNull(in);
            byte[] bytes=new byte[8192];
            for(int n;(n=in.read(bytes))!=-1;)digest.update(bytes,0,n);
        }
        return digest.digest();
    }

    private void setPicker(Uri source,Uri second){
        Intent reply=new Intent().setData(source).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(second!=null){
            ClipData clip=ClipData.newUri(ctx.getContentResolver(),"synthetic clips",source);
            clip.addItem(new ClipData.Item(second));
            reply.setClipData(clip);
        }
        Intents.init();
        intents=true;
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK,reply));
    }

    private static Button button(View view,String text){
        if(view instanceof Button&&((Button)view).getText().toString().contains(text))return (Button)view;
        if(view instanceof ViewGroup){
            ViewGroup parent=(ViewGroup)view;
            for(int i=0;i<parent.getChildCount();i++){
                Button found=button(parent.getChildAt(i),text);
                if(found!=null)return found;
            }
        }
        return null;
    }

    private static void tap(Activity activity,String text){
        Button control=button(activity.getWindow().getDecorView(),text);
        assertNotNull("Missing button: "+text,control);
        assertEquals("Hidden button: "+text,View.VISIBLE,control.getVisibility());
        assertTrue("Disabled button: "+text,control.isEnabled());
        assertTrue("No listener for "+text,control.performClick());
    }

    private boolean hasActivityText(ActivityScenario<MainActivity> screen,String text) {
        java.util.concurrent.atomic.AtomicBoolean found=new java.util.concurrent.atomic.AtomicBoolean();
        screen.onActivity(a->found.set(findText(a.getWindow().getDecorView(),text)));
        return found.get();
    }

    private static boolean findText(View view,String text) {
        if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return true;
        if(view instanceof ViewGroup){
            ViewGroup parent=(ViewGroup)view;
            for(int i=0;i<parent.getChildCount();i++)
                if(findText(parent.getChildAt(i),text))return true;
        }
        return false;
    }

    private void cancelDialog(){
        UiObject2 cancel=device.wait(Until.findObject(By.res("android","button2")),5000);
        assertNotNull("Missing export cancellation button",cancel);
        cancel.click();
        inst.waitForIdleSync();
    }

    private Set<Uri> rows(Uri collection,String prefix){
        Set<Uri> found=new HashSet<>();
        try(Cursor cursor=ctx.getContentResolver().query(MediaStore.setIncludePending(collection),
                new String[]{MediaStore.MediaColumns._ID,MediaStore.MediaColumns.DISPLAY_NAME},
                MediaStore.MediaColumns.DISPLAY_NAME+" LIKE ?",new String[]{prefix+"%"},null)){
            assertNotNull(cursor);
            while(cursor.moveToNext()){
                String name=cursor.getString(1);
                if(name!=null&&name.startsWith(prefix))
                    found.add(ContentUris.withAppendedId(collection,cursor.getLong(0)));
            }
        }
        return found;
    }

    private static void eventually(String problem,BooleanSupplier ready){
        await(problem,10000,ready);
    }

    private static void await(String problem,long timeoutMs,BooleanSupplier ready){
        long deadline=SystemClock.uptimeMillis()+timeoutMs;
        do{
            if(ready.getAsBoolean())return;
            SystemClock.sleep(80);
        }while(SystemClock.uptimeMillis()<deadline);
        assertTrue(problem,ready.getAsBoolean());
    }

    private Set<String> cacheMp4(){
        String[] files=ctx.getCacheDir().list((folder,name)->name.endsWith(".mp4")&&
                (name.startsWith("storyboost_")||name.startsWith("wfs_join_")));
        return files==null?new HashSet<>():new HashSet<>(java.util.Arrays.asList(files));
    }

    private void assertRowState(Uri uri,int expectedPending){
        assertNotNull(uri);
        try(Cursor cursor=ctx.getContentResolver().query(MediaStore.setIncludePending(uri),
                new String[]{MediaStore.MediaColumns.IS_PENDING},null,null,null)){
            assertNotNull(cursor);
            assertTrue("Checkpoint did not reach a real MediaStore row",cursor.moveToFirst());
            assertEquals(expectedPending,cursor.getInt(0));
        }
    }

    private void quickPreferences(boolean original){
        String className=MainActivity.class.getName(), packagePrefix=ctx.getPackageName()+".";
        String localName=className.startsWith(packagePrefix)?className.substring(packagePrefix.length()):className;
        ctx.getSharedPreferences(localName,Context.MODE_PRIVATE).edit()
                .putInt("ratio",original?AspectRatioProfile.ORIGINAL:AspectRatioProfile.PORTRAIT_9_16)
                .putInt("preset",original?ColorLooks.ORIGINAL:1)
                .putInt("frame",AspectRatioProfile.FIT).putInt("social_profile",SocialExportProfile.CUSTOM)
                .putInt("quality",OutputProfile.MAX_DETAIL).putBoolean("dual_ratio",false)
                .putString("trim_start","").putString("trim_end","").commit();
    }

    @Test(timeout=150000) public void quickPhotoCancelsBeforeInsertThenNewQueueSucceeds() throws Exception {
        Uri source=fixture(null);
        byte[] initial=sha(source);
        setPicker(source,null);
        quickPreferences(false);
        Uri collection=MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String prefix="WFS_STORY_";
        Set<Uri> prior=rows(collection,prefix);
        try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)){
            try(Pause gate=new Pause(MediaSaver.Stage.BEFORE_INSERT,prefix)){
                screen.onActivity(a->tap(a,"Auto Enhance Sekarang"));
                gate.await();
                assertNull(gate.row.get());
                assertEquals(prior,rows(collection,prefix));
                screen.onActivity(a->tap(a,"Batalkan antrean"));
                // Start another export before the first writer is even unblocked.
                screen.onActivity(a->tap(a,"Auto Enhance Sekarang"));
            }
            eventually("Replacement Quick Enhance export never completed",
                    ()->hasActivityText(screen,"Proses selesai"));
            Set<Uri> newRows=rows(collection,prefix);
            newRows.removeAll(prior);
            ownedFixtures.addAll(newRows);
            assertEquals("Cancelled run must not publish or disrupt its replacement",1,newRows.size());
            assertRowState(newRows.iterator().next(),0);
        }
        assertArrayEquals(initial,sha(source));
    }

    @Test(timeout=150000) public void quickOriginalMp4CancelsDuringCopyAndKeepsPreviousExport() throws Exception {
        Uri source=fixture("qa-h264-extended.mp4");
        byte[] initial=sha(source);
        setPicker(source,null);
        quickPreferences(true);
        Uri collection=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String prefix="WFS_STORY_";
        Set<Uri> prior=rows(collection,prefix);
        try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)){
            screen.onActivity(a->tap(a,"Auto Enhance Sekarang"));
            eventually("Original MP4 did not finish byte-for-byte",
                    ()->hasActivityText(screen,"Proses selesai"));
            Set<Uri> completed=rows(collection,prefix);
            completed.removeAll(prior);
            ownedFixtures.addAll(completed);
            assertEquals(1,completed.size());
            Uri previous=completed.iterator().next();
            assertArrayEquals(initial,sha(previous));
            try(Pause gate=new Pause(MediaSaver.Stage.CHUNK_WRITTEN,prefix)){
                screen.onActivity(a->tap(a,"Auto Enhance Sekarang"));
                gate.await();
                assertRowState(gate.row.get(),1);
                screen.onActivity(a->tap(a,"Batalkan antrean"));
            }
            prior.add(previous);
            eventually("Cancelled passthrough row remained",()->rows(collection,prefix).equals(prior));
            assertArrayEquals(initial,sha(previous));
            assertTrue(hasActivityText(screen,"Antrean dibatalkan"));
        }
        assertArrayEquals(initial,sha(source));
    }

    @Test(timeout=150000) @SdkSuppress(minSdkVersion=35)
    public void quickEncodedVideoCancelsBeforeGalleryCommit() throws Exception {
        Uri source=fixture("qa-h264-aac.mp4");
        byte[] initial=sha(source);
        setPicker(source,null);
        quickPreferences(false); // Non-original look requires a real transcode.
        Uri collection=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String prefix="WFS_STORY_";
        Set<Uri> prior=rows(collection,prefix);
        Set<String> cache=cacheMp4();
        try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)){
            try(Pause gate=new Pause(MediaSaver.Stage.BEFORE_PUBLISH,prefix)){
                screen.onActivity(a->tap(a,"Auto Enhance Sekarang"));
                gate.await();
                assertRowState(gate.row.get(),1);
                Set<String> active=cacheMp4();
                active.removeAll(cache);
                assertEquals("A real encoder cache file must exist",1,active.size());
                screen.onActivity(a->tap(a,"Batalkan antrean"));
                assertTrue("Cancelled encoder cache deleted before writer finished",
                        cacheMp4().containsAll(active));
            }
            eventually("Cancelled Quick Enhance transcode leaked Gallery/cache output",
                    ()->rows(collection,prefix).equals(prior)&&cacheMp4().equals(cache));
            assertTrue(hasActivityText(screen,"Antrean dibatalkan"));
        }
        assertArrayEquals(initial,sha(source));
    }

    @Test(timeout=150000) @SdkSuppress(minSdkVersion=29,maxSdkVersion=29)
    public void android10QuickEnhancePublishesRealFullHdToGallery() throws Exception {
        Uri source=fixture("qa-h264-aac.mp4");
        byte[] initial=sha(source);
        setPicker(source,null);
        quickPreferences(false);
        String prefs=MainActivity.class.getName();
        if(prefs.startsWith(ctx.getPackageName()+"."))prefs=prefs.substring(ctx.getPackageName().length()+1);
        assertTrue(ctx.getSharedPreferences(prefs,Context.MODE_PRIVATE).edit()
                .putInt("quality",OutputProfile.FULL_HD).commit());
        Uri collection=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Set<Uri> before=rows(collection,"WFS_STORY_");
        Set<String> cache=cacheMp4();
        try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)){
            screen.onActivity(a->tap(a,"Auto Enhance Sekarang"));
            await("Quick Enhance never finished",100000,()->hasActivityText(screen,"Proses selesai"));
            Uri output=newOutput(collection,"WFS_STORY_",before);
            assertFalse("Transcode was an unchanged copy",java.util.Arrays.equals(initial,sha(output)));
            assertMedia(output,1080,1920,2000);
            await("Quick Enhance leaked its intermediate",10000,()->cacheMp4().equals(cache));
        }
        assertArrayEquals(initial,sha(source));
    }

    @Test(timeout=150000) @SdkSuppress(minSdkVersion=29,maxSdkVersion=29)
    public void android10MergeStudioPublishesTwoActualClipsToGallery() throws Exception {
        Uri first=fixture("qa-h264-aac.mp4"),second=fixture("qa-h264-aac.mp4");
        byte[] one=sha(first),two=sha(second);
        setPicker(first,second);
        Uri collection=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Set<Uri> before=rows(collection,"WFS_JOIN_");
        Set<String> cache=cacheMp4();
        try(ActivityScenario<StudioMergeActivity> screen=ActivityScenario.launch(StudioMergeActivity.class)){
            screen.onActivity(a->tap(a,"Tambahkan Video"));
            inst.waitForIdleSync();
            screen.onActivity(a->assertTrue(findText(a.getWindow().getDecorView(),"2 klip dalam storyboard")));
            screen.onActivity(a->tap(a,"Gabungkan & Ekspor"));
            assertTrue("Merge never reported successful Gallery publication",
                    device.wait(Until.hasObject(By.text("Video berhasil digabung")),100000));
            Uri output=newOutput(collection,"WFS_JOIN_",before);
            assertMedia(output,320,568,4000);
            await("Merge Studio leaked its intermediate",10000,()->cacheMp4().equals(cache));
        }
        assertArrayEquals(one,sha(first));
        assertArrayEquals(two,sha(second));
    }

    private Uri newOutput(Uri collection,String prefix,Set<Uri> before){
        Set<Uri> actual=rows(collection,prefix);
        actual.removeAll(before);
        ownedFixtures.addAll(actual);
        assertEquals("Expected one new MediaStore row",1,actual.size());
        Uri output=actual.iterator().next();
        assertRowState(output,0);
        return output;
    }

    private void assertMedia(Uri output,int width,int height,long durationMs) throws Exception {
        AspectRatioProfile.Size actual=MediaDimensions.read(ctx,output);
        assertEquals(width,actual.width);
        assertEquals(height,actual.height);
        MediaProbe.Info info=MediaProbe.inspect(ctx,output);
        assertTrue(info.video&&info.audio);
        assertTrue("Unexpected output duration "+info.durationMs,Math.abs(info.durationMs-durationMs)<450);
        File copy=File.createTempFile("beta10-check-",".mp4",ctx.getCacheDir());
        try {
            try(InputStream in=ctx.getContentResolver().openInputStream(output);
                OutputStream out=new java.io.FileOutputStream(copy)){
                assertNotNull(in);
                byte[] buffer=new byte[8192];
                for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);
            }
            android.media.MediaExtractor extractor=new android.media.MediaExtractor();
            Set<String> mime=new HashSet<>();
            try {
                extractor.setDataSource(copy.getPath());
                for(int i=0;i<extractor.getTrackCount();i++)
                    mime.add(extractor.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME));
            }finally{extractor.release();}
            assertTrue(mime.toString(),mime.contains("video/avc")&&mime.contains("audio/mp4a-latm"));
            MediaProbe.SampleTimeline pts=MediaProbe.inspectSampleTimeline(copy);
            assertTrue("Unexpected last sample drift "+pts.lastPtsDriftMs(),pts.lastPtsDriftMs()>=0&&pts.lastPtsDriftMs()<=250);
            android.util.Log.i("WfsBeta10GalleryQa","actual="+actual+" durationMs="+info.durationMs
                    +" videoSamples="+pts.videoSamples+" audioSamples="+pts.audioSamples
                    +" lastPtsDriftMs="+pts.lastPtsDriftMs());
        }finally{assertTrue("Fixture copy left in cache",copy.delete());}
    }

    @Test(timeout=150000) public void editorPhotoDestroyAfterPublishBeforeUiCallbackRollsBack() throws Exception {
        Uri source=fixture(null);
        byte[] initial=sha(source);
        String prefix="WFS_STUDIO_";
        Uri collection=MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Set<Uri> prior=rows(collection,prefix);
        Intent intent=new Intent(ctx,StudioEditorActivity.class).putExtra("media_uri",source.toString());
        try(ActivityScenario<StudioEditorActivity> screen=ActivityScenario.launch(intent);
            Pause gate=new Pause(MediaSaver.Stage.BEFORE_CALLBACK,prefix)){
            screen.onActivity(a->tap(a,"Ekspor  ↗"));
            gate.await();
            assertRowState(gate.row.get(),0);
            screen.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED);
        }
        eventually("Destroyed Editor left a published or pending row",
                ()->rows(collection,prefix).equals(prior));
        assertArrayEquals(initial,sha(source));
    }

    @Test(timeout=150000) @SdkSuppress(minSdkVersion=35)
    public void editorVideoCancelsAfterEncodeBeforePublish() throws Exception {
        Uri source=fixture("qa-h264-aac.mp4");
        byte[] initial=sha(source);
        String prefix="WFS_STUDIO_";
        Uri collection=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Set<Uri> prior=rows(collection,prefix);
        Set<String> cache=cacheMp4();
        Intent intent=new Intent(ctx,StudioEditorActivity.class).putExtra("media_uri",source.toString());
        try(ActivityScenario<StudioEditorActivity> screen=ActivityScenario.launch(intent)){
            try(Pause gate=new Pause(MediaSaver.Stage.BEFORE_PUBLISH,prefix)){
                screen.onActivity(a->tap(a,"Ekspor  ↗"));
                gate.await();
                assertRowState(gate.row.get(),1);
                Set<String> active=cacheMp4();
                active.removeAll(cache);
                assertEquals("Encoder output must still be owned by the writer",1,active.size());
                cancelDialog();
                assertTrue("Cancel must not delete the writer's file concurrently",cacheMp4().containsAll(active));
            }
            eventually("Editor cancellation left a cache file or Gallery row",
                    ()->cacheMp4().equals(cache)&&rows(collection,prefix).equals(prior));
            assertFalse(device.hasObject(By.text("Ekspor selesai")));
        }
        assertArrayEquals(initial,sha(source));
    }

    @Test(timeout=150000) @SdkSuppress(minSdkVersion=35)
    public void mergeTwoClipsCancelsAfterPublicationAndCleansOnlyItsOutput() throws Exception {
        Uri first=fixture("qa-h264-aac.mp4"),second=fixture("qa-h264-aac.mp4");
        byte[] one=sha(first),two=sha(second);
        setPicker(first,second);
        Uri collection=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String prefix="WFS_JOIN_";
        Set<Uri> prior=rows(collection,prefix);
        Set<String> cache=cacheMp4();
        try(ActivityScenario<StudioMergeActivity> screen=ActivityScenario.launch(StudioMergeActivity.class)){
            screen.onActivity(a->tap(a,"Tambahkan Video"));
            inst.waitForIdleSync();
            screen.onActivity(a->assertTrue("Two real video fixtures were not loaded",
                    findText(a.getWindow().getDecorView(),"2 klip dalam storyboard")));
            try(Pause gate=new Pause(MediaSaver.Stage.AFTER_PUBLISH,prefix)){
                screen.onActivity(a->tap(a,"Gabungkan & Ekspor"));
                gate.await();
                assertRowState(gate.row.get(),0);
                cancelDialog();
            }
            eventually("Merge cancellation left a cache file or Gallery row",
                    ()->cacheMp4().equals(cache)&&rows(collection,prefix).equals(prior));
            assertFalse(device.hasObject(By.text("Video berhasil digabung")));
        }
        assertArrayEquals(one,sha(first));
        assertArrayEquals(two,sha(second));
    }
}
