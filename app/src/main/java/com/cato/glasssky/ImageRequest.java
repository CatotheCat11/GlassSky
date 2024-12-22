package com.cato.glasssky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.conscrypt.Conscrypt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Security;
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
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    public static void makeImageRequest(Context context, String url, OkHttpClient client, ImageCallback callback) {
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
                .apply(requestOptions)
                .into(new CustomTarget<Bitmap>() { // Use CustomTarget to handle the Bitmap directly
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap processedBitmap;
                        if (url.startsWith("https://cdn.bsky.app/img/avatar")) {
                            processedBitmap = processBitmap(resource, 64, 64);
                        } else {
                            processedBitmap = processBitmap(resource, 640, 360);
                        }

                        if (processedBitmap != null) {
                            memoryCache.put(url, processedBitmap);
                            callback.onImageLoaded(processedBitmap);
                        } else {
                            callback.onImageLoaded(null);
                        }
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
    }

    public interface ImageCallback {
        void onImageLoaded(Bitmap bitmap);
    }
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
    }

    @GlideModule
    private class CustomGlideModule extends AppGlideModule {

        @Override
        public void registerComponents(Context context, Glide glide, Registry registry) {
            OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory(okHttpClient);
            glide.getRegistry().replace(GlideUrl.class, InputStream.class, factory);
        }
    }
}
