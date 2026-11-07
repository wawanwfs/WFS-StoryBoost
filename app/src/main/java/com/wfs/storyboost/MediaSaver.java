package com.wfs.storyboost;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Android 10+ scoped-storage writer. No broad gallery permission. */
public final class MediaSaver {
    private MediaSaver() {}

    public static Uri savePhoto(Context context, byte[] jpeg, String fileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = metadata(fileName, "image/jpeg", Environment.DIRECTORY_PICTURES);
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Gagal membuat file foto di galeri.");
        try {
            try (OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream == null) throw new IOException("Galeri tidak bisa ditulis.");
                stream.write(jpeg);
            }
            publish(resolver, uri);
            return uri;
        } catch (Exception ex) {
            resolver.delete(uri, null, null);
            throw new IOException("Gagal menyimpan foto", ex);
        }
    }

    public static Uri saveVideo(Context context, File inputFile, String fileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = metadata(fileName, "video/mp4", Environment.DIRECTORY_MOVIES);
        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Gagal membuat file video di galeri.");
        try {
            try (InputStream input = new FileInputStream(inputFile);
                 OutputStream output = resolver.openOutputStream(uri)) {
                if (output == null) throw new IOException("Galeri tidak bisa ditulis.");
                byte[] buffer = new byte[128 * 1024];
                int length;
                while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            }
            publish(resolver, uri);
            return uri;
        } catch (Exception ex) {
            resolver.delete(uri, null, null);
            throw new IOException("Gagal menyimpan video", ex);
        }
    }

    /** Byte-for-byte original MP4 copy. Used only with the explicit 'Asli' preset. */
    public static Uri copyOriginalMp4(Context context, Uri input, String name) throws IOException {
        ContentResolver resolver=context.getContentResolver();
        Uri destination=resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                metadata(name,"video/mp4",Environment.DIRECTORY_MOVIES));
        if(destination==null)throw new IOException("Tidak bisa membuat video di galeri.");
        try(InputStream in=resolver.openInputStream(input);
            OutputStream out=resolver.openOutputStream(destination)){
            if(in==null||out==null)throw new IOException("File sumber/tujuan tak tersedia");
            byte[] bytes=new byte[128*1024];int length;
            while((length=in.read(bytes))!=-1)out.write(bytes,0,length);
        } catch(Exception e){
            resolver.delete(destination,null,null);
            throw new IOException("Gagal menyalin video sumber",e);
        }
        // Publish only after the MP4 stream has been completely flushed and closed.
        publish(resolver,destination);
        return destination;
    }

    private static ContentValues metadata(String name, String mime, String directory) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, directory + "/WFS StoryBoost");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        return values;
    }

    private static void publish(ContentResolver resolver, Uri uri) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
    }
}
