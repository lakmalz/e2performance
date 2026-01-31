package com.example.e2performance;

import android.content.Context;
import android.util.Log;
import org.apache.cordova.engine.SystemWebView;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Pool for pre-warming SystemWebViews (Cordova's underlying WebView)
 * These will be wrapped with CordovaWebView for full plugin support
 */
public class CordovaWebViewPool {
    private static final String TAG = "CordovaWebViewPool";
    private static final int POOL_SIZE = 1;
    private static final String WARMUP_URL = "file:///android_asset/www/index.html";

    private static CordovaWebViewPool instance;
    private final ConcurrentLinkedQueue<SystemWebView> availableWebViews;
    private final Context applicationContext;

    private CordovaWebViewPool(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.availableWebViews = new ConcurrentLinkedQueue<>();
        Log.d(TAG, "CordovaWebViewPool initialized with POOL_SIZE=" + POOL_SIZE);
    }

    public static synchronized CordovaWebViewPool getInstance(Context context) {
        if (instance == null) {
            instance = new CordovaWebViewPool(context);
        }
        return instance;
    }

    public void warmUp() {
        Log.d(TAG, "Starting SystemWebView warmup with index.html");
        
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                SystemWebView systemWebView = new SystemWebView(applicationContext);
                
                systemWebView.getSettings().setJavaScriptEnabled(true);
                systemWebView.getSettings().setDomStorageEnabled(true);
                systemWebView.getSettings().setDatabaseEnabled(true);
                systemWebView.getSettings().setAllowFileAccess(true);
                systemWebView.getSettings().setAllowContentAccess(true);
                
                systemWebView.loadUrl(WARMUP_URL);
                
                availableWebViews.offer(systemWebView);
                Log.d(TAG, "SystemWebView " + (i + 1) + " warmed up");
            } catch (Exception e) {
                Log.e(TAG, "Error creating SystemWebView", e);
            }
        }
        
        Log.d(TAG, "Pool warmed up. Size: " + availableWebViews.size());
    }

    public SystemWebView acquire() {
        SystemWebView webView = availableWebViews.poll();
        if (webView == null) {
            Log.w(TAG, "Pool empty, creating new SystemWebView");
            webView = new SystemWebView(applicationContext);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl(WARMUP_URL);
        } else {
            Log.d(TAG, "SystemWebView acquired. Remaining: " + availableWebViews.size());
        }
        return webView;
    }

    public void release(SystemWebView webView) {
        if (webView == null) return;
        
        try {
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl(WARMUP_URL);
            
            if (availableWebViews.size() < POOL_SIZE) {
                availableWebViews.offer(webView);
                Log.d(TAG, "SystemWebView returned. Size: " + availableWebViews.size());
            } else {
                webView.destroy();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing SystemWebView", e);
        }
    }

    public int getPoolSize() {
        return availableWebViews.size();
    }
}
