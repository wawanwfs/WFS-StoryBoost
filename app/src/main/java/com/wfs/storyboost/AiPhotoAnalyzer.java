package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.Collections;
import java.util.List;

/** Bundled ML Kit face detector + image labeler. Photos stay on the phone. */
public final class AiPhotoAnalyzer {
    private AiPhotoAnalyzer(){}
    public interface Callback {
        void onResult(Result result);
        void onError(String detail);
    }
    public static final class Result {
        public final AiPhotoPolicy.Suggestion suggestion;
        public final String label;
        public final float confidence;
        public final int faces;
        public final boolean partial;
        Result(AiPhotoPolicy.Suggestion s,String label,float conf,int faces,boolean partial){
            suggestion=s;this.label=label;confidence=conf;this.faces=faces;this.partial=partial;
        }
    }
    public static void analyze(Context context,Uri photo,Callback callback){
        Handler ui=new Handler(Looper.getMainLooper());
        new Thread(()->{
            final Bitmap bitmap;
            try{bitmap=PhotoEngine.aiInput(context,photo);}
            catch(Exception ex){ui.post(()->callback.onError("Foto tidak dapat dibaca: "+ex.getMessage()));return;}
            final FaceDetector detector=FaceDetection.getClient(
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                            .setMinFaceSize(.08f).build());
            final ImageLabeler labeler=ImageLabeling.getClient(
                    new ImageLabelerOptions.Builder().setConfidenceThreshold(.55f).build());
            final InputImage image=InputImage.fromBitmap(bitmap,0);
            try{
                detector.process(image).addOnCompleteListener(faceTask->{
                    List<Face> faces=faceTask.isSuccessful()?faceTask.getResult():Collections.emptyList();
                    try{
                        labeler.process(image).addOnCompleteListener(labelTask->{
                            try{
                                if(!faceTask.isSuccessful()&&!labelTask.isSuccessful()){
                                    String fail=labelTask.getException()==null?
                                            "model tidak tersedia":labelTask.getException().getMessage();
                                    ui.post(()->callback.onError("AI inference gagal: "+fail));
                                    return;
                                }
                                List<ImageLabel> labels=labelTask.isSuccessful()?labelTask.getResult():
                                        Collections.emptyList();
                                ImageLabel best=labels.isEmpty()?null:labels.get(0);
                                String category=best==null?"Tidak teridentifikasi":best.getText();
                                float conf=best==null?0f:best.getConfidence();
                                float x=.5f,y=.5f;
                                if(!faces.isEmpty()){
                                    double weightedX=0,weightedY=0,weights=0;
                                    for(Face face:faces){
                                        Rect rect=face.getBoundingBox();
                                        double weight=Math.max(1d,(double)rect.width()*rect.height());
                                        weightedX+=rect.exactCenterX()*weight;
                                        weightedY+=rect.exactCenterY()*weight;
                                        weights+=weight;
                                    }
                                    x=(float)(weightedX/weights/bitmap.getWidth());
                                    y=(float)(weightedY/weights/bitmap.getHeight());
                                }
                                AiPhotoPolicy.Suggestion suggestion=AiPhotoPolicy.suggest(
                                        category,conf,faces.size(),x,y);
                                Result result=new Result(suggestion,category,conf,faces.size(),
                                        !faceTask.isSuccessful()||!labelTask.isSuccessful());
                                ui.post(()->callback.onResult(result));
                            }finally{
                                labeler.close();detector.close();bitmap.recycle();
                            }
                        });
                    }catch(Exception ex){
                        labeler.close();detector.close();bitmap.recycle();
                        ui.post(()->callback.onError("Labeling gagal: "+ex.getMessage()));
                    }
                });
            }catch(Exception ex){
                labeler.close();detector.close();bitmap.recycle();
                ui.post(()->callback.onError("Face detector gagal: "+ex.getMessage()));
            }
        },"WFS-MLKit-photo").start();
    }
}
