package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.Nullable;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.util.concurrent.ExecutionException;

/**
 * This class allows loading frames for specific index.
 */
public class WebpSeekableFrameLoader {

    private final WebpDecoder webpDecoder;
    private final BitmapPool bitmapPool;
    private final RequestBuilder<Bitmap> requestBuilder;
    private final int width;
    private final int height;

    private Bitmap currentBitmap;

    public WebpSeekableFrameLoader(
            BitmapPool bitmapPool,
            WebpDecoder webpDecoder,
            RequestBuilder<Bitmap> requestBuilder,
            int width,
            int height
    ) {
        this.bitmapPool = bitmapPool;
        this.requestBuilder = requestBuilder;
        this.webpDecoder = webpDecoder;
        this.width = width;
        this.height = height;
    }

    public void loadFrameAtIndex(int index) {
        seekTo(index);
        int frameIndex = webpDecoder.getCurrentFrameIndex();

        WebpFrameCacheStrategy cacheStrategy = webpDecoder.getCacheStrategy();
        RequestOptions options = RequestOptions.signatureOf(getFrameSignature(frameIndex))
                .skipMemoryCache(cacheStrategy.noCache());
        FutureTarget<Bitmap> submit = requestBuilder.apply(options).load(webpDecoder).submit();
        try {
            putBitmapToThePool();
            currentBitmap = submit.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e("WebpSeekableFrameLoader", "Error when fetching bitmap" + e.toString());
        }
    }

    @Nullable
    Bitmap getCurrentFrame() {
        return currentBitmap;
    }

    public int getFrameIndexForTime(long frameStartTimeMs) {
        return webpDecoder.getFrameIndexForTime(frameStartTimeMs);
    }

    public int getDurationMs() {
        return webpDecoder.getDurationMs();
    }

    private void advance() {
        webpDecoder.advance();
    }

    private void seekTo(int frameIndex) {
        while (webpDecoder.getCurrentFrameIndex() < frameIndex) {
            advance();
        }
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    private void putBitmapToThePool() {
        if (currentBitmap != null) {
            bitmapPool.put(currentBitmap);
            currentBitmap = null;
        }
    }

    void clear() {
        putBitmapToThePool();
        webpDecoder.clear();
    }

    private Key getFrameSignature(int frameIndex) {
        // Some devices seem to have crypto bugs that throw exceptions when you create a new UUID.
        // See #1510.
        //return new ObjectKey(Math.random());
        return new WebpFrameLoader.WebpFrameCacheKey(new ObjectKey(webpDecoder), frameIndex);
    }

}
