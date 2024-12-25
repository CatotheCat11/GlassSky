package com.cato.glasssky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.conscrypt.Conscrypt;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class ImageRequest {
    private static final int CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 8); // Use 1/8th of available memory
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };
    static OkHttpClient okHttpClient = null;
    static ExecutorService executor = Executors.newFixedThreadPool(2);
    public static void makeImageRequest(Context context, String url, OkHttpClient client, ImageCallback callback) {
        executor.execute(() -> {
            Bitmap cachedBitmap = memoryCache.get(url);
            if (cachedBitmap != null) {
                callback.onImageLoaded(cachedBitmap);
                return;
            }
            if (okHttpClient == null) {
                okHttpClient = client;
            }
            Security.insertProviderAt(Conscrypt.newProvider(), 1); // Enable Conscrypt
            RequestOptions requestOptions = new RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false);

            Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .apply(requestOptions)
                    .transform(new AspectRatioTransformation(
                            url.startsWith("https://cdn.bsky.app/img/avatar") ? 64 : 640,
                            url.startsWith("https://cdn.bsky.app/img/avatar") ? 64 : 360
                    ))
                    .into(new CustomTarget<Bitmap>() { // Use CustomTarget to handle the Bitmap directly
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            memoryCache.put(url, resource);
                            callback.onImageLoaded(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Clean up resources if needed
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            callback.onImageLoaded(null);
                        }
                    });
        });
    }

    public interface ImageCallback {
        void onImageLoaded(Bitmap bitmap);
    }
    /*
    private static Bitmap processBitmap(Bitmap originalBitmap, Integer targetWidth, Integer targetHeight) {
        if (originalBitmap == null) return null;

        Bitmap scaledBitmap = null;
        Bitmap finalBitmap = null;

        try {
            finalBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(finalBitmap);
            canvas.drawColor(Color.BLACK);

            float originalAspectRatio = (float) originalBitmap.getWidth() / originalBitmap.getHeight();
            float targetAspectRatio = (float) targetWidth / targetHeight;

            int scaledWidth, scaledHeight;
            float dx = 0, dy = 0;

            if (originalAspectRatio > targetAspectRatio) {
                scaledWidth = targetWidth;
                scaledHeight = Math.round(targetWidth / originalAspectRatio);
                dy = (targetHeight - scaledHeight) / 2f;
            } else {
                scaledHeight = targetHeight;
                scaledWidth = Math.round(targetHeight * originalAspectRatio);
                dx = (targetWidth - scaledWidth) / 2f;
            }

            // Scale the original bitmap
            scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true);

            // Draw the scaled bitmap centered on the black background
            canvas.drawBitmap(scaledBitmap, dx, dy, null);

            return finalBitmap;

        } catch (OutOfMemoryError e) {
            System.gc();
            return null;
        } finally {
            // Clean up intermediate bitmap
            if (scaledBitmap != null && scaledBitmap != originalBitmap && scaledBitmap != finalBitmap) {
                scaledBitmap.recycle();
            }
        }
    }*/

    @GlideModule
    private static class CustomGlideModule extends AppGlideModule {

        @Override
        public void registerComponents(Context context, Glide glide, Registry registry) {
            OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory(okHttpClient);
            glide.getRegistry().replace(GlideUrl.class, InputStream.class, factory);
        }
    }
    public static class AspectRatioTransformation extends BitmapTransformation {
        private static final String ID = "com.GlassSky.AspectRatioTransformation";
        private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

        private final int targetWidth;
        private final int targetHeight;

        public AspectRatioTransformation(int targetWidth, int targetHeight) {
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
                                   int outWidth, int outHeight) {
            try {
                // Create output bitmap with exact dimensions needed
                Bitmap result = pool.get(targetWidth, targetHeight, Bitmap.Config.RGB_565);
                result.eraseColor(Color.BLACK);

                float originalAspectRatio = (float) toTransform.getWidth() / toTransform.getHeight();
                float targetAspectRatio = (float) targetWidth / targetHeight;

                int scaledWidth, scaledHeight;
                float dx = 0, dy = 0;

                if (originalAspectRatio > targetAspectRatio) {
                    scaledWidth = targetWidth;
                    scaledHeight = Math.round(targetWidth / originalAspectRatio);
                    dy = (targetHeight - scaledHeight) / 2f;
                } else {
                    scaledHeight = targetHeight;
                    scaledWidth = Math.round(targetHeight * originalAspectRatio);
                    dx = (targetWidth - scaledWidth) / 2f;
                }

                // Create matrix for scaling
                Matrix matrix = new Matrix();
                float scaleX = (float) scaledWidth / toTransform.getWidth();
                float scaleY = (float) scaledHeight / toTransform.getHeight();
                matrix.setScale(scaleX, scaleY);
                matrix.postTranslate(dx, dy);

                // Draw the transformed bitmap
                Canvas canvas = new Canvas(result);
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(toTransform, matrix, paint);

                return result;

            } catch (OutOfMemoryError e) {
                Log.e("AspectRatioTransform", "Out of memory while transforming bitmap", e);
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AspectRatioTransformation that = (AspectRatioTransformation) o;
            return targetWidth == that.targetWidth && targetHeight == that.targetHeight;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ID, targetWidth, targetHeight);
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            messageDigest.update(ID_BYTES);
            messageDigest.update(ByteBuffer.allocate(8)
                    .putInt(targetWidth)
                    .putInt(targetHeight)
                    .array());
        }
    }
}
