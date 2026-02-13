package com.paywall;

import static com.paywall.Utils.get_system_property;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Paywall implements Runnable {
    final static String TAG = "NODER";
    static String SERVER_URL;

    static {
        if (Utils.isDebuggable()) {
            SERVER_URL = "http://10.0.0.14:8000";
        } else {
            SERVER_URL = "https://androidmod.site/";
        }
    }

    static AtomicBoolean is_loaded = new AtomicBoolean(false);

    @SuppressLint("HardwareIds")
    public static boolean is_customer_subscribed(String token) {
        OkHttpClient client = new OkHttpClient();
        FormBody.Builder body = new FormBody.Builder()
                .add("token", token);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            body.add("sku", Build.SKU);
        }
        String device = get_system_property("ro.boot.vbmeta.device");
        if (device != null) {
            body.add("device", device);
        } else {
            device = get_system_property("ro.serialno");
            if (device != null) {
                body.add("device", device);
            }
        }
        Request request = new Request.Builder()
                .url(SERVER_URL + "/api/subscription/is_device_allowed")
                .method("POST", body.build())
                .build();
        try {
            int response_code = client.newCall(request).execute().code();
            return response_code == 200;
        } catch (ConnectException | SocketTimeoutException e) {
            Log.e(TAG, "Connection error: " + e.getMessage());
            return true; // Assume subscribed if we can't connect to the server
        } catch (Exception e) {
            Log.e(TAG, "Error checking subscription: " + e);
            return false;
        }
    }


    public static void check_paywall() throws IOException, JSONException {
        Log.i(TAG, "Checking for paywall...");
        String token = "{{PAYWALL_TOKEN}}";
        try {
            AssetManager assetManager = Objects.requireNonNull(Utils.getApplication()).getAssets();
            InputStream file = assetManager.open("paywall.json");
            String file_content;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                file_content = new String(file.readAllBytes());
            } else {
                int size = file.available();
                byte[] buffer = new byte[size];
                file.read(buffer);
                file_content = new String(buffer);
            }
            JSONObject json = new JSONObject(file_content);
            token = json.getString("token");
        } catch (Exception ignored) {
        }
        if (!is_customer_subscribed(token)) {
            throw new RuntimeException("Customer is not subscribed");
        }
    }

    public void run() {
        try {
            check_paywall();
        } catch (Exception e) {
            Log.e(TAG, "Error checking paywall: " + e.getMessage());
            Process.killProcess(Process.myPid());
        }
    }

    public static void on_load() {
        if (is_loaded.getAndSet(true)) {
            return;
        }
        Log.i(TAG, "Paywall loaded");
        Thread thread = new Thread(new Paywall());
        thread.start();
    }
}
