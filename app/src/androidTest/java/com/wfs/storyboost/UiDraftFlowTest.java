package com.wfs.storyboost;

import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
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
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs real Activities, local MediaStore, editor UI controls, persistence and JPEG
 * export on API 29/35. ACTION_OPEN_DOCUMENT result is STUBBED; this is not a
 * system DocumentsUI picker interaction or a process-death test.
 */
@RunWith(AndroidJUnit4.class)
public class UiDraftFlowTest {
    private final Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
    private Context target;
    private Uri source;
    private byte[] originalDigest;
    private boolean intentsInitialized;

    @Before public void setup() throws Exception {
        target=instrument.getTargetContext();
        target.getSharedPreferences("studio-projects",Context.MODE_PRIVATE).edit().clear().commit();
        StudioDraftStore.clearAll(target);
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,"wfs-qa-"+System.nanoTime()+".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/WFS-StoryBoost-QA");
        source=target.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull("Failed to create local synthetic JPEG",source);
        Bitmap image=Bitmap.createBitmap(640,960,Bitmap.Config.ARGB_8888);
        try{
            for(int y=0;y<960;y++)for(int x=0;x<640;x++)
                image.setPixel(x,y,Color.rgb((x*3+y)%256,(y*2)%256,(x+y)%256));
            try(OutputStream out=target.getContentResolver().openOutputStream(source)){
                assertNotNull(out);
                assertTrue(image.compress(Bitmap.CompressFormat.JPEG,93,out));
            }
        }finally{image.recycle();}
        originalDigest=digest(source);
    }

    @After public void cleanup(){
        if(intentsInitialized){Intents.release();intentsInitialized=false;}
        if(source!=null)try{target.getContentResolver().delete(source,null,null);}catch(Exception ignored){}
        StudioDraftStore.clearAll(target);
        target.getSharedPreferences("studio-projects",Context.MODE_PRIVATE).edit().clear().commit();
    }

    private byte[] digest(Uri uri) throws Exception {
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(InputStream in=target.getContentResolver().openInputStream(uri)){
            assertNotNull(in);
            byte[] bytes=new byte[8192];
            for(int n;(n=in.read(bytes))!=-1;)hash.update(bytes,0,n);
        }
        return hash.digest();
    }
    private void idle(){instrument.waitForIdleSync();}
    private static View locate(View root,String label,boolean description){
        if(root==null)return null;
        if(description&&root.getContentDescription()!=null
                &&root.getContentDescription().toString().contains(label))return root;
        if(!description&&root instanceof TextView
                &&((TextView)root).getText().toString().contains(label))return root;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int n=0;n<group.getChildCount();n++){
                View found=locate(group.getChildAt(n),label,description);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static View input(View root){
        if(root instanceof EditText)return root;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int n=0;n<group.getChildCount();n++){
                View found=input(group.getChildAt(n));
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static View locateExactText(View root,String label){
        if(root==null)return null;
        if(root instanceof TextView&&((TextView)root).getText().toString().equals(label))
            return root;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++){
                View child=locateExactText(group.getChildAt(i),label);
                if(child!=null)return child;
            }
        }
        return null;
    }
    private static View require(Activity a,String label,boolean description){
        View root=a.getWindow().getDecorView();
        View found=description?null:locateExactText(root,label);
        if(found==null)found=locate(root,label,description);
        assertNotNull("Missing UI control "+label,found);
        return found;
    }
    private static void tap(Activity a,String label,boolean description){
        View target=require(a,label,description);
        while(target!=null&&!target.isClickable()){
            if(!(target.getParent() instanceof View))break;
            target=(View)target.getParent();
        }
        assertNotNull("Not clickable "+label,target);
        assertTrue("Click failed "+label,target.performClick());
    }
    private StudioEditorActivity resumedEditor(){
        long timeout=android.os.SystemClock.uptimeMillis()+6000L;
        do{
            AtomicReference<StudioEditorActivity> result=new AtomicReference<>();
            instrument.runOnMainSync(()->{
                Collection<Activity> resumed=ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                for(Activity a:resumed)
                    if(a instanceof StudioEditorActivity)result.set((StudioEditorActivity)a);
            });
            if(result.get()!=null)return result.get();
            android.os.SystemClock.sleep(140);
            idle();
        }while(android.os.SystemClock.uptimeMillis()<timeout);
        return null;
    }
    private ActivityScenario<StudioHomeActivity> importWithSafResult(){
        Intents.init();
        intentsInitialized=true;
        Intent chosen=new Intent().setData(source)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK,chosen));
        ActivityScenario<StudioHomeActivity> home=ActivityScenario.launch(StudioHomeActivity.class);
        home.onActivity(a->tap(a,"Photo Editor",false));
        idle();
        assertNotNull("SAF result should open a real editor",resumedEditor());
        return home;
    }
    private void act(java.util.function.Consumer<StudioEditorActivity> step){
        StudioEditorActivity editor=resumedEditor();
        assertNotNull("Expected resumed editor",editor);
        instrument.runOnMainSync(()->step.accept(editor));
        idle();
    }
    private void back(){
        act(Activity::finish);
        idle();
    }

    @Test public void editedPhotoRestoresFromRecentAndExportsWithoutMutatingSource() throws Exception {
        // Close the original Home Activity too: Recent is exercised from a newly
        // launched Home, rather than invoking an old background Activity instance.
        try(ActivityScenario<StudioHomeActivity> first=importWithSafResult()){
            act(a->{
                tap(a,"Color Look: Vibrant",true);
                tap(a,"Frame",false);
                tap(a,"Crop",false);
                tap(a,"Teks",false);
                EditText field=(EditText)input(a.getWindow().getDecorView());
                assertNotNull("Caption input missing",field);
                field.setText("DRAFT 29-35");
                tap(a,"☷  Ekspor",false);
                tap(a,"1080p",false);
                tap(a,"AI Photo",false);
                Switch bokeh=(Switch)require(a,"Aktifkan AI Portrait Bokeh",true);
                bokeh.setChecked(true);
                bokeh.setChecked(false);
            });
            back();
        }
        StudioDraftStore.Draft saved=StudioDraftStore.load(target,source);
        assertNotNull("onPause did not commit draft",saved);
        assertEquals(2,saved.options.preset);
        assertEquals(AspectRatioProfile.CROP,saved.options.frameStyle);
        assertEquals(OutputProfile.FULL_HD,saved.options.qualityMode);
        assertEquals("DRAFT 29-35",saved.options.caption);
        assertFalse(saved.options.portraitBokeh);
        assertTrue("Original source must remain readable",SourceAccess.canRead(target,source));

        try(ActivityScenario<StudioHomeActivity> freshHome=
                ActivityScenario.launch(StudioHomeActivity.class)){
            freshHome.onActivity(a->tap(a,"Media Proyek 1",false));
            idle();
            assertNotNull("Recent must launch Editor from new Home Activity",resumedEditor());
            StudioDraftStore.Draft restored=StudioDraftStore.load(target,source);
            assertNotNull(restored);
            assertEquals(saved.options.caption,restored.options.caption);
            assertEquals(saved.options.preset,restored.options.preset);
            assertEquals(saved.options.frameStyle,restored.options.frameStyle);
            PhotoEngine.Result exported=PhotoEngine.process(target,source,restored.options);
            Bitmap image=BitmapFactory.decodeByteArray(exported.jpeg,0,exported.jpeg.length);
            assertNotNull("JPEG output must decode",image);
            try{
                assertEquals(1080,image.getWidth());
                assertEquals(1920,image.getHeight());
            }finally{image.recycle();}
            assertArrayEquals("Original media must stay unchanged",originalDigest,digest(source));
            back();
        }
    }

    @Test public void corruptDraftDeletedSourceAndClearRecentDoNotCrash() throws Exception {
        try(ActivityScenario<StudioHomeActivity> first=importWithSafResult()){
            back();
        }
        target.getSharedPreferences("studio-drafts",Context.MODE_PRIVATE).edit()
                .putString(StudioDraftStore.keyFor(source),"{broken-json").commit();
        assertNull("Corrupt state must not restore",StudioDraftStore.load(target,source));
        assertTrue("Local fixture must be accessible before deletion",SourceAccess.canRead(target,source));
        try(ActivityScenario<StudioHomeActivity> freshHome=
                ActivityScenario.launch(StudioHomeActivity.class)){
            freshHome.onActivity(a->tap(a,"Media Proyek 1",false));
            idle();
            assertNotNull("Corrupt draft must still allow Editor",resumedEditor());
            back();
            assertTrue("Delete fixture from MediaStore",target.getContentResolver().delete(source,null,null)>0);
            assertFalse("Deleted source must be unreadable",SourceAccess.canRead(target,source));
            freshHome.onActivity(a->tap(a,"Media Proyek 1",false));
            idle();
            assertNull("Deleted source must not open Editor",resumedEditor());
            freshHome.onActivity(a->tap(a,"BERSIHKAN",false));
            UiDevice device=UiDevice.getInstance(instrument);
            assertTrue("Clear-history confirmation dialog did not appear",
                device.wait(Until.hasObject(By.text("Bersihkan")),10000));
            UiObject2 confirm=device.findObject(By.text("Bersihkan"));
            assertNotNull("Clear-history confirmation button missing",confirm);
            confirm.click();
            idle();
            assertEquals("",target.getSharedPreferences("studio-projects",Context.MODE_PRIVATE)
                    .getString("recent_0",""));
            assertNull(StudioDraftStore.load(target,source));
        }
    }
}
