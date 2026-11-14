package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/** Paints photo captions on the same relative canvas as the export. */
final class PhotoCaptionView extends View {
    private final PhotoCaptionRenderer renderer=new PhotoCaptionRenderer();
    private String text="";
    private int color,profile;

    PhotoCaptionView(Context context){super(context);}

    void update(String caption,int captionColor,int socialProfile,boolean compareOriginal){
        text=caption.trim();color=captionColor;profile=socialProfile;
        setVisibility(text.isEmpty()||compareOriginal?GONE:VISIBLE);
        setContentDescription(text.isEmpty()?null:"Teks foto: "+text);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(getWidth()>0&&getHeight()>0)
            renderer.draw(canvas,text,color,profile,getWidth(),getHeight());
    }
}
