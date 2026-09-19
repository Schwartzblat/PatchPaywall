package com.paywall;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Method;

public class Utils {
    private static Application application;
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    /**
     * Seeded by {@link PaywallProvider} with the Context the framework hands the
     * provider. The provider runs inside installContentProviders(), before the
     * host Application's onCreate(), and that is early enough that the
     * reflective lookup in {@link #getApplication()} can still come back null.
     * First caller wins.
     */
    public static void setContext(Context value) {
        if (value == null || context != null) {
            return;
        }
        Context applicationContext = value.getApplicationContext();
        context = applicationContext != null ? applicationContext : value;
        if (application == null && context instanceof Application) {
            application = (Application) context;
        }
    }

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
        Context applicationContext = getApplicationContext();
        // A null context means we ran before one existed. Assume a release
        // build rather than throwing out of Paywall's static initialiser, which
        // would surface as an ExceptionInInitializerError inside the provider.
        return applicationContext != null
                && (applicationContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
