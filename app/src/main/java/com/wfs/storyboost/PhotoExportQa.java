package com.wfs.storyboost;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Real JPEG export smoke test on the Android emulator, not a mocked renderer.
 * Synthetic fixtures only; writes a machine-readable result to app-private files.
 * Runs solely when the explicit QA launch flag is provided, never for normal users.
 */
public final class PhotoExportQa {
    private static final String TAG="StoryBoostQA";
    private PhotoExportQa(){}
    private static int checks;
    private static void check(boolean condition,String label) {
        if(!condition)throw new AssertionError(label);
        checks++;
    }
    public static void runAsync(Context context) {
        Context app=context.getApplicationContext();
        File report=new File(app.getFilesDir(),"qa-photo-export.txt");
        if(report.exists())report.delete();
        new Thread(()->{
            StringBuilder text=new StringBuilder();
            try{
                checks=0;
                long elapsed=SystemClock.elapsedRealtime();
                long nativeStart=Debug.getNativeHeapAllocatedSize();
                execute(app,text);
                text.append("checks=").append(checks).append('\n');
                text.append("elapsedMs=").append(SystemClock.elapsedRealtime()-elapsed).append('\n');
                text.append("nativeHeapStartBytes=").append(nativeStart).append('\n');
                text.append("nativeHeapEndBytes=").append(Debug.getNativeHeapAllocatedSize()).append('\n');
                text.insert(0,"STATUS=PASS\n");
            }catch(Throwable failure){
                Log.e(TAG,"Actual JPEG export QA failed",failure);
                text.insert(0,"STATUS=FAIL\nerror="+failure.getClass().getSimpleName()+": "
                        +String.valueOf(failure.getMessage()).replace('\n',' ')+"\n");
            }
            try(FileOutputStream out=new FileOutputStream(report)){
                out.write(text.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
                Log.i(TAG,"QA report: "+text.toString().replace('\n',' '));
            }catch(Exception ex){Log.e(TAG,"Cannot save QA report",ex);}
        },"wfs-real-jpeg-qa").start();
    }

    private static void execute(Context app,StringBuilder report)throws Exception{
        File colored=new File(app.getCacheDir(),"qa_v28_scene.jpg");
        Bitmap fixture=Bitmap.createBitmap(480,320,Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas=new Canvas(fixture);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawColor(Color.rgb(14,26,42));
            p.setColor(0xFF2677CC);canvas.drawRect(0,0,240,320,p);
            p.setColor(0xFFE1A256);canvas.drawRect(240,0,480,320,p);
            p.setColor(0xFFFFE49A);canvas.drawCircle(240,160,55,p);
            try(FileOutputStream out=new FileOutputStream(colored)){
                check(fixture.compress(Bitmap.CompressFormat.JPEG,94,out),
                        "fixture JPEG encoder");
            }
        }finally{fixture.recycle();}

        byte[] bytes=Files.readAllBytes(colored.toPath());
        check(bytes.length>2000,"nonempty synthetic JPEG");
        Uri uri=Uri.fromFile(colored);
        ExportOptions base=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true)
                .withQuality(OutputProfile.FULL_HD);
        long start=SystemClock.elapsedRealtime();
        PhotoEngine.Result portrait=PhotoEngine.process(app,uri,base);
        check(portrait.size.width==1080&&portrait.size.height==1920,
                "9:16 JPEG 1080x1920");
        Bitmap portraitDecode=BitmapFactory.decodeByteArray(portrait.jpeg,0,portrait.jpeg.length);
        check(portraitDecode!=null,"JPEG result decodes");
        try{
            check(portraitDecode.getWidth()==1080&&portraitDecode.getHeight()==1920,
                    "JPEG actual dimensions match requested size");
            int sample=portraitDecode.getPixel(540,950);
            check(Color.alpha(sample)==255,"opaque result pixel");
        }finally{portraitDecode.recycle();}
        report.append("portraitExportMs=").append(SystemClock.elapsedRealtime()-start).append('\n');
        report.append("portraitJpegBytes=").append(portrait.jpeg.length).append('\n');

        PhotoEngine.Result adjusted=PhotoEngine.process(app,uri,base
                .withAdjustments(.35f,.15f,.15f));
        check(!Arrays.equals(portrait.jpeg,adjusted.jpeg),
                "real color adjustment must change encoded pixels");
        check(adjusted.size.width==1080&&adjusted.size.height==1920,
                "color enhancement preserves social canvas");

        PhotoEngine.Result custom=PhotoEngine.process(app,uri,
                base.withCustomSize(720,960));
        check(custom.size.width==720&&custom.size.height==960,
                "real 3:4 custom JPEG export size");
        Bitmap customDecode=BitmapFactory.decodeByteArray(
                custom.jpeg,0,custom.jpeg.length);
        check(customDecode!=null,"custom JPEG decodes");
        try{
            check(customDecode.getWidth()==720&&customDecode.getHeight()==960,
                    "custom JPEG metadata matches requested size");
        }finally{customDecode.recycle();}

        // Rotate only the EXIF metadata; the output MUST respect it.
        File rotated=new File(app.getCacheDir(),"qa_v28_rotated.jpg");
        Files.copy(colored.toPath(),rotated.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        ExifInterface exif=new ExifInterface(rotated.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
        exif.saveAttributes();
        AspectRatioProfile.Size rotatedDims=MediaDimensions.read(app,Uri.fromFile(rotated));
        check(rotatedDims.width==320&&rotatedDims.height==480,
                "EXIF metadata orientation swaps width and height");
        PhotoEngine.Result result=PhotoEngine.process(app,Uri.fromFile(rotated),
                new ExportOptions(ColorLooks.ORIGINAL,AspectRatioProfile.ORIGINAL,
                        AspectRatioProfile.FIT,true).withQuality(OutputProfile.HD));
        check(result.size.width==320&&result.size.height==480,
                "real export rotates pixels upright");

        // Test the actual ContentResolver MIME path to enforce exact JPEG copy.
        Uri media=null;
        try{
            ContentValues values=new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,"wfs_qa_unchanged.jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
            if(Build.VERSION.SDK_INT>=29){
                values.put(MediaStore.Images.Media.IS_PENDING,1);
                values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/WFS StoryBoost QA");
            }
            ContentResolver resolver=app.getContentResolver();
            media=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            check(media!=null,"MediaStore QA fixture available");
            try(OutputStream out=resolver.openOutputStream(media)){
                check(out!=null,"MediaStore synthetic stream opened");
                out.write(bytes);
            }
            ExportOptions original=new ExportOptions(ColorLooks.ORIGINAL,
                    AspectRatioProfile.ORIGINAL,AspectRatioProfile.FIT,true)
                    .withQuality(OutputProfile.MAX_DETAIL);
            PhotoEngine.Result exact=PhotoEngine.process(app,media,original);
            check(Arrays.equals(bytes,exact.jpeg),
                    "original JPEG is copied byte-for-byte with no re-encode");
            check(!PhotoPassthrough.allowed(original.withPortraitBokeh(true),
                    "image/jpeg"),"AI edit disables lossless passthrough");
        }finally{
            if(media!=null)app.getContentResolver().delete(media,null,null);
        }

        // A synthetic no-person image probes the REAL ML Kit model. Do not
        // claim portrait quality from this negative case.
        File empty=new File(app.getCacheDir(),"qa_v28_empty.jpg");
        Bitmap blank=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888);
        try{
            blank.eraseColor(Color.rgb(30,110,190));
            try(FileOutputStream out=new FileOutputStream(empty)){
                check(blank.compress(Bitmap.CompressFormat.JPEG,93,out),
                        "empty portrait fixture");
            }
        }finally{blank.recycle();}
        long aiStarted=SystemClock.elapsedRealtime();
        boolean rejected=false;
        try{
            PhotoEngine.process(app,Uri.fromFile(empty),base
                    .withQuality(OutputProfile.HD).withPortraitBokeh(true));
        }catch(java.io.IOException expected){
            rejected=true;
            report.append("noPersonModelError=")
                    .append(String.valueOf(expected.getMessage()).replace('\n',' '))
                    .append('\n');
        }
        check(rejected,"ML model must not silently export portrait on uniform no-person image");
        report.append("noPersonAiMs=").append(SystemClock.elapsedRealtime()-aiStarted).append('\n');
        colored.delete();rotated.delete();empty.delete();
    }
}
