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
import java.util.function.BooleanSupplier;

/** Android 10+ scoped-storage writer. No broad gallery permission. */
public final class MediaSaver {
    private static final int COPY_CHUNK = 128 * 1024;
    enum Stage { BEFORE_INSERT, CHUNK_WRITTEN, BEFORE_PUBLISH, AFTER_PUBLISH, BEFORE_CALLBACK }
    private MediaSaver() {}

    private static void checkpoint(Stage stage, String name, Uri destination) {
        ExportQaCheckpoint.reached(stage, name, destination);
    }

    static void beforeCallback(Uri destination, String name) {
        checkpoint(Stage.BEFORE_CALLBACK, name, destination);
    }

    /** Called only with a row returned by this writer, never an input URI. */
    static void discardUnaccepted(Context context, Uri destination) {
        try {
            context.getContentResolver().delete(destination, null, null);
        } catch (RuntimeException error) {
            android.util.Log.e("WfsMediaSaver", "Could not remove unaccepted export", error);
        }
    }

    static void discardTemporaryFile(File file) {
        if (!file.delete() && file.exists())
            android.util.Log.e("WfsMediaSaver", "Could not remove temporary export");
    }

    public static Uri savePhoto(Context context, byte[] jpeg, String fileName) throws IOException {
        return savePhoto(context,jpeg,fileName,()->false);
    }

    private static void rejectCancellation(BooleanSupplier token) throws IOException {
        if(token!=null&&token.getAsBoolean())
            throw new IOException("Penyimpanan dibatalkan tanpa membuat media di Galeri");
    }

    /** Aborts a photo save before publishing and deletes its owned MediaStore row. */
    public static Uri savePhoto(Context context, byte[] jpeg, String fileName,
                                BooleanSupplier token) throws IOException {
        return save(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileName,
                "image/jpeg", Environment.DIRECTORY_PICTURES, token,
                () -> new java.io.ByteArrayInputStream(jpeg));
    }

    public static Uri saveVideo(Context context, File inputFile, String fileName) throws IOException {
        return saveVideo(context,inputFile,fileName,()->false);
    }

    public static Uri saveVideo(Context context, File inputFile, String fileName,
                                BooleanSupplier token) throws IOException {
        return save(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, fileName,
                "video/mp4", Environment.DIRECTORY_MOVIES, token,
                () -> new FileInputStream(inputFile));
    }

    /** Byte-for-byte original MP4 copy; never opens the source for writing. */
    public static Uri copyOriginalMp4(Context context, Uri input, String name) throws IOException {
        return copyOriginalMp4(context,input,name,()->false);
    }

    public static Uri copyOriginalMp4(Context context, Uri input, String name,
                                      BooleanSupplier token) throws IOException {
        return save(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, name,
                "video/mp4", Environment.DIRECTORY_MOVIES, token,
                () -> context.getContentResolver().openInputStream(input));
    }

    @FunctionalInterface
    private interface Source { InputStream open() throws IOException; }

    private static Uri save(Context context, Uri collection, String name, String mime,
                            String directory, BooleanSupplier token, Source source)
            throws IOException {
        checkpoint(Stage.BEFORE_INSERT, name, null);
        rejectCancellation(token);
        ContentResolver resolver = context.getContentResolver();
        Uri destination = resolver.insert(collection, metadata(name, mime, directory));
        if (destination == null) throw new IOException("Tidak bisa membuat media di Galeri.");
        try {
            rejectCancellation(token);
            try (InputStream in = source.open();
                 OutputStream out = resolver.openOutputStream(destination)) {
                if (in == null || out == null) throw new IOException("File sumber/tujuan tak tersedia");
                byte[] bytes = new byte[COPY_CHUNK];
                boolean written = false;
                for (;;) {
                    rejectCancellation(token);
                    int length = in.read(bytes);
                    if (length == -1) break;
                    if (length == 0) continue;
                    rejectCancellation(token);
                    out.write(bytes, 0, length);
                    written = true;
                    checkpoint(Stage.CHUNK_WRITTEN, name, destination);
                }
                if (!written) throw new IOException("File sumber kosong.");
            }
            checkpoint(Stage.BEFORE_PUBLISH, name, destination);
            rejectCancellation(token);
            publish(resolver, destination);
            checkpoint(Stage.AFTER_PUBLISH, name, destination);
            rejectCancellation(token);
            return destination;
        } catch (Exception error) {
            try {
                resolver.delete(destination, null, null);
            } catch (RuntimeException cleanup) {
                error.addSuppressed(cleanup);
            }
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException("Gagal menyimpan media", error);
        }
    }

    private static ContentValues metadata(String name, String mime, String directory) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, directory + "/WFS StoryBoost");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        return values;
    }

    private static void publish(ContentResolver resolver, Uri uri) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        if (resolver.update(uri, values, null, null) != 1)
            throw new IOException("Publikasi media di Galeri tidak terkonfirmasi.");
    }
}
