package com.wfs.storyboost;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Test APK-owned provider; its private JPEG is inaccessible to the app unless
 * Android grants that app read access to the exact content URI.
 * Not a DocumentsProvider, so this tests OS URI-grant revocation independently
 * of the separately verified real ACTION_OPEN_DOCUMENT picker workflow.
 */
public final class RevocableQaProvider extends ContentProvider {
    static final String AUTHORITY = "com.wfs.storyboost.studio.qa.docs";
    static final String FIXTURE_FILE = "beta4-granted-fixture.jpg";

    @Override public boolean onCreate(){return true;}

    @Override public String getType(Uri uri){return "image/jpeg";}

    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)
            throws FileNotFoundException {
        if(!"r".equals(mode)||uri==null||uri.getPathSegments().size()!=2
                ||!"document".equals(uri.getPathSegments().get(0)))
            throw new FileNotFoundException("Unsupported test document");
        File file=new File(getContext().getFilesDir(),FIXTURE_FILE);
        if(!file.isFile())throw new FileNotFoundException("Synthetic provider fixture missing");
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri,String[] projection,String selection,
                                  String[] selectionArgs,String sortOrder){
        File file=new File(getContext().getFilesDir(),FIXTURE_FILE);
        String[] cols=projection==null
                ?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}
                :projection;
        MatrixCursor c=new MatrixCursor(cols);
        MatrixCursor.RowBuilder row=c.newRow();
        for(String name:cols){
            if(OpenableColumns.DISPLAY_NAME.equals(name))row.add(FIXTURE_FILE);
            else if(OpenableColumns.SIZE.equals(name))row.add(file.length());
            else row.add(null);
        }
        return c;
    }
    @Override public Uri insert(Uri uri,ContentValues values){
        throw new UnsupportedOperationException("Read-only QA document");
    }
    @Override public int delete(Uri uri,String selection,String[] args){
        throw new UnsupportedOperationException("Read-only QA document");
    }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){
        throw new UnsupportedOperationException("Read-only QA document");
    }
}
