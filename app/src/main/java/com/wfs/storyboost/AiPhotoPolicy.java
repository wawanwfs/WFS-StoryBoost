package com.wfs.storyboost;

import java.util.Locale;

/** Deterministic policy applied to on-device ML Kit predictions. Not itself an ML model. */
public final class AiPhotoPolicy {
    private AiPhotoPolicy(){}
    public static final class Suggestion {
        public final int colorLook;
        public final float focusX,focusY;
        public final String reason;
        public final boolean hasFace;
        Suggestion(int look,float x,float y,String reason,boolean face){
            this.colorLook=look;this.focusX=x;this.focusY=y;
            this.reason=reason;this.hasFace=face;
        }
    }
    public static Suggestion suggest(String category,float confidence,int faceCount,
                                     float focusX,float focusY){
        String label=category==null?"":category.toLowerCase(Locale.ROOT);
        boolean trusted=confidence>=.55f;
        int look=faceCount>0?21:0;
        String reason=faceCount>0?"Wajah terdeteksi, warna Portrait Soft direkomendasikan"
                :"Warna Natural dipilih sebagai pilihan aman";
        if(trusted){
            if(any(label,"beach","ocean","sea","surfing","island")){
                look=19;reason="Pemandangan pantai: Beach Light";
            }else if(any(label,"sunset","sunrise","sunlight")){
                look=20;reason="Cahaya hangat: Sunset Glow";
            }else if(any(label,"forest","tree","plant","jungle","grass","green")){
                look=16;reason="Alam hijau: Forest Green";
            }else if(any(label,"selfie","portrait","face","person","smile","people")){
                look=21;reason="Potret manusia: Portrait Soft";
            }else if(any(label,"mountain","landscape","sky")){
                look=6;reason="Lanskap: Golden Hour";
            }else if(any(label,"building","city","street","urban")){
                look=1;reason="Kota dan arsitektur: Cinematic";
            }else if(any(label,"flower","blossom","garden")){
                look=14;reason="Bunga dan taman: Rose Glow";
            }else if(any(label,"coffee","cafe","food","restaurant")){
                look=22;reason="Makanan atau kafe: Cafe Brown";
            }
        }
        return new Suggestion(look,clamp(focusX),clamp(focusY),reason,faceCount>0);
    }
    private static boolean any(String label,String... words){
        for(String word:words)if(label.contains(word))return true;
        return false;
    }
    private static float clamp(float v){
        if(!Float.isFinite(v))return .5f;
        return Math.min(1f,Math.max(0f,v));
    }
}
