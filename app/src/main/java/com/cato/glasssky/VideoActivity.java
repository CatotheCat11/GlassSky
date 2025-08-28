package com.cato.glasssky;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.Slider;


public class VideoActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;
    private String url = "";
    private GestureDetector mGestureDetector;
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        url = getIntent().getStringExtra("url");
        setContentView(R.layout.video_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        mGestureDetector = createGestureDetector(this);

        playerView = findViewById(R.id.playerView);
        mSlider = Slider.from(playerView);
        mIndeterminate = mSlider.startIndeterminate();

        playerView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Use the custom HttpDataSource.Factory
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                this, new CustomHttpDataSourceFactory(this));

        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        2000,
                        5000,
                        1000,
                        1000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(new com.google.android.exoplayer2.source.DefaultMediaSourceFactory(dataSourceFactory))
                .build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state != ExoPlayer.STATE_BUFFERING) {
                    mIndeterminate.hide();
                    mIndeterminate = null;
                    player.removeListener(this);
                }
            }
        });

        playerView.setPlayer(player);

        // Set media item
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.setWakeMode(C.WAKE_MODE_NETWORK);

        player.prepare();
        player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                playerView.showController();
                if (gesture == Gesture.TAP) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    player.seekForward();
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    player.seekBack();
                    return true;
                } else if (gesture == Gesture.SWIPE_DOWN) {
                    player.stop();
                    finish();
                    return true;
                }
                return false;
            }
        });
        return gestureDetector;
    }
    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    /*
     * Send key events to the gesture detector
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onKeyEvent(keyCode);
        }
        return super.onKeyDown(keyCode, event);
    }
}
