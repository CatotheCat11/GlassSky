package com.cato.glasssky;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.github.barcodeeye.scan.CaptureActivity;
import com.google.android.glass.media.Sounds;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class Authenticate extends Activity implements TextToSpeech.OnInitListener {
    String handle;
    String refresh_token;
    String app_password;
    String access_token;
    private TextToSpeech tts;
    private boolean initialized = false;
    private String queuedText;
    private static final int BARCODE_HANDLE_REQUEST = 0;
    private static final int BARCODE_PASSWORD_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        HttpsUtils.makePostRequest("https://api.bsky.app/xrpc/com.atproto.identity.resolveHandle?handle=catothecat.bsky.social", null, null, "GET",
            new HttpsUtils.HttpCallback() {
                @Override
                public void onSuccess(String response) {
                    // Handle successful response
                    Log.d("Response", response);
                }

                @Override
                public void onError(String errorMessage) {
                    if (errorMessage.contains("ExpiredToken")) {
                            Log.e("Session Refresh Error", "The token has expired.");
                            Intent intent = new Intent(MainActivity.this, Authenticate.class);
                            startActivity(intent);
                        } else {
                            Log.e("Session Refresh Error", "An error occurred.");
                            speak("Something went wrong. Try again later.");
                        }
                }
            });
        */

        tts = new TextToSpeech(this, this);

        SharedPreferences sharedPref = Authenticate.this.getSharedPreferences(
                getString(R.string.auth), Context.MODE_PRIVATE);

        handle = sharedPref.getString(getString(R.string.handle), "unset");
        refresh_token = sharedPref.getString(getString(R.string.refresh_token), "unset");
        app_password = sharedPref.getString(getString(R.string.handle), "unset");
        if(handle.equals("unset") | app_password.equals("unset") | refresh_token.equals("unset")) {
            Intent objIntent = CaptureActivity.newIntent(this, true);
            objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(objIntent, BARCODE_HANDLE_REQUEST);
            speak("Scan a QR code with your blue sky handle.");
        } else {
            refreshSession();
            finish();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_HANDLE_REQUEST && resultCode == RESULT_OK) {
            tts.stop();
            String content = data.getStringExtra("result");
            if (content.startsWith("http://")) { // Some QR code generators automatically add http:// to the start, this removes that if it is added
                handle = content.substring(7);
            } else if (content.startsWith("@")) {
                handle = content.substring(1);
            } else {
                handle = content;
            }
            handle = handle.replaceAll("\\p{C}", ""); // Remove any control characters
            HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.identity.resolveHandle?handle=" + handle, null, null, "GET",
                    new HttpsUtils.HttpCallback() {
                        @Override
                        public void onSuccess(String response) {
                            // Handle successful response
                            Intent objIntent = CaptureActivity.newIntent(Authenticate.this, true);
                            objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                            startActivityForResult(objIntent, BARCODE_PASSWORD_REQUEST);
                            speak("Scan a QR code with your app password.");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Handle error
                            Log.e("Error", errorMessage);
                            Intent objIntent = CaptureActivity.newIntent(Authenticate.this, true);
                            objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                            startActivityForResult(objIntent, BARCODE_HANDLE_REQUEST);
                            speak("Error, please try again. Scan a QR code with your blue sky handle.");
                        }
                    });
        }
        if (requestCode == BARCODE_PASSWORD_REQUEST && resultCode == RESULT_OK) {
            tts.stop();
            app_password = data.getStringExtra("result");
            // Create auth session
            JSONObject login = new JSONObject();
            try {
                login.put("identifier", handle);
                login.put("password", app_password);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.createSession", login, null, "POST",
                    new HttpsUtils.HttpCallback() {
                        @Override
                        public void onSuccess(String response) {
                            // Handle successful response
                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            am.playSoundEffect(Sounds.SUCCESS);
                            SharedPreferences sharedPref = Authenticate.this.getSharedPreferences(
                                    getString(R.string.auth), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(getString(R.string.handle), handle);
                            editor.putString(getString(R.string.app_password), app_password);
                            try {
                                editor.putString(getString(R.string.access_token), new JSONObject(response).getString("accessJwt"));
                                editor.putString(getString(R.string.refresh_token), new JSONObject(response).getString("refreshJwt"));
                                editor.apply();
                                speak("Authentication token set");
                                finish();
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Handle error
                            Log.e("Error", errorMessage);
                            Intent objIntent = CaptureActivity.newIntent(Authenticate.this, true);
                            objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                            startActivityForResult(objIntent, BARCODE_HANDLE_REQUEST);
                            speak("Error, please try again. Scan a QR code with your blue sky handle.");
                        }
                    });

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    public void refreshSession() {
        SharedPreferences sharedPref = Authenticate.this.getSharedPreferences(
                getString(R.string.auth), Context.MODE_PRIVATE);
        refresh_token = sharedPref.getString(getString(R.string.refresh_token), "unset");
        HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.refreshSession", null, refresh_token, "POST",
                new HttpsUtils.HttpCallback() {
                    @Override
                    public void onSuccess(String response) {
                        // Handle successful response
                        try {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(getString(R.string.access_token), new JSONObject(response).getString("accessJwt"));
                            editor.putString(getString(R.string.refresh_token), new JSONObject(response).getString("refreshJwt"));
                            editor.apply();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (errorMessage.contains("ExpiredToken")) {
                            Log.e("Session Refresh Error", "The token has expired.");
                            // Attempt to log in again
                            // Create auth session
                            JSONObject login = new JSONObject();
                            try {
                                login.put("identifier", handle);
                                login.put("password", app_password);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                            HttpsUtils.makePostRequest("https://bsky.social/xrpc/com.atproto.server.createSession", login, null, "POST",
                                    new HttpsUtils.HttpCallback() {
                                        @Override
                                        public void onSuccess(String response) {
                                            // Handle successful response
                                            SharedPreferences sharedPref = Authenticate.this.getSharedPreferences(
                                                    getString(R.string.auth), Context.MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPref.edit();
                                            editor.putString(getString(R.string.handle), handle);
                                            editor.putString(getString(R.string.app_password), app_password);
                                            try {
                                                editor.putString(getString(R.string.access_token), new JSONObject(response).getString("accessJwt"));
                                                editor.putString(getString(R.string.refresh_token), new JSONObject(response).getString("refreshJwt"));
                                                editor.apply();
                                                speak("Authentication token set");
                                                finish();
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            // Handle error
                                            Log.e("Error", errorMessage);
                                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                            am.playSoundEffect(Sounds.ERROR);
                                        }
                                    });
                        } else {
                            Log.e("Session Refresh Error", errorMessage);
                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            am.playSoundEffect(Sounds.ERROR);
                        }
                    }
                });
    }

    public void speak(String text) {
        // If not yet initialized, queue up the text.
        if (!initialized) {
            queuedText = text;
            return;
        }
        queuedText = null;
        // Before speaking the current text, stop any ongoing speech.
        tts.stop();
        // Speak the text.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            initialized = true;
            tts.setLanguage(Locale.ENGLISH);

            if (queuedText != null) {
                speak(queuedText);
            }
        }
    }
}
