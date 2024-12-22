package com.cato.glasssky;

import static com.cato.glasssky.ImageRequest.makeImageRequest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;

//TODO: Improve performance

public class Timeline extends Activity {
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    String access_token;
    String mode;
    String uri;
    String cursor = "";
    Boolean like;
    Boolean repost;
    Boolean reply;
    Boolean follow;
    Boolean loading = false;
    int extraCards;
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    JSONArray responseArray;
    JSONObject mainPost;
    JSONObject Author;
    String externalUri = "";
    String videoUrl = "";
    static int REPLY_REQUEST = 0;
    OkHttpClient client;
    static final int limit = 10;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CustomTrust customTrust = new CustomTrust(getApplicationContext());
        client = customTrust.getClient();
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");
        uri = intent.getStringExtra("uri");
        viewPosts(mode, uri);
        mCards = new ArrayList<CardBuilder>();
        mCardScrollView = new CardScrollView(Timeline.this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mSlider = Slider.from(mCardScrollView);
        mIndeterminate = mSlider.startIndeterminate();
        mCardScrollView.activate();
        setupClickListener();
        setupScrollListener();
        setContentView(mCardScrollView);
    }
    public void viewPosts(String mode, String uri) {
        SharedPreferences sharedPref = Timeline.this.getSharedPreferences(
                getString(R.string.auth), Context.MODE_PRIVATE);
        access_token = sharedPref.getString(getString(R.string.access_token), "unset");
        loading=true;
        if (mode.equals("algorithm")) {
            HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.feed.getFeed?feed=" + uri + "&limit=" + limit, null, access_token, "GET",
                    new HttpsUtils.HttpCallback() {
                        @Override
                        public void onSuccess(String response) {
                            // Handle successful response
                            try {
                                JSONObject responseObj = new JSONObject(response);
                                cursor = responseObj.getString("cursor");
                                responseArray = responseObj.getJSONArray("feed");
                                extraCards = 0;
                                SetPosts(responseArray);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (errorMessage.contains("ExpiredToken")) {
                                Log.e("Session Refresh Error", "The token has expired.");
                                Intent intent = new Intent(Timeline.this, Authenticate.class);
                                startActivity(intent);
                            } else {
                                Log.e("Session Refresh Error", "An error occurred.");
                            }
                        }
                    });
        }
        if (mode.equals("post")) {
            HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.feed.getPostThread?uri=" + uri + "&depth=1", null, access_token, "GET", // Limiting depth, only need top level replies
                    new HttpsUtils.HttpCallback() {
                        @Override
                        public void onSuccess(String response) {
                            // Handle successful response

                            try {
                                //initial post
                                mainPost = new JSONObject(response).getJSONObject("thread").getJSONObject("post");
                                String text = mainPost.getJSONObject("record").getString("text");
                                String likeCount = mainPost.getString("likeCount");
                                String replyCount = mainPost.getString("replyCount");
                                String repostCount = mainPost.getString("repostCount");
                                String timestamp = getTimeAgo(mainPost.getString("indexedAt"));
                                String Avatarurl = mainPost.getJSONObject("author").getString("avatar");
                                CardBuilder accountCard = new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                        .setText("View account")
                                        .setIcon(R.drawable.person_64);
                                repost = true;
                                makeImageRequest(Timeline.this, Avatarurl, client, new ImageRequest.ImageCallback() {
                                            @Override
                                            public void onImageLoaded(Bitmap bitmap) {
                                                accountCard.setIcon(bitmap);
                                            }
                                        });
                                mCards.add(accountCard);
                                if (mainPost.getJSONObject("viewer").has("replyDisabled")) {
                                    mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                            .setText("Reply")
                                            .setFootnote("You cannot reply to this post")
                                            .setIcon(R.drawable.reply_64));
                                    reply = false;
                                } else {
                                    mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                            .setText("Reply")
                                            .setIcon(R.drawable.reply_64));
                                    reply = true;
                                }
                                repost = true;
                                if (mainPost.getJSONObject("viewer").has("repost")) {
                                    mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                            .setText("Remove repost")
                                            .setIcon(R.drawable.repost_64));
                                    repost = true;
                                } else {
                                    mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                            .setText("Repost")
                                            .setIcon(R.drawable.repost_64));
                                    repost = false;
                                }
                                if (mainPost.getJSONObject("viewer").has("like")) {
                                    mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                            .setText("Unlike")
                                            .setIcon(R.drawable.heart_broken_64));
                                    like = true;
                                } else {
                                    mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                            .setText("Like")
                                            .setIcon(R.drawable.favorite_64));
                                    like = false;
                                }
                                final CardBuilder[] cardHolder = new CardBuilder[1];
                                if (!text.isEmpty()) {
                                    cardHolder[0] = new CardBuilder(Timeline.this, CardBuilder.Layout.TEXT)
                                            .setText(text)
                                            .setFootnote(replyCount + " replies, " + repostCount + " reposts, " + likeCount + " likes")
                                            .setTimestamp(timestamp);
                                    mCards.add(cardHolder[0]);
                                }
                                if (mainPost.has("embed") && mainPost.getJSONObject("embed").getString("$type").equals("app.bsky.embed.images#view")) {
                                    JSONArray images = mainPost.getJSONObject("embed").getJSONArray("images");
                                    for(int i = 0; i < images.length(); i++) {
                                        final CardBuilder[] card = new CardBuilder[1];
                                        String Imageurl = images.getJSONObject(i).getString("thumb");
                                        String caption = images.getJSONObject(i).getString("alt");
                                        int finalI = i;
                                        makeImageRequest(Timeline.this, Imageurl, client, new ImageRequest.ImageCallback() {
                                            @Override
                                            public void onImageLoaded(Bitmap bitmap) {
                                                card[0] = new CardBuilder(Timeline.this, CardBuilder.Layout.CAPTION)
                                                        .setText(caption)
                                                        .setFootnote(replyCount + " replies, " + repostCount + " reposts, " + likeCount + " likes")
                                                        .setTimestamp(timestamp)
                                                        .addImage(bitmap);
                                                mCards.add(card[0]);
                                                if (finalI == images.length() - 1) {
                                                    try {
                                                        responseArray = new JSONObject(response).getJSONObject("thread").getJSONArray("replies");
                                                    } catch (JSONException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                    if(!text.isEmpty()) {
                                                        extraCards = images.length() + 5;
                                                        mCardScrollView.setSelection(extraCards - (images.length() + 1));
                                                    } else {
                                                        extraCards = images.length() + 4;
                                                        mCardScrollView.setSelection(extraCards - (images.length()));
                                                    }
                                                    mAdapter.notifyDataSetChanged();
                                                    SetPosts(responseArray);
                                                }
                                            }
                                        });
                                    }
                                } else if (mainPost.has("embed") && mainPost.getJSONObject("embed").getString("$type").equals("app.bsky.embed.external#view")) {
                                    final CardBuilder[] card = new CardBuilder[1];
                                    JSONObject external = mainPost.getJSONObject("embed").getJSONObject("external");
                                    externalUri = external.getString("uri");
                                    String caption = external.getString("title");
                                    String footnote = external.getString("description");
                                    String Thumburl = external.getString("thumb");
                                    makeImageRequest(Timeline.this, Thumburl, client, new ImageRequest.ImageCallback() {
                                        @Override
                                        public void onImageLoaded(Bitmap bitmap) {
                                            card[0] = new CardBuilder(Timeline.this, CardBuilder.Layout.CAPTION)
                                                    .setText(caption)
                                                    .setFootnote(footnote)
                                                    .setTimestamp(timestamp)
                                                    .addImage(bitmap)
                                                    .setAttributionIcon(R.drawable.public_64);
                                            mCards.add(card[0]);
                                            try {
                                                responseArray = new JSONObject(response).getJSONObject("thread").getJSONArray("replies");
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            }
                                            if(!text.isEmpty()) {
                                                extraCards = 6;
                                            } else {
                                                extraCards = 5;
                                            }
                                            mCardScrollView.setSelection(extraCards - 2);
                                            mAdapter.notifyDataSetChanged();
                                            SetPosts(responseArray);
                                        }
                                    });
                                } else if (mainPost.has("embed") && mainPost.getJSONObject("embed").getString("$type").equals("app.bsky.embed.video#view")) {
                                    final CardBuilder[] card = new CardBuilder[1];
                                    JSONObject embed = mainPost.getJSONObject("embed");
                                    videoUrl = embed.getString("playlist");
                                    String Thumburl = embed.getString("thumbnail");
                                    makeImageRequest(Timeline.this, Thumburl, client, new ImageRequest.ImageCallback() {
                                        @Override
                                        public void onImageLoaded(Bitmap bitmap) {
                                            card[0] = new CardBuilder(Timeline.this, CardBuilder.Layout.TITLE)
                                                    .setText("Tap to play video")
                                                    .addImage(bitmap)
                                                    .setIcon(R.drawable.play_arrow_64);
                                            mCards.add(card[0]);
                                            try {
                                                responseArray = new JSONObject(response).getJSONObject("thread").getJSONArray("replies");
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            }
                                            if(!text.isEmpty()) {
                                                extraCards = 6;
                                            } else {
                                                extraCards = 5;
                                            }
                                            mCardScrollView.setSelection(extraCards - 2);
                                            mAdapter.notifyDataSetChanged();
                                            SetPosts(responseArray);
                                        }
                                    });
                                } else {
                                    try {
                                        responseArray = new JSONObject(response).getJSONObject("thread").getJSONArray("replies");
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                    extraCards = 5;
                                    mCardScrollView.setSelection(extraCards - 1);
                                    mAdapter.notifyDataSetChanged();
                                    SetPosts(responseArray);
                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (errorMessage.contains("ExpiredToken")) {
                                Log.e("Session Refresh Error", "The token has expired.");
                                Intent intent = new Intent(Timeline.this, Authenticate.class);
                                startActivity(intent);
                            } else {
                                Log.e("Session Refresh Error", "An error occurred.");
                            }
                        }
                    });
        }
        if (mode.equals("author")) {
            HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.actor.getProfile?actor=" + uri + "&limit=" + limit, null, access_token, "GET",
                    new HttpsUtils.HttpCallback() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                Author = new JSONObject(response);
                                String Avatarurl = Author.getString("avatar");
                                makeImageRequest(Timeline.this, Avatarurl, client, new ImageRequest.ImageCallback() {
                                    @Override
                                    public void onImageLoaded(Bitmap bitmap) {
                                        try {
                                            CardBuilder authorCard = new CardBuilder(Timeline.this, CardBuilder.Layout.AUTHOR).setIcon(bitmap);
                                            if (Author.has("banner")) {
                                                String banner = Author.getString("banner");
                                                makeImageRequest(Timeline.this, banner, client, new ImageRequest.ImageCallback() {
                                                    @Override
                                                    public void onImageLoaded(Bitmap bitmap) {
                                                        try {
                                                            String handle = Author.getString("handle");
                                                            String displayName = Author.getString("displayName");
                                                            String description = "";
                                                            if (Author.has("description")) {
                                                                description = Author.getString("description");
                                                            }
                                                            String followersCount = Author.getString("followersCount");
                                                            String followsCount = Author.getString("followsCount");
                                                            String postsCount = Author.getString("postsCount");
                                                            //TODO: Add block and mute options
                                                            if (Author.getJSONObject("viewer").has("following")) {
                                                                mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                                                        .setText("Unfollow"));
                                                                follow = true;
                                                            } else {
                                                                mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                                                        .setText("Follow"));
                                                                follow = false;
                                                            }
                                                            authorCard
                                                                    .addImage(bitmap)
                                                                    .setText(description)
                                                                    .setHeading(displayName)
                                                                    .setSubheading(handle)
                                                                    .setFootnote(followersCount + " followers, " + followsCount + " following, " + postsCount + " posts");
                                                            mCards.add(authorCard);
                                                            mAdapter.notifyDataSetChanged();
                                                            extraCards = 2;
                                                            mCardScrollView.setSelection(extraCards - 1);
                                                            HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.feed.getAuthorFeed?actor=" + uri + "&filter=posts_and_author_threads", null, access_token, "GET",
                                                                    new HttpsUtils.HttpCallback() {

                                                                        @Override
                                                                        public void onSuccess(String response) {
                                                                            try {
                                                                                cursor = new JSONObject(response).getString("cursor");
                                                                                responseArray = new JSONObject(response).getJSONArray("feed");
                                                                            } catch (
                                                                                    JSONException e) {
                                                                                throw new RuntimeException(e);
                                                                            }
                                                                            SetPosts(responseArray);

                                                                        }

                                                                        @Override
                                                                        public void onError(String errorMessage) {

                                                                        }
                                                                    });
                                                        } catch (JSONException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    }
                                                });
                                            } else {
                                                try {
                                                    String handle = Author.getString("handle");
                                                    String displayName = Author.getString("displayName");
                                                    String description = "";
                                                    if (Author.has("description")) {
                                                        description = Author.getString("description");
                                                    }
                                                    String followersCount = Author.getString("followersCount");
                                                    String followsCount = Author.getString("followsCount");
                                                    String postsCount = Author.getString("postsCount");
                                                    //TODO: Add block and mute options here too
                                                    if (Author.getJSONObject("viewer").has("following")) {
                                                        mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                                                .setText("Unfollow"));
                                                        follow = true;
                                                    } else {
                                                        mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                                                .setText("Follow"));
                                                        follow = false;
                                                    }
                                                    authorCard
                                                            .setText(description)
                                                            .setHeading(displayName)
                                                            .setSubheading(handle)
                                                            .setFootnote(followersCount + " followers, " + followsCount + " following, " + postsCount + " posts");
                                                    mCards.add(authorCard);
                                                    mAdapter.notifyDataSetChanged();
                                                    extraCards = 2;
                                                    mCardScrollView.setSelection(extraCards - 1);
                                                    HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.feed.getAuthorFeed?actor=" + uri + "&filter=posts_and_author_threads", null, access_token, "GET",
                                                            new HttpsUtils.HttpCallback() {

                                                                @Override
                                                                public void onSuccess(String response) {
                                                                    try {
                                                                        responseArray = new JSONObject(response).getJSONArray("feed");
                                                                    } catch (
                                                                            JSONException e) {
                                                                        throw new RuntimeException(e);
                                                                    }
                                                                    SetPosts(responseArray);

                                                                }

                                                                @Override
                                                                public void onError(String errorMessage) {

                                                                }
                                                            });
                                                } catch (JSONException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        @Override
                        public void onError(String errorMessage) {

                        }
                    });
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
        mCardScrollView.animate(position, CardScrollView.Animation.INSERTION);
    }
    private void SetPosts(JSONArray postsArray) {
        executor.execute(() -> {
            try {
                for (int i = 0; i < postsArray.length(); i++) {
                    JSONObject post = postsArray.getJSONObject(i).getJSONObject("post");
                    String text = post.getJSONObject("record").getString("text");
                    String heading = post.getJSONObject("author").getString("displayName");
                    String subHeading = post.getJSONObject("author").getString("handle");
                    String likeCount = post.getString("likeCount");
                    String replyCount = post.getString("replyCount");
                    String repostCount = post.getString("repostCount");
                    String timestamp = getTimeAgo(post.getString("indexedAt"));
                    CardBuilder card = new CardBuilder(Timeline.this, CardBuilder.Layout.AUTHOR)
                            .setText(text)
                            .setHeading(heading)
                            .setSubheading(subHeading)
                            .setFootnote(replyCount + " replies, " + repostCount + " reposts, " + likeCount + " likes")
                            .setTimestamp(timestamp)
                            .setIcon(R.drawable.person_64);
                    if (post.getJSONObject("author").has("avatar")) {
                        String Avatarurl = post.getJSONObject("author").getString("avatar");
                        makeImageRequest(this, Avatarurl, client, new ImageRequest.ImageCallback() {
                            @Override
                            public void onImageLoaded(Bitmap bitmap) {
                                card.setIcon(bitmap);
                            }
                        });

                    }
                    if (post.has("embed") && post.getJSONObject("embed").getString("$type").equals("app.bsky.embed.images#view")) {
                        if (post.getJSONObject("embed").getJSONArray("images").length() > 1) {
                            card.setAttributionIcon(R.drawable.collections_64);
                            if (text.isEmpty()) {
                                card.setText("Posted images");
                            }
                        } else {
                            card.setAttributionIcon(R.drawable.image_64);
                            if (text.isEmpty()) {
                                card.setText("Posted an image");
                            }
                        }
                    }
                    if (post.has("embed") && post.getJSONObject("embed").getString("$type").equals("app.bsky.embed.video#view")) {
                        card.setAttributionIcon(R.drawable.movie_64);
                        if (text.isEmpty()) {
                            card.setText("Posted a video");
                        }
                    }
                    if (post.has("embed") && post.getJSONObject("embed").getString("$type").equals("app.bsky.embed.external#view")) {
                        card.setAttributionIcon(R.drawable.public_64);
                        if (text.isEmpty()) {
                            card.setText("Posted a link");
                        }
                    }
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

    public static String getTimeAgo(String timestamp) {
        // Define the date format used in the input string
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setLenient(false);
        // Set the timezone to UTC
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            // Parse the timestamp into a Date object
            Date past = dateFormat.parse(timestamp);
            Date now = new Date();

            // Calculate the time difference in milliseconds
            long difference = now.getTime() - past.getTime();

            // Convert the difference to seconds, minutes, hours, etc.
            long seconds = TimeUnit.MILLISECONDS.toSeconds(difference);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(difference);
            long hours = TimeUnit.MILLISECONDS.toHours(difference);
            long days = TimeUnit.MILLISECONDS.toDays(difference);

            if (seconds < 60) {
                return "just now";
            } else if (minutes < 60) {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (days < 30) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (days < 365) {
                long months = days / 30;
                return months + " month" + (months > 1 ? "s" : "") + " ago";
            } else {
                long years = days / 365;
                return years + " year" + (years > 1 ? "s" : "") + " ago";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid date";
        }
    }
    private void setupClickListener() {
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);
                try {
                    Intent timelineIntent = new Intent(Timeline.this, Timeline.class);
                    if (position == 0 && mode.equals("post")) { // Open account page
                        String authorDid = mainPost.getJSONObject("author").getString("did");
                        timelineIntent.putExtra("uri", authorDid);
                        timelineIntent.putExtra("mode", "author");
                        startActivity(timelineIntent);
                    } else if (position == 0 && mode.equals("author")) { // Follow function
                        mIndeterminate = mSlider.startIndeterminate();
                        HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.getSession", null, access_token, "GET",
                                new HttpsUtils.HttpCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        // Handle successful response
                                        try {
                                            String did = new JSONObject(response).getString("did");
                                            if (follow) { // Unfollowing
                                                JSONObject unfollowObj = new JSONObject();
                                                String followUri = Author.getJSONObject("viewer").getString("following");
                                                // Define the regex pattern to match the rkey
                                                String pattern = ".*/([^/]+)$";

                                                // Compile the regex
                                                Pattern regex = Pattern.compile(pattern);
                                                Matcher matcher = regex.matcher(followUri);
                                                String rkey = "unset";

                                                if (matcher.find()) {
                                                    // Extract the rkey (group 1 in the regex)
                                                    rkey = matcher.group(1);
                                                } else {
                                                    Log.e("Unlike error", "Unable to get rkey from like uri.");
                                                }
                                                unfollowObj.put("repo", did);
                                                unfollowObj.put("collection", "app.bsky.graph.follow");
                                                unfollowObj.put("rkey", rkey);
                                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.deleteRecord", unfollowObj, access_token, "POST",
                                                        new HttpsUtils.HttpCallback() {
                                                            @Override
                                                            public void onSuccess(String response) {
                                                                recreate();

                                                            }

                                                            @Override
                                                            public void onError(String errorMessage) {
                                                                am.playSoundEffect(Sounds.ERROR);
                                                            }
                                                        });


                                            } else { // Following
                                                JSONObject followObj = new JSONObject();
                                                JSONObject record = new JSONObject();
                                                followObj.put("collection", "app.bsky.graph.follow");
                                                followObj.put("repo", did);
                                                String authorDid = Author.getString("did");
                                                record.put("subject", authorDid);
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                                String timestamp = sdf.format(new Date());
                                                record.put("createdAt", timestamp);
                                                followObj.put("record", record);
                                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.createRecord", followObj, access_token, "POST",
                                                        new HttpsUtils.HttpCallback() {
                                                            @Override
                                                            public void onSuccess(String response) {
                                                                am.playSoundEffect(Sounds.SUCCESS);
                                                                recreate();

                                                            }

                                                            @Override
                                                            public void onError(String errorMessage) {
                                                                am.playSoundEffect(Sounds.ERROR);
                                                            }
                                                        });
                                            }
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        if (errorMessage.contains("ExpiredToken")) {
                                            Log.e("Session Refresh Error", "The token has expired.");
                                        } else {
                                            Log.e("Session Refresh Error", "An error occurred.");
                                        }
                                    }
                                });
                    } else if (position == 1 && mode.equals("post") && reply) { // Reply function
                        Intent replyIntent = new Intent(Timeline.this, PostActivity.class);
                        replyIntent.putExtra("parenturi", mainPost.getString("uri"));
                        replyIntent.putExtra("parentcid", mainPost.getString("cid"));
                        if (mainPost.getJSONObject("record").has("reply")) {
                            JSONObject root = mainPost.getJSONObject("record").getJSONObject("reply").getJSONObject("root");
                            replyIntent.putExtra("rooturi", root.getString("uri"));
                            replyIntent.putExtra("rootcid", root.getString("cid"));
                        } else {
                            replyIntent.putExtra("rooturi", mainPost.getString("uri"));
                            replyIntent.putExtra("rootcid", mainPost.getString("cid"));
                        }
                        startActivityForResult(replyIntent, REPLY_REQUEST);
                    } else if ((position == 2) && mode.equals("post")) { // Repost function
                        mIndeterminate = mSlider.startIndeterminate();
                        HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.getSession", null, access_token, "GET",
                                new HttpsUtils.HttpCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        // Handle successful response
                                        try {
                                            String did = new JSONObject(response).getString("did");
                                            if (repost) { //Unreposting
                                                JSONObject unrepostObj = new JSONObject();
                                                String repostUri = mainPost.getJSONObject("viewer").getString("repost");
                                                // Define the regex pattern to match the rkey
                                                String pattern = ".*/([^/]+)$";

                                                // Compile the regex
                                                Pattern regex = Pattern.compile(pattern);
                                                Matcher matcher = regex.matcher(repostUri);
                                                String rkey = "unset";

                                                if (matcher.find()) {
                                                    // Extract the rkey (group 1 in the regex)
                                                    rkey = matcher.group(1);
                                                } else {
                                                    Log.e("Unlike error", "Unable to get rkey from like uri.");
                                                }
                                                unrepostObj.put("repo", did);
                                                unrepostObj.put("collection", "app.bsky.feed.repost");
                                                unrepostObj.put("rkey", rkey);
                                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.deleteRecord", unrepostObj, access_token, "POST",
                                                        new HttpsUtils.HttpCallback() {
                                                            @Override
                                                            public void onSuccess(String response) {
                                                                recreate();

                                                            }

                                                            @Override
                                                            public void onError(String errorMessage) {
                                                                am.playSoundEffect(Sounds.ERROR);
                                                            }
                                                        });


                                            } else { //Reposting
                                                JSONObject repostObj = new JSONObject();
                                                JSONObject record = new JSONObject();
                                                JSONObject subject = new JSONObject();
                                                repostObj.put("collection", "app.bsky.feed.repost");
                                                repostObj.put("repo", did);
                                                subject.put("uri", mainPost.getString("uri"));
                                                subject.put("cid", mainPost.getString("cid"));
                                                record.put("subject", subject);
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                                String timestamp = sdf.format(new Date());
                                                record.put("createdAt", timestamp);
                                                repostObj.put("record", record);
                                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.createRecord", repostObj, access_token, "POST",
                                                        new HttpsUtils.HttpCallback() {
                                                            @Override
                                                            public void onSuccess(String response) {
                                                                am.playSoundEffect(Sounds.SUCCESS);
                                                                recreate();

                                                            }

                                                            @Override
                                                            public void onError(String errorMessage) {
                                                                am.playSoundEffect(Sounds.ERROR);
                                                            }
                                                        });
                                            }
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        if (errorMessage.contains("ExpiredToken")) {
                                            Log.e("Session Refresh Error", "The token has expired.");
                                        } else {
                                            Log.e("Session Refresh Error", "An error occurred.");
                                        }
                                    }
                                });
                    } else if ((position == 3) && mode.equals("post")) { //Like function
                        mIndeterminate = mSlider.startIndeterminate();
                        HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.getSession", null, access_token, "GET",
                                new HttpsUtils.HttpCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        // Handle successful response
                                        try {
                                            String did = new JSONObject(response).getString("did");
                                            if (like) { //Unliking
                                                JSONObject unlikeObj = new JSONObject();
                                                String likeUri = mainPost.getJSONObject("viewer").getString("like");
                                                // Define the regex pattern to match the rkey
                                                String pattern = ".*/([^/]+)$";

                                                // Compile the regex
                                                Pattern regex = Pattern.compile(pattern);
                                                Matcher matcher = regex.matcher(likeUri);
                                                String rkey = "unset";

                                                if (matcher.find()) {
                                                    // Extract the rkey (group 1 in the regex)
                                                    rkey = matcher.group(1);
                                                } else {
                                                    Log.e("Unlike error", "Unable to get rkey from like uri.");
                                                }
                                                unlikeObj.put("repo", did);
                                                unlikeObj.put("collection", "app.bsky.feed.like");
                                                unlikeObj.put("rkey", rkey);
                                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.deleteRecord", unlikeObj, access_token, "POST",
                                                        new HttpsUtils.HttpCallback() {
                                                            @Override
                                                            public void onSuccess(String response) {
                                                                recreate();

                                                            }

                                                            @Override
                                                            public void onError(String errorMessage) {
                                                                am.playSoundEffect(Sounds.ERROR);
                                                            }
                                                        });


                                            } else { //Liking
                                                JSONObject likeObj = new JSONObject();
                                                JSONObject record = new JSONObject();
                                                JSONObject subject = new JSONObject();
                                                likeObj.put("collection", "app.bsky.feed.like");
                                                likeObj.put("repo", did);
                                                subject.put("uri", mainPost.getString("uri"));
                                                subject.put("cid", mainPost.getString("cid"));
                                                record.put("subject", subject);
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                                String timestamp = sdf.format(new Date());
                                                record.put("createdAt", timestamp);
                                                likeObj.put("record", record);
                                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.createRecord", likeObj, access_token, "POST",
                                                        new HttpsUtils.HttpCallback() {
                                                            @Override
                                                            public void onSuccess(String response) {
                                                                am.playSoundEffect(Sounds.SUCCESS);
                                                                recreate();

                                                            }

                                                            @Override
                                                            public void onError(String errorMessage) {
                                                                am.playSoundEffect(Sounds.ERROR);
                                                            }
                                                        });
                                            }
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        if (errorMessage.contains("ExpiredToken")) {
                                            Log.e("Session Refresh Error", "The token has expired.");
                                        } else {
                                            Log.e("Session Refresh Error", "An error occurred.");
                                        }
                                    }
                                });
                    } else if (position == 5 && !externalUri.isEmpty()) {
                        PackageManager packageManager = getPackageManager();
                        String targetPackage = "com.google.glass.browser";
                        boolean isInstalled = isPackageInstalled(targetPackage, packageManager);

                        if (isInstalled) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setClassName("com.google.glass.browser", "com.google.glass.browser.WebBrowserActivity");
                            i.setData(Uri.parse(externalUri));
                            startActivity(i);
                        } else {
                            CharSequence text = "Please install the package com.google.glass.browser";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(Timeline.this, text, duration);
                            toast.show();
                        }

                    } else if (position == 5 && !videoUrl.isEmpty()) { //Play video
                        Intent videoIntent = new Intent(Timeline.this, VideoActivity.class);
                        videoIntent.putExtra("url", videoUrl);
                        startActivity(videoIntent);
                        finish();
                    } else if (position >= extraCards || mode.equals("algorithm")) {
                        String posturi = responseArray.getJSONObject(position - extraCards).getJSONObject("post").getString("uri");
                        timelineIntent.putExtra("uri", posturi);
                        timelineIntent.putExtra("mode", "post");
                        startActivity(timelineIntent);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    public boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    private void setupScrollListener() {
        mCardScrollView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mCards.size() - 5 && !loading && !cursor.isEmpty()) {
                    if (mode.equals("algorithm")) {
                        loading = true;
                        mCards.add(new CardBuilder(Timeline.this, CardBuilder.Layout.MENU)
                                .setText("Loading"));
                        HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.feed.getFeed?feed=" + uri + "&cursor=" + cursor + "&limit=" + limit, null, access_token, "GET",
                                new HttpsUtils.HttpCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        // Handle successful response
                                        mCards.remove(mCards.size() - 1);
                                        try {
                                            JSONObject responseObj = new JSONObject(response);
                                            cursor = responseObj.getString("cursor");
                                            JSONArray newResponseArray = responseObj.getJSONArray("feed");
                                            for (int i = 0; i < newResponseArray.length(); i++) {
                                                responseArray.put(newResponseArray.get(i));
                                            }
                                            SetPosts(newResponseArray);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        if (errorMessage.contains("ExpiredToken")) {
                                            Log.e("Session Refresh Error", "The token has expired.");
                                            Intent intent = new Intent(Timeline.this, Authenticate.class);
                                            startActivity(intent);
                                        } else {
                                            Log.e("Session Refresh Error", "An error occurred.");
                                        }
                                    }
                                });
                    }
                    if (mode.equals("author")) {
                        loading = true;
                        HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.feed.getAuthorFeed?actor=" + uri + "&cursor=" + cursor + "&filter=posts_and_author_threads&limit=" + limit, null, access_token, "GET",
                                new HttpsUtils.HttpCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        try {
                                            cursor = new JSONObject(response).getString("cursor");
                                            JSONArray newResponseArray = new JSONObject(response).getJSONArray("feed");
                                            for (int i = 0; i < newResponseArray.length(); i++) {
                                                responseArray.put(newResponseArray.get(i));
                                            }
                                            SetPosts(newResponseArray);
                                            loading = false;
                                        } catch (
                                                JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                    @Override
                                    public void onError(String errorMessage) {

                                    }
                                });
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                String uri = data.getStringExtra("uri");
                HttpsUtils.makePostRequest("https://bsky.social/xrpc/app.bsky.feed.getPostThread?uri=" + uri + "&depth=0", null, access_token, "GET", //Limiting depth, only need top level replies
                    new HttpsUtils.HttpCallback() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                JSONObject post = new JSONObject(response).getJSONObject("thread").getJSONObject("post");
                                String text = post.getJSONObject("record").getString("text");
                                String heading = post.getJSONObject("author").getString("displayName");
                                String subHeading = post.getJSONObject("author").getString("handle");
                                String likeCount = post.getString("likeCount");
                                String replyCount = post.getString("replyCount");
                                String repostCount = post.getString("repostCount");
                                String timestamp = getTimeAgo(post.getString("indexedAt"));
                                CardBuilder card = new CardBuilder(Timeline.this, CardBuilder.Layout.AUTHOR)
                                        .setText(text)
                                        .setHeading(heading)
                                        .setSubheading(subHeading)
                                        .setFootnote(replyCount + " replies, " + repostCount + " reposts, " + likeCount + " likes")
                                        .setTimestamp(timestamp);
                                if (post.has("embed") && post.getJSONObject("embed").getString("$type").equals("app.bsky.embed.images#view")) {
                                    if (post.getJSONObject("embed").getJSONArray("images").length() > 1) {
                                        card.setAttributionIcon(R.drawable.collections_64);
                                        if (text.isEmpty()){
                                            card.setText("Posted images");
                                        }
                                    } else {
                                        card.setAttributionIcon(R.drawable.image_64);
                                        if (text.isEmpty()){
                                            card.setText("Posted an image");
                                        }
                                    }
                                }
                                if (post.has("embed") && post.getJSONObject("embed").getString("$type").equals("app.bsky.embed.video#view")) {
                                    card.setAttributionIcon(R.drawable.movie_64);
                                    if (text.isEmpty()){
                                        card.setText("Posted a video");
                                    }
                                }
                                if (post.has("embed") && post.getJSONObject("embed").getString("$type").equals("app.bsky.embed.external#view")) {
                                    card.setAttributionIcon(R.drawable.public_64);
                                    if (text.isEmpty()){
                                        card.setText("Posted a link");
                                    }
                                }
                                insertNewCard(extraCards, card);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e("Error", errorMessage);
                        }
                    });

            }
        } catch (Exception ex) {
            Log.e("Error", String.valueOf(ex));
        }
    }
}
