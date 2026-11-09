package com.wfs.storyboost.qaprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.DocumentsContract;
import android.net.Uri;

/**
 * Test runner controls source validity from the PROVIDER UID, not consumer UID.
 * Explicit adb shell broadcasts only; bundled into QA APK, never release.
 */
public final class FixtureControlReceiver extends BroadcastReceiver {
    public static final String REVOKE="com.wfs.storyboost.qaprovider.REVOKE";
    public static final String DELETE="com.wfs.storyboost.qaprovider.DELETE";
    public static final String RESTORE="com.wfs.storyboost.qaprovider.RESTORE";
    @Override public void onReceive(Context context,Intent intent){
        String action=intent==null?"":intent.getAction();
        Uri doc=DocumentsContract.buildDocumentUri(
                FixtureDocumentsProvider.AUTHORITY,FixtureDocumentsProvider.PHOTO);
        if(REVOKE.equals(action)||DELETE.equals(action)){
            if(DELETE.equals(action))FixtureDocumentsProvider.setMissing(context,true);
            // Owner-revoked access: the external consumer is not allowed to
            // quietly reopen its old URI after the provider invalidates it.
            context.revokeUriPermission(doc,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            android.util.Log.i("WfsQaProvider","OWNER_REVOKED action="+action);
            setResultCode(200);
        }else if(RESTORE.equals(action)){
            FixtureDocumentsProvider.setMissing(context,false);
            android.util.Log.i("WfsQaProvider","PROVIDER_RESTORED");
            setResultCode(200);
        }else setResultCode(400);
    }
}
