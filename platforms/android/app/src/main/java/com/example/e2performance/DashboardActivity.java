/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.example.e2performance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import org.apache.cordova.*;

public class DashboardActivity extends CordovaActivity {

    private Button logoutButton;
    private Button settingsButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Add buttons on top of the Cordova webview after it's created
        addButtonsOverlay();
    }

    private void addButtonsOverlay() {
        // Get the root view
        ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        
        // Create buttons layout - vertical orientation
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setGravity(Gravity.CENTER);
        buttonLayout.setPadding(32, 32, 32, 32);
        buttonLayout.setBackgroundColor(0xEEFFFFFF); // Semi-transparent white background

        // Logout button
        logoutButton = new Button(this);
        logoutButton.setText("Logout");
        logoutButton.setTextSize(18);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        // Settings button
        settingsButton = new Button(this);
        settingsButton.setText("Settings");
        settingsButton.setTextSize(18);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        // Set button dimensions and margins
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            (int) (250 * getResources().getDisplayMetrics().density), // 250dp width
            (int) (60 * getResources().getDisplayMetrics().density)   // 60dp height
        );
        buttonParams.setMargins(0, 16, 0, 16); // Vertical margin between buttons

        buttonLayout.addView(logoutButton, buttonParams);
        buttonLayout.addView(settingsButton, buttonParams);

        // Add button layout centered on the screen
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER; // Center both horizontally and vertically

        rootView.addView(buttonLayout, layoutParams);
    }

    private void logout() {
        // Clear any session data if needed
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openSettings() {
        // Navigate to settings or show settings dialog
        // You can implement this based on your requirements
        if (appView != null) {
            appView.loadUrl("javascript:alert('Settings clicked')");
        }
    }
}
