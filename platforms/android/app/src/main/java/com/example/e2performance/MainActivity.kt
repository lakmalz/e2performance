package com.example.e2performance

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import org.apache.cordova.CordovaActivity
import org.apache.cordova.engine.SystemWebView
import org.apache.cordova.engine.SystemWebViewEngine

/**
 * MainActivity - CordovaActivity with Preload Support
 * 
 * PURPOSE:
 * 1. Load index.html during preload (hidden)
 * 2. Show instantly when login succeeds
 * 3. Navigate using hash routes
 * 4. Support HARD LOGOUT with complete cleanup
 * 
 * LAUNCH MODE: singleTask
 * - Only one instance exists
 * - Reuses existing instance via onNewIntent
 * - Critical for preload architecture
 * 
 * LIFECYCLE:
 * 1. onCreate (preload mode) - Load index.html, stay hidden
 * 2. onNewIntent (show mode) - Make visible, navigate to hash
 * 3. onDestroy (logout) - Clean up everything
 * 
 * WHY CordovaActivity:
 * - Full Cordova plugin support
 * - Standard lifecycle management
 * - Compatible with existing plugins
 */
class MainActivity : CordovaActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        
        // Intent extras
        const val EXTRA_PRELOAD_MODE = "preload_mode"
        const val EXTRA_SHOW_NOW = "show_now"
        const val EXTRA_HASH = "hash"
        
        // JavaScript interface names
        private const val JS_INTERFACE_LOGOUT = "NativeLogout"
        private const val JS_INTERFACE_PRELOAD = "NativePreload"
        
        // Preload timeout
        private const val PRELOAD_TIMEOUT_MS = 15000L
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var isPreloadMode = false
    private var isContentReady = false
    private var preloadTimeoutRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Determine if this is preload mode BEFORE super.onCreate
        isPreloadMode = intent.getBooleanExtra(EXTRA_PRELOAD_MODE, false)
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "MainActivity onCreate")
        Log.d(TAG, "Preload mode: $isPreloadMode")
        Log.d(TAG, "═══════════════════════════════════════")
        
        if (isPreloadMode) {
            // CRITICAL: Apply hidden theme for preload
            // This prevents any visible flash
            applyHiddenMode()
        }
        
        super.onCreate(savedInstanceState)
        
        // Register with manager
        CordovaRuntimeManager.registerMainActivity(this)
        
        // Initialize Cordova
        init()
        
        // Add JavaScript interfaces
        addLogoutInterface()
        addPreloadInterface()
        
        // Load the URL
        loadUrl(launchUrl)
        
        // Set preload timeout
        if (isPreloadMode) {
            startPreloadTimeout()
        }
        
        Log.d(TAG, "Loading: $launchUrl")
    }
    
    /**
     * Apply hidden mode for preload
     * 
     * WHY:
     * - Activity must exist to load WebView
     * - But must be invisible to user
     * - Don't use alpha - causes black flash
     * - Just make it not focusable so it stays behind
     */
    private fun applyHiddenMode() {
        Log.d(TAG, "Applying hidden mode for preload")
        
        // Make window not focusable so it doesn't steal focus from LoginActivity
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        
        // The activity will be behind LoginActivity due to task ordering
        // No need for alpha or size changes
    }
    
    /**
     * Restore visible mode when showing
     */
    private fun restoreVisibleMode() {
        Log.d(TAG, "Restoring visible mode")
        
        // Clear the not focusable flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        
        // Ensure WebView is visible and focusable
        appView?.view?.visibility = View.VISIBLE
        appView?.view?.isFocusable = true
        appView?.view?.isFocusableInTouchMode = true
        appView?.view?.requestFocus()
    }
    
    /**
     * Add JavaScript interface for logout callback
     * 
     * WHY:
     * - Web content can trigger native logout
     * - Clean separation of concerns
     * - Secure (native handles cleanup)
     */
    private fun addLogoutInterface() {
        val webView = getWebView()
        webView?.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun logout() {
                Log.d(TAG, "Logout requested from JavaScript")
                handler.post {
                    performHardLogout()
                }
            }
        }, JS_INTERFACE_LOGOUT)
        
        Log.d(TAG, "Logout interface added: $JS_INTERFACE_LOGOUT")
    }
    
    /**
     * Add JavaScript interface for preload ready signal
     * 
     * WHY:
     * - JavaScript signals when content is truly ready
     * - More reliable than page load events
     * - Called from deviceready handler
     */
    private fun addPreloadInterface() {
        val webView = getWebView()
        webView?.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun ready() {
                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "NativePreload.ready() called from JavaScript!")
                Log.d(TAG, "═══════════════════════════════════════")
                
                handler.post {
                    isContentReady = true
                    
                    // Cancel timeout
                    preloadTimeoutRunnable?.let { handler.removeCallbacks(it) }
                    
                    // Mark preload as ready
                    CordovaRuntimeManager.markPreloadReady()
                }
            }
        }, JS_INTERFACE_PRELOAD)
        
        Log.d(TAG, "Preload interface added: $JS_INTERFACE_PRELOAD")
    }
    
    /**
     * Start preload timeout
     * 
     * WHY:
     * - Prevent infinite preload state
     * - Mark as failed if taking too long
     * - Allow fallback to fresh load
     */
    private fun startPreloadTimeout() {
        preloadTimeoutRunnable = Runnable {
            if (!isContentReady) {
                Log.e(TAG, "Preload timeout! Marking as failed.")
                CordovaRuntimeManager.markPreloadFailed("Timeout after ${PRELOAD_TIMEOUT_MS}ms")
            }
        }
        handler.postDelayed(preloadTimeoutRunnable!!, PRELOAD_TIMEOUT_MS)
    }
    
    /**
     * Called when Cordova fires deviceready
     * 
     * Override to detect when content is truly ready
     */
    override fun onReceivedError(errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(errorCode, description, failingUrl)
        Log.e(TAG, "WebView error: $errorCode - $description")
        CordovaRuntimeManager.markPreloadFailed(description ?: "Unknown error")
    }
    
    /**
     * Handle new intent (when already running)
     * 
     * CRITICAL for singleTask:
     * - Called when LoginActivity brings us to front
     * - Contains show/navigation instructions
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "onNewIntent received")
        Log.d(TAG, "═══════════════════════════════════════")
        
        val showNow = intent.getBooleanExtra(EXTRA_SHOW_NOW, false)
        val hash = intent.getStringExtra(EXTRA_HASH)
        
        Log.d(TAG, "Show now: $showNow, Hash: $hash")
        
        if (showNow) {
            isPreloadMode = false
            restoreVisibleMode()
            
            // Navigate to hash
            hash?.let {
                handler.postDelayed({
                    navigateToHash(it)
                }, 100)
            }
        }
    }
    
    /**
     * Handle resume - check for pending hash
     */
    override fun onResume() {
        super.onResume()
        
        // Check for pending hash from manager
        CordovaRuntimeManager.consumePendingHash()?.let { hash ->
            if (!isPreloadMode) {
                Log.d(TAG, "Consuming pending hash: $hash")
                handler.postDelayed({
                    navigateToHash(hash)
                }, 100)
            }
        }
    }
    
    /**
     * Navigate to hash route using JavaScript
     * 
     * WHY window.location.hash:
     * - Standard web navigation
     * - No page reload
     * - Works with all routing frameworks
     * - Instant navigation
     * 
     * WHY evaluateJavascript:
     * - Async, doesn't block UI
     * - Returns result (optional)
     * - Better than loadUrl("javascript:")
     */
    fun navigateToHash(hash: String) {
        val cleanHash = if (hash.startsWith("#")) hash else "#$hash"
        
        Log.d(TAG, "Navigating to: $cleanHash")
        
        val js = """
            (function() {
                console.log('Native navigation to: $cleanHash');
                window.location.hash = '$cleanHash';
                return window.location.hash;
            })();
        """.trimIndent()
        
        val engine = appView?.engine
        if (engine is SystemWebViewEngine) {
            engine.evaluateJavascript(js) { result ->
                Log.d(TAG, "Navigation result: $result")
            }
        }
    }
    
    /**
     * Mark content as ready (called from JavaScript)
     * 
     * Add this to your index.html:
     * document.addEventListener('deviceready', function() {
     *     if (window.NativePreload) {
     *         NativePreload.ready();
     *     }
     * });
     */
    fun markContentReady() {
        if (isContentReady) return
        
        isContentReady = true
        preloadTimeoutRunnable?.let { handler.removeCallbacks(it) }
        
        Log.d(TAG, "✓ Content marked as ready")
        CordovaRuntimeManager.markPreloadReady()
    }
    
    /**
     * Get the underlying WebView
     */
    private fun getWebView(): WebView? {
        return try {
            val engine = appView?.engine
            if (engine is SystemWebViewEngine) {
                engine.view as? SystemWebView
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get WebView", e)
            null
        }
    }
    
    /**
     * HARD LOGOUT - Complete cleanup
     * 
     * CRITICAL for banking apps:
     * - Clear ALL session data
     * - Destroy WebView state
     * - Finish activity
     * - Navigate to login
     */
    fun performHardLogout() {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "HARD LOGOUT initiated")
        Log.d(TAG, "═══════════════════════════════════════")
        
        CordovaRuntimeManager.performHardLogout(applicationContext)
    }
    
    /**
     * Clear all WebView data
     * 
     * WHAT IT CLEARS:
     * - Cookies
     * - LocalStorage
     * - SessionStorage
     * - IndexedDB
     * - WebSQL
     * - Cache
     * - Form data
     */
    fun clearAllWebViewData() {
        Log.d(TAG, "Clearing all WebView data...")
        
        try {
            // Clear cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            Log.d(TAG, "✓ Cookies cleared")
            
            // Clear WebView storage (localStorage, IndexedDB, etc.)
            WebStorage.getInstance().deleteAllData()
            Log.d(TAG, "✓ WebStorage cleared")
            
            // Clear WebView cache
            getWebView()?.apply {
                clearCache(true)
                clearFormData()
                clearHistory()
                clearSslPreferences()
                Log.d(TAG, "✓ WebView cache cleared")
            }
            
            // Clear via JavaScript (belt and suspenders)
            val clearJs = """
                (function() {
                    try {
                        localStorage.clear();
                        sessionStorage.clear();
                        console.log('JS storage cleared');
                    } catch(e) {
                        console.error('Failed to clear JS storage', e);
                    }
                })();
            """.trimIndent()
            
            val engine = appView?.engine
            if (engine is SystemWebViewEngine) {
                engine.evaluateJavascript(clearJs, null)
            }
            
            Log.d(TAG, "✓ All WebView data cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebView data", e)
        }
    }
    
    /**
     * Handle back press
     * 
     * POLICY:
     * - During preload: ignore
     * - While showing: exit to login (HARD LOGOUT behavior)
     *   OR allow web navigation (depends on requirement)
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isPreloadMode) {
            Log.d(TAG, "Back pressed ignored during preload")
            return
        }
        
        // Option 1: Let web handle back navigation
        // super.onBackPressed()
        
        // Option 2: Always exit to login (banking security)
        Log.d(TAG, "Back pressed - exiting to login")
        performHardLogout()
    }
    
    override fun onDestroy() {
        Log.d(TAG, "MainActivity onDestroy")
        
        // Cancel timeout
        preloadTimeoutRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        
        super.onDestroy()
    }
}
