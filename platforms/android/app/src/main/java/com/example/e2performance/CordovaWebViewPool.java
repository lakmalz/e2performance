package com.example.e2performance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.apache.cordova.engine.SystemWebView;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Pool for pre-warming SystemWebViews (Cordova's underlying WebView)
 * These will be wrapped with CordovaWebView for full plugin support
 * WebViews are created on the UI thread to avoid threading issues
 */
public class CordovaWebViewPool {
    private static final String TAG = "CordovaWebViewPool";
    private static final int POOL_SIZE = 1;
    private static final String WARMUP_URL = "file:///android_asset/www/index.html";

    private static CordovaWebViewPool instance;
    private final ConcurrentLinkedQueue<SystemWebView> availableWebViews;
    private final Context applicationContext;
    private final Handler mainHandler;

    private CordovaWebViewPool(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.availableWebViews = new ConcurrentLinkedQueue<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
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
        
        // Use CountDownLatch to wait for UI thread operations to complete
        final CountDownLatch latch = new CountDownLatch(POOL_SIZE);
        
        for (int i = 0; i < POOL_SIZE; i++) {
            final int index = i + 1;
            
            // WebViews MUST be created on the UI thread
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        SystemWebView systemWebView = new SystemWebView(applicationContext);
                        
                        systemWebView.getSettings().setJavaScriptEnabled(true);
                        systemWebView.getSettings().setDomStorageEnabled(true);
                        systemWebView.getSettings().setDatabaseEnabled(true);
                        systemWebView.getSettings().setAllowFileAccess(true);
                        systemWebView.getSettings().setAllowContentAccess(true);
                        
                        systemWebView.loadUrl(WARMUP_URL);
                        
                        availableWebViews.offer(systemWebView);
                        Log.d(TAG, "SystemWebView " + index + " warmed up on UI thread");
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating SystemWebView", e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        // Wait for all WebViews to be created (max 5 seconds)
        try {
            latch.await(5, TimeUnit.SECONDS);
            Log.d(TAG, "Pool warmed up. Size: " + availableWebViews.size());
        } catch (InterruptedException e) {
            Log.e(TAG, "Warmup interrupted", e);
        }
    }

    public SystemWebView acquire() {
        SystemWebView webView = availableWebViews.poll();
        if (webView == null) {
            Log.w(TAG, "Pool empty, creating new SystemWebView on-demand");
            // Must be called from UI thread (typically is, since this is called from Activity onCreate)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                webView = new SystemWebView(applicationContext);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadUrl(WARMUP_URL);
            } else {
                Log.e(TAG, "Cannot create WebView on non-UI thread!");
                return null;
            }
        } else {
            Log.d(TAG, "SystemWebView acquired from pool. Remaining: " + availableWebViews.size());
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
