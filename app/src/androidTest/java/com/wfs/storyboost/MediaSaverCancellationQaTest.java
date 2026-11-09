package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real Android MediaStore abort tests; no fake ContentResolver and no
 * personal media. Verify deletion of IS_PENDING rows too, not only public
 * gallery-visible documents. These synthetic bytes never leave the device.
 */
@RunWith(AndroidJUnit4.class)
public final class MediaSaverCancellationQaTest {
    private final Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static String uniqueName(String ext){
        return "wfs_cancel_qa_"+android.os.Process.myPid()+"_"+
                System.nanoTime()+"."+ext;
    }
    private static byte[] digest(File file)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(java.io.InputStream in=new java.io.FileInputStream(file)){
            byte[] buf=new byte[8192];
            for(int n;(n=in.read(buf))!=-1;)md.update(buf,0,n);
        }
        return md.digest();
    }
    private void assertNoRows(Uri collection,String name)throws Exception{
        Uri includingPending=MediaStore.setIncludePending(collection);
        try(android.database.Cursor cursor=ctx.getContentResolver().query(
                includingPending,new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME+"=?",new String[]{name},null)){
            assertNotNull("MediaStore query failed",cursor);
            assertEquals("A cancelled output still has a pending/published MediaStore row: "+name,
                    0,cursor.getCount());
        }
    }

    @Test public void videoCopyCancellationDeletesEvenHiddenPendingMediaStoreRow()
            throws Exception {
        File source=File.createTempFile("qa-cancel-video-", ".bin",ctx.getCacheDir());
        try{
            // Long enough to cross several 128KiB chunks; no MPEG decoding is
            // used because this specifically tests atomic MediaStore writing.
            byte[] sourceBytes=new byte[650_000];
            new Random(7050705L).nextBytes(sourceBytes);
            try(FileOutputStream out=new FileOutputStream(source)){
                out.write(sourceBytes);
            }
            byte[] initial=digest(source);
            String name=uniqueName("mp4");
            AtomicInteger polls=new AtomicInteger();
            try{
                MediaSaver.saveVideo(ctx,source,name,()->polls.incrementAndGet()>=4);
                fail("Cancellation after first copied chunk did not abort video save");
            }catch(IOException expected){
                assertTrue("Cancellation did not interrupt a partial copy",polls.get()>=4);
            }
            assertArrayEquals("Input bytes changed during cancelled gallery write",
                    initial,digest(source));
            assertNoRows(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,name);
            android.util.Log.i("WfsMediaSaveCancelQA",
                    "PASS video cancellation after chunk copies; no pending rows; source SHA intact");
        }finally{assertTrue("Could not remove synthetic local fixture",source.delete());}
    }

    @Test public void photoCancellationAfterWritePreventsGalleryPublication()throws Exception{
        byte[] syntheticJpegLikeBytes=new byte[240_000];
        new Random(314159L).nextBytes(syntheticJpegLikeBytes);
        byte[] original=syntheticJpegLikeBytes.clone();
        String name=uniqueName("jpg");
        AtomicInteger polls=new AtomicInteger();
        try{
            MediaSaver.savePhoto(ctx,syntheticJpegLikeBytes,name,
                    ()->polls.incrementAndGet()>=3);
            fail("Gallery photo save ignored cancellation before publish");
        }catch(IOException expected){
            assertEquals("Unexpected cancellation boundary",3,polls.get());
        }
        assertArrayEquals("Cancellation modified caller's JPEG bytes",
                original,syntheticJpegLikeBytes);
        assertNoRows(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,name);
        android.util.Log.i("WfsMediaSaveCancelQA",
                "PASS photo cancellation before publish; no pending rows; unchanged source");
    }

    @Test public void cancelledBeforeInsertNeverCreatesGalleryRow()throws Exception{
        File source=File.createTempFile("qa-cancel-before-", ".bin",ctx.getCacheDir());
        try{
            byte[] original=new byte[]{0,1,2,3,4,5};
            try(FileOutputStream out=new FileOutputStream(source)){out.write(original);}
            String name=uniqueName("mp4");
            try{
                MediaSaver.saveVideo(ctx,source,name,()->true);
                fail("Pre-insert cancellation allowed a gallery write");
            }catch(IOException expected){}
            assertNoRows(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,name);
            assertTrue(Arrays.equals(original,java.nio.file.Files.readAllBytes(source.toPath())));
        }finally{assertTrue(source.delete());}
    }
}
