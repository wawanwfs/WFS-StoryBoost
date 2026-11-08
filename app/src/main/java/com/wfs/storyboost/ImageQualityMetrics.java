package com.wfs.storyboost;

/** Deterministic global luminance PSNR/SSIM diagnostics. Not an LPIPS or IQA predictor. */
public final class ImageQualityMetrics {
    private ImageQualityMetrics(){}
    private static void valid(int[] reference,int[] result){
        if(reference==null||result==null||reference.length==0||
                reference.length!=result.length)
            throw new IllegalArgumentException("Samples must have equal nonzero lengths");
    }
    public static double psnr(int[] reference,int[] result){
        valid(reference,result);
        double mse=0;
        for(int i=0;i<reference.length;i++){
            double diff=(reference[i]&255)-(result[i]&255);
            mse+=diff*diff;
        }
        mse/=reference.length;
        return mse==0?Double.POSITIVE_INFINITY:10*Math.log10(255*255/mse);
    }
    public static double ssim(int[] reference,int[] result){
        valid(reference,result);
        double meanA=0,meanB=0;
        for(int i=0;i<reference.length;i++){
            meanA+=reference[i]&255;meanB+=result[i]&255;
        }
        meanA/=reference.length;meanB/=reference.length;
        double varA=0,varB=0,cov=0;
        for(int i=0;i<reference.length;i++){
            double a=(reference[i]&255)-meanA,b=(result[i]&255)-meanB;
            varA+=a*a;varB+=b*b;cov+=a*b;
        }
        varA/=reference.length;varB/=reference.length;cov/=reference.length;
        double c1=Math.pow(.01*255,2),c2=Math.pow(.03*255,2);
        return ((2*meanA*meanB+c1)*(2*cov+c2))/
                ((meanA*meanA+meanB*meanB+c1)*(varA+varB+c2));
    }
}
