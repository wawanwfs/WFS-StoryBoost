package com.wfs.storyboost;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small, private, non-destructive edit drafts keyed by a SHA-256 of the source
 * URI. Neither original media bytes nor the raw source URI are persisted here.
 * Only the explicit Recent Projects button restores the corresponding draft.
 */
public final class StudioDraftStore {
    private static final String PREFS="studio-drafts";
    private static final int FORMAT_VERSION=1;
    private StudioDraftStore(){}

    public static final class Draft{
        public final ExportOptions options;
        public final long splitMs;
        public final boolean dualOutput;
        Draft(ExportOptions o,long split,boolean dual){
            options=o;splitMs=split;dualOutput=dual;
        }
    }
    static String keyFor(Uri uri) {
        if(uri==null)throw new IllegalArgumentException("URI tidak tersedia");
        try{
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(
                    uri.toString().getBytes(StandardCharsets.UTF_8));
            char[] hex="0123456789abcdef".toCharArray();
            char[] result=new char[bytes.length*2];
            for(int i=0;i<bytes.length;i++){
                result[i*2]=hex[(bytes[i]>>>4)&15];
                result[i*2+1]=hex[bytes[i]&15];
            }
            return "draft_v1_"+new String(result);
        }catch(NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 unavailable",e);
        }
    }

    public static boolean save(Context context,Uri source,ExportOptions o,long split,boolean dual){
        if(o==null||source==null) return false;
        try{
            JSONObject json=new JSONObject();
            json.put("v",FORMAT_VERSION);
            json.put("preset",o.preset);
            json.put("ratio",o.ratio);
            json.put("frame",o.frameStyle);
            json.put("quality",o.qualityMode);
            json.put("focusX",o.focusX);
            json.put("focusY",o.focusY);
            json.put("bright",o.brightness);
            json.put("contrast",o.contrast);
            json.put("saturation",o.saturation);
            json.put("caption",o.caption);
            json.put("captionColor",o.captionColor);
            json.put("mute",o.muteAudio);
            json.put("social",o.socialProfile);
            json.put("customW",o.customWidth);
            json.put("customH",o.customHeight);
            json.put("bokeh",o.portraitBokeh);
            json.put("denoise",o.aiDenoise);
            json.put("trimStart",o.trimStartMs);
            json.put("trimEnd",o.trimEndMs);
            json.put("split",split);
            json.put("dual",dual);
            String encoded=json.toString();
            if(encoded.length()>4096)return false;
            return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                    .putString(keyFor(source),encoded).commit();
        }catch(JSONException|IllegalArgumentException e){return false;}
    }

    public static Draft load(Context context,Uri source){
        if(source==null)return null;
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String encoded=p.getString(keyFor(source),null);
        if(encoded==null||encoded.length()>4096)return null;
        try{
            JSONObject j=new JSONObject(encoded);
            if(j.getInt("v")!=FORMAT_VERSION)return null;
            int preset=j.getInt("preset"),ratio=j.getInt("ratio");
            if(preset<0||preset>=ColorLooks.count()||
                    ratio<0||ratio>=AspectRatioProfile.count())return null;
            int frame=j.getInt("frame");
            if(frame<AspectRatioProfile.FIT||frame>AspectRatioProfile.BLUR)return null;
            int customW=j.getInt("customW"),customH=j.getInt("customH");
            long start=j.getLong("trimStart"),end=j.getLong("trimEnd");
            long split=j.getLong("split");
            if(split < -1L || split > 24L*3600*1000) return null;
            int social=j.getInt("social");
            ExportOptions o=new ExportOptions(preset,ratio,frame,
                    true,start,end,(float)j.getDouble("focusX"),
                    (float)j.getDouble("focusY"))
                    .withQuality(j.getInt("quality"))
                    .withAdjustments((float)j.getDouble("bright"),
                            (float)j.getDouble("contrast"),
                            (float)j.getDouble("saturation"))
                    .withCaption(j.getString("caption"),j.getInt("captionColor"))
                    .withMute(j.getBoolean("mute"))
                    .withSocialProfile(social);
            if(ratio==AspectRatioProfile.CUSTOM)o=o.withCustomSize(customW,customH);
            else o=o.forRatio(ratio);
            o=o.withQuality(j.getInt("quality"))
                    .withPortraitBokeh(j.getBoolean("bokeh"))
                    .withAiDenoise(j.getBoolean("denoise"));
            return new Draft(o,split,j.getBoolean("dual"));
        }catch(JSONException|IllegalArgumentException ex){
            return null; // corrupted or version-incompatible state: never crash editor
        }
    }
    public static void clearAll(Context context){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().commit();
    }
}