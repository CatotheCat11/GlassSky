package com.cato.glasssky;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.cato.glasssky.R;
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
import java.util.List;
import java.util.Locale;
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

    private List<CardBuilder> mCards;
    private static CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    private FileObserver observer;
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    Bitmap bitmap;
    String text = "";
    String thumbnailPath = "";
    static String videoPath = "";
    Bitmap image = null;
    String parenturi = "";
    String parentcid = "";
    String rooturi = "";
    String rootcid = "";
    static String did;
    private static FileInputStream fileInputStream;
    private static BufferedInputStream bufferedInputStream;
    private static final AtomicReference<Exception> uploadError = new AtomicReference<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                .setText("Add a photo")
                .setFootnote("Takes a photo with the camera")
                .setIcon(R.drawable.add_a_photo_64));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Add a video")
                .setFootnote("Takes a video with the camera")
                .setIcon(R.drawable.videocam_64));
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
    }
    private void setupClickListener() {
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);
                if (position == 0) {
                    displaySpeechRecognizer();
                }
                if (position == 1) {
                    if (videoPath.isEmpty()) {
                        takePicture();
                    }
                }
                if (position == 2) {
                    if (thumbnailPath.isEmpty()) {
                        takeVideo();
                        }
                }
                if (position == 3) {
                    if (!thumbnailPath.isEmpty() || !videoPath.isEmpty() || !text.isEmpty()) {
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
    private void post(String text, Bitmap image, String video) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mIndeterminate = mSlider.startIndeterminate();
        Log.i(TAG, "Posting...");
        mCards.get(3).setText("Posting");
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
                            makeImageRequest(image, access_token, false, new HttpCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    Log.i("Success", "Success");
                                    try {
                                        JSONObject blob = new JSONObject(response).getJSONObject("blob");
                                        JSONObject imageObj = new JSONObject();
                                        imageObj.put("alt", "");
                                        imageObj.put("image", blob);
                                        JSONArray images = new JSONArray().put(imageObj);
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
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Log.i("Error", "Error");
                                }
                            });
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
                                                        Log.d("Success", "Successfully posted!");
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
                                    Log.i("Error", "Error");
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
                                                Log.d("Success", "Successfully posted!");
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
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

            processPictureWhenReady(picturePath);
            Bitmap thumbnail = BitmapFactory.decodeFile(thumbnailPath);
            mCards.set(1, new CardBuilder(this, CardBuilder.Layout.CAPTION)
                    .addImage(thumbnail)
                    .setFootnote("Tap to take a new picture"));
            mCards.get(2).setFootnote("Cannot have an image and video at the same time");
            mCards.get(3).setFootnote("Picture is processing...");
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
            mCards.set(2, new CardBuilder(this, CardBuilder.Layout.CAPTION)
                    .addImage(videoThumbnail)
                    .setFootnote("Tap to record a new video"));
            mCards.get(1).setFootnote("Cannot have an image and video at the same time");
            mAdapter.notifyDataSetChanged();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            Log.d(TAG, "Picture file detected: " + pictureFile.getAbsolutePath());

            image = BitmapFactory.decodeFile(picturePath);
            mCards.get(3).setFootnote("");
            mAdapter.notifyDataSetChanged();
            if (image == null) {
                Log.e(TAG, "Failed to decode bitmap. File may still be incomplete.");
                return;
            }
            Log.i(TAG, "Picture ready");
        } else {
            Log.i(TAG, "Picture not ready...");
            final File parentDirectory = pictureFile.getParentFile();
            observer = new FileObserver(parentDirectory.getPath(), FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    Log.d(TAG, "Event received: " + event + " Path: " + path);
                    if (!isFileWritten) {
                        File affectedFile = new File(parentDirectory, path);
                        Log.d(TAG, "Affected file path: " + affectedFile.getAbsolutePath());
                        Log.d(TAG, "Expected file path: " + pictureFile.getAbsolutePath());

                        if (affectedFile.getAbsolutePath().equals(pictureFile.getAbsolutePath())) {
                            Log.d(TAG, "Picture fully written detected.");
                            isFileWritten = true;
                            stopWatching();

                            runOnUiThread(() -> processPictureWhenReady(picturePath));
                        }
                    }
                }
            };
            Log.d(TAG, "Observing directory for file changes...");
            observer.startWatching();
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
                            .retryOnConnectionFailure(true);

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
                        Log.e("URL", url);
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