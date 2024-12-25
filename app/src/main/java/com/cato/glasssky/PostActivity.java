package com.cato.glasssky;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.glass.content.Intents;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;

import org.conscrypt.Conscrypt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.tls.OkHostnameVerifier;
import okio.BufferedSink;

public class PostActivity extends Activity {
    private static final int SPEECH_REQUEST = 0;
    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final int TAKE_VIDEO_REQUEST = 2;
    private static final int MEDIA_REQUEST = 3;

    private List<CardBuilder> mCards;
    private static CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    private FileObserver observer;
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    Bitmap bitmap;
    String text = "";
    String thumbnailPath = "";
    static String videoPath = ""; //TODO: TEST VIDEO FILE ADDING (AND IMAGE TOO) CHECK CARD POSITION AND REMOVE COMMENT BLOCK
    ArrayList<Bitmap> image = new ArrayList<>();
    ArrayList<String> processArray = new ArrayList<>();
    Boolean processing = false;
    String parenturi = "";
    String parentcid = "";
    String rooturi = "";
    String rootcid = "";
    static String did;
    static ExecutorService executor = Executors.newFixedThreadPool(1);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoPath = "";

        mCards = new ArrayList<CardBuilder>();

        createCards();
        // Get the shared image
        Intent intent = getIntent();
        if (intent.hasExtra("parenturi")) {
            parenturi = intent.getStringExtra("parenturi");
            parentcid = intent.getStringExtra("parentcid");
            rooturi = intent.getStringExtra("rooturi");
            rootcid = intent.getStringExtra("rootcid");
            mCards.get(3).setText("Reply");
        }

        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        mSlider = Slider.from(mCardScrollView);
        setupClickListener();
        setContentView(mCardScrollView);
    }
    private void createCards() {
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Add text")
                .setFootnote("Inputs text through voice")
                .setIcon(R.drawable.subject_64));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Add media")
                .setIcon(R.drawable.add_a_photo_64));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Post")
                .setIcon(R.drawable.post_add_64));
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
    private void setupClickListener() {
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (position == mCards.size() - 1 && processing) {
                    // Block posting while processing images
                    am.playSoundEffect(Sounds.DISALLOWED);
                    return;
                }
                am.playSoundEffect(Sounds.TAP);
                if (position == 0 && mCards.size() > 1) {
                    displaySpeechRecognizer();
                }
                if (position == mCards.size() - 2 && videoPath.isEmpty()) {
                    Intent mediaSelect = new Intent(PostActivity.this, MediaSelect.class);
                    mediaSelect.putExtra("video", image.isEmpty());
                    startActivityForResult(mediaSelect, MEDIA_REQUEST);
                }
                if (position == mCards.size() - 1) {
                    if ((!thumbnailPath.isEmpty() || !videoPath.isEmpty() || !text.isEmpty() || !image.isEmpty())) {
                        post(text, image, videoPath);
                    }
                }
            }
        });
    }
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }
    private void takeVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, TAKE_VIDEO_REQUEST);
    }
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, SPEECH_REQUEST);
    }
    private void post(String text, ArrayList<Bitmap> image, String video) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.i(TAG, "Posting...");
        mCards.clear();
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Posting...")
                .setIcon(R.drawable.post_add_64));
        mAdapter.notifyDataSetChanged();
        mIndeterminate = mSlider.startIndeterminate();
        SharedPreferences sharedPref = PostActivity.this.getSharedPreferences(
                getString(R.string.auth), Context.MODE_PRIVATE);
        String access_token = sharedPref.getString(getString(R.string.access_token), "unset");
        HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.getSession", null, access_token, "GET",
                new HttpsUtils.HttpCallback() {
                    @Override
                    public void onSuccess(String didresponse) {
                        try {
                            did = new JSONObject(didresponse).getString("did");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        if (image != null) {
                            JSONArray images = new JSONArray();
                            for(int i = 0; i < image.size(); i++) {
                                int finalI = i;
                                makeImageRequest(image.get(i), access_token, false, new HttpCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        try {
                                            JSONObject blob = new JSONObject(response).getJSONObject("blob");
                                            JSONObject imageObj = new JSONObject();
                                            imageObj.put("alt", "");
                                            imageObj.put("image", blob);
                                            images.put(imageObj);
                                            if (finalI == image.size() - 1) {
                                                JSONObject embed = new JSONObject();
                                                embed.put("$type", "app.bsky.embed.images");
                                                embed.put("images", images);
                                                JSONObject record = new JSONObject();
                                                record.put("$type", "app.bsky.feed.post");
                                                if (!text.isEmpty()) {
                                                    record.put("text", text);
                                                }
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                                String timestamp = sdf.format(new Date());
                                                record.put("createdAt", timestamp);
                                                if (!parentcid.isEmpty()) {
                                                    JSONObject root = new JSONObject();
                                                    JSONObject parent = new JSONObject();
                                                    JSONObject reply = new JSONObject();
                                                    root.put("uri", rooturi);
                                                    root.put("cid", rootcid);
                                                    parent.put("uri", rooturi);
                                                    parent.put("cid", parentcid);
                                                    reply.put("root", root);
                                                    reply.put("parent", parent);
                                                    record.put("reply", reply);
                                                }
                                                record.put("embed", embed);
                                                JSONObject imagePost = new JSONObject();
                                                imagePost.put("collection", "app.bsky.feed.post");
                                                imagePost.put("repo", new JSONObject(didresponse).getString("did"));
                                                imagePost.put("record", record);
                                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.createRecord", imagePost, access_token, "POST",
                                                        new HttpsUtils.HttpCallback() {
                                                            @Override
                                                            public void onSuccess(String response) {
                                                                Intent intent = getIntent();
                                                                try {
                                                                    intent.putExtra("uri", new JSONObject(response).getString("uri"));
                                                                } catch (JSONException e) {
                                                                    throw new RuntimeException(e);
                                                                }
                                                                setResult(RESULT_OK, intent);
                                                                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                                                am.playSoundEffect(Sounds.SUCCESS);
                                                                Log.d("Success", "Successfully posted!");
                                                                mIndeterminate.hide();
                                                                finish();
                                                            }

                                                            @Override
                                                            public void onError(String errorMessage) {

                                                            }
                                                        });
                                            }
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        Log.i("Success", "Success");
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e("Error", errorMessage);
                                        finish(); //Exiting to prevent self destruction of the bluesky platform bcz of too many broken requests
                                    }
                                });
                            }
                        } else if (!video.isEmpty()) {
                            makeImageRequest(null, access_token, true, new HttpCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    Log.i("Success", "Success");
                                    try {
                                        JSONObject blob = new JSONObject(response).getJSONObject("blob");
                                        JSONObject embed = new JSONObject();
                                        embed.put("$type", "app.bsky.embed.video");
                                        embed.put("video", blob);
                                        JSONObject record = new JSONObject();
                                        record.put("$type", "app.bsky.feed.post");
                                        if (!text.isEmpty()) {
                                            record.put("text", text);
                                        }
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                        String timestamp = sdf.format(new Date());
                                        record.put("createdAt", timestamp);
                                        if (!parentcid.isEmpty()) {
                                            JSONObject root = new JSONObject();
                                            JSONObject parent = new JSONObject();
                                            JSONObject reply = new JSONObject();
                                            root.put("uri", rooturi);
                                            root.put("cid", rootcid);
                                            parent.put("uri", rooturi);
                                            parent.put("cid", parentcid);
                                            reply.put("root", root);
                                            reply.put("parent", parent);
                                            record.put("reply", reply);
                                        }
                                        record.put("embed", embed);
                                        JSONObject imagePost = new JSONObject();
                                        imagePost.put("collection", "app.bsky.feed.post");
                                        imagePost.put("repo", new JSONObject(didresponse).getString("did"));
                                        imagePost.put("record", record);
                                        HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.createRecord", imagePost, access_token, "POST",
                                                new HttpsUtils.HttpCallback() {
                                                    @Override
                                                    public void onSuccess(String response) {
                                                        Intent intent = getIntent();
                                                        try {
                                                            intent.putExtra("uri", new JSONObject(response).getString("uri"));
                                                        } catch (JSONException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                        setResult(RESULT_OK, intent);
                                                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                                        am.playSoundEffect(Sounds.SUCCESS);
                                                        Log.i("Success", "Successfully posted!");
                                                        mIndeterminate.hide();
                                                        finish();
                                                    }

                                                    @Override
                                                    public void onError(String errorMessage) {

                                                    }
                                                });
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
                            try {
                                JSONObject record = new JSONObject();
                                record.put("$type", "app.bsky.feed.post");
                                if (!text.isEmpty()) {
                                    record.put("text", text);
                                }
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                String timestamp = sdf.format(new Date());
                                record.put("createdAt", timestamp);
                                if (!parentcid.isEmpty()) {
                                    JSONObject root = new JSONObject();
                                    JSONObject parent = new JSONObject();
                                    JSONObject reply = new JSONObject();
                                    root.put("uri", rooturi);
                                    root.put("cid", rootcid);
                                    parent.put("uri", rooturi);
                                    parent.put("cid", parentcid);
                                    reply.put("root", root);
                                    reply.put("parent", parent);
                                    record.put("reply", reply);
                                }
                                JSONObject Post = new JSONObject();
                                Post.put("collection", "app.bsky.feed.post");
                                Post.put("repo", new JSONObject(didresponse).getString("did"));
                                Post.put("record", record);
                                HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.repo.createRecord", Post, access_token, "POST",
                                        new HttpsUtils.HttpCallback() {
                                            @Override
                                            public void onSuccess(String response) {
                                                Intent intent = getIntent();
                                                try {
                                                    intent.putExtra("uri", new JSONObject(response).getString("uri"));
                                                } catch (JSONException e) {
                                                    throw new RuntimeException(e);
                                                }
                                                setResult(RESULT_OK, intent);
                                                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                                am.playSoundEffect(Sounds.SUCCESS);
                                                Log.i("Success", "Successfully posted!");
                                                mIndeterminate.hide();
                                                finish();
                                            }

                                            @Override
                                            public void onError(String errorMessage) {

                                            }
                                        });
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {

                    }
                });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) { //TODO: Change to queue system for multiple images processing
            thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
            processArray.add(picturePath);
            if (!processing) {
                processPictureWhenReady(processArray.get(0));
                processing = true;
            }
            Bitmap thumbnail = BitmapFactory.decodeFile(thumbnailPath);
            insertNewCard(mCards.size() - 2, new CardBuilder(this, CardBuilder.Layout.CAPTION)
                    .addImage(thumbnail));
            mCards.get(mCards.size() - 1).setFootnote("Picture is processing...");
            mCardScrollView.setSelection(1);
            mAdapter.notifyDataSetChanged();
        }
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            text = formatText(results.get(0));
            mCards.set(0, new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(text)
                    .setFootnote("Tap to change text"));
            mAdapter.notifyDataSetChanged();
        }
        if (requestCode == TAKE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            String videoThumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            Bitmap videoThumbnail = BitmapFactory.decodeFile(videoThumbnailPath);
            videoPath = data.getStringExtra(Intents.EXTRA_VIDEO_FILE_PATH);
            mCards.remove(1);
            insertNewCard(mCards.size() - 1, new CardBuilder(this, CardBuilder.Layout.CAPTION)
                    .addImage(videoThumbnail));
        }
        if (requestCode == MEDIA_REQUEST && resultCode == RESULT_OK) {
            if (data.hasExtra("photo")) {
                takePicture();
            } else if (data.hasExtra("video")) {
                takeVideo();
            } else {
                File file = new File(data.getStringExtra("file"));
                int video = 1;
                if (file.getAbsolutePath().endsWith(".mp4")) {
                    videoPath = file.getAbsolutePath();
                    mCards.remove(1);
                    video = 0;
                } else {
                    Bitmap resource = BitmapFactory.decodeFile(file.getAbsolutePath());
                    image.add(resource);
                }
                int finalVideo = video;
                Glide.with(this)
                        .asBitmap()
                        .load(file)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .override(640, 360)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                insertNewCard(mCards.size() - (1 + finalVideo), new CardBuilder(PostActivity.this, CardBuilder.Layout.CAPTION)
                                        .addImage(resource));
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);
        Log.i("Picture Path", picturePath);

        if (pictureFile.exists()) {
            Log.d(TAG, "Picture file detected: " + pictureFile.getAbsolutePath());
            executor.execute(() -> {
                image.add(BitmapFactory.decodeFile(picturePath));
                processArray.remove(picturePath);
                Log.d(TAG, "Picture ready");
                if (processArray.isEmpty()) {
                    mCards.get(mCards.size() - 1).setFootnote("");
                    runOnUiThread(() -> {
                        mAdapter.notifyDataSetChanged();
                    });
                    processing = false;
                    Log.i("Process picture", "Finished processing last image in array");
                } else {
                    processPictureWhenReady(processArray.get(0));
                    Log.i("Process picture", "Moving onto next image"); // TODO: Check if this works properly
                }
            });
        } else {
            Log.d(TAG, "Picture not ready...");
            final File parentDirectory = pictureFile.getParentFile();
            observer = new FileObserver(parentDirectory.getPath(), FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                @Override
                public void onEvent(int event, String path) {
                    if (path == null) return;

                    File affectedFile = new File(parentDirectory, path);
                    if (affectedFile.getAbsolutePath().equals(pictureFile.getAbsolutePath())) {
                        Log.d(TAG, "Picture fully written: " + picturePath);
                        stopWatching();
                        processPictureWhenReady(picturePath);
                    }
                }
            };

            observer.startWatching();
            Log.d(TAG, "Started observing: " + picturePath);
        }
    }
    public static void makeImageRequest(Bitmap bitmap, String auth, Boolean video,  HttpCallback callback) {
        Security.insertProviderAt(Conscrypt.newProvider(), 1); // Enable Conscrypt

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    Debug.enableEmulatorTraceOutput();
                    Debug.startMethodTracing("upload_trace");
                    String url;
                    // Create a custom SSL context that enables multiple TLS protocols
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init((KeyStore) null);
                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

                    // Try to enable TLS protocols manually
                    SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
                    sslContext.init(null, trustManagers, new SecureRandom());

                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                            .hostnameVerifier(OkHostnameVerifier.INSTANCE)
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .retryOnConnectionFailure(true)
                            .connectTimeout(300, TimeUnit.SECONDS)
                            .readTimeout(300, TimeUnit.SECONDS)
                            .writeTimeout(300, TimeUnit.SECONDS);

                    OkHttpClient client = builder.build();
                    RequestBody requestBody;
                    long videoLength;
                    String contentType;
                    if (video) {
                        contentType = "video/mp4";
                        Log.d(TAG, "Video file will be uploaded");
                        url = "https://bsky.social/xrpc/com.atproto.repo.uploadBlob";
                        File videoFile = new File(videoPath);
                        MediaType mediaType = MediaType.parse("video/mp4");
                        // Wrap the video file in a custom RequestBody to track progress
                        requestBody = new RequestBody() {
                            @Override
                            public MediaType contentType() {
                                return mediaType;
                            }

                            @Override
                            public long contentLength() throws IOException {
                                return videoFile.length();
                            }

                            @Override
                            public void writeTo(BufferedSink sink) throws IOException {
                                long fileLength = contentLength();
                                byte[] buffer = new byte[8192]; // 8KB buffer
                                long uploaded = 0;
                                try (FileInputStream fis = new FileInputStream(videoFile);
                                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                                    int read;
                                    while ((read = bis.read(buffer)) != -1) {
                                        uploaded += read;
                                        sink.write(buffer, 0, read);

                                        // Log progress
                                        int progress = (int) ((uploaded * 100) / fileLength);
                                        Log.d(TAG, "Upload progress: " + progress + "%");
                                    }
                                }
                            }
                        };
                    } else {
                        contentType = "image/jpeg";
                        url = "https://bsky.social/xrpc/com.atproto.repo.uploadBlob";
                        Log.d(TAG, "Image file will be uploaded");
                        byte[] imageBytes = bitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, 50);

                        // Define the MediaType for the image
                        MediaType mediaType = MediaType.parse("image/jpeg");

                        // Build the request body with the byte array
                        requestBody = RequestBody.create(mediaType, imageBytes);
                    }

                    Request request = new Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .addHeader("Content-type", contentType)
                            .addHeader("Authorization", "Bearer " + auth)
                            .build();

                    Response response = client.newCall(request).execute();
                    Debug.stopMethodTracing();

                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Successful HTTP Response: " + responseBody);
                        return responseBody;
                    } else {
                        Log.e(TAG, "Unsuccessful HTTP Response Code: " + response.code());
                        Log.e(TAG, "Unsuccessful HTTP Response Message: " + response.message());
                        Log.e(TAG, "Unsuccessful HTTP Response Body: " + responseBody);
                        return null;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Request Error", e);
                    return null;
                }

            }
            @Override
            protected void onPostExecute(String result) {
                if (callback != null) {
                    if (result != null) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(result);
                    }
                }
            }
        }.execute();
    }
    // Helper method to convert Bitmap to Byte Array
    private static byte[] bitmapToByteArray(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(format, quality, outputStream);
        return outputStream.toByteArray();
    }
    public interface HttpCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }
    public static String formatText(String input) {
        input = input.trim();
        if (input.isEmpty()) return input;

        // Capitalize the first character and append the rest of the text
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            event.startTracking();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (thumbnailPath.isEmpty()) {
                takeVideo();
            }
            return true;
        } else {
            return super.onKeyLongPress(keyCode, event);
        }
    }
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (videoPath.isEmpty()) {
                takePicture();
            }
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }
}