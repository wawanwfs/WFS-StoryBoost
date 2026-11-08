package com.wfs.storyboost;

import android.content.Context;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** JPEG export with aspect-aware framing and conservative aesthetic adjustments. */
public final class PhotoEngine {
    private PhotoEngine() {}

    public static final class Result {
        public final byte[] jpeg;
        public final AspectRatioProfile.Size size;
        Result(byte[] jpeg, AspectRatioProfile.Size size) {
            this.jpeg = jpeg;
            this.size = size;
        }
    }

    public static Result process(Context context, Uri uri, ExportOptions settings) throws IOException {
        // The exact-copy path is intentionally opt-in: Original preset + original
        // aspect + Fit + Max Detail on JPEG. It preserves every original pixel
        // and EXIF byte instead of recompressing an already compressed image.
        if (PhotoPassthrough.allowed(settings,
                context.getContentResolver().getType(uri))) {
            AspectRatioProfile.Size nativeSize=MediaDimensions.read(context,uri);
            try(InputStream in=context.getContentResolver().openInputStream(uri)){
                if(in==null)throw new IOException("Gambar tidak tersedia.");
                ByteArrayOutputStream exact=new ByteArrayOutputStream();
                byte[] buffer=new byte[64*1024];
                int n;
                while((n=in.read(buffer))!=-1){
                    if(exact.size()+n>36*1024*1024){ exact=null;break; }
                    exact.write(buffer,0,n);
                }
                if(exact!=null) return new Result(exact.toByteArray(),nativeSize);
            }
        }
        int maxSide = settings.qualityMode == OutputProfile.MAX_DETAIL
                ? preferredDecodeLimit(context) : settings.fullHd ? 2880 : 1920;
        Bitmap source = decode(context, uri, maxSide);
        if (source == null) throw new IOException("Foto tidak bisa dibaca.");
        Bitmap oriented = orient(context, uri, source);
        if (oriented != source) source.recycle();

        try {
            AspectRatioProfile.Size size = AspectRatioProfile.resolve(settings.ratio,
                    settings.qualityMode, oriented.getWidth(), oriented.getHeight(), settings.frameStyle,
                    settings.customWidth,settings.customHeight);
            int width = size.width, height = size.height;
            Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            try {
                Canvas canvas = new Canvas(output);
                canvas.drawColor(Color.rgb(14, 19, 29));
                Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG
                        | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
                photoPaint.setColorFilter(colorFilter(settings));
                if (settings.frameStyle == AspectRatioProfile.BLUR) {
                    drawBlurredBackground(canvas, oriented, width, height);
                }
                FrameMath.Bounds b = FrameMath.center(oriented.getWidth(), oriented.getHeight(),
                        width, height, settings.frameStyle, settings.focusX, settings.focusY);
                if(settings.portraitBokeh){
                    AiPortraitCompositor.draw(canvas,oriented,width,height,b,photoPaint);
                }else{
                    canvas.drawBitmap(oriented,null,
                            new RectF(b.left,b.top,b.right,b.bottom),photoPaint);
                }
                if(settings.hasText())drawCaption(canvas,settings,width,height);

                // Avoid excessive halos and memory usage on large images.
                // No destructive per-pixel sharpening: it created halos and memory
                // spikes on 4K output. Bicubic/filtered scaling is done by Canvas.
                try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    if (!output.compress(Bitmap.CompressFormat.JPEG, 98, bytes)) {
                        throw new IOException("Gagal mengenkode JPEG.");
                    }
                    return new Result(bytes.toByteArray(), size);
                }
            } finally {
                output.recycle();
            }
        } catch (OutOfMemoryError oom) {
            throw new IOException("Memori HP tidak cukup. Coba kualitas HD.", oom);
        } finally {
            oriented.recycle();
        }
    }

    /** Upright, bounded preview of the entire photo for ML Kit on-device inference. */
    public static Bitmap aiInput(Context context,Uri uri)throws IOException{
        Bitmap source=decode(context,uri,1200);
        if(source==null)throw new IOException("Foto tidak terbaca.");
        Bitmap oriented=orient(context,uri,source);
        if(oriented!=source)source.recycle();
        return oriented;
    }

    /** Renders the untouched photo in the same output frame, downsampled for comparison UI. */
    public static Bitmap originalPreview(Context context, Uri uri, ExportOptions settings)
            throws IOException {
        Bitmap source = decode(context, uri, 1200);
        Bitmap oriented = orient(context, uri, source);
        if (oriented != source) source.recycle();
        try {
            AspectRatioProfile.Size resolved = AspectRatioProfile.resolve(settings.ratio,
                    settings.qualityMode, oriented.getWidth(), oriented.getHeight(), settings.frameStyle,
                    settings.customWidth,settings.customHeight);
            float shrink = Math.min(1f, 800f / Math.max(resolved.width, resolved.height));
            int width = Math.max(2, Math.round(resolved.width * shrink));
            int height = Math.max(2, Math.round(resolved.height * shrink));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.rgb(14, 19, 29));
            if (settings.frameStyle == AspectRatioProfile.BLUR) {
                drawBlurredBackground(canvas, oriented, width, height);
            }
            FrameMath.Bounds b = FrameMath.center(oriented.getWidth(), oriented.getHeight(),
                    width, height, settings.frameStyle, settings.focusX, settings.focusY);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(oriented, null,
                    new RectF(b.left, b.top, b.right, b.bottom), paint);
            return bitmap;
        } catch (OutOfMemoryError oom) {
            throw new IOException("Memori tidak cukup untuk pratinjau.", oom);
        } finally {
            oriented.recycle();
        }
    }

    /**
     * ML-based portrait preview at screen size. Its mask is independently generated
     * from an upright downsampled source; color matrix is burned into bitmap.
     */
    public static Bitmap portraitPreview(Context context,Uri uri,ExportOptions settings)
            throws IOException {
        Bitmap source=decode(context,uri,1200);
        if(source==null)throw new IOException("Foto tidak dapat dibaca");
        Bitmap oriented=orient(context,uri,source);
        if(oriented!=source)source.recycle();
        Bitmap output=null;
        try{
            AspectRatioProfile.Size target=AspectRatioProfile.resolve(settings.ratio,
                    settings.qualityMode,oriented.getWidth(),oriented.getHeight(),
                    settings.frameStyle,settings.customWidth,settings.customHeight);
            double factor=Math.min(1d,800d/Math.max(target.width,target.height));
            int w=Math.max(2,(int)Math.round(target.width*factor));
            int h=Math.max(2,(int)Math.round(target.height*factor));
            output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(output);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            p.setColorFilter(colorFilter(settings));
            FrameMath.Bounds b=FrameMath.center(oriented.getWidth(),oriented.getHeight(),
                    w,h,settings.frameStyle,settings.focusX,settings.focusY);
            AiPortraitCompositor.draw(canvas,oriented,w,h,b,p);
            // Caption remains a separate live overlay in StudioEditorActivity.
            return output;
        }catch(IOException|RuntimeException e){
            if(output!=null)output.recycle();
            throw e;
        }catch(OutOfMemoryError oom){
            if(output!=null)output.recycle();
            throw new IOException("RAM tidak cukup untuk preview AI Portrait. Gunakan resolusi HD.",oom);
        }finally{oriented.recycle();}
    }

    /** JPEG byte preview is decoded at screen resolution rather than 2-megapixel size. */
    public static Bitmap editedPreview(byte[] jpeg) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = 1;
        while (Math.max(opts.outWidth, opts.outHeight) / opts.inSampleSize > 800) {
            opts.inSampleSize *= 2;
        }
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
    }

    static void drawBlurredBackground(Canvas canvas, Bitmap source,
                                               int width, int height) {
        // Downsample + double box blur for a soft fill; foreground stays untouched.
        final int lowW = 64;
        final int lowH = Math.max(24, Math.round(64f * height / width));
        Bitmap low = Bitmap.createScaledBitmap(source, lowW, lowH, true);
        try {
            int[] px = new int[lowW * lowH];
            low.getPixels(px, 0, lowW, 0, 0, lowW, lowH);
            blur(px, lowW, lowH, 5);
            blur(px, lowW, lowH, 5);
            low.setPixels(px, 0, lowW, 0, 0, lowW, lowH);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
            canvas.drawBitmap(low, null, new RectF(0, 0, width, height), paint);
            Paint dim = new Paint();
            dim.setColor(Color.argb(75, 8, 12, 22));
            canvas.drawRect(0, 0, width, height, dim);
        } finally {
            low.recycle();
        }
    }

    /** Blur follows the same fitted/cropped rectangle as the sharp subject mask.
     * The full-frame soft fill is intentional only outside a FIT letterbox.
     */
    static void drawPortraitBlurredBackground(Canvas canvas,Bitmap source,
            int width,int height,FrameMath.Bounds bounds,Paint colorPaint){
        drawBlurredBackground(canvas,source,width,height);
        int smallW=Math.min(144,Math.max(64,source.getWidth()/5));
        int smallH=Math.max(24,Math.round(smallW*(float)source.getHeight()/source.getWidth()));
        Bitmap small=Bitmap.createScaledBitmap(source,smallW,smallH,true);
        try{
            int[] px=new int[smallW*smallH];
            small.getPixels(px,0,smallW,0,0,smallW,smallH);
            blur(px,smallW,smallH,5);
            blur(px,smallW,smallH,5);
            small.setPixels(px,0,smallW,0,0,smallW,smallH);
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            paint.setColorFilter(colorPaint.getColorFilter());
            canvas.save();
            canvas.clipRect(0,0,width,height);
            canvas.drawBitmap(small,null,
                    new RectF(bounds.left,bounds.top,bounds.right,bounds.bottom),paint);
            canvas.restore();
        }finally{small.recycle();}
    }

    private static void blur(int[] px, int w, int h, int radius) {
        int[] copy = px.clone();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int red = 0, green = 0, blue = 0, n = 0;
                for (int j = Math.max(0, y - radius); j <= Math.min(h - 1, y + radius); j++) {
                    for (int i = Math.max(0, x - radius); i <= Math.min(w - 1, x + radius); i++) {
                        int c = copy[j * w + i];
                        red += Color.red(c); green += Color.green(c); blue += Color.blue(c); n++;
                    }
                }
                px[y * w + x] = Color.rgb(red / n, green / n, blue / n);
            }
        }
    }

    /** Avoid forced 2x JPEG downsampling on phones with enough memory for 4K sources. */
    private static int preferredDecodeLimit(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int heapMb = manager == null ? 192 : manager.getLargeMemoryClass();
        return heapMb >= 384 ? 6144 : 4096;
    }

    private static Bitmap decode(Context context, Uri uri, int maxSide) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Format foto tidak didukung oleh Android.");
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / opts.inSampleSize > maxSide) {
            opts.inSampleSize *= 2;
        }
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            Bitmap image = BitmapFactory.decodeStream(in, null, opts);
            if (image == null) throw new IOException("Tidak bisa mendekode gambar.");
            return image;
        } catch (OutOfMemoryError oom) {
            throw new IOException("Foto terlalu besar untuk memori HP ini.", oom);
        }
    }

    private static Bitmap orient(Context context, Uri uri, Bitmap src) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in != null) orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignored) { }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.postScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE: matrix.postRotate(90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_TRANSVERSE: matrix.postRotate(270); matrix.postScale(-1, 1); break;
            default: return src;
        }
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    /** Shared look parameters: photo preview and exported JPEG always use same matrix. */
    static ColorMatrixColorFilter colorFilter(int preset){
        return makeColorFilter(preset,0f,0f,0f);
    }
    static ColorMatrixColorFilter colorFilter(ExportOptions opts){
        if(ColorLooks.isOriginal(opts.preset)&&!opts.hasColorAdjustments())return null;
        return makeColorFilter(opts.preset,opts.brightness,opts.contrast,opts.saturation);
    }
    private static ColorMatrixColorFilter makeColorFilter(int preset,float brightness,
                                                          float userContrast,float userSaturation){
        ColorLooks.Look look=ColorLooks.get(preset);
        float sat=Math.max(0f,Math.min(2.3f,look.saturation+userSaturation*.65f));
        float contrast=Math.max(.55f,Math.min(1.55f,look.contrast+userContrast*.30f));
        float lift=look.lift+brightness*38f;
        ColorMatrix matrix=new ColorMatrix();
        matrix.setSaturation(sat);
        matrix.postConcat(new ColorMatrix(new float[]{
                contrast*look.red,0,0,0,lift-(contrast-1f)*128f,
                0,contrast*look.green,0,0,lift-(contrast-1f)*128f,
                0,0,contrast*look.blue,0,lift-(contrast-1f)*128f,
                0,0,0,1,0
        }));
        return new ColorMatrixColorFilter(matrix);
    }
    private static void drawCaption(Canvas canvas,ExportOptions settings,int width,int height){
        String caption=settings.caption.trim().replace((char)10,' ');
        if(caption.isEmpty())return;
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.BOLD));
        float px=Math.max(18f,Math.min(width*.058f,height*.04f));
        paint.setTextSize(px);
        paint.setTextAlign(Paint.Align.CENTER);
        String[] lines;
        if(paint.measureText(caption)>width*.88f&&caption.contains(" ")){
            int middle=caption.length()/2;
            int split=caption.lastIndexOf(' ',middle);
            if(split<caption.length()/4)split=caption.indexOf(' ',middle);
            lines=split>0?new String[]{caption.substring(0,split),caption.substring(split+1)}
                         :new String[]{caption};
        }else lines=new String[]{caption};
        paint.setColor(settings.captionColor);
        paint.setShadowLayer(Math.max(2f,px*.11f),1f,Math.max(2f,px*.10f),0xEE000000);
        float y=height*SocialExportProfile.captionCenterY(settings.socialProfile)
                -(lines.length-1)*px*.65f;
        for(String line:lines){
            // Fit even long captions within the frame, never clip at the sides.
            float fit=paint.measureText(line)>0?Math.min(1f,width*.9f/paint.measureText(line)):1f;
            canvas.save();canvas.translate(width/2f,y);canvas.scale(fit,1f);
            canvas.drawText(line,0,0,paint);canvas.restore();
            y+=px*1.25f;
        }
    }

    private static void sharpen(Bitmap image, int width, int height, float strength) {
        int[] src = new int[width * height];
        image.getPixels(src, 0, width, 0, 0, width, height);
        int[] dst = src.clone();
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int i = y * width + x;
                int c = src[i];
                int l = src[i - 1], r = src[i + 1], u = src[i - width], d = src[i + width];
                int rr = clamp(Math.round(Color.red(c) + strength * (4 * Color.red(c)
                        - Color.red(l) - Color.red(r) - Color.red(u) - Color.red(d))));
                int gg = clamp(Math.round(Color.green(c) + strength * (4 * Color.green(c)
                        - Color.green(l) - Color.green(r) - Color.green(u) - Color.green(d))));
                int bb = clamp(Math.round(Color.blue(c) + strength * (4 * Color.blue(c)
                        - Color.blue(l) - Color.blue(r) - Color.blue(u) - Color.blue(d))));
                dst[i] = Color.rgb(rr, gg, bb);
            }
        }
        image.setPixels(dst, 0, width, 0, 0, width, height);
    }

    private static int clamp(int x) { return Math.max(0, Math.min(255, x)); }
}
