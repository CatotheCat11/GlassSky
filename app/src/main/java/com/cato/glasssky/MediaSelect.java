package com.cato.glasssky;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MediaSelect extends Activity {
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    Intent intent;
    List<File> fileList;
    int video = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = getIntent();

        createCards();

        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setupClickListener();
        getMediaFiles();
        setContentView(mCardScrollView);
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("New photo")
                .setFootnote("Takes a photo with the camera")
                .setIcon(R.drawable.add_a_photo_64));
        if (intent.getBooleanExtra("video", true)) {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("New video")
                    .setFootnote("Takes a video with the camera")
                    .setIcon(R.drawable.videocam_64));
            video = 1;
        }
    }
    private void getMediaFiles() {
        ArrayList<String> filePaths = new ArrayList<>();
        File dcimDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera");
        Log.i("MediaSelector", "DCIM directory: " + dcimDir);

        if (dcimDir != null && dcimDir.exists() && dcimDir.isDirectory()) {
            // Get all files in the DCIM directory
            File[] files = dcimDir.listFiles();
            Log.i("MediaSelector", "Number of files in DCIM directory: " + files.length);

            if (files != null) {
                // Sort files by last modified date in descending order
                fileList = new ArrayList<>(Arrays.asList(files));
                Collections.sort(fileList, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return Long.compare(f2.lastModified(), f1.lastModified());
                    }
                });
                // Take the last 10 files
                for (int i = 0; i < Math.min(10, fileList.size()); i++) {
                    mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                            .setText("Loading..."));
                    File file = fileList.get(i);
                    if (file.isFile()) {
                        filePaths.add(file.getAbsolutePath());
                        int finalI = i;
                        Glide.with(this)
                                .asBitmap()
                                .load(file)
                                .format(DecodeFormat.PREFER_RGB_565)
                                .override(640, 360)
                                .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                mCards.set(1 + video + finalI, new CardBuilder(MediaSelect.this, CardBuilder.Layout.CAPTION)
                                        .setText(file.getName())
                                        .addImage(resource));
                                mAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
                    }
                }
            }
        } else {
            Log.e("MediaSelector", "DCIM directory not found or is not accessible.");
        }
    }

    private class ExampleCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position){
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
    private void setupClickListener() {
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);
                if (position == 0) {
                    setResult(RESULT_OK, new Intent().putExtra("photo", true));
                    finish();
                } else if (position == 1 && intent.getBooleanExtra("video", true)) {
                    setResult(RESULT_OK, new Intent().putExtra("video", true));
                    finish();
                } else {
                    setResult(RESULT_OK, new Intent().putExtra("file", fileList.get(position - (1 + video)).getAbsolutePath()));
                    finish();
                }
            }
        });
    }
}