package com.wfs.storyboost;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.EditText;
import android.text.InputType;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int PICK_MEDIA = 501;
    private static final int BG = 0xFF080E1D;
    private static final int CARD = 0xFF14203A;
    private static final int TEXT = 0xFFF0F5FF;
    private static final int SOFT = 0xFF9EAFCC;
    private static final int ACCENT = 0xFFA9CDFF;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final class ExportJob {
        final Uri uri;
        final ExportOptions options;
        ExportJob(Uri uri,ExportOptions options){this.uri=uri;this.options=options;}
    }
    private final ArrayDeque<ExportJob> pending = new ArrayDeque<>();
    private final VideoEngine videos = new VideoEngine();
    private ProgressBar progress;
    private TextView status, count, outputLabel;
    private Button selectButton, shareButton, cancelButton;
    private BeforeAfterView preview;
    private RadioGroup presets;
    private Spinner ratioSpinner, frameSpinner;
    private TextView ratioHint;
    private Switch hdSwitch,dualAspectSwitch;
    private EditText trimStartField,trimEndField;
    private SeekBar focusXSeek,focusYSeek;
    private VideoView videoPreview;
    private Button originalVideoButton,exportedVideoButton;
    private Uri previewOriginalVideo,previewExportVideo;
    private ExportOptions queueOptions;
    private boolean busy = false;
    private boolean videoActive = false;
    private boolean aborted = false;
    private int total = 0, done = 0, failed = 0;
    private Uri lastSaved = null;
    private String lastType = null;
    private Runnable videoPulse;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(BG);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(22), dp(22), dp(22), dp(32));
        scroll.addView(column);

        TextView eyebrow = text("WFS  /  CREATIVE UTILITIES", 12, ACCENT, true);
        eyebrow.setLetterSpacing(0.15f);
        column.addView(eyebrow);
        space(column, 10);
        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView appLogo = new ImageView(this);
        appLogo.setImageResource(R.mipmap.ic_launcher);
        appLogo.setContentDescription("Logo WFS StoryBoost");
        appLogo.setAdjustViewBounds(true);
        brandRow.addView(appLogo, new LinearLayout.LayoutParams(dp(76), dp(76)));
        LinearLayout brandTitles = new LinearLayout(this);
        brandTitles.setOrientation(LinearLayout.VERTICAL);
        brandTitles.addView(text("StoryBoost", 31, TEXT, true));
        brandTitles.addView(text("PHOTO & VIDEO STUDIO  •  v1.7", 11, ACCENT, true));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        brandRow.addView(brandTitles, titleParams);
        column.addView(brandRow);
        space(column, 6);
        column.addView(text("Foto dan video lebih estetik. Siap unggah sebagai WhatsApp Story.",
                15, SOFT, false));
        space(column, 22);

        LinearLayout card = box(CARD, 22);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));
        column.addView(card);
        card.addView(text("01  •  PILIH LOOK", 12, ACCENT, true));
        space(card, 9);
        card.addView(text("Warna otomatis", 19, TEXT, true));
        space(card, 9);
        presets = new RadioGroup(this);
        presets.setOrientation(RadioGroup.VERTICAL);
        String[] looks = {"Natural  ·  warna realistis", "Cinematic  ·  hangat & tajam",
                "Vibrant  ·  warna lebih hidup", "Soft Night  ·  bayangan lebih terang"};
        for (int i = 0; i < looks.length; i++) {
            RadioButton radio = new RadioButton(this);
            radio.setId(1000 + i);
            radio.setText(looks[i]);
            radio.setTextColor(TEXT);
            radio.setTextSize(14);
            radio.setButtonTintList(ColorStateList.valueOf(ACCENT));
            radio.setPadding(0, dp(4), 0, dp(4));
            presets.addView(radio);
        }
        presets.check(1001);
        card.addView(presets);

        space(column, 14);
        LinearLayout opts = box(CARD, 22);
        opts.setPadding(dp(18), dp(18), dp(18), dp(18));
        column.addView(opts);
        opts.addView(text("02  •  FORMAT STORY", 12, ACCENT, true));
        space(opts, 7);
        opts.addView(text("Rasio & orientasi output", 19, TEXT, true));
        space(opts, 4);
        opts.addView(text("Foto: JPG berkualitas tinggi. Video: MP4 H.264 + AAC.",
                13, SOFT, false));
        space(opts, 12);
        opts.addView(text("Rasio tujuan", 13, TEXT, true));
        ratioSpinner = spinner(new String[]{
                "9:16  •  Story vertikal", "16:9  •  Landscape horizontal",
                "1:1  •  Persegi", "4:5  •  Feed portrait", "Original  •  Rasio sumber"
        });
        opts.addView(ratioSpinner);
        Button reverseRatio = button("⇄  Tukar rasio 9:16 / 16:9", false);
        reverseRatio.setOnClickListener(v -> {
            int current = ratioSpinner.getSelectedItemPosition();
            int reversed = AspectRatioProfile.reverse(current);
            if (current != reversed) {
                ratioSpinner.setSelection(reversed);
                updateRatioHint();
            } else {
                ratioHint.setText("Pilih rasio 9:16 atau 16:9 dahulu untuk menukar orientasi.");
            }
        });
        opts.addView(reverseRatio);
        space(opts, 11);
        opts.addView(text("Cara mengisi bingkai", 13, TEXT, true));
        frameSpinner = spinner(new String[]{
                "Fit  •  Utuh, tanpa potong", "Crop  •  Penuhi layar",
                "Blur  •  Latar kabur foto & video"
        });
        opts.addView(frameSpinner);
        opts.addView(text("Fokus Crop • kiri/kanan",12,SOFT,false));
        focusXSeek=new SeekBar(this);
        focusXSeek.setMax(100);focusXSeek.setProgress(50);
        opts.addView(focusXSeek);
        opts.addView(text("Fokus Crop • atas/bawah",12,SOFT,false));
        focusYSeek=new SeekBar(this);
        focusYSeek.setMax(100);focusYSeek.setProgress(50);
        opts.addView(focusYSeek);
        space(opts, 10);
        dualAspectSwitch=switcher("Ekspor sekaligus 9:16 dan 16:9",false);
        opts.addView(dualAspectSwitch);
        space(opts,9);
        opts.addView(text("Trim video opsional (detik, 0 = awal/akhir)",12,SOFT,false));
        LinearLayout trimRow=new LinearLayout(this);
        trimRow.setOrientation(LinearLayout.HORIZONTAL);
        trimStartField=trimInput("Mulai 0");trimEndField=trimInput("Selesai 0");
        trimRow.addView(trimStartField,new LinearLayout.LayoutParams(0,dp(52),1));
        trimRow.addView(trimEndField,new LinearLayout.LayoutParams(0,dp(52),1));
        opts.addView(trimRow);
        space(opts, 10);
        hdSwitch = switcher("Kualitas Full HD (nonaktif: HD)", true);
        opts.addView(hdSwitch);
        ratioHint = text("", 12, SOFT, false);
        opts.addView(ratioHint);
        space(opts, 8);
        opts.addView(text("Blur video: latar bergerak dari tiap frame via GPU. "
                + "Mode Crop mengikuti slider fokus X/Y, Fit mempertahankan media utuh.",12,SOFT,false));
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        ratioSpinner.setSelection(Math.max(0, Math.min(4, prefs.getInt("ratio", 0))));
        frameSpinner.setSelection(Math.max(0, Math.min(2, prefs.getInt("frame", 0))));
        hdSwitch.setChecked(prefs.getBoolean("full_hd", true));
        presets.check(1000 + Math.max(0, Math.min(3, prefs.getInt("preset", 1))));
        dualAspectSwitch.setChecked(prefs.getBoolean("dual_ratio",false));
        trimStartField.setText(prefs.getString("trim_start",""));
        trimEndField.setText(prefs.getString("trim_end",""));
        focusXSeek.setProgress(Math.max(0,Math.min(100,prefs.getInt("focus_x",50))));
        focusYSeek.setProgress(Math.max(0,Math.min(100,prefs.getInt("focus_y",50))));
        AdapterView.OnItemSelectedListener ratioListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View view, int position, long id) {
                updateRatioHint();
            }
            @Override public void onNothingSelected(AdapterView<?> a) { }
        };
        ratioSpinner.setOnItemSelectedListener(ratioListener);
        hdSwitch.setOnCheckedChangeListener((v, checked) -> updateRatioHint());
        updateRatioHint();

        space(column, 20);
        selectButton = button("+  Pilih foto / video", true);
        column.addView(selectButton);
        selectButton.setOnClickListener(v -> pick());
        space(column, 9);
        column.addView(text("Pilih hingga 10 file. Pemrosesan dimulai otomatis setelah dipilih.",
                12, SOFT, false));

        space(column, 18);
        LinearLayout results = box(CARD, 22);
        results.setPadding(dp(18), dp(18), dp(18), dp(18));
        column.addView(results);
        results.addView(text("03  •  HASIL", 12, ACCENT, true));
        space(results, 9);
        status = text("Belum ada media dipilih", 15, TEXT, true);
        results.addView(status);
        space(results, 5);
        count = text("Semua proses berjalan secara lokal di HP Anda.", 12, SOFT, false);
        results.addView(count);
        space(results, 12);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(ColorStateList.valueOf(ACCENT));
        progress.setProgress(0);
        results.addView(progress, new LinearLayout.LayoutParams(-1, dp(5)));
        space(results, 12);
        preview = new BeforeAfterView(this);
        preview.setContentDescription("Geser pembatas untuk membandingkan foto asli dan hasil edit");
        preview.setVisibility(View.GONE);
        results.addView(preview, new LinearLayout.LayoutParams(-1, dp(260)));
        results.addView(text("Geser pembatas: kiri asli • kanan hasil edit", 12, SOFT, false));
        videoPreview=new VideoView(this);
        videoPreview.setVisibility(View.GONE);
        videoPreview.setBackgroundColor(Color.BLACK);
        MediaController mc=new MediaController(this);
        mc.setAnchorView(videoPreview);videoPreview.setMediaController(mc);
        results.addView(videoPreview,new LinearLayout.LayoutParams(-1,dp(250)));
        originalVideoButton=button("▶ Video asli",false);
        exportedVideoButton=button("▶ Video hasil",false);
        originalVideoButton.setVisibility(View.GONE);
        exportedVideoButton.setVisibility(View.GONE);
        originalVideoButton.setOnClickListener(v->playVideoPreview(false));
        exportedVideoButton.setOnClickListener(v->playVideoPreview(true));
        results.addView(originalVideoButton);results.addView(exportedVideoButton);
        outputLabel = text("", 12, SOFT, false);
        results.addView(outputLabel);
        space(results, 11);
        shareButton = button("Bagikan hasil terakhir", false);
        shareButton.setEnabled(false);
        shareButton.setAlpha(0.5f);
        shareButton.setOnClickListener(v -> share());
        results.addView(shareButton);
        space(results, 10);
        cancelButton = button("Batalkan antrean", false);
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(v -> cancelWork());
        results.addView(cancelButton);

        space(column, 22);
        TextView footer = text("PRIVASI  •  Tanpa akun, tanpa unggah server, tanpa izin akses seluruh galeri.\n"
                + "Catatan: WhatsApp masih dapat mengompresi Story setelah diunggah.",
                12, SOFT, false);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(footer);
        setContentView(scroll);
    }

    private EditText trimInput(String hint){
        EditText f=new EditText(this);f.setHint(hint);f.setHintTextColor(SOFT);
        f.setTextColor(TEXT);f.setSingleLine(true);f.setTextSize(13);
        f.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return f;
    }
    private static long secondsToMs(EditText field){
        String t=field.getText().toString().trim();
        if(t.isEmpty())return 0;
        try{
            double seconds=Double.parseDouble(t);
            if(!Double.isFinite(seconds)||seconds<0||seconds>36000)
                throw new NumberFormatException();
            return Math.round(seconds*1000);
        }catch(NumberFormatException e){
            throw new IllegalArgumentException("Trim harus angka detik 0–36000.");
        }
    }
    private void playVideoPreview(boolean exported){
        Uri uri=exported?previewExportVideo:previewOriginalVideo;
        if(uri==null)return;
        videoPreview.stopPlayback();
        videoPreview.setVideoURI(uri);
        videoPreview.setOnPreparedListener(player->{
            player.setLooping(false);videoPreview.start();
        });
        videoPreview.setVisibility(View.VISIBLE);
    }
    private void hideVideoPreview(){
        if(videoPreview!=null){
            videoPreview.stopPlayback();videoPreview.setVisibility(View.GONE);
            originalVideoButton.setVisibility(View.GONE);
            exportedVideoButton.setVisibility(View.GONE);
        }
    }
    private void updateRatioHint() {
        if (ratioHint == null || ratioSpinner == null || hdSwitch == null) return;
        int ratio = ratioSpinner.getSelectedItemPosition();
        if (ratio == AspectRatioProfile.ORIGINAL) {
            ratioHint.setText("Resolusi original: mengikuti media sumber, maksimal sisi panjang "
                    + (hdSwitch.isChecked() ? 1920 : 1280) + " piksel.");
        } else {
            AspectRatioProfile.Size size = AspectRatioProfile.resolve(ratio,
                    hdSwitch.isChecked(), 1080, 1920);
            ratioHint.setText("Target ekspor: " + size + " piksel  •  "
                    + AspectRatioProfile.label(ratio));
        }
    }

    private Spinner spinner(String[] items) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {
            @Override public View getView(int position, View convertView,
                                          android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(TEXT);
                view.setPadding(dp(5), dp(9), dp(5), dp(9));
                return view;
            }
            @Override public View getDropDownView(int position, View convertView,
                                                  android.view.ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.DKGRAY);
                view.setPadding(dp(12), dp(12), dp(12), dp(12));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void pick() {
        if (busy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_MEDIA);
        } catch (Exception ex) {
            status.setText("Tidak ada pemilih galeri: " + ex.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_MEDIA || resultCode != RESULT_OK || data == null) return;
        pending.clear();
        java.util.ArrayList<Uri> chosen=new java.util.ArrayList<>();
        if (data.getClipData() != null) {
            ClipData clip = data.getClipData();
            for(int i=0;i<clip.getItemCount()&&chosen.size()<10;i++){
                chosen.add(clip.getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            chosen.add(data.getData());
        }
        if(chosen.isEmpty())return;
        try{
            queueOptions=new ExportOptions(Math.max(0,presets.getCheckedRadioButtonId()-1000),
                ratioSpinner.getSelectedItemPosition(),frameSpinner.getSelectedItemPosition(),
                hdSwitch.isChecked(),secondsToMs(trimStartField),secondsToMs(trimEndField),
                focusXSeek.getProgress()/100f,focusYSeek.getProgress()/100f);
        }catch(IllegalArgumentException e){status.setText(e.getMessage());return;}
        for(Uri uri:chosen){
            if(dualAspectSwitch.isChecked()){
                pending.add(new ExportJob(uri,queueOptions.forRatio(AspectRatioProfile.PORTRAIT_9_16)));
                pending.add(new ExportJob(uri,queueOptions.forRatio(AspectRatioProfile.LANDSCAPE_16_9)));
            }else pending.add(new ExportJob(uri,queueOptions));
        }
        getPreferences(MODE_PRIVATE).edit()
                .putInt("ratio", queueOptions.ratio).putInt("frame", queueOptions.frameStyle)
                .putInt("preset",queueOptions.preset).putBoolean("full_hd",queueOptions.fullHd)
                .putBoolean("dual_ratio",dualAspectSwitch.isChecked())
                .putString("trim_start",trimStartField.getText().toString())
                .putString("trim_end",trimEndField.getText().toString())
                .putInt("focus_x",focusXSeek.getProgress())
                .putInt("focus_y",focusYSeek.getProgress()).apply();
        total = pending.size(); done = 0; failed = 0; aborted = false; busy = true;
        selectButton.setEnabled(false); selectButton.setAlpha(0.55f);
        cancelButton.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        processNext();
    }

    private void processNext() {
        videoActive = false;
        if (aborted || pending.isEmpty()) {
            finishQueue();
            return;
        }
        ExportJob job=pending.removeFirst();
        Uri input=job.uri;
        int current = done + failed + 1;
        status.setText("Memproses file " + current + " dari " + total);
        count.setText("Tetap buka aplikasi sampai seluruh file selesai.");
        progress.setProgress(0);
        String mime = getContentResolver().getType(input);
        if (mime != null && mime.startsWith("video/")) {
            processVideo(input,job.options);
        } else if (mime != null && mime.startsWith("image/")) {
            processPhoto(input,job.options);
        } else {
            failed++;
            outputLabel.setText("Jenis file tak didukung: " + (mime == null ? "unknown" : mime));
            processNext();
        }
    }

    private void processPhoto(Uri input, ExportOptions settings) {
        progress.setIndeterminate(true);
        io.execute(() -> {
            try {
                PhotoEngine.Result result = PhotoEngine.process(this, input, settings);
                Uri saved = MediaSaver.savePhoto(this, result.jpeg, filename("jpg"));
                // Preview must never cause a successfully saved export to be marked failed.
                Bitmap before = null, after = null;
                try {
                    before = PhotoEngine.originalPreview(this, input, settings);
                    after = PhotoEngine.editedPreview(result.jpeg);
                } catch (Exception | OutOfMemoryError previewError) {
                    if (before != null) before.recycle();
                    if (after != null) after.recycle();
                    before = null;
                    after = null;
                }
                final Bitmap originalPreview = before, processedPreview = after;
                runOnUiThread(() -> {
                    progress.setIndeterminate(false);
                    progress.setProgress(100);
                    done++;
                    lastSaved = saved; lastType = "image/jpeg";
                    hideVideoPreview();
                    preview.setImages(originalPreview, processedPreview);
                    outputLabel.setText("Foto " + result.size + " ("
                            + AspectRatioProfile.label(settings.ratio)
                            + ") disimpan di Pictures/WFS StoryBoost");
                    updateShare();
                    processNext();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> errorAndContinue(ex));
            }
        });
    }

    private void processVideo(Uri input, ExportOptions settings) {
        videoActive = true;
        progress.setIndeterminate(false);
        try {
            videos.start(this, input, settings, new VideoEngine.Listener() {
                @Override
                public void onSuccess(File file, AspectRatioProfile.Size dimensions) {
                    videoActive = false;
                    stopPulse();
                    progress.setProgress(100);
                    io.execute(() -> {
                        try {
                            Uri saved = MediaSaver.saveVideo(MainActivity.this, file, filename("mp4"));
                            runOnUiThread(() -> {
                                done++;
                                lastSaved = saved; lastType = "video/mp4";
                                preview.clearImages();
                                preview.setVisibility(View.GONE);
                                previewOriginalVideo=input;previewExportVideo=saved;
                                originalVideoButton.setVisibility(View.VISIBLE);
                                exportedVideoButton.setVisibility(View.VISIBLE);
                                videoPreview.setVisibility(View.VISIBLE);
                                videoPreview.setVideoURI(saved);
                                videoPreview.seekTo(1);
                                outputLabel.setText("Video " + dimensions + " ("
                                        + AspectRatioProfile.label(settings.ratio)
                                        + ") disimpan di Movies/WFS StoryBoost");
                                updateShare();
                                videos.cleanup();
                                processNext();
                            });
                        } catch (Exception ex) {
                            runOnUiThread(() -> {
                                videos.cleanup();
                                errorAndContinue(ex);
                            });
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    videoActive = false;
                    stopPulse();
                    videos.cleanup();
                    errorAndContinue(new Exception(message));
                }
            });
            videoPulse = new Runnable() {
                @Override public void run() {
                    if (!videoActive) return;
                    try {
                        int value = videos.progress();
                        if (value >= 0) {
                            progress.setProgress(value);
                            count.setText("Mengolah video: " + value + "% • audio dipertahankan");
                        }
                    } catch (Exception ignored) {}
                    handler.postDelayed(this, 400);
                }
            };
            handler.post(videoPulse);
        } catch (Exception ex) {
            videoActive = false;
            stopPulse();
            videos.cancel();
            errorAndContinue(ex);
        }
    }

    private void errorAndContinue(Exception ex) {
        progress.setIndeterminate(false);
        failed++;
        outputLabel.setText("Satu file gagal: " + ex.getMessage());
        processNext();
    }

    private void finishQueue() {
        busy = false;
        videoActive = false;
        stopPulse();
        progress.setIndeterminate(false);
        selectButton.setEnabled(true); selectButton.setAlpha(1f);
        cancelButton.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        status.setText(aborted ? "Antrean dibatalkan" : "Proses selesai");
        count.setText(done + " berhasil • " + failed + " gagal • " + (aborted ? "sisa dibatalkan" : "siap story"));
        if (done > 0) progress.setProgress(100);
    }

    private void stopPulse() {
        if (videoPulse != null) handler.removeCallbacks(videoPulse);
        videoPulse = null;
    }

    private void cancelWork() {
        aborted = true;
        pending.clear();
        if (videoActive) {
            videoActive = false;
            stopPulse();
            videos.cancel();
            finishQueue();
        } else {
            count.setText("Antrean sisa dibatalkan. Menyelesaikan file yang sedang diproses…");
        }
    }

    private void updateShare() {
        shareButton.setEnabled(true);
        shareButton.setAlpha(1f);
    }

    private void share() {
        if (lastSaved == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(lastType);
        send.putExtra(Intent.EXTRA_STREAM, lastSaved);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Bagikan hasil ke WhatsApp atau aplikasi lain"));
    }

    private static String filename(String ext) {
        return "WFS_STORY_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
                .format(new Date()) + "." + ext;
    }

    private Switch switcher(String caption, boolean checked) {
        Switch widget = new Switch(this);
        widget.setText(caption);
        widget.setTextColor(TEXT);
        widget.setTextSize(13);
        widget.setChecked(checked);
        widget.setThumbTintList(new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{ACCENT, 0xFFADB5C4}));
        widget.setPadding(0, dp(8), 0, dp(8));
        return widget;
    }

    private TextView text(String content, int size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(content);
        tv.setTextColor(color);
        tv.setTextSize(size);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private Button button(String caption, boolean primary) {
        Button button = new Button(this);
        button.setText(caption);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? BG : TEXT);
        button.setBackground(round(primary ? ACCENT : 0xFF243654, 16));
        button.setPadding(dp(10), dp(7), dp(10), dp(7));
        button.setMinimumHeight(dp(54));
        return button;
    }

    private LinearLayout box(int color, int radius) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(round(color, radius));
        return layout;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private void space(LinearLayout parent, int height) {
        View view = new View(this);
        parent.addView(view, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(int units) { return Math.round(units * getResources().getDisplayMetrics().density); }

    @Override
    protected void onDestroy() {
        stopPulse();
        videos.cancel();
        hideVideoPreview();
        preview.clearImages();
        io.shutdown();
        super.onDestroy();
    }
}
