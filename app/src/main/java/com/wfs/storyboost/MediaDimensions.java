package com.wfs.storyboost;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.InputStream;
import java.io.IOException;

/** Lightweight bounds-only probe; never decodes a whole source just to show advice. */
public final class MediaDimensions {
    private MediaDimensions(){}
    public static AspectRatioProfile.Size read(Context context,Uri uri) throws IOException {
        String mime=context.getContentResolver().getType(uri);
        if(mime!=null&&mime.startsWith("video/")){
            MediaMetadataRetriever r=new MediaMetadataRetriever();
            try {
                r.setDataSource(context,uri);
                int w=Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                int h=Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                String rot=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if(rot!=null&&Integer.parseInt(rot)%180!=0){int swap=w;w=h;h=swap;}
                return new AspectRatioProfile.Size(w,h);
            } catch(Exception e){throw new IOException("Metadata video tidak terbaca",e);}
            finally{try{r.release();}catch(Exception ignored){}}
        }
        BitmapFactory.Options o=new BitmapFactory.Options();
        o.inJustDecodeBounds=true;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException("File gambar tidak tersedia");
            BitmapFactory.decodeStream(in,null,o);
        }
        if(o.outWidth<=0||o.outHeight<=0)throw new IOException("Dimensi gambar tidak terbaca");
        int w=o.outWidth,h=o.outHeight;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            if(in!=null){
                int rotation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
                if(rotation==5||rotation==6||rotation==7||rotation==8){int swap=w;w=h;h=swap;}
            }
        }catch(Exception ignored){}
        return new AspectRatioProfile.Size(w,h);
    }
}
