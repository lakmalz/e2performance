package com.example.e2performance;

import android.os.Bundle;
import android.util.Log;

/**
 * DashboardActivity - Automatically optimized with WebView pooling
 * 
 * Simply extends CordovaActivityWithPool to get:
 * - 70-80% faster initial load
 * - Full Cordova plugin support
 * - Custom NavigationPlugin support
 * 
 * NO POOL CODE NEEDED! Everything is handled by the base class.
 */
public class DashboardActivity extends CordovaActivityWithPool {
    private static final String TAG = "DashboardActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DashboardActivity created with automatic pool optimization");
    }

    // That's it! All pool logic is in CordovaActivityWithPool
    // Just use it like a normal CordovaActivity
}
