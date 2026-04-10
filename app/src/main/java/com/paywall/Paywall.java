package com.paywall;

import static com.paywall.Utils.get_system_property;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class Paywall implements Runnable {
    final static String TAG = "NODER";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final String SUBSCRIPTION_ENDPOINT = "/api/subscription/is_device_allowed";
    private static final String DEFAULT_TOKEN = "{{PAYWALL_TOKEN}}";
    static String SERVER_URL;

    static {
        if (Utils.isDebuggable()) {
            SERVER_URL = "http://10.0.0.14:8000";
        } else {
            SERVER_URL = "https://androidmod.site";
        }
    }

    static AtomicBoolean is_loaded = new AtomicBoolean(false);

    private static void append_form_field(StringBuilder body, String key, String value) throws IOException {
        if (body.length() > 0) {
            body.append("&");
        }
        body.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
        body.append("=");
        body.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
    }

    private static String resolve_device_id() {
        String device = get_system_property("ro.boot.vbmeta.device");
        return device != null ? device : get_system_property("ro.serialno");
    }

    @SuppressLint("HardwareIds")
    private static byte[] build_subscription_payload(String token) throws IOException {
        StringBuilder body = new StringBuilder();
        append_form_field(body, "token", token);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            append_form_field(body, "sku", Build.SKU);
        }

        String device = resolve_device_id();
        if (device != null) {
            append_form_field(body, "device", device);
        }

        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static boolean post_subscription_check(byte[] post_data) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SERVER_URL + SUBSCRIPTION_ENDPOINT).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setFixedLengthStreamingMode(post_data.length);

            try (OutputStream output_stream = connection.getOutputStream()) {
                output_stream.write(post_data);
            }

            int response_code = connection.getResponseCode();
            if (response_code != 200) {
                Log.w(TAG, "Subscription check failed with code: " + response_code);
            }
            return response_code == 200;
        } finally {
            connection.disconnect();
        }
    }

    private static String load_token_from_assets() {
        try {
            AssetManager assetManager = Objects.requireNonNull(Utils.getApplication()).getAssets();
            try (InputStream file = assetManager.open("paywall.json")) {
                String file_content = new String(read_all_bytes_compat(file), StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(file_content);
                return json.getString("token");
            }
        } catch (Exception ignored) {
            return DEFAULT_TOKEN;
        }
    }

    private static byte[] read_all_bytes_compat(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        int bytes_read;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            while ((bytes_read = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytes_read);
            }
            return output.toByteArray();
        }
    }

    @SuppressLint("HardwareIds")
    public static boolean is_customer_subscribed(String token) {
        try {
            return post_subscription_check(build_subscription_payload(token));
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
        String token = load_token_from_assets();
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
