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
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.apache.cordova.*;

public class DashboardActivity extends CordovaActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Load index.html which contains both dashboard and settings pages
        loadUrl(launchUrl);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Add JavaScript interface for native navigation
        if (appView != null && appView.getEngine() != null) {
            Object webViewObject = appView.getEngine().getView();
            if (webViewObject instanceof WebView) {
                WebView webView = (WebView) webViewObject;
                webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
            }
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void logout() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Navigate to LoginActivity
                    Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
}
