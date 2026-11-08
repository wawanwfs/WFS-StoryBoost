package com.wfs.storyboost;

/** Pure-Java aspect and export geometry, including custom size. Stable legacy IDs 0-4. */
public final class AspectRatioProfile {
    private AspectRatioProfile(){}

    public static final int PORTRAIT_9_16=0,LANDSCAPE_16_9=1,SQUARE_1_1=2,PORTRAIT_4_5=3,ORIGINAL=4;
    public static final int PORTRAIT_3_4=5,LANDSCAPE_4_3=6,LANDSCAPE_3_2=7,PORTRAIT_2_3=8;
    public static final int ULTRAWIDE_21_9=9,ULTRATALL_9_21=10,CUSTOM=11;
    public static final int FIT=0,CROP=1,BLUR=2;
    private static final int[][] RATIOS={
        {9,16},{16,9},{1,1},{4,5},{0,0},{3,4},{4,3},{3,2},{2,3},{21,9},{9,21},{0,0}
    };
    private static final String[] LABELS={
        "9:16","16:9","1:1","4:5","Original","3:4","4:3","3:2","2:3","21:9","9:21","Custom"
    };
    public static final class Size {
        public final int width,height;
        public Size(int width,int height){
            if(width<=0||height<=0)throw new IllegalArgumentException("Dimensi tidak valid");
            this.width=width;this.height=height;
        }
        @Override public String toString(){return width+" × "+height;}
    }
    public static int count(){return LABELS.length;}
    public static String[] labels(){return LABELS.clone();}
    public static String label(int ratio){validate(ratio);return LABELS[ratio];}
    public static double aspect(int ratio,int sw,int sh,int customW,int customH){
        validate(ratio);
        if(ratio==ORIGINAL){
            if(sw<=0||sh<=0)throw new IllegalArgumentException("Dimensi sumber kosong");
            return (double)sw/sh;
        }
        if(ratio==CUSTOM){
            validateCustom(customW,customH);
            return (double)customW/customH;
        }
        return (double)RATIOS[ratio][0]/RATIOS[ratio][1];
    }
    private static void validate(int r){
        if(r<0||r>=RATIOS.length)throw new IllegalArgumentException("Rasio tidak tersedia: "+r);
    }
    public static void validateCustom(int w,int h){
        if(w<64||h<64||w>3840||h>3840||(long)w*h>8_294_400L)
            throw new IllegalArgumentException("Custom: 64–3840 px; maksimum 8,3 MP.");
    }
    private static int even(int v){return Math.max(2,v-(v&1));}
    private static Size scaled(int w,int h,double scale){
        return new Size(even(Math.max(2,(int)Math.round(w*scale))),
                even(Math.max(2,(int)Math.round(h*scale))));
    }
    public static Size resolve(int r,boolean fullHd,int sw,int sh){
        return resolve(r,fullHd,sw,sh,0,0);
    }
    public static Size resolve(int r,boolean fullHd,int sw,int sh,int customW,int customH){
        validate(r);
        if(sw<=0||sh<=0)throw new IllegalArgumentException("Dimensi sumber tidak valid");
        int longSide=fullHd?1920:1280;
        if(r==ORIGINAL)return scaled(sw,sh,Math.min(1d,(double)longSide/Math.max(sw,sh)));
        if(r==CUSTOM){
            validateCustom(customW,customH);
            return scaled(customW,customH,Math.min(1d,(double)longSide/Math.max(customW,customH)));
        }
        int rw=RATIOS[r][0],rh=RATIOS[r][1];
        // Maintain legacy framing: 4:5 and 9:16 use 1080/720 short side.
        if(r==PORTRAIT_9_16||r==PORTRAIT_4_5||r==PORTRAIT_3_4||r==PORTRAIT_2_3){
            int shortSide=fullHd?1080:720;
            int w=shortSide,h=even((int)Math.round(shortSide*(double)rh/rw));
            return new Size(w,h);
        }
        if(r==SQUARE_1_1){
            int side=fullHd?1080:720;return new Size(side,side);
        }
        if(r==LANDSCAPE_16_9){
            return new Size(longSide,fullHd?1080:720);
        }
        return scaled(rw,rh,(double)longSide/Math.max(rw,rh));
    }
    public static Size resolve(int r,int quality,int sw,int sh,int framing){
        return resolve(r,quality,sw,sh,framing,0,0);
    }
    public static Size resolve(int r,int quality,int sw,int sh,int framing,int customW,int customH){
        validate(r);OutputProfile.normalized(quality);
        if(sw<=0||sh<=0)throw new IllegalArgumentException("Dimensi media tidak valid");
        if(quality!=OutputProfile.MAX_DETAIL)
            return resolve(r,quality==OutputProfile.FULL_HD,sw,sh,customW,customH);
        if(r==ORIGINAL)return scaled(sw,sh,Math.min(1d,3840d/Math.max(sw,sh)));
        int longest=OutputProfile.targetLongEdge(quality,sw,sh);
        Size s;
        if(r==CUSTOM){
            validateCustom(customW,customH);
            s=scaled(customW,customH,Math.min(1d,(double)longest/Math.max(customW,customH)));
        }else{
            int rw=RATIOS[r][0],rh=RATIOS[r][1];
            s=scaled(rw,rh,(double)longest/Math.max(rw,rh));
        }
        int w=s.width,h=s.height;
        if(framing==CROP){
            double limit=Math.min(1d,Math.min((double)sw/w,(double)sh/h));
            w=even(Math.max(2,(int)Math.floor(w*limit)));
            h=even(Math.max(2,(int)Math.floor(h*limit)));
        }
        return new Size(w,h);
    }
    public static int reverse(int r){
        switch(r){
            case PORTRAIT_9_16:return LANDSCAPE_16_9;
            case LANDSCAPE_16_9:return PORTRAIT_9_16;
            case PORTRAIT_3_4:return LANDSCAPE_4_3;
            case LANDSCAPE_4_3:return PORTRAIT_3_4;
            case PORTRAIT_2_3:return LANDSCAPE_3_2;
            case LANDSCAPE_3_2:return PORTRAIT_2_3;
            case ULTRAWIDE_21_9:return ULTRATALL_9_21;
            case ULTRATALL_9_21:return ULTRAWIDE_21_9;
            default:return r;
        }
    }
}
