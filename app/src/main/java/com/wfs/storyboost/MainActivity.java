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
    private Spinner ratioSpinner, frameSpinner, qualitySpinner;
    private TextView ratioHint;
    private Switch dualAspectSwitch;
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
        final int PURPLE = 0xFF9776FF;
        final int CYAN = 0xFF39C9FF;
        final int MUTED_CARD = 0xFF11182D;
        final int INNER = 0xFF202944;
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(BG);
        screen.setFitsSystemWindows(true);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(BG);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(column);
        screen.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Header: branded, consistent with the launcher artwork.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setContentDescription("Logo WFS StoryBoost");
        header.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout name = new LinearLayout(this);
        name.setOrientation(LinearLayout.VERTICAL);
        name.addView(text("WFS StoryBoost", 23, TEXT, true));
        TextView kicker = text("CREATIVE MEDIA STUDIO  /  v2.0", 10, CYAN, true);
        kicker.setLetterSpacing(0.08f);
        name.addView(kicker);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -2, 1f);
        nameLp.leftMargin = dp(9);
        header.addView(name, nameLp);
        TextView offline = text("● OFFLINE", 10, 0xFF91F0CB, true);
        offline.setPadding(dp(10), dp(9), dp(10), dp(9));
        offline.setBackground(round(0xFF183D3C, 14));
        header.addView(offline);
        column.addView(header);
        space(column, 18);

        // Accent hero with the principal one-tap operation.
        LinearLayout hero = box(CARD, 24);
        hero.setPadding(dp(20), dp(21), dp(20), dp(20));
        hero.setBackground(gradientCard(0xFF243E80, 0xFF45246D, 24));
        column.addView(hero);
        TextView heroTag = text("✦  YOUR STORY, BOOSTED", 11, 0xFFD5E4FF, true);
        heroTag.setLetterSpacing(0.12f);
        hero.addView(heroTag);
        space(hero, 12);
        TextView heroTitle = text("Lebih jernih.\nLebih estetik.", 29, TEXT, true);
        heroTitle.setLineSpacing(dp(2), 1.0f);
        hero.addView(heroTitle);
        space(hero, 9);
        hero.addView(text("Foto & video dengan warna sinematik, rasio fleksibel, dan detail ekspor yang dijaga.", 13, 0xFFE0E4FF, false));
        space(hero, 17);
        selectButton = button("✦   Pilih Media & Enhance", true);
        selectButton.setBackground(gradientCard(0xFFC0A7FF, 0xFF73CBFF, 16));
        hero.addView(selectButton);
        selectButton.setOnClickListener(v -> pick());
        space(hero, 12);
        hero.addView(text("✓  Tanpa upload server    ✓  Tanpa watermark", 11, 0xFFDAE7FF, false));
        space(column, 20);

        column.addView(text("QUICK STUDIO", 11, CYAN, true));
        space(column, 7);
        column.addView(text("Mau edit apa hari ini?", 21, TEXT, true));
        space(column, 12);
        LinearLayout toolsFirst = new LinearLayout(this);
        toolsFirst.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout photoTile = actionTile("▧", "Enhance Foto", "Detail & warna", 0xFF8AA7FF, () -> pickOfType("image/*"));
        toolsFirst.addView(photoTile, new LinearLayout.LayoutParams(0, dp(125), 1));
        LinearLayout videoTile = actionTile("▶", "Enhance Video", "FPS & kualitas", 0xFF4DD4FF, () -> pickOfType("video/*"));
        LinearLayout.LayoutParams videoLp = new LinearLayout.LayoutParams(0, dp(125), 1);
        videoLp.leftMargin = dp(10);
        toolsFirst.addView(videoTile, videoLp);
        column.addView(toolsFirst);
        space(column, 10);

        LinearLayout toolsSecond = new LinearLayout(this);
        toolsSecond.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout ratioTile = actionTile("↔", "Ubah Rasio", "9:16 ↔ 16:9", 0xFFCA87FF,
                () -> scroll.smoothScrollTo(0, editorCardTop(column)));
        toolsSecond.addView(ratioTile, new LinearLayout.LayoutParams(0, dp(125), 1));
        LinearLayout batchTile = actionTile("▤", "Batch Export", "Banyak file", 0xFF78E8C3, () -> pick());
        LinearLayout.LayoutParams batchLp = new LinearLayout.LayoutParams(0, dp(125), 1);
        batchLp.leftMargin = dp(10);
        toolsSecond.addView(batchTile, batchLp);
        column.addView(toolsSecond);
        space(column, 21);

        // The next two cards are direct children of column for navigation anchors.
        final LinearLayout editCard = box(CARD, 24);
        editCard.setTag("editor");
        editCard.setPadding(dp(18), dp(19), dp(18), dp(19));
        column.addView(editCard);
        editCard.addView(text("01  /  EDITOR", 11, CYAN, true));
        space(editCard, 6);
        editCard.addView(text("Sesuaikan gayamu", 22, TEXT, true));
        editCard.addView(text("Preset warna, framing, dan pengaturan media", 12, SOFT, false));
        space(editCard, 17);

        editCard.addView(text("COLOR LOOK", 11, CYAN, true));
        space(editCard, 7);
        presets = new RadioGroup(this);
        presets.setOrientation(RadioGroup.HORIZONTAL);
        String[] looks = {"Natural", "Cinematic", "Vibrant", "Soft Night"};
        for (int i = 0; i < looks.length; i++) {
            RadioButton radio = new RadioButton(this);
            radio.setId(1000 + i);
            radio.setText(looks[i]);
            radio.setTextColor(TEXT);
            radio.setTextSize(12);
            radio.setButtonTintList(ColorStateList.valueOf(PURPLE));
            radio.setBackground(round(INNER, 12));
            radio.setPadding(dp(5), dp(12), dp(11), dp(12));
            RadioGroup.LayoutParams radioLp = new RadioGroup.LayoutParams(-2, -2);
            if (i > 0) radioLp.leftMargin = dp(8);
            presets.addView(radio, radioLp);
        }
        android.widget.HorizontalScrollView looksScroller = new android.widget.HorizontalScrollView(this);
        looksScroller.setHorizontalScrollBarEnabled(false);
        looksScroller.addView(presets);
        editCard.addView(looksScroller);
        presets.check(1001);
        space(editCard, 20);

        editCard.addView(text("ASPECT RATIO", 11, CYAN, true));
        space(editCard, 8);
        ratioSpinner = spinner(new String[]{
                "9:16 · Story / Reels", "16:9 · Landscape",
                "1:1 · Square", "4:5 · Feed Portrait", "Original · Rasio Sumber"
        });
        editCard.addView(ratioSpinner);
        space(editCard, 8);
        final Button[] ratioChips = new Button[5];
        String[] chipNames = {"9:16", "16:9", "1:1", "4:5", "Asli"};
        LinearLayout chipBar = new LinearLayout(this);
        chipBar.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < chipNames.length; i++) {
            final int selected = i;
            Button chip = button(chipNames[i], false);
            chip.setTextSize(12);
            chip.setMinimumHeight(dp(42));
            chip.setMinHeight(dp(42));
            chip.setPadding(0, dp(3), 0, dp(3));
            chip.setOnClickListener(v -> ratioSpinner.setSelection(selected));
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i > 0) chipLp.leftMargin = dp(5);
            chipBar.addView(chip, chipLp);
            ratioChips[i] = chip;
        }
        editCard.addView(chipBar);
        Button reverseRatio = button("⇄  Tukar Portrait / Landscape", false);
        reverseRatio.setOnClickListener(v -> ratioSpinner.setSelection(
                AspectRatioProfile.reverse(ratioSpinner.getSelectedItemPosition())));
        editCard.addView(reverseRatio);
        space(editCard, 17);

        editCard.addView(text("FILL MODE", 11, CYAN, true));
        space(editCard, 7);
        frameSpinner = spinner(new String[]{
                "Fit · Media tetap utuh", "Crop · Memenuhi frame",
                "Blur · Background bergerak"
        });
        editCard.addView(frameSpinner);
        space(editCard, 11);
        editCard.addView(text("Posisi fokus crop  •  Kiri ↔ Kanan", 12, SOFT, false));
        focusXSeek = new SeekBar(this);
        focusXSeek.setMax(100); focusXSeek.setProgress(50);
        focusXSeek.setProgressTintList(ColorStateList.valueOf(PURPLE));
        editCard.addView(focusXSeek);
        editCard.addView(text("Posisi fokus crop  •  Atas ↕ Bawah", 12, SOFT, false));
        focusYSeek = new SeekBar(this);
        focusYSeek.setMax(100); focusYSeek.setProgress(50);
        focusYSeek.setProgressTintList(ColorStateList.valueOf(PURPLE));
        editCard.addView(focusYSeek);
        space(editCard, 13);

        editCard.addView(text("OUTPUT QUALITY", 11, CYAN, true));
        space(editCard, 8);
        qualitySpinner = spinner(new String[]{
                "HD · Hemat ruang", "Full HD · 1080p",
                "Max Detail · hingga 4K sumber"
        });
        editCard.addView(qualitySpinner);
        space(editCard, 6);
        ratioHint = text("", 12, SOFT, false);
        editCard.addView(ratioHint);
        space(editCard, 15);
        dualAspectSwitch = switcher("Ekspor dua rasio (9:16 + 16:9)", false);
        editCard.addView(dualAspectSwitch);
        space(editCard, 8);
        editCard.addView(text("TRIM VIDEO (DETIK, OPSIONAL)", 11, CYAN, true));
        space(editCard, 7);
        LinearLayout trimRow = new LinearLayout(this);
        trimRow.setOrientation(LinearLayout.HORIZONTAL);
        trimStartField = trimInput("Mulai 0");
        trimEndField = trimInput("Akhir 0");
        trimStartField.setBackground(round(INNER, 12));
        trimEndField.setBackground(round(INNER, 12));
        trimStartField.setPadding(dp(12), 0, dp(12), 0);
        trimEndField.setPadding(dp(12), 0, dp(12), 0);
        trimRow.addView(trimStartField, new LinearLayout.LayoutParams(0, dp(50), 1));
        LinearLayout.LayoutParams trimLp = new LinearLayout.LayoutParams(0, dp(50), 1);
        trimLp.leftMargin = dp(9);
        trimRow.addView(trimEndField, trimLp);
        editCard.addView(trimRow);
        space(editCard, 16);
        Button runFromEditor = button("✦  Proses dengan pengaturan ini", true);
        runFromEditor.setBackground(gradientCard(0xFFA16AF8, 0xFF458DFF, 16));
        runFromEditor.setTextColor(TEXT);
        runFromEditor.setOnClickListener(v -> pick());
        editCard.addView(runFromEditor);
        space(column, 15);

        final LinearLayout results = box(CARD, 24);
        results.setTag("results");
        results.setPadding(dp(18), dp(19), dp(18), dp(19));
        column.addView(results);
        results.addView(text("02  /  EXPORTS", 11, CYAN, true));
        space(results, 6);
        results.addView(text("Hasil & preview", 22, TEXT, true));
        space(results, 7);
        status = text("Belum ada media dipilih", 15, TEXT, true);
        results.addView(status);
        space(results, 4);
        count = text("Hasil tersimpan di galeri dan bisa langsung dibagikan.", 12, SOFT, false);
        results.addView(count);
        space(results, 13);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(ColorStateList.valueOf(PURPLE));
        progress.setProgress(0);
        results.addView(progress, new LinearLayout.LayoutParams(-1, dp(6)));
        space(results, 14);
        preview = new BeforeAfterView(this);
        preview.setContentDescription("Geser pembatas untuk membandingkan foto asli dan hasil edit");
        preview.setVisibility(View.GONE);
        results.addView(preview, new LinearLayout.LayoutParams(-1, dp(280)));
        results.addView(text("Geser garis tengah untuk membandingkan foto", 11, SOFT, false));
        videoPreview = new VideoView(this);
        videoPreview.setVisibility(View.GONE);
        videoPreview.setBackgroundColor(Color.BLACK);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(videoPreview);
        videoPreview.setMediaController(controls);
        results.addView(videoPreview, new LinearLayout.LayoutParams(-1, dp(260)));
        originalVideoButton = button("▶  Putar video asli", false);
        exportedVideoButton = button("▶  Putar video hasil", false);
        originalVideoButton.setVisibility(View.GONE);
        exportedVideoButton.setVisibility(View.GONE);
        originalVideoButton.setOnClickListener(v -> playVideoPreview(false));
        exportedVideoButton.setOnClickListener(v -> playVideoPreview(true));
        results.addView(originalVideoButton);
        results.addView(exportedVideoButton);
        outputLabel = text("", 12, SOFT, false);
        results.addView(outputLabel);
        space(results, 10);
        shareButton = button("↗  Bagikan hasil terakhir", true);
        shareButton.setBackground(gradientCard(0xFFA16AF8, 0xFF458DFF, 14));
        shareButton.setTextColor(TEXT);
        shareButton.setEnabled(false);
        shareButton.setAlpha(0.5f);
        shareButton.setOnClickListener(v -> share());
        results.addView(shareButton);
        space(results, 9);
        cancelButton = button("Batalkan antrean", false);
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(v -> cancelWork());
        results.addView(cancelButton);
        space(column, 17);
        TextView footer = text("PRIVATE BY DESIGN  ·  Tanpa akun, tanpa server, tanpa watermark.\nKualitas unggahan WhatsApp tetap mengikuti kompresi platform.", 11, SOFT, false);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(footer);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        ratioSpinner.setSelection(Math.max(0, Math.min(4, prefs.getInt("ratio", 0))));
        frameSpinner.setSelection(Math.max(0, Math.min(2, prefs.getInt("frame", 0))));
        qualitySpinner.setSelection(Math.max(0, Math.min(2, prefs.getInt("quality", OutputProfile.MAX_DETAIL))));
        presets.check(1000 + Math.max(0, Math.min(3, prefs.getInt("preset", 1))));
        dualAspectSwitch.setChecked(prefs.getBoolean("dual_ratio", false));
        trimStartField.setText(prefs.getString("trim_start", ""));
        trimEndField.setText(prefs.getString("trim_end", ""));
        focusXSeek.setProgress(Math.max(0, Math.min(100, prefs.getInt("focus_x", 50))));
        focusYSeek.setProgress(Math.max(0, Math.min(100, prefs.getInt("focus_y", 50))));
        ratioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                for (int i = 0; i < ratioChips.length; i++) {
                    ratioChips[i].setBackground(round(i == position ? 0xFF7045B9 : INNER, 12));
                }
                updateRatioHint();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                updateRatioHint();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        updateRatioHint();

        // Pinned bottom navigation: all controls scroll to real active sections.
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(7), dp(10), dp(7));
        nav.setBackgroundColor(0xFF131B30);
        TextView navHome = navItem("⌂\nBeranda", CYAN);
        TextView navEdit = navItem("✦\nEditor", SOFT);
        TextView navOutput = navItem("▤\nHasil", SOFT);
        nav.addView(navHome, new LinearLayout.LayoutParams(0, dp(57), 1));
        nav.addView(navEdit, new LinearLayout.LayoutParams(0, dp(57), 1));
        nav.addView(navOutput, new LinearLayout.LayoutParams(0, dp(57), 1));
        navHome.setOnClickListener(v -> scroll.smoothScrollTo(0, 0));
        navEdit.setOnClickListener(v -> scroll.smoothScrollTo(0, editCard.getTop()));
        navOutput.setOnClickListener(v -> scroll.smoothScrollTo(0, results.getTop()));
        screen.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(screen);
    }

    private int editorCardTop(LinearLayout column) {
        for (int i=0; i<column.getChildCount(); i++) {
            View child = column.getChildAt(i);
            if ("editor".equals(child.getTag())) return child.getTop();
        }
        return 0;
    }

    private GradientDrawable gradientCard(int first, int last, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{first, last});
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout actionTile(String glyph, String title, String description,
                                    int accent, Runnable action) {
        LinearLayout tile = box(CARD, 19);
        tile.setPadding(dp(15), dp(13), dp(9), dp(12));
        TextView glyphView = text(glyph, 25, accent, true);
        tile.addView(glyphView);
        space(tile, 6);
        tile.addView(text(title, 15, TEXT, true));
        space(tile, 3);
        tile.addView(text(description, 11, SOFT, false));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription(title);
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    private TextView navItem(String caption, int color) {
        TextView view = text(caption, 12, color, true);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
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
        if (ratioHint == null || ratioSpinner == null || qualitySpinner == null) return;
        int selected = qualitySpinner.getSelectedItemPosition();
        if (selected == OutputProfile.MAX_DETAIL) {
            ratioHint.setText("Max Detail • resolusi menyesuaikan sumber (hingga 4K). "
                    + "Crop tidak memperbesar piksel asli; hasil bisa lebih kecil.");
        } else {
            AspectRatioProfile.Size size = AspectRatioProfile.resolve(
                    ratioSpinner.getSelectedItemPosition(),
                    selected == OutputProfile.FULL_HD, 1080, 1920);
            ratioHint.setText("Kanvas ekspor " + size + " • "
                    + AspectRatioProfile.label(ratioSpinner.getSelectedItemPosition()));
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

    private void pick() { pickOfType("*/*"); }

    private void pickOfType(String filter) {
        if (busy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(filter);
        if ("*/*".equals(filter)) intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
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
                qualitySpinner.getSelectedItemPosition()!=OutputProfile.HD,secondsToMs(trimStartField),secondsToMs(trimEndField),
                focusXSeek.getProgress()/100f,focusYSeek.getProgress()/100f)
                .withQuality(qualitySpinner.getSelectedItemPosition());
        }catch(IllegalArgumentException e){status.setText(e.getMessage());return;}
        for(Uri uri:chosen){
            if(dualAspectSwitch.isChecked()){
                pending.add(new ExportJob(uri,queueOptions.forRatio(AspectRatioProfile.PORTRAIT_9_16)));
                pending.add(new ExportJob(uri,queueOptions.forRatio(AspectRatioProfile.LANDSCAPE_16_9)));
            }else pending.add(new ExportJob(uri,queueOptions));
        }
        getPreferences(MODE_PRIVATE).edit()
                .putInt("ratio", queueOptions.ratio).putInt("frame", queueOptions.frameStyle)
                .putInt("preset",queueOptions.preset).putInt("quality",queueOptions.qualityMode)
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
