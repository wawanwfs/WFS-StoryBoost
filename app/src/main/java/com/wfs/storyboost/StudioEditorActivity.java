package com.wfs.storyboost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A real single-source timeline editor with playback, frame thumbnails, trim, split-export,
 * non-destructive ratio/crop/filter settings and the same high-quality export engines
 * used by Quick Enhance. No mocked upload or buttons.
 */
public final class StudioEditorActivity extends Activity {
    private static final int BG=StudioUi.BG,TEXT=StudioUi.TEXT,MUTED=StudioUi.MUTED;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    // Cancel only outdated image previews. Never discard export or metadata work.
    private final ThreadPoolExecutor previewWorker=new ThreadPoolExecutor(
            1,1,0L,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());
    private final ArrayDeque<RenderJob> exports=new ArrayDeque<>();
    private VideoEngine activeVideoEngine;
    private Uri mediaUri,lastExport;
    private boolean isVideo,isDemo,destroyed,exportBusy,exportCancelled;
    private java.util.concurrent.atomic.AtomicBoolean exportStop =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private long durationMs=0,trimStartMs=0,trimEndMs=0,splitMs=-1;
    private int ratio=0,preset=1,framing=AspectRatioProfile.FIT,quality=OutputProfile.MAX_DETAIL;
    private float focusX=.5f,focusY=.5f;
    private int completed,failed,downgradedVideoExports;
    private String lastEncodedVideoCanvas="";
    private boolean dualOutput=false;
    private int socialProfile=SocialExportProfile.CUSTOM;
    private int customWidth=1080,customHeight=1920;
    private AiPhotoAnalyzer.Result aiResult;
    private boolean aiRunning=false,aiAutoStart=false;
    private TextView aiStatus;
    private Button aiApplyButton;
    private int lookScrollX=0;
    private SocialSafeZoneView safeZoneView;
    private TextView timeLabel,qualityLabel,trimSummary,sourceLabel,exportStatus,previewLabel;
    private ImageView photo;
    private VideoView video;
    private SeekBar seekbar;
    private FrameLayout canvasArea,outputCanvas;
    private View cropTouchLayer;
    private float cropDownX,cropDownY,cropDownFocusX,cropDownFocusY;
    private LinearLayout toolPane,thumbStrip;
    private Button playButton,exportButton,compareButton;
    private android.app.AlertDialog exportDialog;
    private android.widget.ProgressBar exportProgress;
    private boolean compareOriginal=false;
    private float brightness=0f,contrast=0f,saturation=0f;
    private String caption="";
    private int captionColor=Color.WHITE;
    private boolean muteAudio=false;
    private boolean portraitBokeh=false,aiDenoise=false;
    private final ArrayDeque<EditState> undoStates=new ArrayDeque<>();
    private final ArrayDeque<EditState> redoStates=new ArrayDeque<>();
    private EditState lastState;
    private boolean restoringHistory=false;
    private Button undoButton,redoButton;
    private int activeTool=2;
    private final Runnable draftWriter=this::persistDraft;
    private final Runnable adjustedPreview=()->{if(!destroyed&&!isVideo)refreshImagePreview();};
    private boolean draftRestored=false;
    private CaptionPreviewView captionPreview;
    private Bitmap imagePreviewBitmap;
    private Runnable playTicker;
    private int renderToken=0;
    private final int dp(int a){return StudioUi.dp(this,a);}
    private TextView txt(String s,int size,int col,boolean bold){return StudioUi.text(this,s,size,col,bold);}
    private Button btn(String s,boolean primary){return StudioUi.button(this,s,primary);}
    private void gap(LinearLayout l,int n){StudioUi.gap(l,this,n);}
    private LinearLayout row(){return StudioUi.row(this);}
    private LinearLayout column(){return StudioUi.column(this);}
    private LinearLayout card(int padding){return StudioUi.card(this,padding);}
    private static final class EditState {
        final ExportOptions options;
        final long splitAt;
        final boolean dual;
        final String signature;
        EditState(ExportOptions opts,long splitAt,boolean dual){
            this.options=opts;this.splitAt=splitAt;this.dual=dual;
            signature=opts.preset+"|"+opts.ratio+"|"+opts.frameStyle+"|"+opts.qualityMode+"|"+
                    opts.focusX+"|"+opts.focusY+"|"+opts.brightness+"|"+opts.contrast+"|"+
                    opts.saturation+"|"+opts.caption+"|"+opts.captionColor+"|"+opts.muteAudio+
                    "|"+opts.socialProfile+"|"+opts.customWidth+"|"+opts.customHeight+
                    "|"+opts.portraitBokeh+"|"+opts.aiDenoise+"|"+opts.trimStartMs+
                    "|"+opts.trimEndMs+"|"+splitAt+"|"+dual;
        }
    }
    private EditState captureEdit(){
        return new EditState(options(),splitMs,dualOutput);
    }
    private void rememberEdit(){
        if(restoringHistory||destroyed||isDemo&&mediaUri==null)return;
        EditState next;
        try{next=captureEdit();}catch(Exception ignored){return;}
        if(lastState!=null&&!lastState.signature.equals(next.signature)){
            undoStates.addLast(lastState);
            while(undoStates.size()>35)undoStates.removeFirst();
            redoStates.clear();
        }
        lastState=next;
        updateUndoButtons();
        if(!isDemo&&mediaUri!=null){
            // A pending 800ms postDelayed task is lost if Android kills the
            // foreground process without onPause. Commit completed edit events
            // synchronously so Recent can recover after an abrupt force-stop.
            // This writes only the small private draft, never source pixels.
            handler.removeCallbacks(draftWriter);
            persistDraft();
        }
    }
    private void persistDraft(){
        if(isDemo||mediaUri==null||destroyed)return;
        try{
            StudioDraftStore.save(this,mediaUri,options(),splitMs,dualOutput);
        }catch(RuntimeException ignored){
            // A stale URI or full storage must never prevent the editor opening.
        }
    }
    private void restoreRecentDraft(){
        if(isDemo||!getIntent().getBooleanExtra("resume_project",false))return;
        StudioDraftStore.Draft saved=StudioDraftStore.load(this,mediaUri);
        if(saved==null)return;
        ExportOptions o=saved.options;
        // Never enable photo-only AI controls for a video draft.
        if(isVideo&&(o.aiDenoise||o.portraitBokeh))return;
        preset=o.preset;ratio=o.ratio;framing=o.frameStyle;quality=o.qualityMode;
        focusX=o.focusX;focusY=o.focusY;brightness=o.brightness;contrast=o.contrast;
        saturation=o.saturation;caption=o.caption;captionColor=o.captionColor;
        muteAudio=o.muteAudio;socialProfile=o.socialProfile;
        customWidth=o.customWidth;customHeight=o.customHeight;
        portraitBokeh=o.portraitBokeh;aiDenoise=o.aiDenoise;
        trimStartMs=o.trimStartMs;trimEndMs=o.trimEndMs;
        splitMs=saved.splitMs;dualOutput=saved.dualOutput;
        draftRestored=true;
    }
    private void updateUndoButtons(){
        if(undoButton!=null){
            undoButton.setEnabled(!undoStates.isEmpty());
            undoButton.setAlpha(undoStates.isEmpty()?.45f:1f);
        }
        if(redoButton!=null){
            redoButton.setEnabled(!redoStates.isEmpty());
            redoButton.setAlpha(redoStates.isEmpty()?.45f:1f);
        }
    }
    private void historyNavigate(boolean undo){
        ArrayDeque<EditState> source=undo?undoStates:redoStates;
        ArrayDeque<EditState> destination=undo?redoStates:undoStates;
        if(source.isEmpty()||exportBusy)return;
        EditState state=source.removeLast();
        destination.addLast(captureEdit());
        restoringHistory=true;
        try{
            ExportOptions o=state.options;
            preset=o.preset;ratio=o.ratio;framing=o.frameStyle;quality=o.qualityMode;
            focusX=o.focusX;focusY=o.focusY;brightness=o.brightness;contrast=o.contrast;
            saturation=o.saturation;caption=o.caption;captionColor=o.captionColor;
            muteAudio=o.muteAudio;socialProfile=o.socialProfile;
            customWidth=o.customWidth;customHeight=o.customHeight;
            portraitBokeh=o.portraitBokeh;aiDenoise=o.aiDenoise;
            trimStartMs=o.trimStartMs;trimEndMs=o.trimEndMs;
            splitMs=state.splitAt;dualOutput=state.dual;
            lastState=captureEdit();
            showTool(activeTool);refreshSettings();updateCaptionPreview();
        }finally{restoringHistory=false;updateUndoButtons();}
        handler.removeCallbacks(draftWriter);
        persistDraft(); // Undo/Redo is a committed edit as well.
    }
    private static final class RenderJob {
        final ExportOptions options;
        final String title;
        RenderJob(ExportOptions o,String title){options=o;this.title=title;}
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        String from=getIntent().getStringExtra("media_uri");
        if(from==null||from.isEmpty()){finish();return;}
        mediaUri=Uri.parse(from);isDemo=getIntent().getBooleanExtra("demo",false);
        if(!SourceAccess.canRead(this,mediaUri)){
            Toast.makeText(this,"Media tidak tersedia atau izin baca telah dicabut. Impor ulang dari galeri.",Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String mime;
        try{mime=getContentResolver().getType(mediaUri);}
        catch(RuntimeException unavailable){
            Toast.makeText(this,"Tipe media tidak dapat dibaca. Impor ulang sumber.",Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        isVideo=(mime!=null&&mime.startsWith("video/"))
                ||from.toLowerCase(Locale.US).matches(".*\\.(mp4|mov|m4v|webm)(\\?.*)?$");
        ratio=getIntent().getIntExtra("ratio",0);
        if(ratio<0||ratio>=AspectRatioProfile.count())ratio=0;
        aiAutoStart=getIntent().getBooleanExtra("auto_ai",false);
        if(isVideo){
            durationMs=MediaProbe.duration(this,mediaUri);
            if(durationMs<=0)durationMs=30000;
        }
        restoreRecentDraft();
        build();
        lastState=captureEdit();
        updateUndoButtons();
        if(draftRestored)Toast.makeText(this,"Draft edit dipulihkan",Toast.LENGTH_SHORT).show();
        if(isVideo){renderThumbnails();startTicker();}
        else{
            refreshImagePreview();
            if(aiAutoStart){showTool(8);startAiAnalysis();}
        }
        canvasArea.post(this::layoutPreviewCanvas);
        refreshSourceMetadata();
    }
    private void build(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setFitsSystemWindows(true);
        // Editor header
        LinearLayout header=row();header.setPadding(dp(8),dp(6),dp(8),dp(7));
        Button back=btn("‹",false);
        back.setTextSize(26);
        back.setContentDescription("Kembali ke proyek");
        header.addView(back,new LinearLayout.LayoutParams(dp(40),dp(44)));
        back.setOnClickListener(v->finish());
        LinearLayout title=column();
        TextView editorTitle=txt("Editor",18,TEXT,true);
        editorTitle.setSingleLine(true);
        editorTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.addView(editorTitle);gap(title,3);
        TextView editorSubtitle=txt(isVideo?"VIDEO • WFS":"FOTO • WFS",9,StudioUi.CYAN,true);
        editorSubtitle.setSingleLine(true);
        editorSubtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.addView(editorSubtitle);
        LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(0,-2,1f);titleLp.leftMargin=dp(6);
        header.addView(title,titleLp);
        undoButton=btn("↶",false);
        undoButton.setContentDescription("Undo perubahan terakhir");
        undoButton.setTextSize(22);
        header.addView(undoButton,new LinearLayout.LayoutParams(dp(35),dp(42)));
        undoButton.setOnClickListener(v->historyNavigate(true));
        redoButton=btn("↷",false);
        redoButton.setContentDescription("Redo perubahan");
        redoButton.setTextSize(22);
        LinearLayout.LayoutParams redoLp=new LinearLayout.LayoutParams(dp(35),dp(42));
        redoLp.leftMargin=dp(4);
        header.addView(redoButton,redoLp);
        redoButton.setOnClickListener(v->historyNavigate(false));
        exportButton=btn("Ekspor  ↗",true);
        header.addView(exportButton,new LinearLayout.LayoutParams(dp(87),dp(44)));
        exportButton.setOnClickListener(v->confirmExport());
        root.addView(header);

        // Aspect-specific media monitor; Canvas body expands to available screen height.
        canvasArea=new FrameLayout(this);canvasArea.setBackgroundColor(0xFF050811);
        root.addView(canvasArea,new LinearLayout.LayoutParams(-1,0,1f));
        TextView policy=txt("Edit non-destruktif • Media asli tetap aman",9,MUTED,false);
        policy.setGravity(Gravity.CENTER);
        policy.setPadding(dp(8),dp(4),dp(8),dp(4));
        root.addView(policy,new LinearLayout.LayoutParams(-1,-2));
        outputCanvas=new FrameLayout(this);
        outputCanvas.setBackground(StudioUi.solid(this,0xFF171D2B,8));
        outputCanvas.setClipToOutline(true);
        photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photo.setBackgroundColor(Color.BLACK);photo.setContentDescription("Pratinjau foto");
        video=new VideoView(this);video.setBackgroundColor(Color.BLACK);
        outputCanvas.addView(photo,new FrameLayout.LayoutParams(-1,-1));
        outputCanvas.addView(video,new FrameLayout.LayoutParams(-1,-1));
        safeZoneView=new SocialSafeZoneView(this);
        safeZoneView.setVisibility(View.GONE);
        outputCanvas.addView(safeZoneView,new FrameLayout.LayoutParams(-1,-1));
        captionPreview=new CaptionPreviewView(this);
        outputCanvas.addView(captionPreview,new FrameLayout.LayoutParams(-1,-1));
        cropTouchLayer=new View(this);
        cropTouchLayer.setContentDescription("Drag to reposition image crop focus");
        cropTouchLayer.setClickable(true);
        cropTouchLayer.setVisibility(View.GONE);
        cropTouchLayer.setOnTouchListener((view,event)->{
            if(framing!=AspectRatioProfile.CROP)return false;
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    cropDownX=event.getX();cropDownY=event.getY();
                    cropDownFocusX=focusX;cropDownFocusY=focusY;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX=(event.getX()-cropDownX)/Math.max(1,view.getWidth());
                    float deltaY=(event.getY()-cropDownY)/Math.max(1,view.getHeight());
                    focusX=Math.max(0f,Math.min(1f,cropDownFocusX-deltaX));
                    focusY=Math.max(0f,Math.min(1f,cropDownFocusY-deltaY));
                    previewLabel.setText("CROP FOCUS • X "+Math.round(focusX*100)+
                            "% / Y "+Math.round(focusY*100)+"%");
                    return true;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    refreshSettings();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    focusX=cropDownFocusX;focusY=cropDownFocusY;
                    refreshSettings();
                    return true;
                default:return true;
            }
        });
        outputCanvas.addView(cropTouchLayer,new FrameLayout.LayoutParams(-1,-1));
        updateCaptionPreview();
        if(isVideo){
            photo.setVisibility(View.GONE);video.setVisibility(View.VISIBLE);
            video.setVideoURI(mediaUri);
            video.setOnPreparedListener(p->{
                try{p.setLooping(false);video.seekTo((int)Math.min(trimStartMs,Integer.MAX_VALUE));}
                catch(Exception ignored){}
            });
            video.setOnCompletionListener(mp->setPlaybackControl(false));
        }else{video.setVisibility(View.GONE);}
        canvasArea.addView(outputCanvas,new FrameLayout.LayoutParams(dp(180),dp(250),Gravity.CENTER));
        previewLabel=txt(isVideo?"VIDEO SOURCE PREVIEW":"LIVE PHOTO PREVIEW",9,StudioUi.GREEN,true);
        previewLabel.setPadding(dp(9),dp(5),dp(9),dp(5));
        previewLabel.setBackground(StudioUi.solid(this,0xCE13233A,8));
        FrameLayout.LayoutParams labelLp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);
        labelLp.leftMargin=dp(12);labelLp.topMargin=dp(12);
        canvasArea.addView(previewLabel,labelLp);
        canvasArea.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=or-ol||b-t!=ob-ot)layoutPreviewCanvas();
        });

        // Transport / time / compare
        LinearLayout transport=row();transport.setPadding(dp(13),dp(7),dp(13),dp(5));
        playButton=btn("▶",false);
        playButton.setContentDescription("Putar video");
        if(isVideo){
            transport.addView(playButton,new LinearLayout.LayoutParams(dp(45),dp(40)));
            playButton.setOnClickListener(v->togglePlay());
        }
        timeLabel=txt(isVideo?"00:00 / "+clock(durationMs):"FOTO  •  1 FRAME",12,TEXT,true);
        LinearLayout.LayoutParams clockLp=new LinearLayout.LayoutParams(0,-1,1f);clockLp.leftMargin=dp(12);
        transport.addView(timeLabel,clockLp);
        compareButton=btn("↔ Sebelum",false);
        compareButton.setContentDescription("Lihat sebelum diedit");
        transport.addView(compareButton,new LinearLayout.LayoutParams(dp(104),dp(39)));
        compareButton.setOnClickListener(v->toggleCompare());
        root.addView(transport);
        seekbar=new SeekBar(this);seekbar.setMax(1000);seekbar.setProgress(0);
        seekbar.setProgressTintList(android.content.res.ColorStateList.valueOf(StudioUi.LAVENDER));
        seekbar.setThumbTintList(android.content.res.ColorStateList.valueOf(StudioUi.CYAN));
        seekbar.setEnabled(isVideo);
        seekbar.setVisibility(isVideo?View.VISIBLE:View.GONE);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int progress,boolean user){
                if(!user||!isVideo)return;
                long pos=Math.round(durationMs*(progress/1000d));
                video.seekTo((int)Math.min(Integer.MAX_VALUE,pos));
                timeLabel.setText(clock(pos)+" / "+clock(durationMs));
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){}
        });
        root.addView(seekbar,new LinearLayout.LayoutParams(-1,dp(32)));

        // Timeline with real frames; images occupy a single still strip.
        LinearLayout timelineCard=column();
        timelineCard.setPadding(dp(13),dp(4),dp(13),dp(9));
        timelineCard.setBackgroundColor(0xFF101729);
        LinearLayout tlRow=row();
        tlRow.addView(txt("TIMELINE  •  "+(isVideo?"VIDEO":"STILL PHOTO"),10,StudioUi.CYAN,true),
                new LinearLayout.LayoutParams(0,-2,1f));
        sourceLabel=txt("Membaca sumber…",10,MUTED,false);tlRow.addView(sourceLabel);
        timelineCard.addView(tlRow);gap(timelineCard,8);
        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        thumbStrip=row();thumbStrip.setPadding(dp(4),dp(2),dp(4),dp(2));
        scroll.addView(thumbStrip);
        timelineCard.addView(scroll,new LinearLayout.LayoutParams(-1,dp(59)));
        root.addView(timelineCard);

        // Tool selector; panel below contains real controls for each mode.
        HorizontalScrollView toolsScroll=new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=row();tabs.setPadding(dp(9),dp(8),dp(9),dp(8));
        String[] labels={"✂  Trim","▯  Rasio","✦  Filter","◧  Frame",
                "☼  Adjust","Aa  Teks","♫  Audio","☷  Ekspor","✧  AI Photo"};
        for(int i=0;i<labels.length;i++){
            final int tab=i;
            Button b=btn(labels[i],false);b.setTextSize(12);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(i==0?91:94),dp(44));
            if(i>0)p.leftMargin=dp(7);
            tabs.addView(b,p);b.setOnClickListener(v->showTool(tab));
        }
        toolsScroll.addView(tabs);root.addView(toolsScroll,new LinearLayout.LayoutParams(-1,dp(62)));
        toolPane=column();toolPane.setPadding(dp(13),dp(8),dp(13),dp(10));
        toolPane.setBackground(StudioUi.solid(this,0xFF161D31,0));
        android.widget.ScrollView toolScroll=new android.widget.ScrollView(this);
        toolScroll.setFillViewport(false);
        toolScroll.setVerticalScrollBarEnabled(false);
        toolScroll.setBackgroundColor(0xFF161D31);
        toolScroll.addView(toolPane);
        root.addView(toolScroll,new LinearLayout.LayoutParams(-1,dp(188)));
        showTool(isVideo?0:2);
        setContentView(root);
    }
    private void layoutPreviewCanvas(){
        if(canvasArea==null||outputCanvas==null)return;
        int w=canvasArea.getWidth()-dp(28),h=canvasArea.getHeight()-dp(17);
        if(w<=0||h<=0)return;
        double aspect=9d/16d;
        try{
            AspectRatioProfile.Size nativeSize=MediaDimensions.read(this,mediaUri);
            aspect=AspectRatioProfile.aspect(ratio,nativeSize.width,nativeSize.height,
                    customWidth,customHeight);
        }catch(Exception ignored){}
        int newW=Math.min(w,(int)Math.floor(h*aspect));
        int newH=Math.min(h,(int)Math.floor(newW/aspect));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(
                Math.max(dp(50),newW),Math.max(dp(50),newH),Gravity.CENTER);
        outputCanvas.setLayoutParams(lp);
        if(cropTouchLayer!=null)cropTouchLayer.setVisibility(framing==AspectRatioProfile.CROP
                ?View.VISIBLE:View.GONE);
        if(safeZoneView!=null){
            safeZoneView.setProfile(socialProfile);
            safeZoneView.setVisibility(socialProfile==SocialExportProfile.CUSTOM
                    ?View.GONE:View.VISIBLE);
        }
        // PhotoEngine already renders the requested crop. Do not crop it a
        // second time in the widget, especially with small display rounding.
        if(photo!=null)photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        updateCaptionPreview();
    }
    private void refreshSourceMetadata(){
        io.execute(()->{
            try{
                AspectRatioProfile.Size d=MediaDimensions.read(this,mediaUri);
                QualityAdvisor.Report q=QualityAdvisor.analyze(
                        d.width,d.height,1080,1920,AspectRatioProfile.CROP);
                runOnUiThread(()->{
                    sourceLabel.setText(d.width+"×"+d.height);
                    if(isVideo)previewLabel.setText("VIDEO SOURCE • FILTER SAAT EKSPOR");
                });
            }catch(Exception ex){runOnUiThread(()->sourceLabel.setText("Resolusi tak terbaca"));}
        });
    }
    private void renderThumbnails(){
        io.execute(()->{
            MediaMetadataRetriever reader=new MediaMetadataRetriever();
            try{
                reader.setDataSource(this,mediaUri);
                final int n=8;
                for(int i=0;i<n&&!destroyed;i++){
                    long us=((durationMs*Math.max(0,i))/(n-1))*1000L;
                    Bitmap frame=reader.getFrameAtTime(us,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if(frame==null)continue;
                    Bitmap thumb=Bitmap.createScaledBitmap(frame,dp(60),dp(58),true);
                    if(thumb!=frame)frame.recycle();
                    final int index=i;
                    runOnUiThread(()->{
                        if(destroyed){thumb.recycle();return;}
                        ImageView tile=new ImageView(this);
                        tile.setImageBitmap(thumb);tile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        tile.setBackground(StudioUi.solid(this,0xFF243047,7));tile.setClipToOutline(true);
                        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(60),dp(56));
                        if(thumbStrip.getChildCount()>0)p.leftMargin=dp(3);
                        thumbStrip.addView(tile,p);
                        tile.setOnClickListener(v->{
                            long ms=Math.round(durationMs*index/(double)(n-1));
                            video.seekTo((int)Math.min(ms,Integer.MAX_VALUE));
                            seekbar.setProgress((int)(1000d*ms/durationMs));
                        });
                    });
                }
            }catch(Exception ex){
                runOnUiThread(()->thumbStrip.addView(txt("Preview thumbnail tidak tersedia",12,MUTED,false)));
            }finally{try{reader.release();}catch(Exception ignored){}}
        });
    }
    private void refreshImagePreview(){
        if(isVideo)return;
        final int token=++renderToken;
        ExportOptions options=options();
        final boolean showOriginal=compareOriginal;
        previewWorker.getQueue().clear();
        if((options.portraitBokeh||options.aiDenoise)&&!showOriginal)
            previewLabel.setText("AI • Menjalankan model offline...");
        previewWorker.execute(()->{
            Bitmap rendered=null;
            String failure=null;
            try{
                rendered=(options.portraitBokeh||options.aiDenoise)&&!showOriginal?
                        PhotoEngine.enhancedPreview(this,mediaUri,options):
                        PhotoEngine.originalPreview(this,mediaUri,options);
            }catch(Exception ex){failure=ex.getMessage();}
            Bitmap finalBitmap=rendered;
            String errorMessage=failure;
            runOnUiThread(()->{
                if(destroyed||token!=renderToken){if(finalBitmap!=null)finalBitmap.recycle();return;}
                if(finalBitmap==null){
                    previewLabel.setText(errorMessage==null?"Gambar tidak bisa ditampilkan":
                            "Preview AI gagal: "+errorMessage);
                    return;
                }
                previewLabel.setText((options.portraitBokeh||options.aiDenoise)&&!showOriginal?
                        (options.aiDenoise?"AI DENOISE • TFLITE":"AI PORTRAIT BOKEH"):"LIVE PHOTO PREVIEW");
                Bitmap oldBitmap=imagePreviewBitmap;
                photo.setImageBitmap(finalBitmap);
                imagePreviewBitmap=finalBitmap;
                // The same color matrix is used for the photo export engine.
                photo.setColorFilter(compareOriginal||options.portraitBokeh||options.aiDenoise?
                        null:PhotoEngine.colorFilter(options));
                photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                for(int i=0;i<thumbStrip.getChildCount();i++){
                    View old=thumbStrip.getChildAt(i);
                    if(old instanceof ImageView)((ImageView)old).setImageDrawable(null);
                }
                thumbStrip.removeAllViews();
                ImageView tile=new ImageView(this);tile.setImageBitmap(finalBitmap);
                tile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                tile.setBackground(StudioUi.solid(this,0xFF263452,6));tile.setClipToOutline(true);
                thumbStrip.addView(tile,new LinearLayout.LayoutParams(dp(70),dp(55)));
                thumbStrip.addView(txt("  STILL IMAGE • Non-destructive",11,MUTED,false));
                if(oldBitmap!=null&&oldBitmap!=finalBitmap)oldBitmap.recycle();
            });
        });
    }
    private void togglePlay(){
        if(!isVideo)return;
        if(video.isPlaying()){video.pause();setPlaybackControl(false);}
        else{
            if(video.getCurrentPosition()>=durationMs-300)video.seekTo((int)trimStartMs);
            video.start();setPlaybackControl(true);
        }
    }
    private void setPlaybackControl(boolean playing){
        if(playButton==null)return;
        playButton.setText(playing?"Ⅱ":"▶");
        playButton.setContentDescription(playing?"Jeda video":"Putar video");
    }
    private void startTicker(){
        playTicker=new Runnable(){
            @Override public void run(){
                if(destroyed||!isVideo)return;
                if(video!=null&&video.getVisibility()==View.VISIBLE){
                    int pos=video.getCurrentPosition();
                    if(pos>=0&&durationMs>0){
                        seekbar.setProgress((int)Math.min(1000,1000d*pos/durationMs));
                        timeLabel.setText(clock(pos)+" / "+clock(durationMs));
                        if(trimEndMs>0&&pos>=trimEndMs&&video.isPlaying()){
                            video.pause();setPlaybackControl(false);
                        }
                    }
                }
                handler.postDelayed(this,250);
            }
        };
        handler.post(playTicker);
    }
    private void toggleCompare(){
        compareOriginal=!compareOriginal;
        compareButton.setText(compareOriginal?"✦ Sesudah":"↔ Sebelum");
        compareButton.setContentDescription(compareOriginal?"Lihat sesudah diedit":"Lihat sebelum diedit");
        if(!isVideo){
            if(portraitBokeh||aiDenoise)refreshImagePreview();
            else{
                photo.setColorFilter(compareOriginal?null:PhotoEngine.colorFilter(options()));
                previewLabel.setText(compareOriginal?"SEBELUM • ASLI":"PRATINJAU FILTER");
            }
        }else toast("Pratinjau video memakai sumber; filter diterapkan saat ekspor.");
        updateCaptionPreview();
    }
    private ExportOptions options(){
        return withCustomIfRequired(
                new ExportOptions(preset,ratio,framing,quality!=OutputProfile.HD,
                trimStartMs,trimEndMs,focusX,focusY).withQuality(quality)
                .withAdjustments(brightness,contrast,saturation)
                .withCaption(caption,captionColor).withMute(muteAudio)
                .withSocialProfile(socialProfile)
                .withPortraitBokeh(!isVideo&&portraitBokeh)
                .withAiDenoise(!isVideo&&aiDenoise));
    }
    private ExportOptions withCustomIfRequired(ExportOptions o){
        return o.ratio==AspectRatioProfile.CUSTOM?
                o.withCustomSize(customWidth,customHeight):o;
    }
    private void refreshSettings(){
        rememberEdit();
        layoutPreviewCanvas();
        if(!isVideo)refreshImagePreview();
    }
    private void showTool(int which){
        activeTool=which;
        toolPane.removeAllViews();
        switch(which){
            case 0:toolTrim();break;
            case 1:toolRatio();break;
            case 2:toolFilters();break;
            case 3:toolFraming();break;
            case 4:toolAdjust();break;
            case 5:toolText();break;
            case 6:toolAudio();break;
            case 7:toolExport();break;
            case 8:toolAiPhoto();break;
            default:toolExport();break;
        }
    }
    private void toolbarTitle(String title,String sub){
        toolPane.addView(txt(title,13,TEXT,true));gap(toolPane,4);
        toolPane.addView(txt(sub,10,MUTED,false));gap(toolPane,9);
    }
    private void addChips(String[] titles,int checked,java.util.function.IntConsumer onChoose){
        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout line=row();
        for(int i=0;i<titles.length;i++){
            final int k=i;
            Button b=btn(titles[i],i==checked);
            b.setTextSize(11);
            b.setBackground(i==checked?StudioUi.gradient(this,0xFF9A5AE4,0xFF486DE2,13):
                    StudioUi.solid(this,0xFF26334B,13));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(43));
            if(i>0)p.leftMargin=dp(7);
            line.addView(b,p);b.setOnClickListener(v->onChoose.accept(k));
        }
        scroll.addView(line);toolPane.addView(scroll,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void toolRatio(){
        toolbarTitle("Canvas Ratio","11 standar + ukuran custom. Semua berlaku saat ekspor.");
        addChips(AspectRatioProfile.labels(),ratio,i->{
            if(i==AspectRatioProfile.CUSTOM){promptCustomSize();return;}
            ratio=i;socialProfile=SocialExportProfile.CUSTOM;
            refreshSettings();showTool(1);
        });
        gap(toolPane,5);
        Button custom=btn("✎  Ukuran custom: "+customWidth+" × "+customHeight,false);
        custom.setTextSize(11);
        custom.setOnClickListener(v->promptCustomSize());
        toolPane.addView(custom,new LinearLayout.LayoutParams(-1,dp(35)));
    }
    private void promptCustomSize(){
        LinearLayout inputs=column();inputs.setPadding(dp(20),dp(12),dp(20),dp(7));
        android.widget.EditText w=new android.widget.EditText(this);
        android.widget.EditText h=new android.widget.EditText(this);
        w.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        h.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        w.setSingleLine(true);h.setSingleLine(true);
        w.setText(String.valueOf(customWidth));h.setText(String.valueOf(customHeight));
        w.setHint("Lebar piksel");h.setHint("Tinggi piksel");
        inputs.addView(txt("Lebar (px)",11,TEXT,true));inputs.addView(w);
        gap(inputs,8);inputs.addView(txt("Tinggi (px)",11,TEXT,true));inputs.addView(h);
        new AlertDialog.Builder(this).setTitle("Custom canvas • hingga 8,3 MP")
                .setView(inputs).setNegativeButton("Batal",null)
                .setPositiveButton("Terapkan",(d,v)->{
                    try{
                        int width=Integer.parseInt(w.getText().toString().trim());
                        int height=Integer.parseInt(h.getText().toString().trim());
                        AspectRatioProfile.validateCustom(width,height);
                        customWidth=width;customHeight=height;
                        ratio=AspectRatioProfile.CUSTOM;
                        socialProfile=SocialExportProfile.CUSTOM;
                        refreshSettings();showTool(1);
                    }catch(Exception e){toast("Ukuran tidak valid: "+e.getMessage());}
                }).show();
    }
    private void toolFilters(){
        ColorLooks.Look selectedLook=ColorLooks.get(preset);
        toolbarTitle("24 Color Looks",""+selectedLook.category+"  •  "+selectedLook.name+"  •  Geser untuk melihat semua");
        android.widget.HorizontalScrollView scroller=new android.widget.HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout strip=row();
        int[] colors={0xFF566FAD,0xFF6B6CAE,0xFFB56FA8,0xFF3E5285,
            0xFF344055,0xFF4F9F9D,0xFFB98B4F,0xFF98634D,0xFF6B9CB4,
            0xFF364C81,0xFF7A6C82,0xFF97856D,0xFF878787,0xFF3E3E3E,
            0xFFBF7897,0xFFAAA1C7,0xFF567F5C,0xFF414A79,0xFF9B5CA7,
            0xFF78B4B0,0xFFCC846E,0xFFB59A9D,0xFF92705A,0xFF777A87};
        for(int i=0;i<ColorLooks.count();i++){
            final int index=i;
            ColorLooks.Look look=ColorLooks.get(i);
            LinearLayout tile=column();
            tile.setPadding(dp(4),dp(2),dp(4),dp(2));
            TextView swatch=txt(i==preset?"✓":"✦",19,TEXT,true);
            swatch.setGravity(Gravity.CENTER);
            android.graphics.drawable.GradientDrawable image=StudioUi.gradient(this,
                    colors[i],0xFF1E2C48,12);
            if(i==preset)image.setStroke(dp(2),StudioUi.LAVENDER);
            swatch.setBackground(image);
            tile.addView(swatch,new LinearLayout.LayoutParams(-1,dp(45)));
            gap(tile,5);
            TextView name=txt(look.name,11,i==preset?StudioUi.LAVENDER:TEXT,true);
            name.setGravity(Gravity.CENTER);name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(name);
            LinearLayout.LayoutParams tileLp=new LinearLayout.LayoutParams(dp(103),dp(78));
            if(i>0)tileLp.leftMargin=dp(3);
            strip.addView(tile,tileLp);
            tile.setClickable(true);tile.setFocusable(true);
            tile.setContentDescription("Color Look: "+look.name);
            tile.setOnClickListener(v->{
                lookScrollX=scroller.getScrollX();
                preset=index;refreshSettings();showTool(2);
            });
        }
        scroller.addView(strip);
        toolPane.addView(scroller,new LinearLayout.LayoutParams(-1,dp(83)));
        scroller.post(()->scroller.scrollTo(lookScrollX,0));
        gap(toolPane,1);
        toolPane.addView(txt(isVideo?"Video menggunakan warna ini saat ekspor.":"Foto menampilkan warna secara langsung.",10,StudioUi.GREEN,false));
    }
    private void toolFraming(){
        toolbarTitle("Framing & Crop",
                "Fit = utuh • Crop = penuh • Blur = latar. Saat Crop, geser foto untuk mengatur fokus.");
        addChips(new String[]{"Fit","Crop","Blur"},framing,i->{
            framing=i;refreshSettings();showTool(3);
        });
        gap(toolPane,5);
        LinearLayout focus=row();
        TextView info=txt("Fokus X/Y",11,MUTED,true);
        focus.addView(info);
        Button x=btn("X: "+Math.round(focusX*100)+"%",false);
        Button y=btn("Y: "+Math.round(focusY*100)+"%",false);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(35),1f);p.leftMargin=dp(8);
        focus.addView(x,p);
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(35),1f);p2.leftMargin=dp(7);
        focus.addView(y,p2);
        x.setOnClickListener(v->focusPicker(true));
        y.setOnClickListener(v->focusPicker(false));
        toolPane.addView(focus);
    }
    private void focusPicker(boolean x){
        SeekBar slider=new SeekBar(this);slider.setMax(100);
        slider.setContentDescription(x?"Fokus crop horizontal":"Fokus crop vertikal");
        slider.setProgress(Math.round((x?focusX:focusY)*100));
        new AlertDialog.Builder(this).setTitle(x?"Posisi horizontal crop":"Posisi vertikal crop")
                .setView(slider).setNegativeButton("Batal",null)
                .setPositiveButton("Terapkan",(d,w)->{
                    if(x)focusX=slider.getProgress()/100f;
                    else focusY=slider.getProgress()/100f;
                    refreshSettings();showTool(3);
                }).show();
    }
    private void toolTrim(){
        if(!isVideo){
            toolbarTitle("Photo Duration","Foto statis tidak memiliki track waktu.");
            toolPane.addView(txt("Gunakan Filter, Rasio, Frame atau Ekspor.",12,MUTED,false));
            return;
        }
        toolbarTitle("Trim & Split","Geser posisi trim; Split mengekspor dua segmen terpisah.");
        int max=Math.max(1,(int)Math.min(36000,durationMs/1000));
        LinearLayout startRow=row();
        TextView label=txt("Mulai  "+clock(trimStartMs),11,TEXT,true);
        startRow.addView(label,new LinearLayout.LayoutParams(dp(108),dp(30)));
        SeekBar start=new SeekBar(this);start.setMax(max);
        start.setProgress((int)(trimStartMs/1000));
        startRow.addView(start,new LinearLayout.LayoutParams(0,dp(30),1f));
        toolPane.addView(startRow);
        LinearLayout endRow=row();
        TextView endLabel=txt("Akhir  "+clock(trimEndMs>0?trimEndMs:durationMs),11,TEXT,true);
        endRow.addView(endLabel,new LinearLayout.LayoutParams(dp(108),dp(30)));
        SeekBar end=new SeekBar(this);end.setMax(max);
        end.setProgress((int)((trimEndMs>0?trimEndMs:durationMs)/1000));
        endRow.addView(end,new LinearLayout.LayoutParams(0,dp(30),1f));
        toolPane.addView(endRow);
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int progress,boolean user){
                if(!user)return;
                long lower=start.getProgress()*1000L;
                long upper=Math.min(durationMs,Math.max(1000L,end.getProgress()*1000L));
                if(lower>=upper){toast("Waktu akhir harus lebih besar daripada awal.");return;}
                trimStartMs=lower;trimEndMs=upper;
                label.setText("Mulai  "+clock(trimStartMs));endLabel.setText("Akhir  "+clock(trimEndMs));
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){rememberEdit();}
        };
        start.setOnSeekBarChangeListener(listener);end.setOnSeekBarChangeListener(listener);
        LinearLayout bottom=row();
        Button split=btn(splitMs>0?"✂ Split: "+clock(splitMs):"✂ Split di playhead",false);
        Button reset=btn("Reset",false);
        bottom.addView(split,new LinearLayout.LayoutParams(0,dp(38),2f));
        LinearLayout.LayoutParams resetLp=new LinearLayout.LayoutParams(0,dp(38),1f);
        resetLp.leftMargin=dp(9);bottom.addView(reset,resetLp);
        split.setOnClickListener(v->{
            long play=video.getCurrentPosition();
            long upper=trimEndMs>0?trimEndMs:durationMs;
            if(play<=trimStartMs+500||play>=upper-500){
                toast("Geser playhead ke tengah rentang sebelum Split.");return;
            }
            splitMs=play;rememberEdit();split.setText("✂ Split: "+clock(splitMs));
            toast("Ekspor akan menghasilkan dua segmen video.");
        });
        reset.setOnClickListener(v->{
            splitMs=-1;trimStartMs=0;trimEndMs=0;rememberEdit();showTool(0);
        });
        toolPane.addView(bottom);
    }
    private void updateCaptionPreview(){
        if(captionPreview!=null)
            captionPreview.update(caption,captionColor,socialProfile,compareOriginal,isVideo);
    }
    private void updateLiveAdjust(){
        if(isVideo||compareOriginal)return;
        if(aiDenoise||portraitBokeh){
            // Debounce neural re-inference while a color seekbar is being dragged.
            handler.removeCallbacks(adjustedPreview);
            handler.postDelayed(adjustedPreview,350);
        }else photo.setColorFilter(PhotoEngine.colorFilter(options()));
    }
    private void toolAdjust(){
        toolbarTitle("Color Adjust","Atur warna sebelum mengekspor. Nilai 0 = netral.");
        adjustment("Brightness",brightness,value->{brightness=value;updateLiveAdjust();});
        adjustment("Contrast",contrast,value->{contrast=value;updateLiveAdjust();});
        adjustment("Saturation",saturation,value->{saturation=value;updateLiveAdjust();});
    }
    private void adjustment(String title,float initial,java.util.function.Consumer<Float> changed){
        LinearLayout row=row();
        TextView label=txt(title,11,TEXT,true);
        row.addView(label,new LinearLayout.LayoutParams(dp(82),dp(30)));
        SeekBar bar=new SeekBar(this);bar.setMax(200);
        bar.setProgress(Math.max(0,Math.min(200,Math.round(100+initial*100))));
        row.addView(bar,new LinearLayout.LayoutParams(0,dp(30),1f));
        TextView valueText=txt(String.format(Locale.US,"%+d",Math.round(initial*100)),10,MUTED,true);
        valueText.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        row.addView(valueText,new LinearLayout.LayoutParams(dp(37),dp(30)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int progress,boolean user){
                if(!user)return;
                float value=(progress-100)/100f;
                valueText.setText(String.format(Locale.US,"%+d",progress-100));
                changed.accept(value);
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){rememberEdit();}
        });
        toolPane.addView(row);
    }
    private void toolText(){
        toolbarTitle("Text Overlay","Teks statis akan ikut masuk ke foto atau seluruh durasi video.");
        EditText field=new EditText(this);
        field.setSingleLine(true);field.setTextSize(13);
        field.setTextColor(TEXT);field.setHintTextColor(MUTED);
        field.setHint("Judul / subtitle singkat");
        field.setBackground(StudioUi.solid(this,0xFF2B3450,11));
        field.setPadding(dp(12),dp(8),dp(12),dp(8));
        field.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(80)});
        field.setText(caption);
        field.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                caption=s.toString();updateCaptionPreview();rememberEdit();
            }
            @Override public void afterTextChanged(Editable e){}
        });
        toolPane.addView(field,new LinearLayout.LayoutParams(-1,dp(42)));
        gap(toolPane,9);
        LinearLayout colors=row();
        colors.addView(txt("Warna",12,MUTED,true),new LinearLayout.LayoutParams(0,-2,1f));
        Button white=btn("● Putih",captionColor==Color.WHITE);
        colors.addView(white,new LinearLayout.LayoutParams(dp(93),dp(35)));
        Button yellow=btn("● Emas",captionColor!=Color.WHITE);
        LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(dp(93),dp(35));
        second.leftMargin=dp(8);colors.addView(yellow,second);
        white.setOnClickListener(v->{captionColor=Color.WHITE;rememberEdit();updateCaptionPreview();showTool(5);});
        yellow.setOnClickListener(v->{captionColor=0xFFFFD786;rememberEdit();updateCaptionPreview();showTool(5);});
        toolPane.addView(colors);
    }
    private void toolAudio(){
        toolbarTitle("Original Audio","Kontrol audio asli dari media sumber.");
        if(!isVideo){
            toolPane.addView(txt("Foto tidak memiliki track audio.",12,MUTED,false));
            return;
        }
        LinearLayout toggle=row();
        toggle.addView(txt("Hapus audio dari hasil ekspor",12,TEXT,true),
                new LinearLayout.LayoutParams(0,dp(42),1f));
        Switch muteSwitch=new Switch(this);muteSwitch.setChecked(muteAudio);
        muteSwitch.setOnCheckedChangeListener((v,checked)->{muteAudio=checked;rememberEdit();});
        toggle.addView(muteSwitch);
        toolPane.addView(toggle);gap(toolPane,8);
        toolPane.addView(txt("Mode mute menghilangkan track audio output. Preview tetap memutar audio asli.",11,MUTED,false));
    }

    private void toolAiPhoto(){
        if(isVideo){
            toolbarTitle("AI Photo","Fitur ini menganalisis foto, bukan video.");
            toolPane.addView(txt("Pilih Photo Editor untuk menggunakan ML Kit.",11,MUTED,false));
            return;
        }
        toolbarTitle("AI Photo Studio","ML Kit offline • deteksi wajah + isi foto • tidak mengubah wajah.");
        aiStatus=txt(aiResult==null?(aiRunning?"Menganalisis foto...":
                "Ketuk Analisis AI untuk mendapatkan rekomendasi."):
                "Terdeteksi: "+aiResult.label+" ("+Math.round(aiResult.confidence*100)+
                "%), wajah: "+aiResult.faces,11,MUTED,false);
        toolPane.addView(aiStatus);
        gap(toolPane,8);
        LinearLayout portraitLine=row();
        portraitLine.addView(txt("AI Portrait Bokeh • blur latar",12,TEXT,true),
                new LinearLayout.LayoutParams(0,dp(40),1f));
        Switch portraitSwitch=new Switch(this);
        portraitSwitch.setContentDescription("Aktifkan AI Portrait Bokeh");
        portraitSwitch.setChecked(portraitBokeh);
        portraitSwitch.setOnCheckedChangeListener((v,checked)->{
            portraitBokeh=checked;
            rememberEdit();
            if(checked)compareOriginal=false;
            compareButton.setText("↔ Sebelum");
            refreshImagePreview();
        });
        portraitLine.addView(portraitSwitch);
        toolPane.addView(portraitLine);
        toolPane.addView(txt("Pakai model segmentasi sungguhan; tepi rambut bisa kurang akurat.",
                10,MUTED,false));
        gap(toolPane,9);
        LinearLayout denoiseRow=row();
        denoiseRow.addView(txt("AI Denoise • noise luminance",12,TEXT,true),
                new LinearLayout.LayoutParams(0,dp(42),1f));
        Switch denoiseSwitch=new Switch(this);
        denoiseSwitch.setContentDescription("Aktifkan AI Denoise DnCNN");
        denoiseSwitch.setChecked(aiDenoise);
        denoiseSwitch.setOnCheckedChangeListener((view,on)->{
            if(on&&!AiDenoiseEngine.included(this)){
                denoiseSwitch.setChecked(false);
                toast("Bobot model DnCNN tidak tersedia");
                return;
            }
            aiDenoise=on;
            rememberEdit();
            if(on)compareOriginal=false;
            compareButton.setText("↔ Sebelum");
            refreshImagePreview();
        });
        denoiseRow.addView(denoiseSwitch);
        toolPane.addView(denoiseRow);
        toolPane.addView(txt("Model neural TFLite CPU • untuk noise Gaussian; bukan AI deblur/upscale. "+
                "Foto di atas 2,6 MP akan ditolak, bukan diturunkan diam-diam.",
                10,MUTED,false));
        gap(toolPane,8);
        LinearLayout commands=row();
        Button analyze=btn(aiRunning?"Memproses...":"✧ Analisis AI",true);
        analyze.setEnabled(!aiRunning);
        commands.addView(analyze,new LinearLayout.LayoutParams(0,dp(38),1f));
        analyze.setOnClickListener(v->startAiAnalysis());
        aiApplyButton=btn("Gunakan Look",false);
        aiApplyButton.setEnabled(aiResult!=null);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(38),1f);
        lp.leftMargin=dp(8);commands.addView(aiApplyButton,lp);
        aiApplyButton.setOnClickListener(v->applyAiLook());
        toolPane.addView(commands);
        gap(toolPane,7);
        if(aiResult!=null){
            toolPane.addView(txt(aiResult.suggestion.reason+
                    (aiResult.partial?" • analisis sebagian":""),
                    10,StudioUi.GREEN,false));
            if(aiResult.suggestion.hasFace){
                Button smart=btn("⌖ Terapkan AI Smart Crop (posisi wajah)",false);
                smart.setTextSize(11);
                smart.setOnClickListener(v->{
                    focusX=aiResult.suggestion.focusX;
                    focusY=aiResult.suggestion.focusY;
                    framing=AspectRatioProfile.CROP;
                    refreshSettings();toast("Smart Crop wajah aktif. Periksa sebelum ekspor.");
                });
                toolPane.addView(smart,new LinearLayout.LayoutParams(-1,dp(35)));
            }
        }
    }
    private void startAiAnalysis(){
        if(isVideo||aiRunning)return;
        aiRunning=true;aiResult=null;
        if(aiStatus!=null)aiStatus.setText("Menjalankan model AI di perangkat...");
        AiPhotoAnalyzer.analyze(this,mediaUri,new AiPhotoAnalyzer.Callback(){
            @Override public void onResult(AiPhotoAnalyzer.Result result){
                if(destroyed)return;
                aiRunning=false;aiResult=result;showTool(8);
            }
            @Override public void onError(String detail){
                if(destroyed)return;
                aiRunning=false;aiResult=null;
                showTool(8);
                if(aiStatus!=null)aiStatus.setText("AI belum berhasil: "+detail);
            }
        });
    }
    private void applyAiLook(){
        if(aiResult==null)return;
        preset=aiResult.suggestion.colorLook;
        refreshSettings();
        toast("AI merekomendasikan: "+ColorLooks.get(preset).name);
        showTool(8);
    }

    private void toolExport(){
        toolbarTitle("Social Export","Pilih tujuan unggahan; kualitas tidak menjamin tanpa kompresi.");
        Button destination=btn("▯  "+SocialExportProfile.get(socialProfile).name+"  ▾",false);
        destination.setTextSize(12);
        toolPane.addView(destination,new LinearLayout.LayoutParams(-1,dp(36)));
        destination.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Optimasi ekspor untuk")
                .setSingleChoiceItems(SocialExportProfile.names(),socialProfile,(dialog,selected)->{
                    socialProfile=selected;
                    if(selected!=SocialExportProfile.CUSTOM){
                        ratio=AspectRatioProfile.PORTRAIT_9_16;
                        framing=AspectRatioProfile.FIT;
                        quality=OutputProfile.FULL_HD;
                        dualOutput=false;
                    }
                    dialog.dismiss();refreshSettings();showTool(7);
                })
                .setNegativeButton("Batal",null).show());
        gap(toolPane,4);
        addChips(new String[]{"HD","1080p","Max Detail"},quality,i->{
            quality=i;
            if(socialProfile!=SocialExportProfile.CUSTOM)
                socialProfile=SocialExportProfile.CUSTOM;
            refreshSettings();showTool(7);
        });
        LinearLayout dual=row();
        dual.addView(txt(socialProfile==SocialExportProfile.CUSTOM?
                "Ekspor ganda 9:16 + 16:9":"1080 × 1920 • H.264/AAC • Safe Zone",11,TEXT,true),
                new LinearLayout.LayoutParams(0,-1,1f));
        if(socialProfile==SocialExportProfile.CUSTOM){
            Switch dualSwitch=new Switch(this);dualSwitch.setChecked(dualOutput);
            dualSwitch.setOnCheckedChangeListener((v,b)->{dualOutput=b;rememberEdit();});
            dual.addView(dualSwitch);
        }
        toolPane.addView(dual);
    }
    private void confirmExport(){
        if(exportBusy)return;
        if(framing==AspectRatioProfile.CROP){
            try{
                AspectRatioProfile.Size original=MediaDimensions.read(this,mediaUri);
                AspectRatioProfile.Size output=AspectRatioProfile.resolve(ratio,quality,
                        original.width,original.height,framing,customWidth,customHeight);
                QualityAdvisor.Report report=QualityAdvisor.analyze(original.width,original.height,
                        output.width,output.height,framing);
                if(report.severeLoss()){
                    new AlertDialog.Builder(this).setTitle("Crop mengurangi detail")
                            .setMessage(report.description()+"\n\nPilih Blur untuk mempertahankan semua bagian gambar.")
                            .setNegativeButton("Batal",null)
                            .setNeutralButton("Ganti Blur",(dialog,v)->{
                                framing=AspectRatioProfile.BLUR;refreshSettings();startExport();
                            })
                            .setPositiveButton("Tetap Crop",(dialog,v)->startExport()).show();
                    return;
                }
            }catch(Exception ignored){}
        }
        startExport();
    }
    private void startExport(){
        exports.clear();exportCancelled=false;exportBusy=true;
        exportStop=new java.util.concurrent.atomic.AtomicBoolean(false);
        completed=0;failed=0;downgradedVideoExports=0;lastEncodedVideoCanvas="";
        ExportOptions current;
        try{current=options();}
        catch(Exception ex){exportBusy=false;toast(ex.getMessage());return;}
        ArrayList<ExportOptions> sequences=new ArrayList<>();
        if(isVideo&&splitMs>0){
            long ending=trimEndMs>0?trimEndMs:durationMs;
            if(splitMs>trimStartMs&&splitMs<ending){
                sequences.add(withCustomIfRequired(new ExportOptions(preset,ratio,framing,quality!=OutputProfile.HD,
                        trimStartMs,splitMs,focusX,focusY).withQuality(quality)
                        .withAdjustments(brightness,contrast,saturation)
                        .withCaption(caption,captionColor).withMute(muteAudio)
                        .withSocialProfile(socialProfile)));
                sequences.add(withCustomIfRequired(new ExportOptions(preset,ratio,framing,quality!=OutputProfile.HD,
                        splitMs,trimEndMs,focusX,focusY).withQuality(quality)
                        .withAdjustments(brightness,contrast,saturation)
                        .withCaption(caption,captionColor).withMute(muteAudio)
                        .withSocialProfile(socialProfile)));
            }
        }
        if(sequences.isEmpty())sequences.add(current);
        int index=0;
        for(ExportOptions sequence:sequences){
            String prefix=sequences.size()>1?"Segmen "+(++index):"Hasil";
            if(dualOutput){
                exports.add(new RenderJob(sequence.forRatio(AspectRatioProfile.PORTRAIT_9_16),prefix+" 9:16"));
                exports.add(new RenderJob(sequence.forRatio(AspectRatioProfile.LANDSCAPE_16_9),prefix+" 16:9"));
            }else exports.add(new RenderJob(sequence,prefix));
        }
        LinearLayout dialogCard=card(16);
        dialogCard.addView(txt("Sedang mengolah media",18,TEXT,true));gap(dialogCard,8);
        exportStatus=txt("Memulai…",12,MUTED,false);dialogCard.addView(exportStatus);gap(dialogCard,12);
        exportProgress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        exportProgress.setMax(100);dialogCard.addView(exportProgress,new LinearLayout.LayoutParams(-1,dp(6)));
        exportDialog=new AlertDialog.Builder(this).setView(dialogCard)
                .setNegativeButton("Batalkan",(d,w)->cancelExport()).setCancelable(false).create();
        exportDialog.show();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        runNextExport();
    }
    private void runNextExport(){
        if(exportCancelled||exports.isEmpty()){finishExports();return;}
        RenderJob job=exports.removeFirst();
        final java.util.concurrent.atomic.AtomicBoolean cancelToken=exportStop;
        exportStatus.setText("Mengekspor "+job.title+"…");
        exportProgress.setIndeterminate(isVideo?false:true);exportProgress.setProgress(0);
        if(!isVideo){
            io.execute(()->{
                try{
                    PhotoEngine.Result result=PhotoEngine.process(this,mediaUri,job.options);
                    if(cancelToken.get())return;
                    String exportName=fileName("jpg");
                    Uri saved=MediaSaver.savePhoto(this,result.jpeg,exportName,cancelToken::get);
                    MediaSaver.beforeCallback(saved,exportName);
                    runOnUiThread(()->{
                        if(destroyed||cancelToken.get()){
                            MediaSaver.discardUnaccepted(StudioEditorActivity.this,saved);
                            return;
                        }
                        lastExport=saved;completed++;
                        exportProgress.setProgress(100);runNextExport();
                    });
                }catch(Exception ex){
                    if(!cancelToken.get())runOnUiThread(()->failExport(cancelToken,ex.getMessage()));
                }
            });
        }else{
            final VideoEngine worker=new VideoEngine();
            activeVideoEngine=worker;
            // Media3 can downgrade a failed FHD encode to HD; record the
            // actually requested dimensions so success cannot masquerade
            // as a completed 1080p export in the user-facing result dialog.
            final AspectRatioProfile.Size[] requestedVideoCanvas=new AspectRatioProfile.Size[1];
            try{
                requestedVideoCanvas[0]=worker.start(this,mediaUri,
                        job.options,new VideoEngine.Listener(){
                    @Override public void onSuccess(File source,AspectRatioProfile.Size dims){
                        if(cancelToken.get())return;
                        worker.transferCompletedFile(source);
                        io.execute(()->{
                            try{
                                if(cancelToken.get())return;
                                String exportName=fileName("mp4");
                                Uri saved=MediaSaver.saveVideo(StudioEditorActivity.this,source,exportName,cancelToken::get,java.util.Collections.singletonList(mediaUri));
                                MediaSaver.beforeCallback(saved,exportName);
                                runOnUiThread(()->{
                                    if(destroyed||cancelToken.get()){
                                        MediaSaver.discardUnaccepted(StudioEditorActivity.this,saved);
                                        return;
                                    }
                                    lastExport=saved;completed++;
                                    lastEncodedVideoCanvas=dims.width+"×"+dims.height;
                                    AspectRatioProfile.Size requested=requestedVideoCanvas[0];
                                    if(requested!=null &&
                                            (requested.width!=dims.width||
                                             requested.height!=dims.height))
                                        downgradedVideoExports++;
                                    activeVideoEngine=null;
                                    runNextExport();
                                });
                            }catch(Exception ex){
                                if(!cancelToken.get())runOnUiThread(()->failExport(cancelToken,ex.getMessage()));
                            }finally{
                                MediaSaver.discardTemporaryFile(source);
                            }
                        });
                    }
                    @Override public void onError(String message){
                        if(!cancelToken.get())failExport(cancelToken,message);
                    }
                });
                handler.post(new Runnable(){
                    @Override public void run(){
                        if(destroyed||!exportBusy||cancelToken.get()||activeVideoEngine!=worker)return;
                        int pos=worker.progress();
                        if(pos>=0&&!exportCancelled)exportProgress.setProgress(pos);
                        handler.postDelayed(this,400);
                    }
                });
            }catch(Exception ex){failExport(cancelToken,ex.getMessage());}
        }
    }
    private void failExport(java.util.concurrent.atomic.AtomicBoolean token,String error){
        if(exportCancelled||token!=exportStop||token.get()||destroyed)return;
        failed++;
        if(activeVideoEngine!=null){activeVideoEngine.cleanup();activeVideoEngine=null;}
        toast("Ekspor gagal: "+error);runNextExport();
    }
    private void cancelExport(){
        exportCancelled=true;exportStop.set(true);exports.clear();
        if(activeVideoEngine!=null){activeVideoEngine.cancel();activeVideoEngine=null;}
        exportBusy=false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(exportDialog!=null)exportDialog.dismiss();
    }
    private void finishExports(){
        exportBusy=false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(exportDialog!=null)exportDialog.dismiss();
        if(exportCancelled){toast("Antrean ekspor dibatalkan.");return;}
        new AlertDialog.Builder(this)
                .setTitle(completed>0?"Ekspor selesai":"Ekspor gagal")
                .setMessage(completed+" berhasil • "+failed+" gagal.\n"+
                        (completed>0?"Media telah disimpan ke Galeri/WFS StoryBoost.":"Coba format lain atau mode HD.")+
                        (lastEncodedVideoCanvas.isEmpty()?"":"\nResolusi video tersimpan: "+lastEncodedVideoCanvas)+
                        (downgradedVideoExports>0?"\nPERINGATAN: "+
                                downgradedVideoExports+
                                " ekspor video diturunkan dari resolusi yang dipilih " +
                                "karena encoder FHD tidak tersedia. " +
                                "Periksa resolusi aktual sebelum dibagikan.":""))
                .setPositiveButton("Selesai",null)
                .setNeutralButton(completed>0?"Bagikan":"Tutup",(dialog,w)->{if(completed>0)shareLast();})
                .show();
    }
    private void shareLast(){
        if(lastExport==null)return;
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType(isVideo?"video/mp4":"image/jpeg");
        send.putExtra(Intent.EXTRA_STREAM,lastExport);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send,"Bagikan hasil"));
    }
    private String fileName(String ext){
        return "WFS_STUDIO_"+System.currentTimeMillis()+"_"+Math.abs(System.nanoTime()%9999)+"."+ext;
    }
    private static String clock(long ms){
        long seconds=Math.max(0,ms/1000);
        return String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60);
    }
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
    @Override protected void onPause(){
        handler.removeCallbacks(draftWriter);
        handler.removeCallbacks(adjustedPreview);
        persistDraft();
        if(video!=null&&video.isPlaying()){video.pause();setPlaybackControl(false);}
        super.onPause();
    }
    @Override protected void onDestroy(){
        exportStop.set(true);
        destroyed=true;handler.removeCallbacksAndMessages(null);
        if(activeVideoEngine!=null)activeVideoEngine.cancel();
        if(video!=null)video.stopPlayback();
        if(photo!=null)photo.setImageDrawable(null);
        if(imagePreviewBitmap!=null)imagePreviewBitmap.recycle();
        previewWorker.shutdownNow();
        // Let a queued writer release its transferred file after seeing cancellation.
        io.shutdown();
        super.onDestroy();
    }
}
