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
        // Real neural inference: feed synthetic sigma-25 Gaussian noise to the
        // actual bundled Qualcomm DnCNN model, compare to known clean ground truth.
        int side=256;int[] cleanGray=new int[side*side],noisyGray=new int[side*side];
        int[] noisyArgb=new int[side*side];
        java.util.Random random=new java.util.Random(291122L);
        for(int py=0;py<side;py++)for(int px=0;px<side;px++){
            int idx=py*side+px;
            int cleanValue=Math.max(0,Math.min(255,Math.round(
                    72+94f*px/(side-1)+26f*(float)Math.sin(py/24f))));
            int corrupted=Math.max(0,Math.min(255,
                    (int)Math.round(cleanValue+random.nextGaussian()*25)));
            cleanGray[idx]=cleanValue;
            noisyGray[idx]=corrupted;
            noisyArgb[idx]=Color.rgb(corrupted,corrupted,corrupted);
        }
        check(AiDenoiseEngine.included(app),"real DnCNN weights packaged in APK");
        Bitmap noisyBitmap=Bitmap.createBitmap(noisyArgb,side,side,Bitmap.Config.ARGB_8888);
        AiDenoiseEngine.Result enhanced=null;
        try{
            enhanced=AiDenoiseEngine.process(app,noisyBitmap);
            check(enhanced.stats.tiles==1,"neural model executed on one full 256 tile");
            int[] resultRgb=new int[side*side];
            enhanced.bitmap.getPixels(resultRgb,0,side,0,0,side,side);
            int[] denoisedGray=new int[side*side];
            long difference=0;
            for(int i=0;i<resultRgb.length;i++){
                denoisedGray[i]=Color.red(resultRgb[i]);
                difference+=Math.abs(denoisedGray[i]-noisyGray[i]);
            }
            check(difference>0,"neural denoise changed actual pixels");
            double psnrBefore=ImageQualityMetrics.psnr(cleanGray,noisyGray);
            double psnrAfter=ImageQualityMetrics.psnr(cleanGray,denoisedGray);
            double ssimBefore=ImageQualityMetrics.ssim(cleanGray,noisyGray);
            double ssimAfter=ImageQualityMetrics.ssim(cleanGray,denoisedGray);
            report.append("dncnnInferenceMs=").append(enhanced.stats.inferenceMs).append('\n');
            report.append("dncnnTotalMs=").append(enhanced.stats.totalMs).append('\n');
            report.append("dncnnPsnrBefore=").append(psnrBefore).append('\n');
            report.append("dncnnPsnrAfter=").append(psnrAfter).append('\n');
            report.append("dncnnSsimBefore=").append(ssimBefore).append('\n');
            report.append("dncnnSsimAfter=").append(ssimAfter).append('\n');
            check(Double.isFinite(psnrBefore)&&Double.isFinite(psnrAfter),
                    "finite neural restoration PSNR metrics");
            check(psnrAfter>psnrBefore+.25d,
                    "actual DnCNN must improve PSNR by at least 0.25 dB");
            check(ssimAfter>ssimBefore,
                    "actual DnCNN must improve global SSIM");
            check(!PhotoPassthrough.allowed(new ExportOptions(ColorLooks.ORIGINAL,
                    AspectRatioProfile.ORIGINAL,AspectRatioProfile.FIT,true)
                    .withQuality(OutputProfile.MAX_DETAIL).withAiDenoise(true),
                    "image/jpeg"),"neural photo export never copies original JPEG");
        }finally{
            if(enhanced!=null)enhanced.bitmap.recycle();
            noisyBitmap.recycle();
        }
        // End-to-end JPEG render with neural option: verifies that the export
        // pipeline (not only model unit inference) actually invokes DnCNN.
        File noisyJpeg=new File(app.getCacheDir(),"qa_v29_dncnn.jpg");
        Bitmap noisyFixture=Bitmap.createBitmap(noisyArgb,side,side,Bitmap.Config.ARGB_8888);
        try(FileOutputStream out=new FileOutputStream(noisyJpeg)){
            check(noisyFixture.compress(Bitmap.CompressFormat.JPEG,95,out),
                    "neural end-to-end JPEG fixture");
        }finally{noisyFixture.recycle();}
        Uri noisyUri=Uri.fromFile(noisyJpeg);
        ExportOptions denoiseOptions=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.ORIGINAL,AspectRatioProfile.FIT,true)
                .withQuality(OutputProfile.HD).withAiDenoise(true);
        PhotoEngine.Result jpegWithAi=PhotoEngine.process(app,noisyUri,denoiseOptions);
        PhotoEngine.Result jpegNoAi=PhotoEngine.process(app,noisyUri,
                denoiseOptions.withAiDenoise(false));
        check(jpegWithAi.size.width==256&&jpegWithAi.size.height==256,
                "real neural JPEG export preserves source dimensions");
        check(!Arrays.equals(jpegWithAi.jpeg,jpegNoAi.jpeg),
                "neural flag changes actual exported JPEG bytes");
        Bitmap verified=BitmapFactory.decodeByteArray(jpegWithAi.jpeg,0,jpegWithAi.jpeg.length);
        check(verified!=null,"neural JPEG output decodable");
        if(verified!=null)verified.recycle();
        report.append("dncnnExportJpegBytes=").append(jpegWithAi.jpeg.length).append('\n');
        noisyJpeg.delete();
        // v3 beta: REAL private draft persistence. Media itself remains untouched.
        ExportOptions savedOptions=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.CUSTOM,AspectRatioProfile.CROP,true,
                200,900,.24f,.81f)
                .withCustomSize(1200,900)
                .withQuality(OutputProfile.HD)
                .withAdjustments(.25f,-.1f,.15f)
                .withCaption("WFS draft restored",Color.WHITE)
                .withMute(true)
                .withAiDenoise(true)
                .withPortraitBokeh(true);
        check(StudioDraftStore.save(app,uri,savedOptions,450,true),
                "private editor draft stored successfully");
        StudioDraftStore.Draft recovered=StudioDraftStore.load(app,uri);
        check(recovered!=null,"draft can reopen after preferences commit");
        check(recovered.options.ratio==AspectRatioProfile.CUSTOM
                &&recovered.options.customWidth==1200
                &&recovered.options.customHeight==900,
                "custom dimensions recovered from disk");
        check(recovered.options.aiDenoise&&recovered.options.portraitBokeh,
                "neural and bokeh switches recovered");
        check(recovered.options.caption.equals("WFS draft restored")
                &&recovered.options.muteAudio,
                "text and audio state recovered");
        check(Math.abs(recovered.options.focusX-.24f)<.001f
                &&Math.abs(recovered.options.focusY-.81f)<.001f,
                "crop focus recovered without rounding");
        check(recovered.splitMs==450&&recovered.dualOutput,
                "timeline split and dual export options recovered");
        check(!app.getSharedPreferences("studio-drafts",Context.MODE_PRIVATE)
                .getAll().toString().contains(uri.toString()),
                "raw source URI is never stored as a draft value or key");
        Uri unrelated=Uri.parse("content://some-other-media/5678");
        check(StudioDraftStore.load(app,unrelated)==null,
                "draft is isolated to its own media source");
        app.getSharedPreferences("studio-drafts",Context.MODE_PRIVATE).edit()
                .putString(StudioDraftStore.keyFor(uri),"{corrupt-json").commit();
        check(StudioDraftStore.load(app,uri)==null,
                "corrupt draft fails closed without crashing");
        StudioDraftStore.clearAll(app);
        check(StudioDraftStore.load(app,uri)==null,
                "clear history also clears private editing draft");
        report.append("draftPersistenceTests=11").append('\n');

        // Validate source pixel count BEFORE decoding or running neural inference.
        File huge=new File(app.getCacheDir(),"qa_v30_too_large.jpg");
        Bitmap oversized=Bitmap.createBitmap(1900,1400,Bitmap.Config.ARGB_8888);
        try{
            oversized.eraseColor(Color.rgb(26,64,124));
            try(FileOutputStream out=new FileOutputStream(huge)){
                check(oversized.compress(Bitmap.CompressFormat.JPEG,85,out),
                        "oversized source fixture encoded");
            }
        }finally{oversized.recycle();}
        ExportOptions hugeOptions=new ExportOptions(ColorLooks.ORIGINAL,
                AspectRatioProfile.ORIGINAL,AspectRatioProfile.FIT,true)
                .withAiDenoise(true);
        boolean oversizedRejected=false;
        try{PhotoEngine.process(app,Uri.fromFile(huge),hugeOptions);}
        catch(java.io.IOException expected){
            oversizedRejected=expected.getMessage().contains("2,6 MP");
        }
        check(oversizedRejected,"full-resolution AI export rejects >2.6 MP source");
        oversizedRejected=false;
        try{
            Bitmap unexpectedlyRendered=PhotoEngine.enhancedPreview(
                    app,Uri.fromFile(huge),hugeOptions);
            if(unexpectedlyRendered!=null)unexpectedlyRendered.recycle();
        }catch(java.io.IOException expected){
            oversizedRejected=expected.getMessage().contains("2,6 MP");
        }
        check(oversizedRejected,"preview also rejects oversized source before downsampling");
        report.append("over2_6MPGuard=PASS").append('\n');
        huge.delete();
        // v3 beta2: real multi-tile model inference on synthetic RGB/chroma
        // input, without claiming that synthetic noise represents real skin.
        final int tileW=480,tileH=480;
        Bitmap chromaSource=Bitmap.createBitmap(tileW,tileH,Bitmap.Config.ARGB_8888);
        int[] chromaPixels=new int[tileW*tileH];
        java.util.Random chromaRandom=new java.util.Random(301420L);
        for(int cy=0;cy<tileH;cy++)for(int cx=0;cx<tileW;cx++){
            int index=cy*tileW+cx;
            int n=chromaRandom.nextInt(13)-6;
            chromaPixels[index]=Color.rgb(72+cx/6+n,75+cy/6-n,100+(cx+cy)/13+n);
        }
        chromaSource.setPixels(chromaPixels,0,tileW,0,0,tileW,tileH);
        AiDenoiseEngine.Result chromaResult=null;
        try{
            chromaResult=AiDenoiseEngine.process(app,chromaSource);
            check(chromaResult.stats.tiles>1,"real DnCNN covers overlapping tiles");
            check(chromaResult.bitmap.getWidth()==tileW&&
                    chromaResult.bitmap.getHeight()==tileH,"tiled output keeps source size");
            int[] enhancedPixels=new int[chromaPixels.length];
            chromaResult.bitmap.getPixels(enhancedPixels,0,tileW,0,0,tileW,tileH);
            int sameChroma=0;
            long seamChange=0;
            for(int cy=0;cy<tileH;cy++){
                int seam=cy*tileW+224;
                seamChange+=Math.abs(Color.red(enhancedPixels[seam])-
                        Color.red(enhancedPixels[seam-1]));
            }
            for(int j=0;j<chromaPixels.length;j++){
                int originalRG=Color.red(chromaPixels[j])-Color.green(chromaPixels[j]);
                int enhancedRG=Color.red(enhancedPixels[j])-Color.green(enhancedPixels[j]);
                if(Math.abs(originalRG-enhancedRG)<=1)sameChroma++;
            }
            check(sameChroma>=chromaPixels.length*.98,
                    "luminance-only neural correction preserves 98% RGB chroma");
            report.append("dncnnMultiTileCount=").append(chromaResult.stats.tiles).append('\n');
            report.append("dncnnMultiTileMs=").append(chromaResult.stats.inferenceMs).append('\n');
            report.append("dncnnSeamMeanAbsRedJump=")
                    .append(seamChange/(double)tileH).append('\n');
            report.append("dncnnChromaPreservationFraction=")
                    .append(sameChroma/(double)chromaPixels.length).append('\n');
        }finally{
            if(chromaResult!=null)chromaResult.bitmap.recycle();
            chromaSource.recycle();
        }
        colored.delete();rotated.delete();empty.delete();
    }
}
