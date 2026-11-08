package com.wfs.storyboost;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Real platform URI grant/revocation between two different app UIDs.
 * Uses a test-owned ContentProvider and never MediaStore ownership.
 * Does not simulate a persistable DocumentsProvider grant.
 */
@RunWith(AndroidJUnit4.class)
public final class RevokedProviderGrantTest {
    private final Context owner=InstrumentationRegistry.getInstrumentation().getContext();
    private final Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();

    private static byte[] digestUri(Context context,Uri uri)throws Exception{
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        try(java.io.InputStream in=context.getContentResolver().openInputStream(uri)){
            assertNotNull(in);
            byte[] chunk=new byte[8192];
            for(int n;(n=in.read(chunk))!=-1;)sha.update(chunk,0,n);
        }
        return sha.digest();
    }
    private static View find(View view,String label){
        if(view instanceof TextView
                &&((TextView)view).getText().toString().contains(label))return view;
        if(view instanceof ViewGroup){
            ViewGroup g=(ViewGroup)view;
            for(int n=0;n<g.getChildCount();n++){
                View v=find(g.getChildAt(n),label);
                if(v!=null)return v;
            }
        }
        return null;
    }
    private static void click(Activity activity,String label){
        View view=find(activity.getWindow().getDecorView(),label);
        assertNotNull("Recent row not present: "+label,view);
        while(!view.isClickable()&&view.getParent() instanceof View)
            view=(View)view.getParent();
        assertTrue("Recent was not clickable",view.performClick());
    }
    private static boolean editorResumed(){
        for(Activity a:ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED))
            if(a instanceof StudioEditorActivity)return true;
        return false;
    }
    private void assertEditor(boolean expected)throws Exception {
        final boolean[] resumed=new boolean[1];
        for(int i=0;i<35;i++){
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    ()->resumed[0]=editorResumed());
            if(resumed[0]==expected)return;
            SystemClock.sleep(125);
        }
        assertEquals("Editor Activity state did not match URI access",expected,resumed[0]);
    }
    @Test public void androidUriGrantRevocationRejectsRecentAndRegrantRecovers()
            throws Exception {
        Uri uri=Uri.parse("content://"+RevocableQaProvider.AUTHORITY+
                "/document/fixture.jpg");
        int read=Intent.FLAG_GRANT_READ_URI_PERMISSION;
        byte[] original;
        assertNotEquals("Test provider must have a different UID",owner.getApplicationInfo().uid,
                target.getApplicationInfo().uid);
        try{
            assertFalse("Non-exported test provider unexpectedly readable without grant",
                    SourceAccess.canRead(target,uri));
            owner.getContentResolver().call(uri,"qa-grant",target.getPackageName(),null);
            assertTrue("Real platform URI grant not honored",SourceAccess.canRead(target,uri));
            original=digestUri(target,uri);
            ExportOptions options=new ExportOptions(ColorLooks.ORIGINAL,
                    AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true)
                    .withCaption("GRANT QA",Color.WHITE);
            assertTrue(StudioDraftStore.save(target,uri,options,-1,false));
            assertTrue(target.getSharedPreferences("studio-projects",Context.MODE_PRIVATE)
                    .edit().putString("recent_0",uri.toString()).commit());
            PhotoEngine.Result processed=PhotoEngine.process(target,uri,options);
            Bitmap exported=BitmapFactory.decodeByteArray(processed.jpeg,0,processed.jpeg.length);
            assertNotNull("Export from granted provider did not decode",exported);
            exported.recycle();

            owner.getContentResolver().call(uri,"qa-revoke",target.getPackageName(),null);
            assertFalse("Revocation must prevent source read via target app",
                    SourceAccess.canRead(target,uri));
            try(ActivityScenario<StudioHomeActivity> home=
                        ActivityScenario.launch(StudioHomeActivity.class)){
                home.onActivity(a->click(a,"Media Proyek 1"));
                assertEditor(false);
                assertNotNull("Access revocation must preserve non-destructive draft",
                        StudioDraftStore.load(target,uri));
                // Recover without re-import and verify the same original.
                owner.getContentResolver().call(uri,"qa-grant",target.getPackageName(),null);
                assertTrue(SourceAccess.canRead(target,uri));
                home.onActivity(a->click(a,"Media Proyek 1"));
                assertEditor(true);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                    for(Activity a:ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED))
                        if(a instanceof StudioEditorActivity)a.finish();
                });
            }
            assertArrayEquals("Provider's original bytes were changed",original,digestUri(target,uri));
        }finally{
            owner.getContentResolver().call(uri,"qa-revoke",target.getPackageName(),null);
            target.getSharedPreferences("studio-projects",Context.MODE_PRIVATE)
                    .edit().clear().commit();
            StudioDraftStore.clearAll(target);

        }
    }
}
