package com.example.e2performance

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import java.lang.ref.WeakReference

/**
 * CordovaRuntimeManager - Banking-Grade State Manager
 * 
 * PURPOSE:
 * Manages the complete lifecycle of CordovaActivity preloading with hard logout support.
 * This is a singleton that tracks:
 * - Preload state (NOT_STARTED → IN_PROGRESS → READY → SHOWING)
 * - Authentication state
 * - MainActivity instance reference (weak to avoid memory leaks)
 * 
 * WHY SINGLETON:
 * - Single source of truth for preload state across activities
 * - Survives activity transitions
 * - Easy state reset on logout
 * 
 * THREAD SAFETY:
 * - All state changes are synchronized
 * - Safe for access from multiple activities
 */
object CordovaRuntimeManager {
    
    private const val TAG = "CordovaRuntimeManager"
    
    /**
     * Preload lifecycle states
     */
    enum class PreloadState {
        NOT_STARTED,    // Initial state, or after logout
        IN_PROGRESS,    // MainActivity started but not ready
        READY,          // index.html loaded, deviceready fired
        SHOWING,        // MainActivity visible to user
        FAILED          // Preload failed (timeout, error)
    }
    
    // Current preload state
    @Volatile
    private var preloadState: PreloadState = PreloadState.NOT_STARTED
    
    // Weak reference to MainActivity to avoid memory leaks
    private var mainActivityRef: WeakReference<MainActivity>? = null
    
    // Pending navigation hash (set before showing MainActivity)
    @Volatile
    private var pendingHash: String? = null
    
    // Callbacks for preload completion
    private val readyCallbacks = mutableListOf<() -> Unit>()
    
    // Preload start time for performance tracking
    private var preloadStartTime: Long = 0
    
    /**
     * Get current preload state (thread-safe)
     */
    @Synchronized
    fun getState(): PreloadState = preloadState
    
    /**
     * Check if MainActivity is preloaded and ready
     */
    @Synchronized
    fun isReady(): Boolean = preloadState == PreloadState.READY
    
    /**
     * Check if MainActivity is currently showing
     */
    @Synchronized
    fun isShowing(): Boolean = preloadState == PreloadState.SHOWING
    
    /**
     * Start preloading MainActivity (called from SplashActivity)
     * 
     * WHY HERE:
     * - Centralized preload trigger
     * - Prevents duplicate preloads
     * - Tracks timing for performance metrics
     * 
     * @param context Context for starting activity (should be SplashActivity)
     */
    @Synchronized
    fun startPreload(context: Context) {
        if (preloadState != PreloadState.NOT_STARTED) {
            Log.w(TAG, "Preload already in progress or completed. State: $preloadState")
            return
        }
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "Starting MainActivity preload...")
        Log.d(TAG, "═══════════════════════════════════════")
        
        preloadState = PreloadState.IN_PROGRESS
        preloadStartTime = System.currentTimeMillis()
        
        // Start MainActivity in HIDDEN mode
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_PRELOAD_MODE, true)
        }
        context.startActivity(intent)
        
        Log.d(TAG, "MainActivity preload intent sent")
    }
    
    /**
     * Register MainActivity instance (called from MainActivity.onCreate)
     * 
     * WHY WEAK REFERENCE:
     * - Prevents memory leaks if logout doesn't clean properly
     * - Activity can be garbage collected if needed
     */
    @Synchronized
    fun registerMainActivity(activity: MainActivity) {
        mainActivityRef = WeakReference(activity)
        Log.d(TAG, "MainActivity registered")
    }
    
    /**
     * Mark preload as complete (called when deviceready fires)
     * 
     * WHY IMPORTANT:
     * - Signals that WebView is fully loaded
     * - Cordova plugins are initialized
     * - Safe to navigate and show
     */
    @Synchronized
    fun markPreloadReady() {
        if (preloadState != PreloadState.IN_PROGRESS) {
            Log.w(TAG, "Unexpected markPreloadReady. State: $preloadState")
            return
        }
        
        val duration = System.currentTimeMillis() - preloadStartTime
        preloadState = PreloadState.READY
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "✓ Preload READY in ${duration}ms")
        Log.d(TAG, "═══════════════════════════════════════")
        
        // Execute any waiting callbacks
        readyCallbacks.forEach { it.invoke() }
        readyCallbacks.clear()
    }
    
    /**
     * Mark preload as failed
     */
    @Synchronized
    fun markPreloadFailed(reason: String) {
        Log.e(TAG, "Preload FAILED: $reason")
        preloadState = PreloadState.FAILED
        readyCallbacks.clear()
    }
    
    /**
     * Show MainActivity and navigate to hash
     * 
     * @param fromActivity The activity to launch from (LoginActivity)
     * @param hash The route hash (e.g., "dashboard", "settings")
     * 
     * WHY THIS PATTERN:
     * - Sets pending hash BEFORE showing
     * - MainActivity reads hash in onNewIntent/onResume
     * - Ensures navigation happens immediately when visible
     */
    @Synchronized
    fun showMainActivity(fromActivity: Activity, hash: String) {
        Log.d(TAG, "showMainActivity called. Hash: $hash, State: $preloadState")
        
        pendingHash = hash
        
        when (preloadState) {
            PreloadState.READY -> {
                // Perfect - show immediately
                bringMainActivityToFront(fromActivity)
            }
            PreloadState.IN_PROGRESS -> {
                // Wait for ready, then show
                Log.d(TAG, "Waiting for preload to complete...")
                readyCallbacks.add {
                    bringMainActivityToFront(fromActivity)
                }
            }
            PreloadState.NOT_STARTED, PreloadState.FAILED -> {
                // Fallback - start fresh
                Log.w(TAG, "Preload not ready. Starting fresh MainActivity...")
                startFreshMainActivity(fromActivity, hash)
            }
            PreloadState.SHOWING -> {
                // Already showing - just navigate
                navigateToHash(hash)
            }
        }
    }
    
    /**
     * Bring preloaded MainActivity to front
     */
    private fun bringMainActivityToFront(fromActivity: Activity) {
        Log.d(TAG, "Bringing MainActivity to front...")
        
        val intent = Intent(fromActivity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_PRELOAD_MODE, false)
            putExtra(MainActivity.EXTRA_SHOW_NOW, true)
            pendingHash?.let { putExtra(MainActivity.EXTRA_HASH, it) }
        }
        fromActivity.startActivity(intent)
        
        preloadState = PreloadState.SHOWING
        
        // Finish login activity so back doesn't go there
        fromActivity.finish()
        
        Log.d(TAG, "✓ MainActivity brought to front")
    }
    
    /**
     * Start fresh MainActivity (fallback when preload failed)
     */
    private fun startFreshMainActivity(fromActivity: Activity, hash: String) {
        val intent = Intent(fromActivity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_PRELOAD_MODE, false)
            putExtra(MainActivity.EXTRA_HASH, hash)
        }
        fromActivity.startActivity(intent)
        fromActivity.finish()
        
        preloadState = PreloadState.SHOWING
    }
    
    /**
     * Navigate to hash route in WebView
     * 
     * WHY evaluateJavascript:
     * - No page reload
     * - Instant navigation
     * - Preserves JS state
     */
    fun navigateToHash(hash: String) {
        val activity = mainActivityRef?.get()
        if (activity == null) {
            Log.e(TAG, "Cannot navigate: MainActivity is null")
            return
        }
        
        val cleanHash = if (hash.startsWith("#")) hash else "#$hash"
        Log.d(TAG, "Navigating to: $cleanHash")
        
        activity.navigateToHash(cleanHash)
    }
    
    /**
     * Get and clear pending hash
     */
    @Synchronized
    fun consumePendingHash(): String? {
        val hash = pendingHash
        pendingHash = null
        return hash
    }
    
    /**
     * HARD LOGOUT - Complete state reset
     * 
     * This is the CRITICAL method for banking apps.
     * Must completely destroy all session data.
     * 
     * WHAT IT DOES:
     * 1. Clears WebView data (cache, cookies, localStorage)
     * 2. Finishes MainActivity
     * 3. Resets all manager state
     * 4. Navigates to LoginActivity
     * 
     * WHY HARD LOGOUT:
     * - Banking security requirements
     * - No session data can persist
     * - Fresh Cordova runtime on next login
     */
    @Synchronized
    fun performHardLogout(context: Context) {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "HARD LOGOUT - Starting complete cleanup")
        Log.d(TAG, "═══════════════════════════════════════")
        
        val activity = mainActivityRef?.get()
        
        // Step 1: Clear WebView data
        activity?.clearAllWebViewData()
        
        // Step 2: Finish MainActivity
        activity?.finishAndRemoveTask()
        
        // Step 3: Reset manager state
        resetState()
        
        // Step 4: Navigate to LoginActivity
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(LoginActivity.EXTRA_FROM_LOGOUT, true)
        }
        context.startActivity(intent)
        
        Log.d(TAG, "✓ HARD LOGOUT complete")
    }
    
    /**
     * Reset all state (called after logout or app restart)
     */
    @Synchronized
    fun resetState() {
        Log.d(TAG, "Resetting CordovaRuntimeManager state")
        preloadState = PreloadState.NOT_STARTED
        mainActivityRef = null
        pendingHash = null
        readyCallbacks.clear()
        preloadStartTime = 0
    }
    
    /**
     * Handle app going to background during preload
     */
    @Synchronized
    fun onAppBackgrounded() {
        if (preloadState == PreloadState.IN_PROGRESS) {
            Log.w(TAG, "App backgrounded during preload - will continue")
            // Preload continues in background
        }
    }
    
    /**
     * Get performance metrics
     */
    fun getPreloadDuration(): Long {
        return if (preloadStartTime > 0 && preloadState == PreloadState.READY) {
            System.currentTimeMillis() - preloadStartTime
        } else {
            -1
        }
    }
}
