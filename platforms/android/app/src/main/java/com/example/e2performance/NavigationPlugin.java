package com.example.e2performance;

import android.content.Intent;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Custom Cordova Plugin for Native Navigation
 * Provides methods to navigate between native Android activities from JavaScript
 */
public class NavigationPlugin extends CordovaPlugin {
    private static final String TAG = "NavigationPlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Plugin action called: " + action);

        if (action.equals("logout")) {
            this.logout(callbackContext);
            return true;
        } else if (action.equals("navigateToLogin")) {
            this.navigateToLogin(callbackContext);
            return true;
        } else if (action.equals("openSettings")) {
            this.openSettings(args, callbackContext);
            return true;
        }

        return false;
    }

    /**
     * Logout and navigate to LoginActivity
     * Clears the activity stack
     */
    private void logout(CallbackContext callbackContext) {
        Log.d(TAG, "Logout action called");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(cordova.getActivity(), LoginActivity.class);
                    // Clear the entire back stack
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    cordova.getActivity().startActivity(intent);
                    cordova.getActivity().finish();
                    
                    callbackContext.success("Logged out successfully");
                    Log.d(TAG, "Successfully navigated to LoginActivity");
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to LoginActivity", e);
                    callbackContext.error("Failed to logout: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Navigate to LoginActivity without clearing stack
     */
    private void navigateToLogin(CallbackContext callbackContext) {
        Log.d(TAG, "NavigateToLogin action called");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(cordova.getActivity(), LoginActivity.class);
                    cordova.getActivity().startActivity(intent);
                    
                    callbackContext.success("Navigated to Login");
                    Log.d(TAG, "Successfully navigated to LoginActivity");
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to LoginActivity", e);
                    callbackContext.error("Navigation failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Open settings with optional data
     * Can be extended to open other activities
     */
    private void openSettings(JSONArray args, CallbackContext callbackContext) {
        Log.d(TAG, "OpenSettings action called");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Example: Could navigate to a native settings activity
                    // For now, just return success
                    callbackContext.success("Settings opened");
                    Log.d(TAG, "Settings action completed");
                } catch (Exception e) {
                    Log.e(TAG, "Error opening settings", e);
                    callbackContext.error("Failed to open settings: " + e.getMessage());
                }
            }
        });
    }
}
