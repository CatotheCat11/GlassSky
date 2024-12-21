package com.cato.glasssky;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

//TODO: Add feed loading indicator (could be card or slider)
//TODO: Don't allow click when loading

public class FeedMenu extends Activity {
    int LOGIN_REQUEST = 3;
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    JSONArray pinned = null;
    String access_token;
    private Slider.GracePeriod mGracePeriod;
    private Slider.Indeterminate mIndeterminate;
    private Slider mSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = FeedMenu.this.getSharedPreferences(
                getString(R.string.auth), Context.MODE_PRIVATE);
        access_token = sharedPref.getString(getString(R.string.access_token), "unset");

        mCards = new ArrayList<CardBuilder>();
        createCards();

        SetFeeds();

        mCardScrollView = new CardScrollView(FeedMenu.this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        mSlider = Slider.from(mCardScrollView);
        setupClickListener();

        setContentView(mCardScrollView);
    }

    private void SetFeeds() {
        HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.actor.getPreferences", null, access_token, "GET",
                new HttpsUtils.HttpCallback() {
                    @Override
                    public void onSuccess(String response) {
                        // Handle successful response
                        try {
                            JSONArray prefsArray = new JSONObject(response).getJSONArray("preferences");
                            for (int i = 0; i < prefsArray.length(); i++) {
                                JSONObject savedFeeds = prefsArray.getJSONObject(i);
                                if (savedFeeds.getString("$type").equals("app.bsky.actor.defs#savedFeedsPref")) {
                                    pinned = savedFeeds.getJSONArray("pinned");
                                    break;
                                }
                            }
                            StringBuilder getFeeds = new StringBuilder();
                            for (int i = 0; i < pinned.length(); i++) {
                                String feed = pinned.getString(i);
                                if (i == 0) {
                                    getFeeds = new StringBuilder("https://bsky.social/xrpc/app.bsky.feed.getFeedGenerators?feeds=" + feed);
                                } else {
                                    getFeeds.append("&feeds=").append(feed);
                                }
                            }
                            String getFeedsURL = getFeeds.toString();
                            HttpsUtils.makePostRequest(getFeedsURL, null, access_token, "GET",
                                    new HttpsUtils.HttpCallback() {
                                        @Override
                                        public void onSuccess(String response) {
                                            // Handle successful response
                                            try {
                                                if (mCards.size() != 1) { //In case user is in the middle of logging out
                                                    mCards.remove(mCards.size() - 1); //Run twice to remove log out and loading cards
                                                    mCards.remove(mCards.size() - 1);
                                                    JSONArray feedArray = new JSONObject(response).getJSONArray("feeds");
                                                    for (int i = 0; i < feedArray.length(); i++) {
                                                        JSONObject feed = feedArray.getJSONObject(i);
                                                        String displayName = feed.getString("displayName");
                                                        String description = feed.getString("description");
                                                        mCards.add(new CardBuilder(FeedMenu.this, CardBuilder.Layout.MENU)
                                                                .setText(displayName)
                                                                .setFootnote(description));
                                                    }
                                                    mCards.add(new CardBuilder(FeedMenu.this, CardBuilder.Layout.MENU)
                                                            .setText("Log out")
                                                            .setIcon(R.drawable.logout_64));
                                                    if (mCardScrollView.getSelectedItemPosition() == 3) {
                                                        mCardScrollView.setSelection(mCards.size() - 1);
                                                    }
                                                    mAdapter.notifyDataSetChanged();
                                                }
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            Log.e("An error occurred", errorMessage);
                                        }
                                    });
                            for (int i = 0; i < pinned.length(); i++) {
                                String feed = pinned.getString(i);
                                int finalI = i;
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Intent loginIntent = new Intent(FeedMenu.this, Authenticate.class);
                        startActivityForResult(loginIntent, LOGIN_REQUEST);
                    }
                });
    }

    private void createCards() {
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("New post")
                .setIcon(R.drawable.post_add_64));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Profile")
                .setIcon(R.drawable.person_64));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Loading feeds"));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Log out")
                .setIcon(R.drawable.logout_64));
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
        // Inserts a card into the adapter, without notifying.
        public void insertCardWithoutNotification(int position, CardBuilder card) {
            mCards.add(position, card);
        }
    }

    private void insertNewCard(int position, CardBuilder card) {
        // Insert new card in the adapter, but don't call
        // notifyDataSetChanged() yet. Instead, request proper animation
        // to inserted card from card scroller, which will notify the
        // adapter at the right time during the animation.
        mAdapter.insertCardWithoutNotification(position, card);
    }
    private void setupClickListener() {
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);
                if (position == mCards.size() - 1) {
                    mCards.clear();
                    mCards.add(new CardBuilder(FeedMenu.this, CardBuilder.Layout.MENU)
                            .setText("Logging out")
                            .setFootnote("Swipe down to cancel")
                            .setIcon(R.drawable.logout_64));
                    mAdapter.notifyDataSetChanged();
                    mGracePeriod = mSlider.startGracePeriod(mGracePeriodListener);
                } else if (position == 0) {
                    Intent postIntent = new Intent(FeedMenu.this, PostActivity.class);
                    startActivity(postIntent);
                } else if (position == 1) {
                    HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.getSession", null, access_token, "GET",
                            new HttpsUtils.HttpCallback() {
                                @Override
                                public void onSuccess(String didresponse) {
                                    try {
                                        String did = new JSONObject(didresponse).getString("did");
                                        Intent timelineIntent = new Intent(FeedMenu.this, Timeline.class);
                                        timelineIntent.putExtra("uri", did);
                                        timelineIntent.putExtra("mode", "author");
                                        startActivity(timelineIntent);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Log.e("Error", errorMessage);
                                }
                    });
                } else {
                    Intent timelineIntent = new Intent(FeedMenu.this, Timeline.class);
                    timelineIntent.putExtra("mode", "algorithm");
                    try {
                        timelineIntent.putExtra("uri", pinned.getString(position - 2));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    startActivity(timelineIntent);
                }
            }
        });
    }
    private final Slider.GracePeriod.Listener mGracePeriodListener =
            new Slider.GracePeriod.Listener() {

                @Override
                public void onGracePeriodEnd() {
                    // Log out.
                    SharedPreferences sharedPref = FeedMenu.this.getSharedPreferences(
                            getString(R.string.auth), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.clear();
                    editor.apply();
                    // Play a SUCCESS sound to indicate the end of the grace period.
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.playSoundEffect(Sounds.SUCCESS);
                    mGracePeriod = null;
                    finish();
                }

                @Override
                public void onGracePeriodCancel() {
                    // Play a DISMISS sound to indicate the cancellation of the grace period.
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.playSoundEffect(Sounds.DISMISSED);
                    mGracePeriod = null;
                    recreate();
                }
            };
    @Override
    public void onBackPressed() {
        // If the Grace Period is running,
        // cancel it instead of finishing the Activity.
        if (mGracePeriod != null) {
            mGracePeriod.cancel();
        } else {
            super.onBackPressed();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        recreate();
        super.onActivityResult(requestCode, resultCode, data);
    }
}
