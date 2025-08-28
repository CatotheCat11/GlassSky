package com.google.android.glass.touchpad;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GestureDetector {
   private GestureDetector.BaseListener mBaseListener;

   public GestureDetector(Context context) {
      Log.e("GDK", "Not implemented: GestureDetector(Context)");
   }

   public GestureDetector setBaseListener(GestureDetector.BaseListener listener) {
      this.mBaseListener = listener;
      return this;
   }

   public GestureDetector setFingerListener(GestureDetector.FingerListener listener) {
      Log.e("GDK", "Not implemented: setFingerListener(Context)");
      return this;
   }

   public GestureDetector setOneFingerScrollListener(GestureDetector.OneFingerScrollListener listener) {
      Log.e("GDK", "Not implemented: setOneFingerScrollListener(OneFingerScrollListener)");
      return this;
   }

   public GestureDetector setScrollListener(GestureDetector.ScrollListener listener) {
     Log.e("GDK", "Not implemented: setScrollListener(ScrollListener)");
     return this;
   }

   public GestureDetector setTwoFingerScrollListener(GestureDetector.TwoFingerScrollListener listener) {
      Log.e("GDK", "Not implemented: setTwoFingerScrollListener(TwoFingerScrollListener)");
      return this;
   }

   public GestureDetector setAlwaysConsumeEvents(boolean enabled) {
      Log.e("GDK", "Not implemented: setAlwaysConsumeEvents(boolean)");
      return this;
   }

   public boolean onMotionEvent(MotionEvent event) {
      return true;
   }

   public boolean onKeyEvent(int keyCode) {
      if (mBaseListener == null) return false;
      Gesture gesture = null;
      switch (keyCode) {
         case KeyEvent.KEYCODE_DPAD_CENTER:
         case KeyEvent.KEYCODE_ENTER:
            gesture = Gesture.TAP;
            break;
         case KeyEvent.KEYCODE_DPAD_UP:
            gesture = Gesture.SWIPE_UP;
            break;
         case KeyEvent.KEYCODE_DPAD_DOWN:
         case KeyEvent.KEYCODE_ESCAPE:
         case KeyEvent.KEYCODE_BACK:
            gesture = Gesture.SWIPE_DOWN;
            break;
         case KeyEvent.KEYCODE_DPAD_LEFT:
         case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
            gesture = Gesture.SWIPE_LEFT;
            break;
         case KeyEvent.KEYCODE_DPAD_RIGHT:
         case KeyEvent.KEYCODE_NAVIGATE_NEXT:
            gesture = Gesture.SWIPE_RIGHT;
            break;
         default:
            Log.e("GDK", "Unknown key code: " + keyCode);
      }
      if (gesture != null) {
         return mBaseListener.onGesture(gesture);
      }
      return false;
   }

   public static boolean isForward(Gesture gesture) {
      Log.e("GDK", "Not implemented: isForward(Gesture)");
      return false;
   }

   public static boolean isForward(float deltaX) {
      Log.e("GDK", "Not implemented: isForward(float)");
      return false;
   }

   public interface TwoFingerScrollListener {
      boolean onTwoFingerScroll(float var1, float var2, float var3);
   }

   public interface ScrollListener {
      boolean onScroll(float var1, float var2, float var3);
   }

   public interface OneFingerScrollListener {
      boolean onOneFingerScroll(float var1, float var2, float var3);
   }

   public interface FingerListener {
      void onFingerCountChanged(int var1, int var2);
   }

   public interface BaseListener {
      boolean onGesture(Gesture gesture);
   }
}
