package com.example.e2performance;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Background service to warm up Cordova WebView pool
 * Pre-creates SystemWebViews that will be wrapped with Cordova later
 */
public class WarmupService extends IntentService {
    private static final String TAG = "WarmupService";
    
    public WarmupService() {
        super("WarmupService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "WarmupService started - warming up Cordova WebView pool");
        
        try {
            // Get CordovaWebViewPool instance and warm it up
            CordovaWebViewPool pool = CordovaWebViewPool.getInstance(getApplicationContext());
            pool.warmUp();
            
            Log.d(TAG, "✓ Cordova WebView pool warmed up successfully. Pool size: " + pool.getPoolSize());
        } catch (Exception e) {
            Log.e(TAG, "Error warming up Cordova WebView pool", e);
        }
    }
}
