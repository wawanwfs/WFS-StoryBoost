package com.wfs.storyboost;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small no-dependency visual system, shared between the project home and editor. */
public final class StudioUi {
    private StudioUi(){}
    public static final int BG=0xFF090D19, PANEL=0xFF151B2C, INNER=0xFF202940;
    public static final int TEXT=0xFFF5F5FF, MUTED=0xFF9DA8C5, LAVENDER=0xFFC39EFF;
    public static final int CYAN=0xFF69CFFE, GREEN=0xFF94E6CB;
    public static int dp(Context c,int value){return (int)(value*c.getResources().getDisplayMetrics().density+.5f);}
    public static GradientDrawable solid(Context c,int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;
    }
    public static GradientDrawable gradient(Context c,int a,int b,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});
        d.setCornerRadius(dp(c,radius));return d;
    }
    public static TextView text(Context c,String value,int size,int color,boolean bold){
        TextView t=new TextView(c);
        t.setText(value);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);return t;
    }
    public static Button button(Context c,String text,boolean primary){
        Button b=new Button(c);
        b.setAllCaps(false);b.setText(text);b.setTextSize(13);b.setMinHeight(0);b.setMinimumHeight(0);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setBackground(primary?gradient(c,0xFF9A57E9,0xFF3B7EEC,15):solid(c,INNER,15));
        b.setPadding(dp(c,11),dp(c,5),dp(c,11),dp(c,5));
        return b;
    }
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static void gap(LinearLayout l,Context c,int h){View spacer=new View(c);l.addView(spacer,new LinearLayout.LayoutParams(1,dp(c,h)));}
    public static LinearLayout card(Context c,int padding){
        LinearLayout l=column(c);l.setBackground(solid(c,PANEL,20));
        l.setPadding(dp(c,padding),dp(c,padding),dp(c,padding),dp(c,padding));return l;
    }
}
