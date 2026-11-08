package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/** Two independent test runner processes, separated externally by adb force-stop.
 * Uses a local synthetic MediaStore source, NOT Android DocumentsUI: genuine
 * DocumentsUI picker acceptance is maintained separately and not implied here.
 */
@RunWith(AndroidJUnit4.class)
public class ProcessDeathLocalTest {
    private static final String PREF="wfs-beta3-process-death";
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private Context app(){return inst.getTargetContext();}
    private void phase(String p){Bundle args=InstrumentationRegistry.getArguments();
        Assume.assumeTrue(p.equals(args.getString("wfs.phase")));}
    private static String sha(Context context,Uri uri)throws Exception{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            assertNotNull(in);
            byte[] bytes=new byte[8192];
            for(int n;(n=in.read(bytes))!=-1;)hash.update(bytes,0,n);
        }
        return Arrays.toString(hash.digest());
    }
    private static View find(View v,String content,boolean desc){
        if(v==null)return null;
        if(desc&&v.getContentDescription()!=null
                &&v.getContentDescription().toString().contains(content))return v;
        if(!desc&&v instanceof TextView&&((TextView)v).getText().toString().contains(content))
            return v;
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View child=find(g.getChildAt(i),content,desc);
                if(child!=null)return child;
            }
        }
        return null;
    }
    private static void tap(Activity activity,String name,boolean desc){
        View v=find(activity.getWindow().getDecorView(),name,desc);
        assertNotNull("Missing editor UI "+name,v);
        while(!v.isClickable()&&v.getParent() instanceof View)v=(View)v.getParent();
        assertTrue("Not clickable "+name,v.performClick());
    }
    private StudioEditorActivity editor(){
        long deadline=android.os.SystemClock.uptimeMillis()+20000L;
        do{
            AtomicReference<StudioEditorActivity> current=new AtomicReference<>();
            inst.runOnMainSync(()->{
                Collection<Activity> activities=ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                for(Activity a:activities)if(a instanceof StudioEditorActivity)
                    current.set((StudioEditorActivity)a);
            });
            if(current.get()!=null)return current.get();
            android.os.SystemClock.sleep(180);
        }while(android.os.SystemClock.uptimeMillis()<deadline);
        return null;
    }
    private Uri createPhoto()throws Exception{
        ContentValues cv=new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME,"wfs-pid-qa-"+System.nanoTime()+".jpg");
        cv.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        cv.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/WFS-StoryBoost-QA/");
        // Explicitly publish the JPEG in MediaStore after writing. A private
        // pending record is not a persistent gallery source across processes.
        cv.put(MediaStore.Images.Media.IS_PENDING,1);
        Uri uri=app().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
        assertNotNull(uri);
        Bitmap img=Bitmap.createBitmap(640,960,Bitmap.Config.ARGB_8888);
        try{
            for(int y=0;y<960;y++)for(int x=0;x<640;x++)
                img.setPixel(x,y,Color.rgb((x*2)%256,(y*4)%256,(x+y)%256));
            try(OutputStream out=app().getContentResolver().openOutputStream(uri)){
                assertNotNull(out);assertTrue(img.compress(Bitmap.CompressFormat.JPEG,93,out));
            }
        }finally{img.recycle();}
        ContentValues visible=new ContentValues();
        visible.put(MediaStore.Images.Media.IS_PENDING,0);
        assertEquals("Unable to publish synthetic MediaStore photo",
                1,app().getContentResolver().update(uri,visible,null,null));
        assertTrue("Source not readable immediately after publishing",
                SourceAccess.canRead(app(),uri));
        return uri;
    }
    @Test public void stageDraftBeforeForceStop()throws Exception{
        phase("stage");
        Uri uri=createPhoto();
        String hash=sha(app(),uri);
        app().getSharedPreferences("studio-projects",Context.MODE_PRIVATE)
                .edit().putString("recent_0",uri.toString()).commit();
        Intent open=new Intent(app(),StudioEditorActivity.class);
        open.putExtra("media_uri",uri.toString());
        open.putExtra("resume_project",false);
        try(ActivityScenario<StudioEditorActivity> activity=ActivityScenario.launch(open)){
            activity.onActivity(editor->{
                tap(editor,"Color Look: Vibrant",true);
                tap(editor,"Teks",false);
                ViewGroup root=(ViewGroup)editor.getWindow().getDecorView();
                EditText field=null;
                java.util.ArrayDeque<View> nodes=new java.util.ArrayDeque<>();
                nodes.add(root);
                while(!nodes.isEmpty()){
                    View v=nodes.removeFirst();
                    if(v instanceof EditText){field=(EditText)v;break;}
                    if(v instanceof ViewGroup){
                        ViewGroup g=(ViewGroup)v;
                        for(int i=0;i<g.getChildCount();i++)nodes.addLast(g.getChildAt(i));
                    }
                }
                assertNotNull(field);
                field.setText("PERSISTED AFTER FORCE STOP");
                tap(editor,"☷  Ekspor",false);
                tap(editor,"1080p",false);
                editor.finish();
            });
            inst.waitForIdleSync();
        }
        StudioDraftStore.Draft draft=StudioDraftStore.load(app(),uri);
        assertNotNull("First process did not commit onPause",draft);
        assertEquals(2,draft.options.preset);
        assertEquals("PERSISTED AFTER FORCE STOP",draft.options.caption);
        assertEquals(OutputProfile.FULL_HD,draft.options.qualityMode);
        assertEquals(hash,sha(app(),uri));
        assertTrue("Failed to store two-process QA marker",
                app().getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                .putString("uri",uri.toString()).putString("sha",hash)
                .putInt("pid",Process.myPid()).commit());
    }
    @Test public void verifyReopenAfterExternalForceStop()throws Exception{
        phase("verify");
        android.content.SharedPreferences p=app().getSharedPreferences(PREF,Context.MODE_PRIVATE);
        String source=p.getString("uri",null);
        assertNotNull("First process did not persist source",source);
        assertNotEquals("No actual new process observed",p.getInt("pid",-1),Process.myPid());
        Uri uri=Uri.parse(source);
        // MediaProvider may still be completing a publish after process restart.
        // Retry only briefly; persistent permission or missing-file failures
        // remain hard errors with the real openFileDescriptor exception attached.
        boolean accessible=false;
        for(int attempt=0;attempt<12;attempt++){
            if(SourceAccess.canRead(app(),uri)){accessible=true;break;}
            android.os.SystemClock.sleep(200);
        }
        if(!accessible){
            String detail;
            try(android.os.ParcelFileDescriptor fd=app().getContentResolver()
                    .openFileDescriptor(uri,"r")){
                detail="unexpected null FileDescriptor="+(fd==null);
            }catch(Exception ex){detail=ex.getClass().getName()+": "+ex.getMessage();}
            fail("Published MediaStore source unreadable after force-stop: "+
                    uri+"; cause="+detail);
        }
        assertEquals("Original JPEG hash changed",p.getString("sha",""),sha(app(),uri));
        try(ActivityScenario<StudioHomeActivity> home=ActivityScenario.launch(StudioHomeActivity.class)){
            inst.waitForIdleSync();
            home.onActivity(a->{
                assertTrue("Recent source not readable at click time",SourceAccess.canRead(a,uri));
                tap(a,"Media Proyek 1",false);
            });
            inst.waitForIdleSync();
            StudioEditorActivity resumed=editor();
            if(resumed==null){
                StringBuilder lifecycle=new StringBuilder();
                inst.runOnMainSync(()->{
                    for(Stage stage:new Stage[]{Stage.RESUMED,Stage.STARTED,Stage.CREATED}){
                        Collection<Activity> actors=ActivityLifecycleMonitorRegistry.getInstance()
                                .getActivitiesInStage(stage);
                        for(Activity activity:actors){
                            lifecycle.append(stage).append(':')
                                .append(activity.getClass().getSimpleName()).append(' ');
                        }
                    }
                });
                fail("Recent did not reopen after force-stop; source readable="+
                        SourceAccess.canRead(app(),uri)+"; lifecycle="+lifecycle);
            }
            StudioDraftStore.Draft draft=StudioDraftStore.load(app(),uri);
            assertNotNull(draft);
            assertEquals("PERSISTED AFTER FORCE STOP",draft.options.caption);
            assertEquals(2,draft.options.preset);
            PhotoEngine.Result result=PhotoEngine.process(app(),uri,draft.options);
            Bitmap decoded=BitmapFactory.decodeByteArray(result.jpeg,0,result.jpeg.length);
            assertNotNull("Actual resumed JPEG is unreadable",decoded);
            try{
                assertEquals(1080,decoded.getWidth());
                assertEquals(1920,decoded.getHeight());
            }finally{decoded.recycle();}
            assertEquals("Original changed during export",p.getString("sha",""),sha(app(),uri));
            inst.runOnMainSync(resumed::finish);
        }
        try{app().getContentResolver().delete(uri,null,null);}catch(Exception ignored){}
        p.edit().clear().commit();
        app().getSharedPreferences("studio-projects",Context.MODE_PRIVATE).edit().clear().commit();
        StudioDraftStore.clearAll(app());
    }
}
