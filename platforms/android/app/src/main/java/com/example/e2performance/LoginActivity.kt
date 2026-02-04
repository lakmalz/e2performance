package com.example.e2performance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * LoginActivity - Native Authentication Screen
 * 
 * PURPOSE:
 * 1. Handle user authentication (credentials + soft token)
 * 2. Wait for preload to complete (if not already)
 * 3. Show preloaded MainActivity after successful login
 * 
 * WHY NATIVE:
 * - Better security for credential handling
 * - Faster UI response
 * - Soft token integration
 * - No WebView dependency for authentication
 * 
 * FLOW:
 * 1. User enters credentials
 * 2. Validate with backend
 * 3. On success: Show MainActivity at #dashboard
 * 4. On logout: Return here for fresh login
 */
class LoginActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "LoginActivity"
        const val EXTRA_FROM_LOGOUT = "from_logout"
    }
    
    private lateinit var loginButton: Button
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "LoginActivity onCreate")
        Log.d(TAG, "═══════════════════════════════════════")
        
        setupViews()
        
        // Check if coming from logout
        val fromLogout = intent.getBooleanExtra(EXTRA_FROM_LOGOUT, false)
        if (fromLogout) {
            Log.d(TAG, "Returned from HARD LOGOUT")
        }
        
        // Start preloading MainActivity in background after 500ms
        handler.postDelayed({
            Log.d(TAG, "Starting MainActivity preload...")
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("PRELOAD_MODE", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
        }, 500)
    }
    
    private fun setupViews() {
        loginButton = findViewById(R.id.loginButton)
        
        loginButton.setOnClickListener {
            performLogin()
        }
    }
    
    /**
     * Perform login authentication
     * 
     * TODO: Replace with actual authentication logic
     * - Validate credentials
     * - Call authentication API
     * - Handle soft token
     */
    private fun performLogin() {
        Log.d(TAG, "Login button clicked")
        
        // Show loading state
        loginButton.isEnabled = false
        loginButton.text = "Authenticating..."
        
        // Simulate authentication (replace with real auth)
        handler.postDelayed({
            onLoginSuccess()
        }, 500)
    }
    
    /**
     * Handle successful login
     * 
     * CRITICAL:
     * - Uses preloaded MainActivity if ready
     * - Falls back to starting fresh MainActivity
     * - Navigates to #dashboard
     * - Finishes LoginActivity (no back navigation)
     */
    private fun onLoginSuccess() {
        Log.d(TAG, "Login successful!")
        
        // Bring MainActivity to front and navigate to #settings
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.putExtra("SHOW_NOW", true)
        intent.putExtra("HASH", "settings")
        startActivity(intent)
        finish()
    }
    
    /**
     * Handle login failure
     */
    private fun onLoginFailed(error: String) {
        Log.e(TAG, "Login failed: $error")
        
        loginButton.isEnabled = true
        loginButton.text = "Login"
        
        Toast.makeText(this, "Login failed: $error", Toast.LENGTH_LONG).show()
    }
    
    /**
     * Handle back press
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Exit app (no previous screen)
        finishAffinity()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
