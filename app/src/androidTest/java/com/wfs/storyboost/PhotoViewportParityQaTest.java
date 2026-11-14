package com.wfs.storyboost;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** Synthetic color markers, real editor gestures and decoded production JPEG. */
@RunWith(AndroidJUnit4.class)
public class PhotoViewportParityQaTest {
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private final Context app=inst.getTargetContext();
    private static final String TITLE="STORY FRAME PREVIEW AND EXPORT";
    private static final int GOLD=0xffffd786;

    @Test(timeout=100000) public void cropGestureAndSocialCaptionAgreeWithExport() throws Exception {
        Bundle args=InstrumentationRegistry.getArguments();
        String expected=args.getString("wfs.viewport");
        Assume.assumeTrue("Run only with explicitly configured screen dimensions",expected!=null);
        UiDevice device=UiDevice.getInstance(inst);
        assertEquals(expected,device.getDisplayWidth()+"x"+device.getDisplayHeight());
        int dpi=app.getResources().getDisplayMetrics().densityDpi;
        assertEquals(Integer.parseInt(args.getString("wfs.density")),dpi);
        Uri source=makeFixture();
        byte[] original=sha(source);
        String draftKey=StudioDraftStore.keyFor(source);
        try {
            ExportOptions initial=new ExportOptions(ColorLooks.ORIGINAL,AspectRatioProfile.PORTRAIT_9_16,
                    AspectRatioProfile.CROP,true,0,0,.5f,.5f)
                    .withSocialProfile(SocialExportProfile.INSTAGRAM_STORY)
                    .withCaption(TITLE,GOLD);
            assertTrue(StudioDraftStore.save(app,source,initial,-1,false));
            Intent launch=new Intent(app,StudioEditorActivity.class)
                    .putExtra("media_uri",source.toString()).putExtra("resume_project",true);
            try(ActivityScenario<StudioEditorActivity> scenario=ActivityScenario.launch(launch)){
                Bitmap centered=waitForPreview(scenario,false);
                centered.recycle();
                scenario.onActivity(a->{
                    View crop=find(a.getWindow().getDecorView(),
                            v->"Drag to reposition image crop focus".contentEquals(
                                    v.getContentDescription()==null?"":v.getContentDescription()));
                    assertNotNull(crop);
                    long time=SystemClock.uptimeMillis();
                    touch(crop,time,MotionEvent.ACTION_DOWN,.85f);
                    touch(crop,time,MotionEvent.ACTION_MOVE,.20f);
                    touch(crop,time,MotionEvent.ACTION_UP,.20f);
                });
                StudioDraftStore.Draft edited=StudioDraftStore.load(app,source);
                assertNotNull(edited);
                assertTrue("Gesture did not move crop",edited.options.focusX>.9f);
                Bitmap preview=waitForPreview(scenario,true);
                PhotoEngine.Result jpeg=PhotoEngine.process(app,source,edited.options);
                Bitmap decoded=BitmapFactory.decodeByteArray(jpeg.jpeg,0,jpeg.jpeg.length);
                assertNotNull(decoded);
                try{
                    assertEquals(1080,decoded.getWidth());
                    assertEquals(1920,decoded.getHeight());
                    assertTrue("Output selected wrong source stripe",blueAtCenter(decoded));
                    Rect a=captionBounds(preview),b=captionBounds(decoded);
                    assertCaptionInsideSafeZone(a,preview);
                    assertCaptionInsideSafeZone(b,decoded);
                    assertEquals("Caption start x",b.left/(float)decoded.getWidth(),a.left/(float)preview.getWidth(),.065f);
                    assertEquals("Caption end x",b.right/(float)decoded.getWidth(),a.right/(float)preview.getWidth(),.065f);
                    assertEquals("Caption start y",b.top/(float)decoded.getHeight(),a.top/(float)preview.getHeight(),.035f);
                    assertEquals("Caption end y",b.bottom/(float)decoded.getHeight(),a.bottom/(float)preview.getHeight(),.035f);
                    android.util.Log.i("WfsViewportQA","viewport="+expected+" dpi="+dpi+
                            " canvas="+preview.getWidth()+"x"+preview.getHeight()+
                            " focus="+edited.options.focusX+" previewCaption="+a+" jpegCaption="+b);
                }finally{preview.recycle();decoded.recycle();}

                scenario.onActivity(a->{
                    tap(a,"Frame");
                    tap(a,"X:");
                });
                UiObject2 slider=device.wait(Until.findObject(By.desc("Fokus crop horizontal")),5000);
                assertNotNull("Accessibility tree does not expose named crop slider",slider);
                // Verify actual accessibility action, not a mock or touch injection.
                android.app.UiAutomation automation=inst.getUiAutomation();
                AccessibilityNodeInfo root=automation.getRootInActiveWindow();
                assertNotNull(root);
                AccessibilityNodeInfo node=findNode(root,"Fokus crop horizontal");
                assertNotNull(node);
                Bundle progress=new Bundle();
                progress.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,25f);
                assertTrue(node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),progress));
                UiObject2 apply=device.wait(Until.findObject(By.res("android","button1")),5000);
                assertNotNull(apply);
                apply.click();
                inst.waitForIdleSync();
                assertEquals(.25f,StudioDraftStore.load(app,source).options.focusX,.015f);
                assertArrayEquals(original,sha(source));
            }
        }finally{
            app.getSharedPreferences("studio-drafts",Context.MODE_PRIVATE).edit().remove(draftKey).commit();
            app.getContentResolver().delete(source,null,null);
        }
    }

    private Uri makeFixture() throws Exception {
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,"wfs-beta10-stripes-"+System.nanoTime()+".jpg");
        values.put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/WFS-StoryBoost-QA");
        values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=app.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(uri);
        Bitmap bmp=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas=new Canvas(bmp);
            canvas.drawColor(0xff102035);
            canvas.clipRect(600,0,960,540);
            canvas.drawColor(0xff173dc8);
            try(OutputStream out=app.getContentResolver().openOutputStream(uri)){
                assertNotNull(out);
                assertTrue(bmp.compress(Bitmap.CompressFormat.JPEG,98,out));
            }
            ContentValues publish=new ContentValues();
            publish.put(MediaStore.MediaColumns.IS_PENDING,0);
            assertEquals(1,app.getContentResolver().update(uri,publish,null,null));
            return uri;
        }catch(Exception|AssertionError e){
            app.getContentResolver().delete(uri,null,null);
            throw e;
        }finally{bmp.recycle();}
    }

    private byte[] sha(Uri uri) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=app.getContentResolver().openInputStream(uri)){
            assertNotNull(in);
            byte[] buf=new byte[8192];
            for(int n;(n=in.read(buf))!=-1;)digest.update(buf,0,n);
        }
        return digest.digest();
    }

    private static View find(View v,Predicate<View> test){
        if(test.test(v))return v;
        if(v instanceof ViewGroup){
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++){
                View found=find(group.getChildAt(i),test);
                if(found!=null)return found;
            }
        }
        return null;
    }

    private static void tap(StudioEditorActivity a,String label){
        View control=find(a.getWindow().getDecorView(),
                v->v instanceof Button&&((Button)v).getText().toString().contains(label));
        assertNotNull("Missing control "+label,control);
        assertTrue(control.performClick());
    }

    private Bitmap waitForPreview(ActivityScenario<StudioEditorActivity> scenario,boolean blue){
        long deadline=SystemClock.uptimeMillis()+10000;
        do{
            AtomicReference<Bitmap> out=new AtomicReference<>();
            scenario.onActivity(a->{
                View image=find(a.getWindow().getDecorView(),v->v instanceof ImageView
                        &&"Pratinjau foto".contentEquals(v.getContentDescription()==null?"":v.getContentDescription()));
                if(!(image instanceof ImageView)||((ImageView)image).getDrawable()==null)return;
                View canvas=(View)image.getParent();
                if(canvas.getWidth()<30||canvas.getHeight()<60)return;
                Bitmap frame=Bitmap.createBitmap(canvas.getWidth(),canvas.getHeight(),Bitmap.Config.ARGB_8888);
                canvas.draw(new Canvas(frame));
                if(blueAtCenter(frame)==blue)out.set(frame);else frame.recycle();
            });
            if(out.get()!=null)return out.get();
            SystemClock.sleep(80);
        }while(SystemClock.uptimeMillis()<deadline);
        throw new AssertionError("Preview did not render requested crop focus");
    }

    private static boolean blueAtCenter(Bitmap image){
        int pixel=image.getPixel(image.getWidth()/2,image.getHeight()/3);
        return Color.blue(pixel)>130&&Color.blue(pixel)-Color.red(pixel)>80;
    }

    private static Rect captionBounds(Bitmap image){
        Rect result=new Rect();
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int pixel=image.getPixel(x,y);
            if(Color.red(pixel)>165&&Color.green(pixel)>125&&Color.blue(pixel)<190){
                if(result.isEmpty())result.set(x,y,x+1,y+1);
                else result.union(x,y,x+1,y+1);
            }
        }
        assertFalse("No caption pixels",result.isEmpty());
        return result;
    }

    private static void assertCaptionInsideSafeZone(Rect rect,Bitmap img){
        SocialExportProfile.Profile profile=SocialExportProfile.get(SocialExportProfile.INSTAGRAM_STORY);
        assertTrue(rect.toShortString(),rect.top>=img.getHeight()*profile.safeTop
                &&rect.bottom<=img.getHeight()*(1f-profile.safeBottom));
    }

    private static void touch(View v,long now,int type,float x){
        MotionEvent e=MotionEvent.obtain(now,now,type,v.getWidth()*x,v.getHeight()*.4f,0);
        try{assertTrue(v.dispatchTouchEvent(e));}finally{e.recycle();}
    }

    private static AccessibilityNodeInfo findNode(AccessibilityNodeInfo n,String description){
        if(n==null)return null;
        if(description.equals(String.valueOf(n.getContentDescription())))return n;
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo found=findNode(n.getChild(i),description);
            if(found!=null)return found;
        }
        return null;
    }
}
