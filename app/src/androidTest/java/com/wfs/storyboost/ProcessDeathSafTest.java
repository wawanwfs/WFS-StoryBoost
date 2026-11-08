package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.SystemClock;
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
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Two separate instrumentation invocations, separated by an EXTERNAL adb
 * "am force-stop" in scripts/qa-process-death-saf.sh.
 *
 * No Espresso Intent stubbing is used: ACTION_OPEN_DOCUMENT launches actual
 * DocumentsUI. Only test-generated JPEG bytes are written to Downloads.
 * During the normal Gradle connected test suite both methods are skipped; the
 * shell runner invokes each by name with -e wfs.phase stage / verify.
 */
@RunWith(AndroidJUnit4.class)
public class ProcessDeathSafTest {
    private static final String PREF="wfs-beta3-process-qa";
    private static final String PREFIX="wfs_beta3_picker_";
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private Context app(){return inst.getTargetContext();}
    private void phase(String expected){
        Bundle args=InstrumentationRegistry.getArguments();
        Assume.assumeTrue("Must run via two-phase adb QA runner",
                expected.equals(args.getString("wfs.phase")));
    }
    private static String sha(Context c,Uri uri) throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            assertNotNull(in);
            byte[] bytes=new byte[16384];
            for(int n;(n=in.read(bytes))!=-1;)md.update(bytes,0,n);
        }
        return Arrays.toString(md.digest());
    }
    private static View find(View view,String text,boolean desc){
        if(view==null)return null;
        if(desc&&view.getContentDescription()!=null
                &&view.getContentDescription().toString().contains(text))return view;
        if(!desc&&view instanceof TextView
                &&((TextView)view).getText().toString().contains(text))return view;
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++){
                View child=find(group.getChildAt(i),text,desc);
                if(child!=null)return child;
            }
        }
        return null;
    }
    private static void tap(Activity a,String name,boolean desc){
        View child=find(a.getWindow().getDecorView(),name,desc);
        assertNotNull("Button not found: "+name,child);
        while(child!=null&&!child.isClickable()){
            if(!(child.getParent() instanceof View))break;
            child=(View)child.getParent();
        }
        assertNotNull(child);
        assertTrue("Unable to tap "+name,child.performClick());
    }
    private StudioEditorActivity waitForEditor() {
        long deadline=SystemClock.uptimeMillis()+15000;
        do {
            AtomicReference<StudioEditorActivity> found=new AtomicReference<>();
            inst.runOnMainSync(()->{
                Collection<Activity> resumed=ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                for(Activity a:resumed)if(a instanceof StudioEditorActivity)
                    found.set((StudioEditorActivity)a);
            });
            if(found.get()!=null)return found.get();
            SystemClock.sleep(240);
            inst.waitForIdleSync();
        }while(SystemClock.uptimeMillis()<deadline);
        return null;
    }
    private Uri createDownload(String filename) throws Exception {
        ContentValues values=new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME,filename);
        values.put(MediaStore.Downloads.MIME_TYPE,"image/jpeg");
        values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/");
        values.put(MediaStore.Downloads.IS_PENDING,1);
        Uri uri=app().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        assertNotNull("Cannot create synthetic DocumentsUI fixture in Downloads",uri);
        Bitmap image=Bitmap.createBitmap(640,960,Bitmap.Config.ARGB_8888);
        try{
            for(int y=0;y<960;y++)for(int x=0;x<640;x++)
                image.setPixel(x,y,Color.rgb((x*7)%256,(y*3)%256,((x+y)*2)%256));
            try(OutputStream out=app().getContentResolver().openOutputStream(uri)){
                assertNotNull(out);
                assertTrue(image.compress(Bitmap.CompressFormat.JPEG,94,out));
            }
        }finally{image.recycle();}
        ContentValues published=new ContentValues();
        published.put(MediaStore.Downloads.IS_PENDING,0);
        assertEquals("Fixture did not publish into Downloads",1,
                app().getContentResolver().update(uri,published,null,null));
        return uri;
    }
    private boolean clickStable(UiDevice device,androidx.test.uiautomator.BySelector match){
        for(int attempt=0;attempt<5;attempt++){
            try{
                UiObject2 object=device.wait(Until.findObject(match),2000);
                if(object!=null){
                    object.click();
                    return true;
                }
            }catch(androidx.test.uiautomator.StaleObjectException stale){
                // DocumentsUI may replace its accessibility tree while opening roots.
            }
            SystemClock.sleep(350);
        }
        return false;
    }
    private String pickerDiagnostics(UiDevice device){
        StringBuilder text=new StringBuilder();
        for(UiObject2 item:device.findObjects(By.clazz("android.widget.TextView"))){
            String value=item.getText();
            if(value!=null&&text.length()<1600)text.append('[').append(value).append(']');
        }
        for(UiObject2 item:device.findObjects(By.clazz("android.widget.Button"))){
            try{
                String value=item.getText();
                if(value!=null&&text.length()<2000)text.append('{').append(value).append('}');
            }catch(androidx.test.uiautomator.StaleObjectException ignored){}
        }
        for(UiObject2 item:device.findObjects(By.pkg("com.google.android.documentsui"))){
            try{
                String desc=item.getContentDescription();
                if(desc!=null&&text.length()<3500)text.append("<").append(desc).append(">");
            }catch(androidx.test.uiautomator.StaleObjectException ignored){}
        }
        for(UiObject2 item:device.findObjects(By.pkg("com.android.documentsui"))){
            try{
                String desc=item.getContentDescription();
                if(desc!=null&&text.length()<4200)text.append("<").append(desc).append(">");
            }catch(androidx.test.uiautomator.StaleObjectException ignored){}
        }
        return text.toString();
    }
    private androidx.test.uiautomator.BySelector fixtureFile(){
        // The row accessibility name begins with the filename:
        // "wfs_beta3_picker_....jpg, 125 kB, ...". Do NOT match the
        // "Preview the file ..." control, which opens a preview, not a URI.
        return By.desc(java.util.regex.Pattern.compile(
                "^"+PREFIX+"[^,]*[.]jpg(?:,.*)?$"));
    }
    private androidx.test.uiautomator.BySelector fixtureText(){
        return By.clazz("android.widget.TextView").text(
                java.util.regex.Pattern.compile(".*"+PREFIX+".*"));
    }
    private UiObject2 findFixture(UiDevice device,long timeout){
        UiObject2 image=device.wait(Until.findObject(fixtureFile()),timeout/2);
        return image==null?device.wait(Until.findObject(fixtureText()),timeout/2):image;
    }
    private UiObject2 locateDownload(UiDevice device,String filename) {
        UiObject2 file=findFixture(device,4000);
        if(file!=null)return file;
        // Real DocumentsUI may open on Recent or Photos. Select Downloads.
        UiObject2 nav=device.findObject(By.descContains("Show roots"));
        if(nav==null)nav=device.findObject(By.descContains("Show navigation drawer"));
        if(nav!=null)clickStable(device,By.descContains("Show roots"));
        boolean downloads=clickStable(device,By.text("Downloads"));
        if(!downloads)downloads=clickStable(device,By.text("Download"));
        assertTrue("DocumentsUI Downloads root not clickable. UI: "+
                pickerDiagnostics(device),downloads);
        file=findFixture(device,9000);
        if(file==null){
            UiObject2 search=device.findObject(By.descContains("Search"));
            if(search!=null){
                clickStable(device,By.descContains("Search"));
                UiObject2 input=device.wait(Until.findObject(By.clazz("android.widget.EditText")),4500);
                if(input!=null){
                    input.setText(PREFIX);
                    device.pressEnter();
                    file=findFixture(device,8000);
                }
            }
        }
        assertNotNull("Synthetic JPEG "+filename+" missing. DocumentsUI: "+
                pickerDiagnostics(device),file);
        return file;
    }
    @Test public void stageRealDocumentsUiImportAndDraft() throws Exception {
        phase("stage");
        String file=PREFIX+System.nanoTime()+".jpg";
        Uri synthetic=createDownload(file);
        String original=sha(app(),synthetic);
        UiDevice device=UiDevice.getInstance(inst);
        try(ActivityScenario<StudioHomeActivity> home=ActivityScenario.launch(StudioHomeActivity.class)){
            home.onActivity(a->tap(a,"Photo Editor",false));
            assertTrue("Actual picker UI did not open",
                    device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")),5000)
                    ||device.wait(Until.hasObject(By.pkg("com.android.documentsui")),5000)
                    ||device.wait(Until.hasObject(By.text("Recent")),5000));
            UiObject2 target=locateDownload(device,file);
            assertNotNull("Synthetic JPEG not visible in real DocumentsUI",target);
            assertTrue("DocumentsUI file selection was not clickable",
                    clickStable(device,fixtureFile())||clickStable(device,fixtureText()));
            inst.waitForIdleSync();
            StudioEditorActivity editor=waitForEditor();
            assertNotNull("Real picker result did not launch app Editor. Picker UI: "+
                    pickerDiagnostics(device)+"; recent="+app().getSharedPreferences(
                    "studio-projects",Context.MODE_PRIVATE).getString("recent_0",""),
                    editor);
            inst.runOnMainSync(()->{
                tap(editor,"Color Look: Vibrant",true);
                tap(editor,"Teks",false);
                EditText input=null;
                java.util.ArrayDeque<View> queue=new java.util.ArrayDeque<>();
                queue.add(editor.getWindow().getDecorView());
                while(!queue.isEmpty()){
                    View v=queue.removeFirst();
                    if(v instanceof EditText){input=(EditText)v;break;}
                    if(v instanceof ViewGroup){
                        ViewGroup g=(ViewGroup)v;
                        for(int i=0;i<g.getChildCount();i++)queue.addLast(g.getChildAt(i));
                    }
                }
                assertNotNull("Caption field missing",input);
                input.setText("SAF FORCE STOP");
                tap(editor,"☷  Ekspor",false);
                tap(editor,"1080p",false);
                editor.finish();
            });
            inst.waitForIdleSync();
        }
        String selected=app().getSharedPreferences("studio-projects",Context.MODE_PRIVATE)
                .getString("recent_0","");
        assertTrue("App did not persist document URI",selected.startsWith("content://"));
        Uri picked=Uri.parse(selected);
        assertTrue("SAF read grant missing before force stop",SourceAccess.canRead(app(),picked));
        StudioDraftStore.Draft saved=StudioDraftStore.load(app(),picked);
        assertNotNull("Activity.onPause did not save picked-document draft",saved);
        assertEquals("SAF FORCE STOP",saved.options.caption);
        assertEquals(2,saved.options.preset);
        assertEquals(OutputProfile.FULL_HD,saved.options.qualityMode);
        assertEquals(original,sha(app(),picked));
        // The picked DocumentsProvider URI (and its SHA256) is the contract
        // under test. Its original MediaStore staging URI can be transient.
        assertTrue(app().getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                .putString("document",selected).putString("fixture",synthetic.toString())
                .putString("sha",original).putInt("pid",Process.myPid())
                .putString("caption",saved.options.caption).commit());
    }
    @Test public void verifyAfterExternalForceStopAndReopenRecent() throws Exception {
        phase("verify");
        android.content.SharedPreferences record=app().getSharedPreferences(PREF,Context.MODE_PRIVATE);
        String doc=record.getString("document",null);
        assertNotNull("No first-process stage record",doc);
        Uri selected=Uri.parse(doc);
        assertNotEquals("Process did not restart after external force-stop",
                record.getInt("pid",-1),Process.myPid());
        assertTrue("Persisted SAF grant failed in NEW process",SourceAccess.canRead(app(),selected));
        assertEquals("Original fixture SHA changed across restart",record.getString("sha",""),
                sha(app(),selected));
        try(ActivityScenario<StudioHomeActivity> home=ActivityScenario.launch(StudioHomeActivity.class)){
            home.onActivity(a->tap(a,"Media Proyek 1",false));
            inst.waitForIdleSync();
            StudioEditorActivity editor=waitForEditor();
            assertNotNull("Recent did not reopen after real process force-stop",editor);
            StudioDraftStore.Draft state=StudioDraftStore.load(app(),selected);
            assertNotNull(state);
            assertEquals("SAF FORCE STOP",state.options.caption);
            assertEquals(2,state.options.preset);
            assertEquals(OutputProfile.FULL_HD,state.options.qualityMode);
            PhotoEngine.Result export=PhotoEngine.process(app(),selected,state.options);
            Bitmap decoded=BitmapFactory.decodeByteArray(export.jpeg,0,export.jpeg.length);
            assertNotNull("Real restored output is not a decodable JPEG",decoded);
            try{
                assertEquals(1080,decoded.getWidth());
                assertEquals(1920,decoded.getHeight());
            }finally{decoded.recycle();}
            assertEquals(record.getString("sha",""),sha(app(),selected));
            inst.runOnMainSync(editor::finish);
        }
        String fixture=record.getString("fixture",null);
        if(fixture!=null)try{
            app().getContentResolver().delete(Uri.parse(fixture),null,null);
        }catch(Exception ignored){}
        record.edit().clear().commit();
        app().getSharedPreferences("studio-projects",Context.MODE_PRIVATE).edit().clear().commit();
        StudioDraftStore.clearAll(app());
    }
}
