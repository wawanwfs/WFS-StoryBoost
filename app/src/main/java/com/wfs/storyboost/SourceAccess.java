package com.wfs.storyboost;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

/** Non-destructive, permission-aware access check before opening a Recent project. */
public final class SourceAccess {
    private SourceAccess() {}

    static void requireReadable(Context context,Uri uri) throws java.io.IOException {
        if(!canRead(context,uri))
            throw new java.io.IOException("Akses media sumber hilang. Impor ulang dari galeri.");
    }

    public static boolean canRead(Context context, Uri uri) {
        if(context==null || uri==null) return false;
        String scheme=uri.getScheme();
        if(!"content".equals(scheme) && !"file".equals(scheme)) return false;
        try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")){
            return fd!=null;
        }catch(Exception inaccessible){
            return false;
        }
    }
}
