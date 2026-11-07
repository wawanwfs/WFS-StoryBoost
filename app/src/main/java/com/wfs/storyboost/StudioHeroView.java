package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * Real on-device scenic illustration for the home hero, not a promotional mockup.
 * Scales from small phones to large screens without bitmap decoding or a network call.
 */
public final class StudioHeroView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StudioHeroView(Context context) { super(context); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, w * .2f, h,
                new int[]{0xFF121B48, 0xFF56428B, 0xFFE99A72, 0xFF131E3B},
                new float[]{0f, .43f, .74f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(new RadialGradient(w * .76f, h * .49f, w * .4f,
                new int[]{0x77FFD49B, 0x22F1A3BD, Color.TRANSPARENT}, null,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(w * .76f, h * .49f, w * .4f, paint);
        paint.setShader(null);
        paint.setColor(0xFFFFC795);
        canvas.drawCircle(w * .77f, h * .53f, w * .055f, paint);

        mountain(canvas,w,h,.69f,.1f,0xFF323666);
        mountain(canvas,w,h,.76f,.3f,0xFF1D2854);
        mountain(canvas,w,h,.86f,.5f,0xFF101D3D);

        // Perspective city street, retained as decorative art beneath legible text.
        Path road = new Path();
        road.moveTo(w*.52f,h*.68f);road.lineTo(w*.72f,h*.68f);
        road.lineTo(w*1.1f,h);road.lineTo(w*.12f,h);road.close();
        paint.setColor(0xFF273454);canvas.drawPath(road,paint);
        paint.setColor(0xFF6A6678);
        canvas.drawLine(w*.62f,h*.71f,w*.59f,h*.96f,paint);
        for(int i=0;i<8;i++){
            float t=i/8f, houseH=(.025f+.095f*t)*h;
            paint.setColor(i%2==0?0xFF14223D:0xFF20304A);
            canvas.drawRect(w*(.52f-.08f*i),h*(.72f+t*.24f)-houseH,
                    w*(.57f-.08f*i),h*(.72f+t*.24f),paint);
            canvas.drawRect(w*(.72f+.065f*i),h*(.72f+t*.24f)-houseH,
                    w*(.77f+.065f*i),h*(.72f+t*.24f),paint);
        }
        // Blue/purple light ribbons and subtle stars.
        paint.setColor(0x8841D6FF);
        paint.setStrokeWidth(Math.max(1.5f,w*.004f));
        canvas.drawLine(w*.08f,h*.92f,w*.55f,h*.7f,paint);
        paint.setColor(0x999C87FF);
        canvas.drawLine(w*.98f,h*.97f,w*.72f,h*.69f,paint);
        paint.setColor(0xFFE8E9FF);
        for(int i=0;i<11;i++){
            float x=w*(.49f+.045f*(i%8)),y=h*(.12f+.055f*((i*3)%7));
            canvas.drawCircle(x,y,Math.max(.7f,w*.0022f),paint);
        }
        // Contrast scrim ensures headline is readable on any device.
        paint.setShader(new LinearGradient(0,0,w,0,
                new int[]{0xE9091029,0xBF101835,0x2A101638},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);
        paint.setShader(new LinearGradient(0,h*.4f,0,h,
                new int[]{Color.TRANSPARENT,0xD20D1433},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,h*.4f,w,h,paint);
        paint.setShader(null);
    }

    private void mountain(Canvas c,float w,float h,float base,float shift,int color){
        Path p=new Path();p.moveTo(0,h);
        p.lineTo(0,h*base);
        p.lineTo(w*.16f,h*(base-.11f+shift*.1f));
        p.lineTo(w*.28f,h*(base-.02f));
        p.lineTo(w*.42f,h*(base-.17f+shift*.1f));
        p.lineTo(w*.59f,h*(base-.05f));
        p.lineTo(w*.76f,h*(base-.14f));
        p.lineTo(w,h*(base+.055f));p.lineTo(w,h);p.close();
        paint.setColor(color);c.drawPath(p,paint);
    }
}
