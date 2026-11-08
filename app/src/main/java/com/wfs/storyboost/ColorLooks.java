package com.wfs.storyboost;

/** Consistent looks catalog for photos, video and both editor UIs. Values are deterministic, offline. */
public final class ColorLooks {
    private ColorLooks(){}
    public static final int ORIGINAL=4;
    public static final class Look {
        public final String name,category;
        public final float saturation,contrast,red,green,blue,lift;
        private Look(String name,String category,float saturation,float contrast,
                     float red,float green,float blue,float lift){
            this.name=name;this.category=category;
            this.saturation=saturation;this.contrast=contrast;
            this.red=red;this.green=green;this.blue=blue;this.lift=lift;
        }
    }
    private static final Look[] LOOKS={
        new Look("Natural","Essential",1.055f,1.035f,1.01f,1.01f,1.01f,3f),
        new Look("Cinematic","Essential",1.12f,1.07f,1.045f,1.008f,0.99f,2f),
        new Look("Vibrant","Essential",1.21f,1.095f,1.045f,1.01f,1.01f,2f),
        new Look("Soft Night","Night",1.075f,1.025f,1.018f,1.01f,1.01f,10f),
        new Look("Original","Essential",1f,1f,1f,1f,1f,0f),
        new Look("Teal & Orange","Cinematic",1.12f,1.09f,1.1f,0.99f,1.07f,-3f),
        new Look("Golden Hour","Warm",1.17f,1.055f,1.15f,1.06f,0.91f,6f),
        new Look("Warm Film","Vintage",0.9f,1.025f,1.095f,1.02f,0.92f,5f),
        new Look("Cool Breeze","Cool",1.035f,1.025f,0.94f,1.005f,1.13f,4f),
        new Look("Moody Blue","Cinematic",0.83f,1.12f,0.88f,0.97f,1.17f,-5f),
        new Look("Matte Film","Vintage",0.83f,0.9f,1.025f,1f,0.985f,14f),
        new Look("Vintage 90s","Vintage",0.78f,0.96f,1.1f,1.02f,0.89f,10f),
        new Look("Black & White","Mono",0f,1.075f,1f,1f,1f,1f),
        new Look("Silver Noir","Mono",0f,1.24f,1.01f,1.01f,1.025f,-7f),
        new Look("Rose Glow","Portrait",1.07f,0.98f,1.12f,0.975f,1.03f,10f),
        new Look("Pastel Dream","Portrait",0.73f,0.91f,1.06f,1.04f,1.07f,15f),
        new Look("Forest Green","Nature",1.17f,1.07f,0.93f,1.13f,1.02f,-2f),
        new Look("Urban Night","Night",1.11f,1.14f,0.94f,1.025f,1.14f,-8f),
        new Look("Neon City","Night",1.28f,1.16f,1.07f,0.96f,1.18f,-5f),
        new Look("Beach Light","Travel",1.16f,0.99f,0.99f,1.09f,1.14f,9f),
        new Look("Sunset Glow","Warm",1.17f,1.07f,1.18f,0.97f,0.9f,5f),
        new Look("Portrait Soft","Portrait",0.96f,0.96f,1.055f,1.02f,1f,7f),
        new Look("Cafe Brown","Vintage",0.84f,1.08f,1.15f,0.99f,0.83f,2f),
        new Look("Mono Fade","Mono",0f,0.9f,1.04f,1.04f,1.04f,19f)
    };
    public static int count(){return LOOKS.length;}
    public static Look get(int id){
        if(id<0||id>=LOOKS.length)throw new IllegalArgumentException("Color look tidak tersedia: "+id);
        return LOOKS[id];
    }
    public static String[] names(){
        String[] names=new String[LOOKS.length];
        for(int i=0;i<LOOKS.length;i++)names[i]=LOOKS[i].name;
        return names;
    }
    public static boolean isOriginal(int id){return id==ORIGINAL;}
}
