package com.cato.glasssky;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.barcodeeye.scan.CaptureActivity;
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
    int PERMISSION_REQUEST_CODE = 1;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                Log.i("MediaSelect", "Requesting permission to read media images and videos");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, PERMISSION_REQUEST_CODE);
            } else {
                getMediaFiles();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.i("MediaSelect", "Requesting permission to read external storage");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                getMediaFiles();
            }
        }
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

        if (dcimDir.exists() && dcimDir.isDirectory()) {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("MediaSelect", "Permission granted to read media images and videos");
                getMediaFiles();
            } else {
                Log.w("MediaSelect", "Permission denied to read media images and videos");
            }
        }
    }
}