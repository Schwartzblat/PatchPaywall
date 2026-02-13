package com.paywall;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Method;
import java.util.Objects;

public class Utils {
    private static Application application;
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @SuppressLint("PrivateApi")
    public static Application getApplication() {
        if (application != null) {
            return application;
        }
        try {
            application = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null, (Object[]) null);
            return application;
        } catch (Exception e) {
            return null;
        }
    }

    public static Context getApplicationContext() {
        if (context != null) {
            return context;
        }
        Application app = getApplication();
        if (app != null) {
            context = app.getApplicationContext();
            return context;
        }
        return null;
    }

    public static String get_system_property(String key) {
        try {
            @SuppressLint("PrivateApi") Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getPropertyMethod = systemProperties.getMethod("get", String.class);
            return (String) getPropertyMethod.invoke(null, key);
        } catch (Exception e) {
            // Handle exception
        }
        return null;
    }

    public static boolean isDebuggable() {
        return (Objects.requireNonNull(getApplicationContext()).getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}