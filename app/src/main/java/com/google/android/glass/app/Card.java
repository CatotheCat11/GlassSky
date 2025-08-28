package com.google.android.glass.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.glass.widget.CardBuilder;

/** @deprecated */
@Deprecated
public class Card extends CardBuilder {
   /** @deprecated */
   @Deprecated
   public Card(Context context) {
      super((Context)null, (CardBuilder.Layout)null);
      Log.e("GDK", "Stub: Card(Context)");
   }

   public Card setText(CharSequence text) {
      Log.e("GDK", "Stub: setText(CharSequence)");
      return this;
   }

   public Card setText(int textId) {
      Log.e("GDK", "Stub: setText(int)");
      return this;
   }

   /** @deprecated */
   @Deprecated
   public CharSequence getText() {
      Log.e("GDK", "Stub: getText()");
      return "Not implemented";
   }

   public Card setFootnote(CharSequence footnote) {
      Log.e("GDK", "Stub: setFootnote(CharSequence)");
      return this;
   }

   public Card setFootnote(int footnoteId) {
      Log.e("GDK", "Stub: setFootnote(int)");
      return this;
   }

   /** @deprecated */
   @Deprecated
   public CharSequence getFootnote() {
      Log.e("GDK", "Stub: getFootnote()");
      return "Not implemented";
   }

   public Card setTimestamp(CharSequence timestamp) {
      Log.e("GDK", "Stub: setTimestamp(CharSequence)");
      return this;
   }

   public Card setTimestamp(int timestampId) {
      Log.e("GDK", "Stub: setTimestamp(int)");
      return this;
   }

   /** @deprecated */
   @Deprecated
   public CharSequence getTimestamp() {
      Log.e("GDK", "Stub: getTimestamp()");
      return "Not implemented";
   }

   public Card addImage(Drawable imageDrawable) {
      Log.e("GDK", "Stub: addImage(Drawable)");
      return this;
   }

   public Card addImage(Bitmap imageBitmap) {
      Log.e("GDK", "Stub: addImage(Bitmap)");
      return this;
   }

   public Card addImage(int imageId) {
      Log.e("GDK", "Stub: addImage(int)");
      return this;
   }

   /** @deprecated */
   @Deprecated
   public Drawable getImage(int n) {
      Log.e("GDK", "Stub: getImage(int)");
      return new Drawable() {
         @Override
         public void draw(@NonNull Canvas canvas) {

         }

         @Override
         public void setAlpha(int alpha) {

         }

         @Override
         public void setColorFilter(@Nullable ColorFilter colorFilter) {

         }

         @Override
         public int getOpacity() {
            return PixelFormat.UNKNOWN;
         }
      };
   }

   /** @deprecated */
   @Deprecated
   public int getImageCount() {
      Log.e("GDK", "Stub: getImageCount()");
      return 0;
   }

   /** @deprecated */
   @Deprecated
   public Card setImageLayout(Card.ImageLayout imageLayout) {
      Log.e("GDK", "Stub: setImageLayout(Card.ImageLayout)");
      return this;
   }

   /** @deprecated */
   @Deprecated
   public Card.ImageLayout getImageLayout() {
      Log.e("GDK", "Stub: getImageLayout()");
      return ImageLayout.FULL;
   }

   /** @deprecated */
   @Deprecated
   public static enum ImageLayout {
      FULL,
      LEFT;

      private ImageLayout() {
         Log.e("GDK", "Stub: ImageLayout()");
      }
   }
}
