package com.cato.glasssky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    // Resize and crop bitmap to fit Google Glass resolution
    private static Bitmap processBitmap(Bitmap originalBitmap, Integer newWidth, Integer newHeight) {
        if (originalBitmap == null) return null;

        Bitmap scaledBitmap = null;
        Bitmap croppedBitmap = null;
        Bitmap finalBitmap = null;

        try {
            float scaleFactor = Math.min((float) newWidth / originalBitmap.getWidth(),
                    (float) newHeight / originalBitmap.getHeight());

            int scaledWidth = Math.round(originalBitmap.getWidth() * scaleFactor);
            int scaledHeight = Math.round(originalBitmap.getHeight() * scaleFactor);

            // Create scaled bitmap
            scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true);

            int cropX = Math.max(0, (scaledWidth - newWidth) / 2);
            int cropY = Math.max(0, (scaledHeight - newHeight) / 2);

            // Create cropped bitmap
            croppedBitmap = Bitmap.createBitmap(
                    scaledBitmap,
                    cropX,
                    cropY,
                    Math.min(newWidth, scaledWidth),
                    Math.min(newHeight, scaledHeight)
            );

            // Compress to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] compressedData = outputStream.toByteArray();

            // Create final bitmap with RGB_565 config
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            finalBitmap = BitmapFactory.decodeByteArray(compressedData, 0, compressedData.length, options);

            return finalBitmap;

        } catch (OutOfMemoryError e) {
            System.gc();
            return null;
        } finally {
            // Clean up intermediate bitmaps
            if (scaledBitmap != null && scaledBitmap != originalBitmap && scaledBitmap != finalBitmap) {
                scaledBitmap.recycle();
            }
            if (croppedBitmap != null && croppedBitmap != scaledBitmap && croppedBitmap != finalBitmap) {
                croppedBitmap.recycle();
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
