package com.google.android.glass.widget;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

public class Slider {
   Slider() {
      Log.e("GDK", "Stub: Slider()");
   }

   static final int GRACE_PERIOD_TIME_IN_MS = 2000;
   private GracePeriod mGracePeriod;
   private GracePeriod.Listener mGracePeriodListener;
   private Handler mHandler = new Handler(Looper.getMainLooper());
   private Runnable mGracePeriodEndRunnable;

   public static Slider from(View view) {
      Log.e("GDK", "Stub: from(View)");
      return new Slider();
   }

   public Slider.Scroller startScroller(int maxPosition, float initialPosition) {
      Log.e("GDK", "Stub: startScroller(int, float)");
        return new Slider.Scroller() {

           @Override
           public int getMax() {
              return 0;
           }

           @Override
           public float getPosition() {
              return 0;
           }

           @Override
           public void setPosition(float var1) {

           }

           @Override
           public void show() {

           }

           @Override
           public void hide() {

           }
        };
   }

   public Slider.Determinate startDeterminate(int maxPosition, float initialPosition) {
      Log.e("GDK", "Stub: startDeterminate(int, float)");
        return new Slider.Determinate() {

           @Override
           public int getMax() {
              return 0;
           }

           @Override
           public float getPosition() {
              return 0;
           }

           @Override
           public void setPosition(float var1) {

           }

           @Override
           public void show() {

           }

           @Override
           public void hide() {

           }
        };
   }

   public Slider.GracePeriod startGracePeriod(Slider.GracePeriod.Listener listener) {
      Log.e("GDK", "Stub: startGracePeriod(Slider.GracePeriod.Listener)");
      this.mGracePeriodListener = listener;
      if (mGracePeriodEndRunnable != null) {
         mHandler.removeCallbacks(mGracePeriodEndRunnable);
      }
      mGracePeriodEndRunnable = new Runnable() {
         @Override
         public void run() {
            if (mGracePeriodListener != null) {
               mGracePeriodListener.onGracePeriodEnd();
            }
         }
      };
      mHandler.postDelayed(mGracePeriodEndRunnable, GRACE_PERIOD_TIME_IN_MS);
      this.mGracePeriod = new Slider.GracePeriod() {
         @Override
         public void cancel() {
            mHandler.removeCallbacks(mGracePeriodEndRunnable);
            if (mGracePeriodListener != null) {
               mGracePeriodListener.onGracePeriodCancel();
            }
         }
      };
      return this.mGracePeriod;
   }

   public Slider.Indeterminate startIndeterminate() {
      Log.e("GDK", "Stub: startIndeterminate()");
        return new Slider.Indeterminate() {

           @Override
           public void show() {

           }

           @Override
           public void hide() {

           }
        };
   }

   public interface Indeterminate {
      void show();

      void hide();
   }

   public interface GracePeriod {
      void cancel();

      public interface Listener {
         void onGracePeriodEnd();

         void onGracePeriodCancel();
      }
   }

   public interface Determinate {
      int getMax();

      float getPosition();

      void setPosition(float var1);

      void show();

      void hide();
   }

   public interface Scroller {
      int getMax();

      float getPosition();

      void setPosition(float var1);

      void show();

      void hide();
   }
}
