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
                        SystemWebView systemWebView = createConfiguredWebView();
                        availableWebViews.offer(systemWebView);
                        Log.d(TAG, "SystemWebView " + index + " warmed up on UI thread");
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating SystemWebView " + index, e);
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

    /**
     * Acquire a pre-warmed SystemWebView from the pool
     * @return SystemWebView instance, or null if pool is empty and not on UI thread
     */
    public SystemWebView acquire() {
        SystemWebView webView = availableWebViews.poll();
        
        // Check if pooled WebView is still alive
        if (webView != null) {
            try {
                webView.getUrl(); // Test if alive
                Log.d(TAG, "SystemWebView acquired from pool. Remaining: " + availableWebViews.size());
                return webView;
            } catch (Exception e) {
                Log.w(TAG, "Pooled WebView was destroyed, creating new one", e);
                webView = null; // Force creation of new WebView
            }
        }
        
        if (webView == null) {
            Log.d(TAG, "Pool empty or destroyed, creating new SystemWebView on-demand");
            
            // WebView creation MUST happen on UI thread
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.e(TAG, "Cannot create WebView on non-UI thread! Current thread: " + Thread.currentThread().getName());
                return null;
            }
            
            try {
                webView = createConfiguredWebView();
                Log.d(TAG, "Created new on-demand SystemWebView");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create on-demand WebView", e);
                return null;
            }
        } else {
            Log.d(TAG, "SystemWebView acquired from pool. Remaining: " + availableWebViews.size());
        }
        
        return webView;
    }
    
    /**
     * Create and configure a new SystemWebView with standard settings
     */
    private SystemWebView createConfiguredWebView() {
        SystemWebView webView = new SystemWebView(applicationContext);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.loadUrl(WARMUP_URL);
        return webView;
    }

    /**
     * Release a SystemWebView back to the pool for reuse
     * @param webView The SystemWebView to release
     */
    public void release(SystemWebView webView) {
        if (webView == null) {
            Log.w(TAG, "Attempted to release null WebView");
            return;
        }
        
        try {
            // Check if WebView is already destroyed
            try {
                webView.getUrl(); // Test if WebView is still alive
            } catch (Exception e) {
                Log.e(TAG, "WebView already destroyed, cannot reuse", e);
                return;
            }
            
            // DON'T call any WebView methods here - it can destroy the renderer!
            // Just add it back to pool as-is, we'll reset it when acquiring
            Log.d(TAG, "Returning WebView to pool (no cleanup to keep it alive)");
            
            if (availableWebViews.size() < POOL_SIZE) {
                availableWebViews.offer(webView);
                Log.d(TAG, "SystemWebView returned to pool (live and ready). Current size: " + availableWebViews.size());
            } else {
                Log.d(TAG, "Pool full, destroying excess WebView");
                webView.destroy();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing SystemWebView, destroying it", e);
            try {
                webView.destroy();
            } catch (Exception destroyError) {
                Log.e(TAG, "Error destroying WebView", destroyError);
            }
        }
    }

    public int getPoolSize() {
        return availableWebViews.size();
    }
}
