package com.wfs.storyboost;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Touch-and-drag image comparison; images share the same output frame geometry. */
public final class BeforeAfterView extends View {
    private Bitmap before;
    private Bitmap after;
    private float split = 0.5f;
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    public BeforeAfterView(Context context) { super(context); init(); }
    public BeforeAfterView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() { setBackgroundColor(0xFF0A1428); }

    /** View owns both bitmaps and releases old previews when replaced. */
    public void setImages(Bitmap original, Bitmap edited) {
        clearImages();
        before = original;
        after = edited;
        split = 0.5f;
        setVisibility(before != null && after != null ? VISIBLE : GONE);
        invalidate();
    }

    public void clearImages() {
        if (before != null && !before.isRecycled()) before.recycle();
        if (after != null && !after.isRecycled() && after != before) after.recycle();
        before = null;
        after = null;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (before == null || after == null) return;
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        float contentAspect = (float) after.getWidth() / after.getHeight();
        float availAspect = width / height;
        if (contentAspect >= availAspect) {
            float displayH = width / contentAspect;
            bounds.set(0, (height - displayH) / 2, width, (height + displayH) / 2);
        } else {
            float displayW = height * contentAspect;
            bounds.set((width - displayW) / 2, 0, (width + displayW) / 2, height);
        }
        canvas.drawBitmap(before, null, bounds, imagePaint);
        float x = bounds.left + bounds.width() * split;
        canvas.save();
        canvas.clipRect(x, bounds.top, bounds.right, bounds.bottom);
        canvas.drawBitmap(after, null, bounds, imagePaint);
        canvas.restore();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(dp(2));
        canvas.drawLine(x, bounds.top, x, bounds.bottom, linePaint);
        canvas.drawCircle(x, bounds.centerY(), dp(15), linePaint);
        linePaint.setColor(0xFF15233A);
        canvas.drawCircle(x, bounds.centerY(), dp(12), linePaint);
        linePaint.setColor(Color.WHITE);
        linePaint.setTextSize(dp(11));
        linePaint.setFakeBoldText(true);
        canvas.drawText("‹  ›", x - dp(11), bounds.centerY() + dp(4), linePaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (before == null || after == null) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (bounds.width() <= 0) return false;
            split = Math.max(0f, Math.min(1f, (event.getX() - bounds.left) / bounds.width()));
            invalidate();
            return true;
        }
        return action == MotionEvent.ACTION_UP || super.onTouchEvent(event);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
