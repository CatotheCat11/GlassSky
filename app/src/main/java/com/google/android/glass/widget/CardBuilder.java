package com.google.android.glass.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.cato.glasssky.R;

import java.util.ArrayList;

public class CardBuilder {

   private final Context context;
   private final Layout layout;
   private CharSequence heading;
   private CharSequence subheading;
   private CharSequence text;
   private CharSequence footnote;
   private CharSequence timestamp;
   private Drawable icon;
   private Drawable attributionIcon;
   private ArrayList<Drawable> images = new ArrayList<>();

   public CardBuilder(Context context, CardBuilder.Layout layout) {
      this.context = context;
      this.layout = layout;
   }

   public CardBuilder setText(CharSequence text) {
      this.text = text;
      return this;
   }

   public CardBuilder setText(int textId) {
      this.text = context.getString(textId);
      return this;
   }

   public CardBuilder setFootnote(CharSequence footnote) {
      this.footnote = footnote;
      return this;
   }

   public CardBuilder setFootnote(int footnoteId) {
      this.footnote = context.getString(footnoteId);
      return this;
   }

   public CardBuilder setTimestamp(CharSequence timestamp) {
      this.timestamp = timestamp;
      return this;
   }

   public CardBuilder setTimestamp(int timestampId) {
      this.timestamp = context.getString(timestampId);
      return this;
   }

   public CardBuilder setHeading(CharSequence heading) {
      this.heading = heading;
      return this;
   }

   public CardBuilder setHeading(int headingId) {
      this.heading = context.getString(headingId);
      return this;
   }

   public CardBuilder setSubheading(CharSequence subheading) {
      this.subheading = subheading;
      return this;
   }

   public CardBuilder setSubheading(int subheadingId) {
      this.subheading = context.getString(subheadingId);
      return this;
   }

   public CardBuilder addImage(Drawable imageDrawable) {
      this.images.add(imageDrawable);
      return this;
   }

   public CardBuilder addImage(Bitmap imageBitmap) {
      this.images.add(new BitmapDrawable(context.getResources(), imageBitmap));
      return this;
   }

   public CardBuilder addImage(int imageId) {
      this.images.add(ContextCompat.getDrawable(context, imageId));
      return this;
   }

   public void clearImages() {
      Log.e("GDK", "Stub: clearImages()");
   }

   public CardBuilder setIcon(Drawable iconDrawable) {
      this.icon = iconDrawable;
      return this;
   }

   public CardBuilder setIcon(Bitmap iconBitmap) {
      this.icon = new BitmapDrawable(context.getResources(), iconBitmap);
      return this;
   }

   public CardBuilder setIcon(int iconId) {
      this.icon = ContextCompat.getDrawable(context, iconId);
      return this;
   }

   public CardBuilder setAttributionIcon(Drawable iconDrawable) {
      this.attributionIcon = iconDrawable;
      return this;
   }

   public CardBuilder setAttributionIcon(Bitmap iconBitmap) {
      this.attributionIcon = new BitmapDrawable(context.getResources(), iconBitmap);
      return this;
   }

   public CardBuilder setAttributionIcon(int iconId) {
      this.attributionIcon = ContextCompat.getDrawable(context, iconId);
      return this;
   }

   public CardBuilder showStackIndicator(boolean visible) {
      Log.e("GDK", "Stub: showStackIndicator(boolean)");
      return this;
   }

   public CardBuilder setEmbeddedLayout(int layoutResId) {
      Log.e("GDK", "Stub: setEmbeddedLayout(int)");
      return this;
   }

   public View getView() {
      return getView(null, null);
   }

   public View getView(View convertView, ViewGroup parent) {
      LayoutInflater inflater = LayoutInflater.from(context);
      View layout = null;
      if (this.layout == Layout.MENU) {
         layout = inflater.inflate(R.layout.card_builder_menu, parent, false);
      } else if (this.layout == Layout.AUTHOR || this.layout == Layout.TEXT || this.layout == Layout.TEXT_FIXED) {
         layout = inflater.inflate(R.layout.card_builder_author, parent, false);
      } else if (this.layout == Layout.CAPTION) {
         layout = inflater.inflate(R.layout.card_builder_caption, parent, false);
      } else if (this.layout == Layout.TITLE) {
         layout = inflater.inflate(R.layout.card_builder_title, parent, false);
      } else {
         Log.e("GDK", "Unsupported layout type: " + this.layout);
         layout = inflater.inflate(R.layout.card_builder_menu, parent, false); //Default to menu layout
      }

      ImageView imageView = layout.findViewById(R.id.card_image);
      ImageView iconView = layout.findViewById(R.id.card_icon);
      ImageView attributionIconView = layout.findViewById(R.id.card_attribution_icon);
      TextView textView = layout.findViewById(R.id.card_text);
      TextView headingView = layout.findViewById(R.id.card_heading);
      TextView subheadingView = layout.findViewById(R.id.card_subheading);
      TextView footnoteView = layout.findViewById(R.id.card_footnote);
      TextView timestampView = layout.findViewById(R.id.card_timestamp);

      if (imageView != null) {
         if (!images.isEmpty()) { // Mosaic not supported yet, use first image for now
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(images.get(0));
         } else {
            imageView.setVisibility(View.GONE);
         }
      }

      if (iconView != null) {
         if (icon != null) {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageDrawable(icon);
         } else {
            iconView.setVisibility(View.GONE);
         }
      }

      if (attributionIconView != null) {
         if (attributionIcon != null) {
            attributionIconView.setVisibility(View.VISIBLE);
            attributionIconView.setImageDrawable(attributionIcon);
         } else {
            attributionIconView.setVisibility(View.GONE);
         }
      }

      if (headingView != null) {
         if (heading != null) {
            headingView.setVisibility(View.VISIBLE);
            headingView.setText(heading);
         } else {
            headingView.setVisibility(View.GONE);
         }
      }

      if (subheadingView != null) {
         if (subheading != null) {
            subheadingView.setVisibility(View.VISIBLE);
            subheadingView.setText(subheading);
         } else {
            subheadingView.setVisibility(View.GONE);
         }
      }

      if (timestampView != null) {
         if (timestamp != null) {
            timestampView.setVisibility(View.VISIBLE);
            timestampView.setText(timestamp);
         } else {
            timestampView.setVisibility(View.GONE);
         }
      }

      if (text != null) {
         textView.setVisibility(View.VISIBLE);
         textView.setText(text);
      } else {
         textView.setVisibility(View.GONE);
      }

      if (footnote != null) {
         footnoteView.setVisibility(View.VISIBLE);
         footnoteView.setText(footnote);
      } else {
         footnoteView.setVisibility(View.GONE);
      }

      return layout;
   }

   public RemoteViews getRemoteViews() {
      throw new RuntimeException("Unimplemented method: getRemoteViews()");
   }

   public int getItemViewType() {
      return 0;
   }

   public static int getViewTypeCount() {
      return 1;
   }

   public static enum Layout {
      ALERT,
      AUTHOR,
      CAPTION,
      COLUMNS,
      COLUMNS_FIXED,
      EMBED_INSIDE,
      MENU,
      TEXT,
      TEXT_FIXED,
      TITLE;

      private Layout() {
         Log.e("GDK", "Stub: Layout()");
      }
   }
}

