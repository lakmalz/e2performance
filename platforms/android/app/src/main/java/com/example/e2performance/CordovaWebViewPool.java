package com.example.e2performance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

/**
 * OPTION A: Pool for pre-warming complete Cordova stack
 * - SystemWebView (UI component) with MutableContextWrapper
 * - SystemWebViewEngine (wrapper)
 * - CordovaWebViewImpl (Cordova integration)
 * 
 * Enables reuse WITHOUT init() or loadUrl() for 95% faster performance
 * 
 * KEY TECHNIQUE: MutableContextWrapper
 * - WebView created with Application context wrapped in MutableContextWrapper
 * - When Activity starts: contextWrapper.setBaseContext(activity)
 * - This rebinds WebView to Activity's context for proper lifecycle
 */
public class CordovaWebViewPool {
    private static final String TAG = "CordovaWebViewPool";
    private static final int POOL_SIZE = 1;
    private static final String WARMUP_URL = "file:///android_asset/www/index.html";

    private static CordovaWebViewPool instance;
    private final Context applicationContext;
    private final Handler mainHandler;
    
    // OPTION A: Store complete pre-warmed Cordova stack
    private SystemWebView preWarmedWebView;
    private SystemWebViewEngine preWarmedEngine;
    private CordovaWebViewImpl preWarmedCordovaWebView;
    private MutableContextWrapper mutableContextWrapper;  // CRITICAL for context switching
    private ConfigXmlParser configParser;  // Store for later plugin initialization in Activity
    private boolean componentsReady = false;

    private CordovaWebViewPool(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "CordovaWebViewPool initialized for OPTION A");
    }

    public static synchronized CordovaWebViewPool getInstance(Context context) {
        if (instance == null) {
            instance = new CordovaWebViewPool(context);
        }
        return instance;
    }

    /**
     * OPTION A: Warm up complete Cordova stack for maximum reuse
     * CRITICAL FIX: NO plugin initialization here (no Activity = would crash)
     * Plugins will be initialized later in Activity with real context
     */
    public void warmUp() {
        Log.d(TAG, "OPTION A: Starting warmup (WebView + Engine + CordovaWebView WITHOUT plugins)");
        
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Parse config.xml and store for later plugin initialization
                    configParser = new ConfigXmlParser();
                    configParser.parse(applicationContext);
                    Log.d(TAG, "✓ Parsed config.xml");
                    
                    // Create SystemWebView
                    preWarmedWebView = createConfiguredWebView();
                    Log.d(TAG, "✓ SystemWebView created");
                    
                    // Create Engine wrapper
                    preWarmedEngine = new SystemWebViewEngine(preWarmedWebView);
                    Log.d(TAG, "✓ SystemWebViewEngine created");
                    
                    // Create CordovaWebView (NO plugins yet - will init in Activity)
                    preWarmedCordovaWebView = new CordovaWebViewImpl(preWarmedEngine);
                    Log.d(TAG, "✓ CordovaWebViewImpl created (NO plugins - will init in Activity)");
                    
                    // Load index.html ONCE
                    preWarmedWebView.loadUrl(WARMUP_URL);
                    Log.d(TAG, "✓ Loaded: " + WARMUP_URL);
                    
                    componentsReady = true;
                    Log.d(TAG, "✓ OPTION A: Pool ready (plugins will init in Activity with real context)");
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to warm up Cordova stack", e);
                    componentsReady = false;
                }
            }
        });
    }
    
    /**
     * Check if complete pre-warmed components are available
     */
    public boolean hasPreWarmedComponents() {
        return componentsReady && 
               preWarmedWebView != null && 
               preWarmedEngine != null && 
               preWarmedCordovaWebView != null;
    }
    
    /**
     * Get pre-warmed SystemWebView
     */
    public SystemWebView getPreWarmedWebView() {
        return preWarmedWebView;
    }
    
    /**
     * Get pre-warmed SystemWebViewEngine
     */
    public SystemWebViewEngine getPreWarmedEngine() {
        return preWarmedEngine;
    }
    
    /**
     * Get pre-warmed CordovaWebViewImpl
     */
    public CordovaWebViewImpl getPreWarmedCordovaWebView() {
        return preWarmedCordovaWebView;
    }

    /**
     * Create and configure a new SystemWebView with MutableContextWrapper
     * 
     * CRITICAL: Uses MutableContextWrapper so context can be switched later
     * - Initial context: Application context (for warmup)
     * - Later: Activity context (via setBaseContext in Activity)
     */
    private SystemWebView createConfiguredWebView() {
        // Create wrapper WITHOUT base context initially
        // Context will be set in Activity via setBaseContext()
        mutableContextWrapper = new MutableContextWrapper(null);
        mutableContextWrapper.setBaseContext(applicationContext);
        
        // Create WebView with wrapper context
        SystemWebView webView = new SystemWebView(mutableContextWrapper);
        
        // Configure WebView settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        
        Log.d(TAG, "✓ WebView created with MutableContextWrapper (ready for context switch)");
        return webView;
    }
    
    /**
     * Get the MutableContextWrapper for context switching
     * USAGE: Call this in Activity, then call setBaseContext(activity)
     */
    public MutableContextWrapper getMutableContextWrapper() {
        return mutableContextWrapper;
    }
    
    public ConfigXmlParser getConfigParser() {
        return configParser;
    }

    /**
     * Release components back to pool
     * For single-use: marks components as unavailable
     * For multi-use: could reset state and keep available
     */
    public void release(SystemWebView webView) {
        if (webView == null) {
            Log.w(TAG, "Attempted to release null WebView");
            return;
        }
        
        // OPTION A: Single-use strategy - clear components
        if (webView == preWarmedWebView) {
            Log.d(TAG, "OPTION A: Clearing pre-warmed components (single-use strategy)");
            componentsReady = false;
            preWarmedWebView = null;
            preWarmedEngine = null;
            preWarmedCordovaWebView = null;
            mutableContextWrapper = null;
            configParser = null;
        }
        
        Log.d(TAG, "Components released (will need to re-warm for next use)");
    }
    
    public int getPoolSize() {
        return componentsReady ? 1 : 0;
    }
}
