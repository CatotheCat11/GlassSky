package com.cato.glasssky;

import static com.cato.glasssky.ImageRequest.makeImageRequest;
import static com.cato.glasssky.Timeline.round;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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
import java.util.Objects;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;


public class SearchActivity extends Activity {
    private static final int SPEECH_REQUEST = 0;
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    private String access_token;
    private String query = "";
    private JSONArray responseArray;
    OkHttpClient client;
    Boolean loading = false;
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    private String cursor = "";
    private boolean showingActors = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CustomTrust customTrust = new CustomTrust(getApplicationContext());
        client = customTrust.getClient();

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.auth), Context.MODE_PRIVATE);
        access_token = sharedPref.getString(getString(R.string.access_token), "unset");

        mCards = new ArrayList<CardBuilder>();
        createCards();
        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setupClickListener();
        setupScrollListener();
        setContentView(mCardScrollView);

        displaySpeechRecognizer();
    }

    private void createCards() {
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Top"));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Latest"));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("People"));
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
                if (!query.isEmpty() && !showingActors) {
                    if (position != 2){
                        Intent timelineIntent = new Intent(SearchActivity.this, Timeline.class);
                        timelineIntent.putExtra("query", query);
                        timelineIntent.putExtra("mode", "search");
                        switch(position) {
                            case 0: // app.bsky.feed.searchPosts filter=top
                                timelineIntent.putExtra("searchType", "top");
                                break;
                            case 1: // app.bsky.feed.searchPosts filter=latest
                                timelineIntent.putExtra("searchType", "latest");
                                break;
                        }
                        startActivity(timelineIntent);
                    } else {
                        showingActors = true;
                        mSlider = Slider.from(mCardScrollView);
                        mIndeterminate = mSlider.startIndeterminate();
                        HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.actor.searchActors?q=" + query, null, access_token, "GET",
                                new HttpsUtils.HttpCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        mCards.clear();
                                        try {
                                            JSONObject responseObj = new JSONObject(response);
                                            if (responseObj.has("cursor")) {
                                                cursor = responseObj.getString("cursor");
                                            } else cursor = "";
                                            responseArray = responseObj.getJSONArray("actors");
                                            SetActors(responseArray);
                                            runOnUiThread(() -> {
                                                mAdapter.notifyDataSetChanged();
                                                mCardScrollView.setSelection(0);
                                            });
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        if (errorMessage.contains("ExpiredToken")) {
                                            Log.e("Session Refresh Error", "The token has expired.");
                                            Intent intent = new Intent(SearchActivity.this, Authenticate.class);
                                            startActivity(intent);
                                        } else {
                                            Log.e("Session Refresh Error", "An error occurred.");
                                        }
                                    }
                                });
                    }
                } else {
                    try {
                        Intent timelineIntent = new Intent(SearchActivity.this, Timeline.class);
                        String actorUri = responseArray.getJSONObject(position).getString("did");
                        timelineIntent.putExtra("uri", actorUri);
                        timelineIntent.putExtra("mode", "author");
                        startActivity(timelineIntent);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void SetActors(JSONArray actorsArray) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i < actorsArray.length(); i++) {
                    JSONObject actor = actorsArray.getJSONObject(i);
                    String avatarUrl = actor.getString("avatar").replace("/avatar/", "/avatar_thumbnail/");
                    String handle = actor.getString("handle");
                    String displayName = actor.getString("displayName");
                    String description = "";
                    if (actor.has("description")) {
                        description = actor.getString("description");
                    }
                    JSONObject viewer = actor.getJSONObject("viewer");
                    String relation = viewer.has("following")
                            ? "Following"
                            : "Not following";
                    if (viewer.getBoolean("muted")) {
                        relation = " • Muted";
                    }
                    if (viewer.has("blocking")) {
                        relation += " • Blocked";
                    }
                    if (viewer.getBoolean("blockedBy")) {
                        relation += " • Blocked you";
                    }
                    if (viewer.has("followedBy")) {
                        relation += " • Follows you";
                    }
                    CardBuilder card = new CardBuilder(SearchActivity.this, CardBuilder.Layout.AUTHOR)
                            .setIcon(R.drawable.person_64)
                            .setText(description)
                            .setHeading(displayName)
                            .setSubheading("@" + handle)
                            .setFootnote(relation);
                    int finalI = i;
                    makeImageRequest(SearchActivity.this, avatarUrl, client, bitmap -> {
                        card.setIcon(bitmap);
                        runOnUiThread(() -> {
                            if (finalI == actorsArray.length() - 1) { // Refresh cards when last avatar icon has been loaded
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    });
                    mCards.add(card);
                }
                loading = false;
                if (mIndeterminate != null) {
                    mIndeterminate.hide();
                    mIndeterminate = null;
                }
                runOnUiThread(() -> {
                    mAdapter.notifyDataSetChanged();
                });
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setupScrollListener() {
        mCardScrollView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mCards.size() - 5 && !loading && !cursor.isEmpty() && showingActors) {
                    Log.i("Timeline", "Loading more posts");
                    loading = true;
                    mCards.add(new CardBuilder(SearchActivity.this, CardBuilder.Layout.MENU)
                            .setText("Loading"));
                    HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.actor.searchActors?q=" + query + "&cursor=" + cursor, null, access_token, "GET",
                            new HttpsUtils.HttpCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    // Handle successful response
                                    mCards.remove(mCards.size() - 1);
                                    try {
                                        JSONObject responseObj = new JSONObject(response);
                                        if (responseObj.has("cursor")) {
                                            cursor = responseObj.getString("cursor");
                                        } else cursor = "";
                                        JSONArray newResponseArray = responseObj.getJSONArray("actors");
                                        for (int i = 0; i < newResponseArray.length(); i++) {
                                            responseArray.put(newResponseArray.get(i));
                                        }
                                        SetActors(newResponseArray);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }

                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (errorMessage.contains("ExpiredToken")) {
                                        Log.e("Session Refresh Error", "The token has expired.");
                                        Intent intent = new Intent(SearchActivity.this, Authenticate.class);
                                        startActivity(intent);
                                    } else {
                                        Log.e("Session Refresh Error", "An error occurred.");
                                    }
                                }
                            });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                query = results.get(0);
            } else finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
