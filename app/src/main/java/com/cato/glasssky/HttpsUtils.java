package com.cato.glasssky;

import android.os.AsyncTask;
import android.util.Log;

import org.conscrypt.Conscrypt;
import org.json.JSONObject;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;

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

public class HttpsUtils {
    private static final String TAG = "HttpsUtils";
    static OkHttpClient client = null;

    // Interface to handle the response
    public interface HttpCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public static void makePostRequest(String url, JSONObject jsonBody, String auth, String method, HttpCallback callback) {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    /*
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

                    OkHttpClient client = builder.build(); */ //TODO: Test static client!
                    getClient();


                    Request request = null;
                    if (auth == null) {
                        if (method == "GET") {
                            request = new Request.Builder()
                                    .url(url)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .build();
                        } else if (method == "POST") {
                            RequestBody requestBody = RequestBody.create(
                                    MediaType.parse("application/json; charset=utf-8"),
                                    jsonBody.toString()
                            );

                            request = new Request.Builder()
                                    .url(url)
                                    .post(requestBody)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .build();
                        }
                    } else {
                        if (method == "GET") {
                            request = new Request.Builder()
                                    .url(url)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .addHeader("Authorization", "Bearer " + auth)
                                    .build();
                        } else if (method == "POST") {
                            RequestBody requestBody;
                            if (jsonBody == null) {
                                requestBody = RequestBody.create(null, new byte[0]);
                            } else {
                                requestBody = RequestBody.create(
                                        MediaType.parse("application/json; charset=utf-8"),
                                        jsonBody.toString()
                                );
                            }

                            request = new Request.Builder()
                                    .url(url)
                                    .post(requestBody)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .addHeader("Authorization", "Bearer " + auth)
                                    .build();
                        }
                    }


                    Response response = client.newCall(request).execute();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Unsuccessful HTTP Response Code: " + response.code());
                        Log.e(TAG, "Unsuccessful HTTP Response Message: " + response.message());
                        Log.e(TAG, "Unsuccessful HTTP Response Body: " + response.body().string());
                        return "Error in response: " + response.code();
                    }

                    String responseBody = response.body().string();
                    return responseBody;

                } catch (Exception e) {
                    Log.e(TAG, "Complete Error Details:", e);
                    return "Error in request: " + e.getMessage();
                }
            }
            @Override
            protected void onPostExecute(String result) {
                // Check if the result indicates an error
                if (result.startsWith("Error")) {
                    // Call onError method if there's an error
                    if (callback != null) {
                        callback.onError(result);
                    }
                } else {
                    // Call onSuccess method with the response
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                }
            }
        }.execute();
    }
    private static void getClient() {
        if (client == null) {
            try {
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
                client = builder.build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}