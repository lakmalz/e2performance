package com.example.e2performance;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.os.Build;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WebView Pool Manager
 * Pre-creates and manages a pool of warm WebViews for optimal performance
 * Uses MutableContextWrapper to safely rebind context and avoid memory leaks
 * Pre-loads index.html to warm up actual app content
 */
public class WebViewPool {
    private static final String TAG = "WebViewPool";
    private static final int POOL_SIZE = 2;
    // Use index.html to pre-load actual app content for maximum performance
    private static final String WARMUP_URL = "file:///android_asset/www/index.html";

    private static WebViewPool instance;
    private final ConcurrentLinkedQueue<WebView> availableWebViews;
    private final Context applicationContext;

    private WebViewPool(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.availableWebViews = new ConcurrentLinkedQueue<>();
        Log.d(TAG, "WebViewPool initialized");
    }

    public static synchronized WebViewPool getInstance(Context context) {
        if (instance == null) {
            instance = new WebViewPool(context);
        }
        return instance;
    }

    /**
     * Pre-create WebViews in the pool
     * Should be called from background thread
     */
    public void warmUp() {
        Log.d(TAG, "Starting WebView warmup with index.html, creating " + POOL_SIZE + " instances");
        
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                WebView webView = createWebView();
                availableWebViews.offer(webView);
                Log.d(TAG, "WebView " + (i + 1) + " created with index.html and added to pool");
            } catch (Exception e) {
                Log.e(TAG, "Error creating WebView during warmup", e);
            }
        }
        
        Log.d(TAG, "WebView warmup completed with index.html. Pool size: " + availableWebViews.size());
    }

    /**
     * Get a pre-warmed WebView from the pool
     * If pool is empty, creates a new one
     */
    public WebView acquire(Context activityContext) {
        WebView webView = availableWebViews.poll();
        
        if (webView == null) {
            Log.d(TAG, "Pool empty, creating new WebView");
            webView = createWebView();
        } else {
            Log.d(TAG, "WebView acquired from pool (index.html pre-loaded). Remaining: " + availableWebViews.size());
        }

        // Rebind to activity context using MutableContextWrapper
        MutableContextWrapper contextWrapper = (MutableContextWrapper) webView.getContext();
        contextWrapper.setBaseContext(activityContext);
        
        return webView;
    }

    /**
     * Return a WebView to the pool for reuse
     */
    public void release(WebView webView) {
        if (webView == null) {
            return;
        }

        try {
            // Clear the WebView state
            webView.clearHistory();
            webView.clearCache(true);
            // Reload index.html to prepare for next use
            webView.loadUrl(WARMUP_URL);
            
            // Rebind to application context
            MutableContextWrapper contextWrapper = (MutableContextWrapper) webView.getContext();
            contextWrapper.setBaseContext(applicationContext);
            
            // Return to pool if not full
            if (availableWebViews.size() < POOL_SIZE) {
                availableWebViews.offer(webView);
                Log.d(TAG, "WebView returned to pool. Pool size: " + availableWebViews.size());
            } else {
                webView.destroy();
                Log.d(TAG, "Pool full, WebView destroyed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing WebView", e);
        }
    }

    /**
     * Create a new WebView with optimal settings
     */
    private WebView createWebView() {
        // Use MutableContextWrapper to allow context rebinding
        MutableContextWrapper contextWrapper = new MutableContextWrapper(applicationContext);
        WebView webView = new WebView(contextWrapper);
        
        // Configure WebView settings for optimal performance
        configureWebView(webView);
        
        // Load warmup URL to initialize the engine
        webView.loadUrl(WARMUP_URL);
        
        return webView;
    }

    /**
     * Configure WebView with optimal performance settings
     */
    private void configureWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        
        // Enable JavaScript
        settings.setJavaScriptEnabled(true);
        
        // Enable DOM storage
        settings.setDomStorageEnabled(true);
        
        // Enable database
        settings.setDatabaseEnabled(true);
        
        // Enable caching
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Enable hardware acceleration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        }
        
        // Allow file access
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // Enable zoom controls
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        
        // Set user agent
        settings.setUserAgentString(settings.getUserAgentString() + " E2Performance/1.0");
        
        // Enable mixed content (if needed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        
        Log.d(TAG, "WebView configured with optimal settings");
    }

    /**
     * Clear the entire pool and destroy all WebViews
     * Should be called when app is being terminated
     */
    public void destroy() {
        Log.d(TAG, "Destroying WebView pool");
        
        WebView webView;
        while ((webView = availableWebViews.poll()) != null) {
            try {
                webView.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying WebView", e);
            }
        }
        
        Log.d(TAG, "WebView pool destroyed");
    }

    /**
     * Get current pool size
     */
    public int getPoolSize() {
        return availableWebViews.size();
    }
}
