package com.wfs.storyboost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real multi-clip storyboard with reorder/delete, single-clip preview and
 * Media3 sequence export. Clip transitions and audio mixing are not simulated.
 */
public final class StudioMergeActivity extends Activity {
    private static final int PICK_CLIPS=901;
    private final ArrayList<Uri> clips=new ArrayList<>();
    private final ExecutorService thumbs=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private VideoMergeEngine engine;
    private VideoView preview;
    private LinearLayout clipList;
    private TextView count,info;
    private int quality=OutputProfile.FULL_HD,selected=-1;
    private boolean working,destroyed;
    private java.util.concurrent.atomic.AtomicBoolean mergeStop=
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private ProgressBar progress;
    private AlertDialog dialog;
    private Uri lastSaved;
    private int d(int n){return StudioUi.dp(this,n);}
    private TextView txt(String text,int size,int color,boolean bold){
        return StudioUi.text(this,text,size,color,bold);
    }
    private Button button(String t,boolean main){return StudioUi.button(this,t,main);}
    private void gap(LinearLayout l,int n){StudioUi.gap(l,this,n);}
    private LinearLayout col(){return StudioUi.column(this);}
    private LinearLayout row(){return StudioUi.row(this);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(StudioUi.BG);
        getWindow().setNavigationBarColor(StudioUi.BG);build();
    }
    private void build(){
        LinearLayout root=col();root.setBackgroundColor(StudioUi.BG);
        root.setFitsSystemWindows(true);
        LinearLayout header=row();header.setPadding(d(12),d(9),d(12),d(7));
        Button back=button("‹",false);back.setTextSize(26);
        header.addView(back,new LinearLayout.LayoutParams(d(44),d(45)));
        back.setOnClickListener(v->finish());
        LinearLayout title=col();title.addView(txt("Video Merge Studio",19,StudioUi.TEXT,true));
        title.addView(txt("MULTI-CLIP  •  REORDER & JOIN",10,StudioUi.CYAN,true));
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(0,-2,1f);l.leftMargin=d(9);
        header.addView(title,l);
        root.addView(header);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout body=col();body.setPadding(d(16),d(13),d(16),d(30));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=StudioUi.card(this,16);
        hero.setBackground(StudioUi.gradient(this,0xFF343568,0xFF19283E,18));
        hero.addView(txt("Buat satu cerita dari banyak klip.",19,StudioUi.TEXT,true));
        gap(hero,8);
        hero.addView(txt("Pilih sampai 10 video. Ubah urutan, preview satu klip, lalu gabungkan menjadi MP4.",12,StudioUi.MUTED,false));
        gap(hero,12);
        Button add=button("＋  Tambahkan Video",true);
        hero.addView(add,new LinearLayout.LayoutParams(-1,d(48)));
        add.setOnClickListener(v->pickVideos());
        body.addView(hero);gap(body,15);

        preview=new VideoView(this);
        preview.setBackground(StudioUi.solid(this,0xFF080B18,17));
        body.addView(preview,new LinearLayout.LayoutParams(-1,d(190)));
        gap(body,12);
        LinearLayout actions=row();
        count=txt("Belum ada klip",13,StudioUi.TEXT,true);
        actions.addView(count,new LinearLayout.LayoutParams(0,-2,1f));
        Button play=button("▶ Play",false);
        actions.addView(play,new LinearLayout.LayoutParams(d(104),d(40)));
        play.setOnClickListener(v->{if(selected<0)return;
            if(preview.isPlaying())preview.pause();else preview.start();});
        body.addView(actions);gap(body,11);
        clipList=col();body.addView(clipList);
        info=txt("Klip akan digabung sesuai urutan. Belum ada transisi silang.",11,StudioUi.MUTED,false);
        gap(body,10);body.addView(info);

        LinearLayout qualityBar=row();qualityBar.setPadding(d(9),d(8),d(9),d(8));
        qualityBar.addView(txt("KUALITAS",11,StudioUi.CYAN,true),
                new LinearLayout.LayoutParams(0,-2,1f));
        String[] names={"HD","1080p","Max"};
        for(int i=0;i<3;i++){
            final int mode=i;
            Button q=button(names[i],i==quality);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(d(69),d(38));
            if(i>0)p.leftMargin=d(5);
            qualityBar.addView(q,p);
            q.setOnClickListener(v->{
                quality=mode;
                for(int index=0;index<qualityBar.getChildCount();index++){
                    View child=qualityBar.getChildAt(index);
                    if(child instanceof Button){
                        int selected=index-1;
                        child.setBackground(selected==quality?
                                StudioUi.gradient(this,0xFF9A57E9,0xFF3B7EEC,12):
                                StudioUi.solid(this,StudioUi.INNER,12));
                    }
                }
            });
        }
        root.addView(qualityBar);
        Button export=button("Gabungkan & Ekspor MP4   ↗",true);
        LinearLayout.LayoutParams e=new LinearLayout.LayoutParams(-1,d(55));
        e.setMargins(d(16),0,d(16),d(14));root.addView(export,e);
        export.setOnClickListener(v->startMerge());
        renderList();
        setContentView(root);
    }
    private void pickVideos(){
        if(working)return;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("video/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(i,PICK_CLIPS);}
        catch(Exception e){toast("Galeri tidak tersedia: "+e.getMessage());}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PICK_CLIPS||result!=RESULT_OK||data==null)return;
        ArrayList<Uri> incoming=new ArrayList<>();
        if(data.getClipData()!=null){
            for(int i=0;i<data.getClipData().getItemCount();i++)
                incoming.add(data.getClipData().getItemAt(i).getUri());
        }else if(data.getData()!=null)incoming.add(data.getData());
        for(Uri item:incoming){
            if(clips.size()>=10)break;
            if(clips.contains(item))continue;
            try{getContentResolver().takePersistableUriPermission(item,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
            catch(Exception ignored){}
            clips.add(item);
        }
        if(selected<0&&!clips.isEmpty())select(0);
        renderList();
    }
    private void select(int index){
        if(index<0||index>=clips.size())return;
        selected=index;
        preview.stopPlayback();
        preview.setVideoURI(clips.get(index));
        preview.setOnPreparedListener(player->{preview.seekTo(250);});
        renderList();
    }
    private String name(Uri uri){
        try(Cursor c=getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst())return c.getString(0);
        }catch(Exception ignored){}
        return "Video "+(clips.indexOf(uri)+1);
    }
    private void move(int index,int delta){
        int newPos=index+delta;
        if(newPos<0||newPos>=clips.size())return;
        Collections.swap(clips,index,newPos);
        if(selected==index)selected=newPos;else if(selected==newPos)selected=index;
        renderList();
    }
    private void renderList(){
        if(clipList==null)return;
        clipList.removeAllViews();
        count.setText(clips.isEmpty()?"Belum ada klip":clips.size()+" klip dalam storyboard");
        if(clips.isEmpty()){
            clipList.addView(txt("Tambah beberapa video untuk mulai menyusun urutan.",12,StudioUi.MUTED,false));
            return;
        }
        for(int i=0;i<clips.size();i++){
            final int index=i;
            Uri clip=clips.get(index);
            LinearLayout entry=row();
            entry.setPadding(d(9),d(8),d(9),d(8));
            entry.setBackground(StudioUi.solid(this,index==selected?0xFF34345B:StudioUi.PANEL,13));
            TextView number=txt(String.valueOf(index+1),14,StudioUi.LAVENDER,true);
            number.setGravity(Gravity.CENTER);
            number.setBackground(StudioUi.solid(this,0xFF202D4A,9));
            entry.addView(number,new LinearLayout.LayoutParams(d(33),d(48)));
            LinearLayout title=col();
            TextView file=txt(name(clip),12,StudioUi.TEXT,true);file.setMaxLines(1);file.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.addView(file);gap(title,3);
            long duration=MediaProbe.duration(this,clip);
            title.addView(txt(duration>0?String.format(java.util.Locale.US,"%d detik",duration/1000):"Preview",10,StudioUi.MUTED,false));
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1f);
            tp.leftMargin=d(9);entry.addView(title,tp);
            String[] symbols={"↑","↓","×"};
            for(int j=0;j<3;j++){
                final int action=j;
                Button b=button(symbols[j],false);b.setTextSize(18);
                LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(d(32),d(38));
                bp.leftMargin=d(4);entry.addView(b,bp);
                b.setOnClickListener(v->{
                    if(action==0)move(index,-1);
                    else if(action==1)move(index,1);
                    else{
                        clips.remove(index);
                        if(clips.isEmpty()){selected=-1;preview.stopPlayback();}
                        else if(selected>=clips.size())selected=clips.size()-1;
                        renderList();
                    }
                });
            }
            entry.setOnClickListener(v->select(index));
            clipList.addView(entry);gap(clipList,7);
        }
    }
    private void startMerge(){
        if(working)return;
        if(clips.isEmpty()){toast("Tambahkan video terlebih dahulu.");return;}
        working=true;
        mergeStop=new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicBoolean token=mergeStop;
        engine=new VideoMergeEngine();
        LinearLayout progressCard=StudioUi.card(this,17);
        progressCard.addView(txt("Sedang menggabungkan "+clips.size()+" klip",17,StudioUi.TEXT,true));
        gap(progressCard,12);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progressCard.addView(progress);
        dialog=new AlertDialog.Builder(this).setView(progressCard).setCancelable(false)
                .setNegativeButton("Batal",(d,w)->cancelMerge()).create();
        dialog.show();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try{
            engine.start(this,new ArrayList<>(clips),quality,new VideoMergeEngine.Listener(){
                @Override public void onCompleted(java.io.File file){
                    if(token.get())return;
                    thumbs.execute(()->{
                        try{
                            if(token.get())return;
                            Uri saved=MediaSaver.saveVideo(StudioMergeActivity.this,file,
                                    "WFS_JOIN_"+System.currentTimeMillis()+".mp4",token::get);
                            runOnUiThread(()->{
                                if(token.get()||destroyed){
                                    getContentResolver().delete(saved,null,null);
                                    return;
                                }
                                lastSaved=saved;engine.clean();finishMerge(null);
                            });
                        }catch(Exception ex){
                            if(!token.get())
                                runOnUiThread(()->finishMerge(ex.getMessage()));
                        }
                    });
                }
                @Override public void onFailure(String message){
                    if(!token.get())finishMerge(message);
                }
            });
            main.post(new Runnable(){
                @Override public void run(){
                    if(!working||engine==null||destroyed)return;
                    int value=engine.progress();if(value>=0)progress.setProgress(value);
                    main.postDelayed(this,400);
                }
            });
        }catch(Exception e){finishMerge(e.getMessage());}
    }
    private void cancelMerge(){
        mergeStop.set(true);
        if(engine!=null)engine.cancel();
        finishMerge("Penggabungan dibatalkan.");
    }
    private void finishMerge(String failure){
        working=false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(dialog!=null)dialog.dismiss();
        if(failure!=null){toast(failure);if(engine!=null)engine.clean();return;}
        new AlertDialog.Builder(this).setTitle("Video berhasil digabung")
            .setMessage("MP4 tersimpan di Galeri → Movies/WFS StoryBoost.")
            .setPositiveButton("Selesai",null)
            .setNeutralButton("Bagikan",(d,w)->share()).show();
    }
    private void share(){
        if(lastSaved==null)return;
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("video/mp4");
        send.putExtra(Intent.EXTRA_STREAM,lastSaved);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send,"Bagikan gabungan video"));
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onPause(){super.onPause();if(preview!=null)preview.pause();}
    @Override protected void onDestroy(){
        destroyed=true;mergeStop.set(true);main.removeCallbacksAndMessages(null);
        if(engine!=null&&working)engine.cancel();
        thumbs.shutdownNow();
        if(preview!=null)preview.stopPlayback();
        super.onDestroy();
    }
}
