package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Non-exported preview overlay for safe placement of text behind platform UI. */
public final class SocialSafeZoneView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int profile=SocialExportProfile.CUSTOM;
    public SocialSafeZoneView(Context c){super(c);setClickable(false);}
    public void setProfile(int profile){
        SocialExportProfile.get(profile);this.profile=profile;invalidate();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(profile==SocialExportProfile.CUSTOM)return;
        SocialExportProfile.Profile p=SocialExportProfile.get(profile);
        float w=getWidth(),h=getHeight();
        float top=h*p.safeTop,bottom=h*(1f-p.safeBottom);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0x48050A20);
        canvas.drawRect(0,0,w,top,paint);
        canvas.drawRect(0,bottom,w,h,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f);
        paint.setColor(0xA0C8C9FF);
        canvas.drawLine(0,top,w,top,paint);
        canvas.drawLine(0,bottom,w,bottom,paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
