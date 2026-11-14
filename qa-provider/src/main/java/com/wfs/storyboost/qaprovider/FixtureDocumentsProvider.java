package com.wfs.storyboost.qaprovider;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Independent-UID, test-only provider for real DocumentsUI/Saf security QA.
 * Generates only a synthetic JPEG. No source photos or network.
 */
public final class FixtureDocumentsProvider extends DocumentsProvider {
    public static final String AUTHORITY="com.wfs.storyboost.qaprovider.documents";
    public static final String ROOT="qa-root", PHOTO="qa-jpeg-v1";
    public static final String NAME="wfs-provider-qa.jpg";
    static final String VIDEO="qa-mp4-v1";
    static final String VIDEO_NAME="qa-h264-extended.mp4";

    static File video(Context context) throws IOException {
        File target=new File(context.getFilesDir(),VIDEO_NAME);
        if(!target.isFile()||target.length()<128*1024){
            try(java.io.InputStream in=context.getAssets().open(VIDEO_NAME);
                FileOutputStream out=new FileOutputStream(target)){
                byte[] bytes=new byte[8192];
                for(int n;(n=in.read(bytes))!=-1;)out.write(bytes,0,n);
            }
        }
        return target;
    }
    private static final String PREF="qa-fixture-state";
    private static final String[] ROOT_COLUMNS={
        DocumentsContract.Root.COLUMN_ROOT_ID,
        DocumentsContract.Root.COLUMN_DOCUMENT_ID,
        DocumentsContract.Root.COLUMN_TITLE,
        DocumentsContract.Root.COLUMN_SUMMARY,
        DocumentsContract.Root.COLUMN_FLAGS,
        DocumentsContract.Root.COLUMN_ICON,
        DocumentsContract.Root.COLUMN_MIME_TYPES
    };
    private static final String[] DOC_COLUMNS={
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_FLAGS,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED
    };
    static boolean missing(Context ctx) {
        return ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).getBoolean("missing",false);
    }
    static void setMissing(Context ctx,boolean value){
        ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean("missing",value).commit();
        ctx.getContentResolver().notifyChange(DocumentsContract.buildRootsUri(AUTHORITY),null);
    }
    static File file(Context context) {
        return new File(context.getFilesDir(),"fixture-original-jpeg.jpg");
    }
    private static void ensureImage(Context ctx) throws IOException {
        File f=file(ctx);
        if(f.isFile() && f.length()>2048)return;
        Bitmap bitmap=Bitmap.createBitmap(480,720,Bitmap.Config.ARGB_8888);
        try{
            for(int y=0;y<720;y++)for(int x=0;x<480;x++)
                bitmap.setPixel(x,y,Color.rgb(30+x%210,40+y%195,45+(x+y)%200));
            try(FileOutputStream out=new FileOutputStream(f)){
                if(!bitmap.compress(Bitmap.CompressFormat.JPEG,91,out))
                    throw new IOException("Synthetic JPEG compression failed");
            }
        }finally{bitmap.recycle();}
    }
    @Override public boolean onCreate(){
        try{ensureImage(getContext());return true;}
        catch(IOException e){android.util.Log.e("WfsQaProvider","Cannot create test JPEG",e);return false;}
    }
    private static MatrixCursor cursor(String[] requested,String[] fallback) {
        return new MatrixCursor(requested==null?fallback:requested);
    }
    private static void add(MatrixCursor c,Map<String,Object> fields){
        Object[] values=new Object[c.getColumnCount()];
        for(int i=0;i<values.length;i++)values[i]=fields.get(c.getColumnName(i));
        c.addRow(values);
    }
    @Override public Cursor queryRoots(String[] projection){
        MatrixCursor out=cursor(projection,ROOT_COLUMNS);
        Map<String,Object> fields=new HashMap<>();
        fields.put(DocumentsContract.Root.COLUMN_ROOT_ID,ROOT);
        fields.put(DocumentsContract.Root.COLUMN_DOCUMENT_ID,ROOT);
        fields.put(DocumentsContract.Root.COLUMN_TITLE,"WFS QA Provider");
        fields.put(DocumentsContract.Root.COLUMN_SUMMARY,"Independent test-only provider");
        fields.put(DocumentsContract.Root.COLUMN_FLAGS,DocumentsContract.Root.FLAG_LOCAL_ONLY);
        fields.put(DocumentsContract.Root.COLUMN_ICON,android.R.drawable.ic_menu_gallery);
        fields.put(DocumentsContract.Root.COLUMN_MIME_TYPES,"image/jpeg");
        add(out,fields);
        return out;
    }
    private void appendDocument(MatrixCursor out,String documentId)throws FileNotFoundException{
        Map<String,Object> fields=new HashMap<>();
        fields.put(DocumentsContract.Document.COLUMN_DOCUMENT_ID,documentId);
        fields.put(DocumentsContract.Document.COLUMN_FLAGS,0);
        if(ROOT.equals(documentId)){
            fields.put(DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.MIME_TYPE_DIR);
            fields.put(DocumentsContract.Document.COLUMN_DISPLAY_NAME,"QA synthetic media");
            fields.put(DocumentsContract.Document.COLUMN_SIZE,0);
        }else if(PHOTO.equals(documentId)&&!missing(getContext())){
            File f=file(getContext());
            if(!f.isFile())throw new FileNotFoundException("Fixture absent");
            fields.put(DocumentsContract.Document.COLUMN_MIME_TYPE,"image/jpeg");
            fields.put(DocumentsContract.Document.COLUMN_DISPLAY_NAME,NAME);
            fields.put(DocumentsContract.Document.COLUMN_SIZE,f.length());
            fields.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED,f.lastModified());
        }else if(VIDEO.equals(documentId)){
            fields.put(DocumentsContract.Document.COLUMN_MIME_TYPE,"video/mp4");
            fields.put(DocumentsContract.Document.COLUMN_DISPLAY_NAME,VIDEO_NAME);
            try{fields.put(DocumentsContract.Document.COLUMN_SIZE,video(getContext()).length());}
            catch(IOException e){throw new FileNotFoundException(e.toString());}
        }else throw new FileNotFoundException("Unknown/deleted QA document: "+documentId);
        add(out,fields);
    }
    @Override public Cursor queryDocument(String id,String[] projection)throws FileNotFoundException{
        MatrixCursor out=cursor(projection,DOC_COLUMNS);
        appendDocument(out,id);
        return out;
    }
    @Override public Cursor queryChildDocuments(String parentId,String[] projection,String sortOrder)
            throws FileNotFoundException{
        if(!ROOT.equals(parentId))throw new FileNotFoundException("No children for "+parentId);
        MatrixCursor out=cursor(projection,DOC_COLUMNS);
        if(!missing(getContext()))appendDocument(out,PHOTO);
        return out;
    }
    @Override public boolean isChildDocument(String parentId,String documentId){
        return ROOT.equals(parentId)&&PHOTO.equals(documentId);
    }
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal)
            throws FileNotFoundException{
        if(VIDEO.equals(id)&&"r".equals(mode)){
            try{return ParcelFileDescriptor.open(video(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);}
            catch(IOException e){throw new FileNotFoundException(e.toString());}
        }
        if(!PHOTO.equals(id)||missing(getContext())||!"r".equals(mode))
            throw new FileNotFoundException("QA source deleted or mode rejected");
        return ParcelFileDescriptor.open(file(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
