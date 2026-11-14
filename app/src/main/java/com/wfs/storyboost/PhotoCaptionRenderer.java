package com.wfs.storyboost;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/** Shared, resolution-proportional photo caption drawing for preview and JPEG. */
final class PhotoCaptionRenderer {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);

    PhotoCaptionRenderer(){
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
    }

    void draw(Canvas canvas,String text,int color,int profile,int width,int height){
        String caption=text.trim().replace((char)10,' ');
        if(caption.isEmpty())return;
        float px=Math.min(width*.058f,height*.04f);
        paint.setTextSize(px);
        String[] lines;
        if(paint.measureText(caption)>width*.88f&&caption.contains(" ")){
            int middle=caption.length()/2;
            int split=caption.lastIndexOf(' ',middle);
            if(split<caption.length()/4)split=caption.indexOf(' ',middle);
            lines=split>0?new String[]{caption.substring(0,split),caption.substring(split+1)}
                         :new String[]{caption};
        }else lines=new String[]{caption};
        paint.setColor(color);
        paint.setShadowLayer(px*.11f,px*.03f,px*.10f,0xEE000000);
        float y=height*SocialExportProfile.captionCenterY(profile)
                -(lines.length-1)*px*.65f;
        for(String line:lines){
            // Fit even long captions within the frame, never clip at the sides.
            float fit=paint.measureText(line)>0?Math.min(1f,width*.9f/paint.measureText(line)):1f;
            canvas.save();canvas.translate(width/2f,y);canvas.scale(fit,1f);
            canvas.drawText(line,0,0,paint);canvas.restore();
            y+=px*1.25f;
        }
    }

}
