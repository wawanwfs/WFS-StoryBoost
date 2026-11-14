package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/** Paints captions on the same relative canvas as the export. */
final class CaptionPreviewView extends View {
    private final CaptionRenderer renderer=new CaptionRenderer();
    private String text="";
    private int color,profile;

    CaptionPreviewView(Context context){super(context);}

    void update(String caption,int captionColor,int socialProfile,boolean compareOriginal,boolean video){
        text=caption.trim();color=captionColor;profile=socialProfile;
        setVisibility(text.isEmpty()||compareOriginal?GONE:VISIBLE);
        setContentDescription(text.isEmpty()?null:(video?"Teks video: ":"Teks foto: ")+text);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(getWidth()>0&&getHeight()>0)
            renderer.draw(canvas,text,color,profile,getWidth(),getHeight());
    }
}
