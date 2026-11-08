package com.wfs.storyboost;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Real Activity/UI-instrumentation interactions with an actual MediaStore JPEG.
 * SAF return is simulated through its ActivityResult callback, not through the
 * device file picker. The picker UI itself remains a manual QA requirement.
 */
@RunWith(AndroidJUnit4.class)
public final class StudioFlowInstrumentedTest {
    private Context app;
    private Instrumentation instrumentation;
    private Uri source;
    private byte[] original;

    @Before public void setup() throws Exception {
        instrumentation=InstrumentationRegistry.getInstrumentation();
        app=instrumentation.getTargetContext();
        app.getSharedPreferences("studio-projects",Context.MODE_PRIVATE).edit().clear().commit();
        StudioDraftStore.clearAll(app);
        Bitmap bitmap=Bitmap.createBitmap(384,384,Bitmap.Config.ARGB_8888);
        for(int y=0;y<384;y++)for(int x=0;x<384;x++)
            bitmap.setPixel(x,y,Color.rgb(30+x/2,30+y/2,150));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try{
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,94,bytes));
            original=bytes.toByteArray();
        }finally{bitmap.recycle();}
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "wfs_qa_v30_"+System.nanoTime()+".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        if(Build.VERSION.SDK_INT>=29){
            values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/WFS StoryBoost QA");
            values.put(MediaStore.Images.Media.IS_PENDING,1);
        }
        source=app.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(source);
        try(OutputStream out=app.getContentResolver().openOutputStream(source)){
            assertNotNull(out);out.write(original);
        }
        if(Build.VERSION.SDK_INT>=29){
            ContentValues ready=new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING,0);
            app.getContentResolver().update(source,ready,null,null);
        }
    }
    @After public void cleanup(){
        if(source!=null)try{app.getContentResolver().delete(source,null,null);}
        catch(Exception ignored){}
        StudioDraftStore.clearAll(app);
        app.getSharedPreferences("studio-projects",Context.MODE_PRIVATE).edit().clear().commit();
    }
    private static View find(View root,String needle,boolean description){
        if(root==null)return null;
        CharSequence value=description?root.getContentDescription():
                root instanceof TextView?((TextView)root).getText():null;
        if(value!=null&&value.toString().contains(needle))return root;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++){
                View match=find(group.getChildAt(i),needle,description);
                if(match!=null)return match;
            }
        }
        return null;
    }
    private static Button findButton(View root,String title){
        if(root instanceof Button){
            String text=((Button)root).getText().toString();
            if(text.equals(title)||text.contains(title))return (Button)root;
        }
        if(root instanceof ViewGroup){
            ViewGroup g=(ViewGroup)root;
            for(int i=0;i<g.getChildCount();i++){
                Button button=findButton(g.getChildAt(i),title);
                if(button!=null)return button;
            }
        }
        return null;
    }
    private static void click(Activity a,String text){
        Button view=findButton(a.getWindow().getDecorView(),text);
        assertNotNull("UI button missing: "+text,view);
        assertTrue("UI button inactive: "+text,view.performClick());
    }
    private static ExportOptions snapshot(StudioEditorActivity activity)throws Exception{
        Method m=StudioEditorActivity.class.getDeclaredMethod("options");
        m.setAccessible(true);
        return (ExportOptions)m.invoke(activity);
    }
    @Test public void realEditorUiRestoresSettingsAndExportsJpeg() throws Exception{
        // Genuine Activity UI, actual MediaStore bytes, and a returned picker
        // result. This intentionally does NOT claim automation of DocumentsUI.
        try(ActivityScenario<StudioHomeActivity> home=
                ActivityScenario.launch(new Intent(app,StudioHomeActivity.class))){
            Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(
                    StudioEditorActivity.class.getName(),null,false);
            try{
                home.onActivity(h->h.onActivityResult(802,Activity.RESULT_OK,
                        new Intent().setData(source).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)));
                Activity launched=instrumentation.waitForMonitorWithTimeout(monitor,12000);
                assertTrue("SAF callback must launch editor",launched instanceof StudioEditorActivity);
                StudioEditorActivity editor=(StudioEditorActivity)launched;
                instrumentation.runOnMainSync(()->{
                    View natural=find(editor.getWindow().getDecorView(),
                            "Color Look: Natural",true);
                    assertNotNull("Natural look tile",natural);
                    assertTrue(natural.performClick());
                    click(editor,"Rasio");
                    click(editor,"4:5");
                    click(editor,"Frame");
                    click(editor,"Crop");
                    click(editor,"Teks");
                    // Text hint is not .getText(), so find the EditText recursively.
                    EditText caption=findEdit(editor.getWindow().getDecorView());
                    assertNotNull("caption editor",caption);
                    caption.setText("WFS QA persist");
                    click(editor,"AI Photo");
                    View ai=find(editor.getWindow().getDecorView(),"Aktifkan AI Denoise DnCNN",true);
                    assertTrue(ai instanceof Switch);
                    ((Switch)ai).setChecked(true);
                });
                instrumentation.runOnMainSync(editor::finish);
                instrumentation.waitForIdleSync();
                StudioDraftStore.Draft persisted=StudioDraftStore.load(app,source);
                assertNotNull("real onPause must commit draft",persisted);
                assertEquals(0,persisted.options.preset);
                assertEquals(AspectRatioProfile.PORTRAIT_4_5,persisted.options.ratio);
                assertEquals(AspectRatioProfile.CROP,persisted.options.frameStyle);
                assertEquals("WFS QA persist",persisted.options.caption);
                assertTrue(persisted.options.aiDenoise);
                home.onActivity(h->{
                    View recent=find(h.getWindow().getDecorView(),
                            "Buka draft edit tersimpan",false);
                    assertNotNull("Recent projects entry",recent);
                    assertTrue(recent.getParent() instanceof View);
                    assertTrue(((View)((View)recent.getParent()).getParent()).performClick());
                });
                Activity reopened=instrumentation.waitForMonitorWithTimeout(monitor,12000);
                assertTrue(reopened instanceof StudioEditorActivity);
                StudioEditorActivity restored=(StudioEditorActivity)reopened;
                final ExportOptions[] options=new ExportOptions[1];
                instrumentation.runOnMainSync(()->{
                    try{options[0]=snapshot(restored);}
                    catch(Exception e){throw new AssertionError(e);}
                });
                assertEquals(persisted.options.preset,options[0].preset);
                assertEquals(persisted.options.ratio,options[0].ratio);
                assertEquals(persisted.options.frameStyle,options[0].frameStyle);
                assertEquals(persisted.options.caption,options[0].caption);
                assertEquals(persisted.options.aiDenoise,options[0].aiDenoise);
                PhotoEngine.Result actual=PhotoEngine.process(app,source,options[0]);
                Bitmap image=BitmapFactory.decodeByteArray(actual.jpeg,0,actual.jpeg.length);
                assertNotNull("real export must be valid JPEG",image);
                try{
                    AspectRatioProfile.Size expected=AspectRatioProfile.resolve(
                            options[0].ratio,options[0].qualityMode,384,384,
                            options[0].frameStyle,options[0].customWidth,options[0].customHeight);
                    assertEquals(expected.width,image.getWidth());
                    assertEquals(expected.height,image.getHeight());
                }finally{image.recycle();}
                byte[] after;
                try(InputStream stream=app.getContentResolver().openInputStream(source)){
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    byte[] buf=new byte[4096];int n;
                    while((n=stream.read(buf))!=-1)bytes.write(buf,0,n);
                    after=bytes.toByteArray();
                }
                assertArrayEquals("source media must remain byte-identical",original,after);
                instrumentation.runOnMainSync(restored::finish);
            }finally{instrumentation.removeMonitor(monitor);}
        }
    }
    private static EditText findEdit(View v){
        if(v instanceof EditText)return (EditText)v;
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                EditText found=findEdit(g.getChildAt(i));
                if(found!=null)return found;
            }
        }
        return null;
    }
    @Test public void corruptedDraftIsRejectedAndCleared()throws Exception{
        ExportOptions a=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.ORIGINAL,AspectRatioProfile.FIT,true);
        assertTrue(StudioDraftStore.save(app,source,a,-1,false));
        String key=StudioDraftStore.keyFor(source);
        app.getSharedPreferences("studio-drafts",Context.MODE_PRIVATE).edit()
                .putString(key,"{\"v\":1,\"preset\":4,\"ratio\":4,\"frame\":99}").commit();
        assertNull("invalid crop mode must be rejected",StudioDraftStore.load(app,source));
        assertTrue(StudioDraftStore.save(app,source,a,-1,false));
        assertNotNull(StudioDraftStore.load(app,source));
        StudioDraftStore.clearAll(app);
        assertNull(StudioDraftStore.load(app,source));
    }
    @Test public void deletedMediaDoesNotOpenEditor()throws Exception{
        app.getContentResolver().delete(source,null,null);
        try(ActivityScenario<StudioHomeActivity> home=
                ActivityScenario.launch(new Intent(app,StudioHomeActivity.class))){
            home.onActivity(h->{
                h.getSharedPreferences("studio-projects",Context.MODE_PRIVATE).edit()
                        .putString("recent_0",source.toString()).commit();
            });
            home.recreate();
            home.onActivity(h->{
                View recent=find(h.getWindow().getDecorView(),
                        "Buka draft edit tersimpan",false);
                assertNotNull(recent);
                assertTrue(((View)((View)recent.getParent()).getParent()).performClick());
            });
            instrumentation.waitForIdleSync();
            // An AlertDialog displays a recoverable error; Home remains alive.
            home.onActivity(h->assertFalse("Home must survive revoked URI",
                    h.isFinishing()));
        }
    }
}
