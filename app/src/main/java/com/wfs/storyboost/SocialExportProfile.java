package com.wfs.storyboost;

/**
 * Export recipes, not a promise that a platform won't recompress an uploaded file.
 * All profiles choose portrait 1080p H.264/AAC; source fps is not forcibly changed.
 */
public final class SocialExportProfile {
    private SocialExportProfile(){}
    public static final int CUSTOM=0,WHATSAPP_STATUS=1,INSTAGRAM_STORY=2,
                            INSTAGRAM_REELS=3,TIKTOK=4;
    public static final class Profile {
        public final String name,description;
        public final int maxBitrate;
        public final float safeTop,safeBottom;
        private Profile(String n,String d,int b,float top,float bottom){
            name=n;description=d;maxBitrate=b;safeTop=top;safeBottom=bottom;
        }
    }
    private static final Profile[] PROFILES={
        new Profile("Manual / Original","Resolusi dan bitrate sesuai pilihan editor",60000000,0f,0f),
        new Profile("WhatsApp Status","Story 9:16 1080p • konservatif 8 Mbps",8000000,.15f,.22f),
        new Profile("Instagram Story","9:16 1080p • 12 Mbps • teks aman",12000000,.14f,.25f),
        new Profile("Instagram Reels","9:16 1080p • 16 Mbps • area UI",16000000,.14f,.35f),
        new Profile("TikTok","9:16 1080p • 18 Mbps • area UI",18000000,.15f,.35f)
    };
    public static int count(){return PROFILES.length;}
    public static Profile get(int id){
        if(id<0||id>=PROFILES.length)throw new IllegalArgumentException("Profil sosial tidak dikenal");
        return PROFILES[id];
    }
    public static String[] names(){
        String[] names=new String[PROFILES.length];
        for(int i=0;i<PROFILES.length;i++)names[i]=PROFILES[i].name;
        return names;
    }
    public static int cappedBitrate(int id,int targetWidth,int targetHeight,int quality,float fps){
        int estimated=VideoBitrate.estimate(targetWidth,targetHeight,quality,fps);
        return id==CUSTOM?estimated:Math.min(estimated,get(id).maxBitrate);
    }
    public static float captionCenterY(int id){
        if(id==CUSTOM)return .83f;
        Profile p=get(id);
        return p.safeTop+(1f-p.safeTop-p.safeBottom)*.55f;
    }
}
