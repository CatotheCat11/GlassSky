package com.cato.glasssky;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
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
        setContentView(R.layout.video_card);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mGestureDetector = createGestureDetector(this);

        playerView = findViewById(R.id.playerView);
        mSlider = Slider.from(playerView);
        mIndeterminate = mSlider.startIndeterminate();

        // Use the custom HttpDataSource.Factory
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                this, new CustomHttpDataSourceFactory(this));

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this).setMediaSourceFactory(
                new com.google.android.exoplayer2.source.DefaultMediaSourceFactory(dataSourceFactory)
        ).build();
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
}
