package com.example.e2performance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * SplashActivity - Entry Point with Preload Trigger
 * 
 * PURPOSE:
 * 1. Show branded splash screen
 * 2. Trigger MainActivity preload in background
 * 3. Navigate to LoginActivity when ready
 * 
 * WHY PRELOAD HERE:
 * - Splash is the first activity
 * - User expects loading time during splash
 * - Perfect time to preload Cordova in background
 * 
 * TIMING:
 * - Minimum splash duration: 1.5 seconds (branding)
 * - Maximum splash duration: 5 seconds (failsafe)
 * - Preload runs in parallel
 */
class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SplashActivity"
        private const val MIN_SPLASH_DURATION = 1500L   // Minimum time to show splash
        private const val MAX_SPLASH_DURATION = 5000L   // Maximum time before proceeding
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var splashStartTime: Long = 0
    private var hasNavigated = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "SplashActivity onCreate")
        Log.d(TAG, "═══════════════════════════════════════")
        
        // Use fullscreen splash layout
        setContentView(R.layout.activity_splash)
        
        // Make immersive (hide system bars)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
        
        splashStartTime = System.currentTimeMillis()
        
        // STEP 1: Reset any previous state (fresh start)
        CordovaRuntimeManager.resetState()
        
        // STEP 2: Start preloading MainActivity in background
        startPreload()
        
        // STEP 3: Set minimum splash duration
        handler.postDelayed({
            checkAndNavigate()
        }, MIN_SPLASH_DURATION)
        
        // STEP 4: Set maximum splash duration (failsafe)
        handler.postDelayed({
            forceNavigate()
        }, MAX_SPLASH_DURATION)
    }
    
    /**
     * Start preloading MainActivity
     * 
     * WHY DELAYED 100ms:
     * - Let SplashActivity finish rendering
     * - Prevent UI jank during transition
     */
    private fun startPreload() {
        handler.postDelayed({
            Log.d(TAG, "Triggering MainActivity preload...")
            CordovaRuntimeManager.startPreload(applicationContext)
        }, 100)
    }
    
    /**
     * Check if preload is ready and navigate
     */
    private fun checkAndNavigate() {
        if (hasNavigated) return
        
        val state = CordovaRuntimeManager.getState()
        Log.d(TAG, "Checking preload state: $state")
        
        when (state) {
            CordovaRuntimeManager.PreloadState.READY -> {
                navigateToLogin()
            }
            CordovaRuntimeManager.PreloadState.IN_PROGRESS -> {
                // Wait a bit more
                handler.postDelayed({
                    checkAndNavigate()
                }, 500)
            }
            else -> {
                // Proceed anyway - LoginActivity will handle
                navigateToLogin()
            }
        }
    }
    
    /**
     * Force navigation after max duration
     */
    private fun forceNavigate() {
        if (hasNavigated) return
        
        Log.w(TAG, "Max splash duration reached. Forcing navigation.")
        navigateToLogin()
    }
    
    /**
     * Navigate to LoginActivity
     */
    private fun navigateToLogin() {
        if (hasNavigated) return
        hasNavigated = true
        
        val duration = System.currentTimeMillis() - splashStartTime
        Log.d(TAG, "Navigating to LoginActivity after ${duration}ms")
        
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        
        // Fade out transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
    
    /**
     * Prevent back press during splash
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing - splash cannot be dismissed
    }
}
