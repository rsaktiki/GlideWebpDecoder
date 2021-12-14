package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A Drawable that allows to render specified frames. To specify frames you have to call
 * either seekTo(timeMs) or seekTo(frameNumber). In order to transform a frame you can pass
 * a Transformer using setTransform() method.
 *
 * @author liuchun
 */
public class WebpSeekableDrawable extends Drawable {

    private static final int GRAVITY = Gravity.FILL;

    /**
     * True if the drawable's resources have been recycled.
     */
    public boolean isRecycled;

    private boolean applyGravity;
    private Paint paint;
    private Rect destRect;
    private Transform transform;
    private final WebpSeekableFrameLoader frameLoader;

    public WebpSeekableDrawable(@NonNull WebpSeekableFrameLoader frameLoader) {
        this.frameLoader = frameLoader;
    }

    public int getIntrinsicWidth() {
        return frameLoader.getWidth();
    }

    public int getIntrinsicHeight() {
        return frameLoader.getHeight();
    }

    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        applyGravity = true;
    }

    public void draw(Canvas canvas) {
        if (isRecycled()) {
            return;
        }

        if (applyGravity) {
            Gravity.apply(GRAVITY, getIntrinsicWidth(), getIntrinsicHeight(), getBounds(), getDestRect());
            applyGravity = false;
        }

        Bitmap currentFrame = frameLoader.getCurrentFrame();
        if (transform == null) {
            canvas.drawBitmap(currentFrame, null, getDestRect(), getPaint());
        } else {
            transform.onDraw(canvas, getPaint(), currentFrame);
        }
    }

    public void setAlpha(int i) {
        getPaint().setAlpha(i);
    }

    public void setColorFilter(ColorFilter colorFilter) {
        getPaint().setColorFilter(colorFilter);
    }

    @Deprecated
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    public void recycle() {
        isRecycled = true;
        frameLoader.clear();
    }

    boolean isRecycled() {
        return isRecycled;
    }

    public int getDurationMs() {
        return frameLoader.getDurationMs();
    }

    public int getFrameIndexForTime(long frameStartTimeMs) {
        final long loopedTimeMs = frameStartTimeMs % getDurationMs();
        return frameLoader.getFrameIndexForTime(loopedTimeMs);
    }

    public void setTransform(@Nullable Transform transform) {
        this.transform = transform;
        if (transform != null) {
            transform.onBoundsChange(destRect);
        }
    }

    private int recentFrameIndex = -1;

    public void seekTo(long timeMs) {
        final int newFrameIndex = getFrameIndexForTime(timeMs);
        if (recentFrameIndex < 0 || newFrameIndex < 0 || recentFrameIndex != newFrameIndex) {
            seekTo(newFrameIndex);
        }
    }

    public void seekTo(int newFrameIndex) {
        recentFrameIndex = newFrameIndex;
        frameLoader.loadFrameAtIndex(newFrameIndex);
    }

    public Paint getPaint() {
        if (paint == null) {
            paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        }

        return paint;
    }

    private Rect getDestRect() {
        if (destRect == null) {
            destRect = new Rect();
        }

        return destRect;
    }

    /**
     * Interface to support clients performing custom transformations before the current WebP Bitmap is drawn.
     */
    public interface Transform {

        void onBoundsChange(Rect bounds);

        void onDraw(Canvas canvas, Paint paint, Bitmap buffer);
    }
}
