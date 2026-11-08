package com.wfs.storyboost;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/**
 * Real JPEG export (not preview or visual beauty scoring) for all 24 filters
 * using identical, color-rich, synthetic input on API29 and API35.
 */
@RunWith(AndroidJUnit4.class)
public final class ColorLookExportTest {
    @Test(timeout=190000)
    public void all24LooksRenderDecodableDistinctNonDestructiveOutputs()throws Exception{
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File source=new File(ctx.getCacheDir(),"wfs-look-qa-"+System.nanoTime()+".jpg");
        Bitmap original=Bitmap.createBitmap(480,720,Bitmap.Config.ARGB_8888);
        try{
            for(int y=0;y<720;y++)for(int x=0;x<480;x++){
                // Saturation + midtone + highlight and dark gradients to
                // distinguish mono, pastel, night, warm and cool looks.
                int red=25+(x*220/480);
                int green=20+(y*230/720);
                int blue=(x*5+y*3)%255;
                original.setPixel(x,y,Color.rgb(red,green,blue));
            }
            try(FileOutputStream out=new FileOutputStream(source)){
                assertTrue(original.compress(Bitmap.CompressFormat.JPEG,93,out));
            }
        }finally{original.recycle();}
        final long size=source.length();
        Uri uri=Uri.fromFile(source);
        try{
            assertEquals("Expected exactly 24 named Color Looks",24,ColorLooks.count());
            Set<String> outputs=new HashSet<>();
            for(int preset=0;preset<24;preset++){
                ColorLooks.Look look=ColorLooks.get(preset);
                ExportOptions options=new ExportOptions(preset,
                    AspectRatioProfile.PORTRAIT_9_16,AspectRatioProfile.FIT,true)
                    .withCustomSize(360,640);
                PhotoEngine.Result image=PhotoEngine.process(ctx,uri,options);
                assertNotNull(look.name,image.jpeg);
                assertTrue(look.name+" generated empty JPEG",image.jpeg.length>1024);
                Bitmap decoded=BitmapFactory.decodeByteArray(image.jpeg,0,image.jpeg.length);
                assertNotNull(look.name+" export is not a decodable JPEG",decoded);
                try{
                    assertEquals(look.name+" width",360,decoded.getWidth());
                    assertEquals(look.name+" height",640,decoded.getHeight());
                }finally{decoded.recycle();}
                byte[] hash=MessageDigest.getInstance("SHA-256").digest(image.jpeg);
                StringBuilder hex=new StringBuilder();
                for(byte b:hash)hex.append(String.format(java.util.Locale.US,"%02x",b&0xff));
                assertTrue("Duplicate output pixels or identical JPEG for Look "+preset+
                        " "+look.name,outputs.add(hex.toString()));
            }
            assertEquals("Distinct export count",24,outputs.size());
            assertEquals("Color export modified source JPEG",size,source.length());
        }finally {
            if(!source.delete())source.deleteOnExit();
        }
    }
}
