package com.wfs.storyboost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import java.util.ArrayList;

/** Project-focused landing screen. One project opens the actual media timeline editor. */
public final class StudioHomeActivity extends Activity {
    private static final int PICK=802;
    private int requestedRatio=0;
    private LinearLayout recentCard;
    private TextView emptyRecent;
    private ScrollView mainScroll;
    private final int BG=StudioUi.BG,TEXT=StudioUi.TEXT,MUTED=StudioUi.MUTED;
    private int d(int x){return StudioUi.dp(this,x);}
    private TextView text(String s,int size,int color,boolean bold){return StudioUi.text(this,s,size,color,bold);}
    private void gap(LinearLayout l,int size){StudioUi.gap(l,this,size);}
    private LinearLayout card(int pad){return StudioUi.card(this,pad);}
    private Button button(String label,boolean primary){return StudioUi.button(this,label,primary);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        render();
        if(getIntent().getBooleanExtra("qa_demo",false)){
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::tryDemo,650);
        }
    }
    private void render(){
        LinearLayout root=StudioUi.column(this);root.setBackgroundColor(BG);root.setFitsSystemWindows(true);
        mainScroll=new ScrollView(this);mainScroll.setFillViewport(true);mainScroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=StudioUi.column(this);content.setPadding(d(18),d(17),d(18),d(22));mainScroll.addView(content);
        root.addView(mainScroll,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout header=StudioUi.row(this);
        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);
        header.addView(logo,new LinearLayout.LayoutParams(d(49),d(49)));
        LinearLayout identity=StudioUi.column(this);identity.addView(text("WFS StoryBoost",20,TEXT,true));
        gap(identity,3);identity.addView(text("CREATIVE EDITING SUITE  /  v2.2",10,StudioUi.CYAN,true));
        LinearLayout.LayoutParams identityLp=new LinearLayout.LayoutParams(0,-2,1);
        identityLp.leftMargin=d(8);header.addView(identity,identityLp);
        TextView settings=text("⚙",23,StudioUi.LAVENDER,true);
        settings.setGravity(Gravity.CENTER);settings.setContentDescription("Informasi aplikasi");
        header.addView(settings,new LinearLayout.LayoutParams(d(45),d(45)));
        settings.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("WFS StoryBoost")
                .setMessage("Editor offline • Pengolahan non-destruktif sebelum ekspor. Video dan foto disimpan ke Galeri. Pengunggahan ke WhatsApp dapat memicu kompresi dari platform.")
                .setPositiveButton("Tutup",null).show());
        content.addView(header);gap(content,17);

        FrameLayout hero=new FrameLayout(this);
        hero.setBackground(StudioUi.solid(this,0xFF192447,22));hero.setClipToOutline(true);
        hero.addView(new StudioHeroView(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout overlays=StudioUi.column(this);overlays.setPadding(d(19),d(18),d(19),d(18));
        overlays.addView(text("✦  CREATE SOMETHING BEAUTIFUL",10,0xFFDEDBFF,true));
        overlays.addView(new View(this),new LinearLayout.LayoutParams(1,0,1f));
        TextView title=text("Your Story.\nYour Studio.",29,TEXT,true);
        title.setLineSpacing(d(3),1);overlays.addView(title);gap(overlays,7);
        overlays.addView(text("Editor foto & video kreatif dengan timeline, filter, trim dan kontrol kualitas.",12,0xFFEEE8FF,false));
        gap(overlays,13);
        Button create=button("＋  Proyek Baru",true);
        overlays.addView(create,new LinearLayout.LayoutParams(-1,d(48)));
        create.setOnClickListener(v->choose(0));
        hero.addView(overlays,new FrameLayout.LayoutParams(-1,-1));
        content.addView(hero,new LinearLayout.LayoutParams(-1,d(278)));
        gap(content,19);

        LinearLayout heading=StudioUi.row(this);
        heading.addView(text("Studio Tools",21,TEXT,true),new LinearLayout.LayoutParams(0,-2,1f));
        heading.addView(text("PILIH WORKFLOW",10,StudioUi.LAVENDER,true));
        content.addView(heading);gap(content,10);

        LinearLayout first=StudioUi.row(this);
        first.addView(tile("▧","Photo Editor","Filter & komposisi",0xFF9B6FE9,()->choose(0,"image/*")),new LinearLayout.LayoutParams(0,d(113),1f));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(113),1f);lp.leftMargin=d(10);
        first.addView(tile("▶","Video Editor","Trim & timeline",0xFF297FC0,()->choose(0,"video/*")),lp);
        content.addView(first);gap(content,10);

        LinearLayout second=StudioUi.row(this);
        second.addView(tile("✦","Quick Enhance","10 media sekaligus",0xFF9B658A,()->startActivity(new Intent(this,MainActivity.class))),
                new LinearLayout.LayoutParams(0,d(112),1f));
        LinearLayout.LayoutParams lp2=new LinearLayout.LayoutParams(0,d(112),1f);lp2.leftMargin=d(10);
        second.addView(tile("◫","Coba Editor","Media demo offline",0xFF2D988F,()->tryDemo()),lp2);
        content.addView(second);gap(content,19);

        content.addView(text("Format Cepat",21,TEXT,true));gap(content,9);
        LinearLayout formats=StudioUi.row(this);
        String[] names={"▯  9:16","▭  16:9","□  1:1"};
        for(int i=0;i<3;i++){
            final int ratio=i;
            Button b=button(names[i],false);
            LinearLayout.LayoutParams parms=new LinearLayout.LayoutParams(0,d(47),1f);
            if(i>0)parms.leftMargin=d(9);
            formats.addView(b,parms);b.setOnClickListener(v->choose(ratio));
        }
        content.addView(formats);gap(content,21);

        LinearLayout recentHeading=StudioUi.row(this);
        recentHeading.addView(text("Proyek Terakhir",21,TEXT,true),new LinearLayout.LayoutParams(0,-2,1f));
        TextView clear=text("BERSIHKAN",10,StudioUi.MUTED,true);
        clear.setPadding(d(9),d(7),0,d(7));recentHeading.addView(clear);
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Bersihkan riwayat?")
                .setMessage("Hanya menghapus daftar proyek dalam aplikasi. Media asli dan file ekspor tidak dihapus.")
                .setNegativeButton("Batal",null).setPositiveButton("Bersihkan",(a,b)->{
                    getSharedPreferences("studio-projects",MODE_PRIVATE).edit().clear().apply();populateRecent();
                }).show());
        content.addView(recentHeading);gap(content,11);
        recentCard=card(13);content.addView(recentCard);
        emptyRecent=text("Belum ada proyek. Impor foto atau video, atau coba demo.",12,MUTED,false);
        populateRecent();gap(content,15);
        content.addView(text("PRIVATE BY DESIGN  •  Offline-first  •  No watermark",11,MUTED,false));

        LinearLayout bottom=StudioUi.row(this);
        bottom.setBackgroundColor(0xFF121A2B);bottom.setPadding(d(10),d(8),d(10),d(7));
        nav(bottom,"⌂\nBeranda",true,()->mainScroll.smoothScrollTo(0,0));
        nav(bottom,"＋\nBuat",false,()->choose(0));
        nav(bottom,"◷\nProyek",false,()->{
            recentCard.requestFocus();mainScroll.smoothScrollTo(0,recentCard.getTop());
        });
        root.addView(bottom,new LinearLayout.LayoutParams(-1,d(62)));
        setContentView(root);
    }
    private LinearLayout tile(String glyph,String title,String detail,int accent,Runnable action){
        LinearLayout tile=card(11);tile.setBackground(StudioUi.gradient(this,0xFF1B2440,0xFF151B2E,18));
        TextView icon=text(glyph,20,0xFFFFFFFF,true);icon.setGravity(Gravity.CENTER);
        icon.setBackground(StudioUi.gradient(this,accent,0xFF32427D,12));
        tile.addView(icon,new LinearLayout.LayoutParams(d(39),d(39)));gap(tile,7);
        tile.addView(text(title,14,TEXT,true));gap(tile,3);tile.addView(text(detail,10,MUTED,false));
        tile.setClickable(true);tile.setFocusable(true);tile.setOnClickListener(v->action.run());
        return tile;
    }
    private void nav(LinearLayout parent,String text,boolean selected,Runnable action){
        TextView label=text(text,12,selected?StudioUi.LAVENDER:MUTED,true);
        label.setGravity(Gravity.CENTER);parent.addView(label,new LinearLayout.LayoutParams(0,-1,1f));
        label.setOnClickListener(v->action.run());
    }
    private void choose(int ratio){choose(ratio,"*/*");}
    private void choose(int ratio,String mime){
        requestedRatio=ratio;
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType(mime);
        if("*/*".equals(mime)){
            pick.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});
        }
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(pick,PICK);}
        catch(Exception e){toast("Galeri tidak tersedia: "+e.getMessage());}
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req!=PICK||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        catch(Exception ignored){}
        saveRecent(uri);openEditor(uri,requestedRatio,false);
    }
    private void tryDemo(){
        try{openEditor(StudioDemoMedia.generate(this),0,true);}
        catch(Exception e){toast("Demo gagal: "+e.getMessage());}
    }
    private void openEditor(Uri uri,int ratio,boolean demo){
        Intent i=new Intent(this,StudioEditorActivity.class);
        i.putExtra("media_uri",uri.toString());i.putExtra("ratio",ratio);i.putExtra("demo",demo);
        startActivity(i);
    }
    private void saveRecent(Uri uri){
        SharedPreferences p=getSharedPreferences("studio-projects",MODE_PRIVATE);
        ArrayList<String> values=new ArrayList<>();
        values.add(uri.toString());
        for(int j=0;j<5;j++){
            String old=p.getString("recent_"+j,"");
            if(!old.isEmpty()&&!values.contains(old))values.add(old);
        }
        SharedPreferences.Editor edit=p.edit();
        for(int j=0;j<5;j++)edit.putString("recent_"+j,j<values.size()?values.get(j):"");
        edit.apply();
    }
    private void populateRecent(){
        if(recentCard==null)return;
        recentCard.removeAllViews();
        SharedPreferences p=getSharedPreferences("studio-projects",MODE_PRIVATE);
        int count=0;
        for(int i=0;i<5;i++){
            String value=p.getString("recent_"+i,"");
            if(value.isEmpty())continue;
            final Uri uri=Uri.parse(value);
            LinearLayout row=StudioUi.row(this);
            TextView glyph=text("◫",25,StudioUi.LAVENDER,true);
            glyph.setGravity(Gravity.CENTER);glyph.setBackground(StudioUi.solid(this,0xFF293350,11));
            row.addView(glyph,new LinearLayout.LayoutParams(d(48),d(48)));
            LinearLayout labels=StudioUi.column(this);
            labels.addView(text("Media Proyek "+(i+1),13,TEXT,true));gap(labels,4);
            labels.addView(text("Ketuk untuk melanjutkan editing",11,MUTED,false));
            LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(0,-2,1f);
            labelParams.leftMargin=d(12);row.addView(labels,labelParams);
            row.addView(text("›",25,MUTED,true));
            recentCard.addView(row);gap(recentCard,12);count++;
            row.setOnClickListener(v->openEditor(uri,0,false));
        }
        if(count==0)recentCard.addView(emptyRecent);
    }
    private void toast(String s){android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_LONG).show();}
    @Override protected void onResume(){super.onResume();if(recentCard!=null)populateRecent();}
}
