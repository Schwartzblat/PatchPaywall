package com.paywall;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Entry point of the paywall module.
 *
 * Stitch registers the generated subclass of this class in the target app's
 * AndroidManifest:
 *
 *   <provider android:name="com.paywall.InitProviderPaywall<App>"
 *             android:authorities="<target package>.com.paywall.InitProviderPaywall<App>"
 *             android:exported="false"
 *             android:initOrder="2147483647"/>
 *
 * Stitch scopes the authority to the target's package, so Android's
 * device-wide uniqueness rule is already satisfied. The class name varies per
 * app anyway: the subclass is generated at build time from the
 * `paywallProviderClass` Gradle property, which the patcher supplies. See
 * README.md.
 *
 * initOrder is the highest possible value, so onCreate() runs before any other
 * provider and before the host Application's onCreate(). Two consequences:
 *
 *   - Nothing may escape onCreate(). An exception here kills the host app
 *     before a single line of its own code has run.
 *   - The host Application has been constructed but not created, so its DI
 *     graph and SDK bootstraps have not run. Do not touch host classes here.
 */
public abstract class PaywallProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        try {
            Log.i(Paywall.TAG, getClass().getSimpleName() + ": onCreate");
            // The framework hands us a Context directly. Reaching for
            // ActivityThread.currentApplication() this early is a coin flip.
            Utils.setContext(getContext());
            Paywall.on_load();
        } catch (Throwable throwable) {
            // A paywall that cannot start must not turn into a crash at launch
            // for a paying customer.
            Log.e(Paywall.TAG, "Paywall failed to start: " + throwable);
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
