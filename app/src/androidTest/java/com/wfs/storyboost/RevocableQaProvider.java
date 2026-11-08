package com.wfs.storyboost;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import java.io.FileOutputStream;
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

    /**
     * Test APK only: grant/revoke exactly one synthetic URI, via provider owner
     * UID. Reading still explicitly requires a framework URI grant.
     */
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(!"com.wfs.storyboost.studio".equals(arg))
            throw new SecurityException("Not a WFS StoryBoost test target");
        Uri uri=Uri.parse("content://"+AUTHORITY+"/document/fixture.jpg");
        if("qa-grant".equals(method))
            getContext().grantUriPermission(arg,uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        else if("qa-revoke".equals(method))
            getContext().revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        else throw new UnsupportedOperationException("Unknown QA command");
        return new Bundle();
    }


    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)
            throws FileNotFoundException {
        if(!"r".equals(mode)||uri==null||uri.getPathSegments().size()!=2
                ||!"document".equals(uri.getPathSegments().get(0)))
            throw new FileNotFoundException("Unsupported test document");
        // Manifest signature-level readPermission is enforced by Android
        // ContentProvider transport. This owner process can only open the
        // private synthetic fixture after the platform admits the URI grant.
        File file=new File(getContext().getFilesDir(),FIXTURE_FILE);
        if(!file.exists()){
            Bitmap image=Bitmap.createBitmap(240,360,Bitmap.Config.ARGB_8888);
            try{
                for(int y=0;y<360;y++)for(int x=0;x<240;x++)
                    image.setPixel(x,y,Color.rgb(75+x%120,80+y%130,(x+y)%240));
                try(FileOutputStream out=new FileOutputStream(file)){
                    if(!image.compress(Bitmap.CompressFormat.JPEG,90,out))
                        throw new FileNotFoundException("Could not encode QA JPEG");
                }
            }catch(java.io.IOException e){
                throw new FileNotFoundException("QA fixture setup: "+e);
            }finally{image.recycle();}
        }
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
