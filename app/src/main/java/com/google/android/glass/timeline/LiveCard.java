package com.google.android.glass.timeline;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.RemoteViews;

public class LiveCard {
   public static final String EXTRA_FROM_LIVECARD_VOICE = "android.intent.extra.EXTRA_FROM_LIVECARD";

   public LiveCard(Context context, String tag) {
      Log.e("GDK", "Stub: LiveCard(Context, String)");
   }

   public void publish(LiveCard.PublishMode mode) {
      Log.e("GDK", "Stub: publish(LiveCard.PublishMode)");
   }

   public void unpublish() {
      Log.e("GDK", "Stub: unpublish()");
   }

   public boolean isPublished() {
      Log.e("GDK", "Stub: isPublished()");
      return false;
   }

   public void navigate() {
      Log.e("GDK", "Stub: navigate()");
   }

   public LiveCard setViews(RemoteViews views) {
      Log.e("GDK", "Stub: setViews(RemoteViews)");
      return this;
   }

   public LiveCard setAction(PendingIntent intent) {
      Log.e("GDK", "Stub: setAction(PendingIntent)");
      return this;
   }

   public LiveCard setVoiceActionEnabled(boolean enable) {
      Log.e("GDK", "Stub: setVoiceActionEnabled(boolean)");
      return this;
   }

   public LiveCard setDirectRenderingEnabled(boolean enable) {
      Log.e("GDK", "Stub: setDirectRenderingEnabled(boolean)");
      return this;
   }

   public LiveCard setRenderer(GlRenderer renderer) {
      Log.e("GDK", "Stub: setRenderer(GlRenderer)");
      return this;
   }

   public LiveCard attach(Service service) {
      Log.e("GDK", "Stub: attach(Service)");
      return this;
   }

   public SurfaceHolder getSurfaceHolder() {
      Log.e("GDK", "Stub: getSurfaceHolder()");
      return new SurfaceHolder() {
         @Override
         public void addCallback(Callback callback) {

         }

         @Override
         public void removeCallback(Callback callback) {

         }

         @Override
         public boolean isCreating() {
            return false;
         }

         @Override
         public void setType(int type) {

         }

         @Override
         public void setFixedSize(int width, int height) {

         }

         @Override
         public void setSizeFromLayout() {

         }

         @Override
         public void setFormat(int format) {

         }

         @Override
         public void setKeepScreenOn(boolean screenOn) {

         }

         @Override
         public Canvas lockCanvas() {
            return null;
         }

         @Override
         public Canvas lockCanvas(Rect dirty) {
            return null;
         }

         @Override
         public void unlockCanvasAndPost(Canvas canvas) {

         }

         @Override
         public Rect getSurfaceFrame() {
            return null;
         }

         @Override
         public Surface getSurface() {
            return null;
         }
      };
   }

   public static enum PublishMode {
      REVEAL,
      SILENT;

      private PublishMode() {
         Log.e("GDK", "Stub: PublishMode()");
      }
   }
}
