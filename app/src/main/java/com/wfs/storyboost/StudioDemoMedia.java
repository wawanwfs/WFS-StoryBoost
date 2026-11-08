package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Generated offline sample for trying the real editing workspace without gallery access. */
public final class StudioDemoMedia {
    private StudioDemoMedia(){}
    public static Uri generate(Context context) throws IOException{
        int w=900,h=1600;
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        try{
            Canvas canvas=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new LinearGradient(0,0,w,h,
                    new int[]{0xFF174A7C,0xFF72B6DB,0xFFE9B37D,0xFF2A3454},
                    new float[]{0f,.39f,.64f,1f},Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,p);p.setShader(null);
            p.setColor(0xFFFFDDAB);canvas.drawCircle(630,820,80,p);
            for(int k=0;k<4;k++){
                p.setColor(new int[]{0xFF667F94,0xFF50667C,0xFF354F66,0xFF253B58}[k]);
                Path hills=new Path();int y=860+k*115;hills.moveTo(0,h);
                hills.lineTo(0,y+70);hills.lineTo(160,y-80);hills.lineTo(350,y+50);
                hills.lineTo(530,y-150);hills.lineTo(780,y+10);hills.lineTo(w,y-85);
                hills.lineTo(w,h);hills.close();canvas.drawPath(hills,p);
            }
            Path road=new Path();road.moveTo(405,1040);road.lineTo(505,1040);
            road.lineTo(920,1600);road.lineTo(-10,1600);road.close();
            p.setColor(0xFF4B5D72);canvas.drawPath(road,p);
            p.setColor(0xFFF7CFAC);p.setStrokeWidth(7);
            canvas.drawLine(453,1130,465,1310,p);
            canvas.drawLine(466,1360,480,1600,p);
            File f=new File(context.getCacheDir(),"storyboost-demo.jpg");
            try(FileOutputStream out=new FileOutputStream(f)){
                if(!bitmap.compress(Bitmap.CompressFormat.JPEG,97,out))
                    throw new IOException("Demo JPEG gagal dibuat");
            }
            return Uri.fromFile(f);
        }finally{bitmap.recycle();}
    }
}
