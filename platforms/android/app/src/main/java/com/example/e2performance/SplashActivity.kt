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
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
        
        splashStartTime = System.currentTimeMillis()
        
        // STEP 1: Reset any previous state (fresh start)
        CordovaRuntimeManager.resetState()
        
        // STEP 2: Navigate to Login after minimum splash duration
        // Preload will be triggered from LoginActivity AFTER it's visible
        handler.postDelayed({
            navigateToLogin()
        }, MIN_SPLASH_DURATION)
    }
    
    /**
     * Navigate to LoginActivity

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
