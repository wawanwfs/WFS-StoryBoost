package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exact external test order defined by qa-external-provider.sh.
 * NEVER replace the native DocumentsUI picker with an Espresso Intent stub.
 * The source is in a distinct com.wfs.storyboost.qaprovider Android UID.
 */
@RunWith(AndroidJUnit4.class)
public final class QaExternalProviderTest {
    private static final String AUTH="com.wfs.storyboost.qaprovider.documents";
    private static final String FILE="wfs-provider-qa.jpg";
    private static final String PREF="wfs-beta5-external-provider-state";
    private static final String CAPTION="EXTERNAL PROVIDER SAF";
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private Context app(){return inst.getTargetContext();}
    private void phase(String name){
        Bundle args=InstrumentationRegistry.getArguments();
        Assume.assumeTrue("Only executed via external provider QA script",
                name.equals(args.getString("wfs.provider.phase")));
    }
    private static String sha(Context ctx,Uri uri)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=ctx.getContentResolver().openInputStream(uri)){
            assertNotNull("No read stream",in);
            byte[] bytes=new byte[8192];
            for(int n;(n=in.read(bytes))!=-1;)digest.update(bytes,0,n);
        }
        return Arrays.toString(digest.digest());
    }
    private static View find(View v,String text){
        if(v==null)return null;
        if(v.getContentDescription()!=null&&v.getContentDescription().toString().contains(text))return v;
        if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return v;
        if(v instanceof ViewGroup){
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++){
                View found=find(group.getChildAt(i),text);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static void tap(Activity activity,String label){
        View v=find(activity.getWindow().getDecorView(),label);
        assertNotNull("Missing Activity control "+label,v);
        while(!v.isClickable()&&v.getParent() instanceof View)v=(View)v.getParent();
        assertTrue("Unable to click "+label,v.performClick());
    }
    private StudioEditorActivity editor(long timeoutMs){
        long deadline=SystemClock.uptimeMillis()+timeoutMs;
        do{
            AtomicReference<StudioEditorActivity> live=new AtomicReference<>();
            inst.runOnMainSync(()->{
                Collection<Activity> activities=ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                for(Activity activity:activities)
                    if(activity instanceof StudioEditorActivity)
                        live.set((StudioEditorActivity)activity);
            });
            if(live.get()!=null)return live.get();
            SystemClock.sleep(200);
        }while(SystemClock.uptimeMillis()<deadline);
        return null;
    }
    private String uiText(UiDevice device){
        StringBuilder s=new StringBuilder();
        for(UiObject2 text:device.findObjects(By.clazz("android.widget.TextView"))){
            try{
                String value=text.getText();
                if(value!=null&&s.length()<2200)s.append('[').append(value).append(']');
            }catch(androidx.test.uiautomator.StaleObjectException stale){
                // Accessibility trees can change while diagnostics are read.
            }
        }
        return s.toString();
    }
    private void clickNode(UiDevice device,UiObject2 node){
        assertNotNull(node);
        try{node.click();}
        catch(androidx.test.uiautomator.StaleObjectException stale){
            fail("Stale picker node: "+uiText(device));
        }
        SystemClock.sleep(450);
    }
    private void chooseExternalFile(UiDevice device){
        assertTrue("System file picker did not launch",
                device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")),7000)
                ||device.wait(Until.hasObject(By.pkg("com.android.documentsui")),7000)
                ||device.wait(Until.hasObject(By.text("Recent")),7000));
        UiObject2 root=device.findObject(By.textContains("WFS QA Provider"));
        if(root==null){
            UiObject2 nav=device.findObject(By.descContains("Show roots"));
            if(nav==null)nav=device.findObject(By.descContains("Show navigation drawer"));
            assertNotNull("Cannot open DocumentsUI navigation. UI="+uiText(device),nav);
            clickNode(device,nav);
            root=device.wait(Until.findObject(By.textContains("WFS QA Provider")),9000);
        }
        assertNotNull("Independent provider not listed by system DocumentsUI. UI="+uiText(device),root);
        clickNode(device,root);
        UiObject2 list=device.findObject(By.desc("List view"));
        if(list!=null)clickNode(device,list);
        UiObject2 file=device.wait(Until.findObject(By.text(FILE)),4000);
        if(file==null)file=device.wait(Until.findObject(By.descContains(FILE)),7000);
        assertNotNull("Provider JPEG not shown. UI="+uiText(device),file);
        android.graphics.Rect rect=file.getVisibleBounds();
        // Avoid thumbnail preview child at the center of DocumentsUI grid rows.
        assertTrue("Invalid picker file bounds "+rect,rect.width()>20&&rect.height()>10);
        assertTrue("Picker file click failed",device.click(
                rect.right-Math.min(14,Math.max(5,rect.width()/9)),
                rect.bottom-Math.min(10,Math.max(4,rect.height()/8))));
        SystemClock.sleep(550);
        for(String confirm:new String[]{"Open","OPEN","Select","SELECT","Use this photo"}){
            UiObject2 button=device.findObject(By.text(confirm));
            if(button!=null){clickNode(device,button);break;}
        }
    }
    private Uri importFromNativePicker(boolean reimport)throws Exception {
        Uri direct=DocumentsContract.buildDocumentUri(AUTH,"qa-jpeg-v1");
        if(!reimport)assertFalse(
                "External document unexpectedly readable WITHOUT user grant",
                SourceAccess.canRead(app(),direct));
        // An independent UID is asserted by adb package metadata in the
        // external runner; Android package-visibility restrictions must not
        // make this genuine DocumentsUI test appear to fail.
        UiDevice device=UiDevice.getInstance(inst);
        try(ActivityScenario<StudioHomeActivity> home=
                ActivityScenario.launch(StudioHomeActivity.class)){
            home.onActivity(a->tap(a,"Photo Editor"));
            chooseExternalFile(device);
            inst.waitForIdleSync();
            StudioEditorActivity editor=editor(15000);
            assertNotNull("Native picker never returned URI from third-party provider; "+uiText(device),editor);
            if(reimport){
                // Old draft may still exist, but only Recent is allowed to restore it.
                java.lang.reflect.Field preset=StudioEditorActivity.class.getDeclaredField("preset");
                preset.setAccessible(true);
                assertEquals("Fresh import inherited old Recent filter",1,preset.getInt(editor));
            }
            inst.runOnMainSync(()->{
                tap(editor,"Color Look: Vibrant");
                tap(editor,"Teks");
                EditText caption=null;
                java.util.ArrayDeque<View> queue=new java.util.ArrayDeque<>();
                queue.add(editor.getWindow().getDecorView());
                while(!queue.isEmpty()){
                    View v=queue.removeFirst();
                    if(v instanceof EditText){caption=(EditText)v;break;}
                    if(v instanceof ViewGroup){
                        ViewGroup g=(ViewGroup)v;
                        for(int i=0;i<g.getChildCount();i++)queue.addLast(g.getChildAt(i));
                    }
                }
                assertNotNull("Caption input not visible",caption);
                caption.setText(CAPTION);
                tap(editor,"☷  Ekspor");
                tap(editor,"1080p");
                editor.finish();
            });
            inst.waitForIdleSync();
        }
        String text=app().getSharedPreferences("studio-projects",Context.MODE_PRIVATE)
                .getString("recent_0","");
        assertTrue("Native picked URI is not provider-owned: "+text,
                text.startsWith("content://"+AUTH+"/"));
        return Uri.parse(text);
    }
    private boolean persisted(Uri uri){
        for(android.content.UriPermission p:app().getContentResolver().getPersistedUriPermissions())
            if(uri.equals(p.getUri())&&p.isReadPermission())return true;
        return false;
    }
    @Test public void stageNativeImportFromSeparateProvider()throws Exception{
        phase("stage");
        Uri picked=importFromNativePicker(false);
        assertTrue("External persistent read grant absent",persisted(picked));
        assertTrue(SourceAccess.canRead(app(),picked));
        String hash=sha(app(),picked);
        StudioDraftStore.Draft draft=StudioDraftStore.load(app(),picked);
        assertNotNull("No persisted editor draft",draft);
        assertEquals(CAPTION,draft.options.caption);
        assertEquals(2,draft.options.preset);
        assertEquals(OutputProfile.FULL_HD,draft.options.qualityMode);
        assertTrue(app().getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                .putString("uri",picked.toString()).putString("sha",hash)
                .putInt("pid",Process.myPid()).commit());
    }
    @Test public void verifyNewProcessRecentAndExport()throws Exception{
        phase("verify");
        android.content.SharedPreferences p=app().getSharedPreferences(PREF,Context.MODE_PRIVATE);
        Uri picked=Uri.parse(p.getString("uri",""));
        assertEquals(AUTH,picked.getAuthority());
        assertNotEquals("Stage app PID reused",p.getInt("pid",-1),Process.myPid());
        assertTrue("Provider grant lost across app force-stop",persisted(picked));
        assertEquals(p.getString("sha",""),sha(app(),picked));
        try(ActivityScenario<StudioHomeActivity> home=ActivityScenario.launch(StudioHomeActivity.class)){
            home.onActivity(a->tap(a,"Media Proyek 1"));
            StudioEditorActivity live=editor(10000);
            assertNotNull("Recent not opened after process death",live);
            StudioDraftStore.Draft draft=StudioDraftStore.load(app(),picked);
            assertNotNull(draft);
            assertEquals(CAPTION,draft.options.caption);
            PhotoEngine.Result jpeg=PhotoEngine.process(app(),picked,draft.options);
            Bitmap bitmap=BitmapFactory.decodeByteArray(jpeg.jpeg,0,jpeg.jpeg.length);
            assertNotNull(bitmap);
            try{assertEquals(1080,bitmap.getWidth());assertEquals(1920,bitmap.getHeight());}
            finally{bitmap.recycle();}
            assertEquals("Provider original JPEG altered",p.getString("sha",""),sha(app(),picked));
            inst.runOnMainSync(live::finish);
        }
    }
    private Uri savedUri(){
        String uri=app().getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("uri","");
        assertFalse("Missing previous provider stage",uri.isEmpty());
        return Uri.parse(uri);
    }
    private void denyRead(String what)throws Exception{
        Uri uri=savedUri();
        assertFalse(what+" still readable after external provider denial",SourceAccess.canRead(app(),uri));
        try(android.os.ParcelFileDescriptor fd=
                    app().getContentResolver().openFileDescriptor(uri,"r")){
            fail(what+" unexpectedly reopened source after grant invalidation");
        }catch(java.io.FileNotFoundException|SecurityException expected){}
        StudioDraftStore.Draft draft=StudioDraftStore.load(app(),uri);
        assertNotNull("Inaccessible media should not destructively remove draft",draft);
        assertEquals(CAPTION,draft.options.caption);
        try(ActivityScenario<StudioHomeActivity> home=ActivityScenario.launch(StudioHomeActivity.class)){
            home.onActivity(a->tap(a,"Media Proyek 1"));
            assertNull(what+" unexpectedly opened inaccessible editor",editor(2000));
        }
    }
    @Test public void verifyOwnerRevokedGrant()throws Exception{
        phase("revoked");
        denyRead("Provider-owner REVOKE");
    }
    @Test public void verifyDisabledOrDeletedProvider()throws Exception{
        phase("unavailable");
        denyRead("Disabled/deleted third-party provider");
    }
    @Test public void reimportAfterOwnerRevoke()throws Exception{
        phase("reimport");
        Uri picked=importFromNativePicker(true);
        assertTrue("Reimport failed to request new persisted read grant",persisted(picked));
        assertTrue("Reimport still cannot read",SourceAccess.canRead(app(),picked));
        assertEquals("Original fixture changed",app().getSharedPreferences(PREF,
                Context.MODE_PRIVATE).getString("sha",""),sha(app(),picked));
    }

    private Uri providerVideo(){
        return DocumentsContract.buildDocumentUri(AUTH,"qa-mp4-v1");
    }

    private void videoControl(String action) throws Exception {
        String result=UiDevice.getInstance(inst).executeShellCommand(
                "am broadcast --receiver-foreground -n com.wfs.storyboost.qaprovider/.FixtureControlReceiver"
                +" -a com.wfs.storyboost.qaprovider.VIDEO_"+action);
        assertTrue("Provider owner did not acknowledge "+action+": "+result,result.contains("result=200"));
    }

    private void assertNoVideoRows(String name) {
        Uri all=android.provider.MediaStore.setIncludePending(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        try(android.database.Cursor rows=app().getContentResolver().query(all,
                new String[]{"_id"},"_display_name=?",new String[]{name},null)){
            assertNotNull(rows);
            assertEquals("Revoked input left a published/pending row",0,rows.getCount());
        }
    }

    @Test(timeout=90000) public void verifyVideoGrantRevokedDuringOriginalCopy() throws Exception {
        phase("video-copy");
        Uri source=providerVideo();
        String name="wfs-revoked-passthrough-"+System.nanoTime()+".mp4";
        videoControl("REVOKE");
        assertFalse(SourceAccess.canRead(app(),source));
        videoControl("GRANT");
        String initial=sha(app(),source);
        java.util.concurrent.atomic.AtomicInteger chunks=new java.util.concurrent.atomic.AtomicInteger();
        AtomicReference<Exception> controlFailure=new AtomicReference<>();
        try{
            ExportQaCheckpoint.observer=(stage,filename,row)->{
                if(!name.equals(filename)||stage!=MediaSaver.Stage.CHUNK_WRITTEN)return;
                if(chunks.incrementAndGet()==1){
                    try{videoControl("REVOKE");}
                    catch(Exception e){controlFailure.set(e);}
                }
            };
            try{
                Uri published=MediaSaver.copyOriginalMp4(app(),source,name);
                app().getContentResolver().delete(published,null,null);
                fail("Permission loss during copy was silently published");
            }catch(java.io.IOException expected){
                assertTrue(expected.toString(),expected.getMessage().contains("Akses media sumber hilang"));
            }
            assertNull(controlFailure.get());
            assertTrue("An already-open descriptor should finish reading until fresh grant check",
                    chunks.get()>1);
            assertFalse(SourceAccess.canRead(app(),source));
            assertNoVideoRows(name);
            videoControl("GRANT");
            assertEquals("Source bytes changed",initial,sha(app(),source));
            System.out.println("VIDEO_REVOKE_COPY chunks="+chunks.get()+" originalSha="+initial+" noPending=true");
        }finally{
            ExportQaCheckpoint.observer=null;
            videoControl("REVOKE");
        }
    }

    @Test(timeout=150000) public void verifyVideoGrantRevokedDuringRealEncoder() throws Exception {
        phase("video-encode");
        Uri source=providerVideo();
        videoControl("GRANT");
        VideoEngine engine=new VideoEngine();
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        AtomicReference<String> error=new AtomicReference<>();
        AtomicReference<java.io.File> output=new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger progress=new java.util.concurrent.atomic.AtomicInteger(-1);
        ExportOptions config=new ExportOptions(ColorLooks.ORIGINAL,AspectRatioProfile.PORTRAIT_9_16,
                AspectRatioProfile.FIT,true,0,0).withQuality(OutputProfile.FULL_HD);
        String[] before=app().getCacheDir().list((d,n)->n.startsWith("storyboost_")&&n.endsWith(".mp4"));
        try{
            String initial=sha(app(),source);
            inst.runOnMainSync(()->engine.start(app(),source,config,new VideoEngine.Listener(){
                @Override public void onSuccess(java.io.File f,AspectRatioProfile.Size size){
                    output.set(f);done.countDown();
                }
                @Override public void onError(String message){error.set(message);done.countDown();}
            }));
            long deadline=SystemClock.uptimeMillis()+65000;
            do{
                inst.runOnMainSync(()->progress.set(engine.progress()));
                if(progress.get()>0)break;
                SystemClock.sleep(40);
            }while(SystemClock.uptimeMillis()<deadline&&done.getCount()>0);
            assertTrue("Encoder did not reach real progress",progress.get()>0&&progress.get()<100);
            videoControl("REVOKE");
            assertFalse(SourceAccess.canRead(app(),source));
            assertTrue("Revoked encoder did not complete",done.await(50,java.util.concurrent.TimeUnit.SECONDS));
            assertNull("Encoded video accepted after source revocation",output.get());
            assertNotNull("Missing explicit failure",error.get());
            assertTrue(error.get(),error.get().contains("Akses media sumber hilang")||
                    error.get().startsWith("ERROR_CODE_IO_NO_PERMISSION"));
            String[] after=app().getCacheDir().list((d,n)->n.startsWith("storyboost_")&&n.endsWith(".mp4"));
            assertNotNull(before);
            assertNotNull(after);
            Arrays.sort(before);
            Arrays.sort(after);
            assertArrayEquals("Revoked encoder leaked a cache MP4",before,after);
            videoControl("GRANT");
            assertEquals(initial,sha(app(),source));
            System.out.println("VIDEO_REVOKE_ENCODE progress="+progress.get()+"% reason="+error.get()+" noCacheLeak=true");
        }finally{
            inst.runOnMainSync(engine::cancel);
            videoControl("REVOKE");
        }
    }
}
