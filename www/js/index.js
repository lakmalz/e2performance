/**
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

(function() {
    'use strict';

    // Constants
    const PAGES = {
        DASHBOARD: 'dashboard',
        SETTINGS: 'settings'
    };

    const ROUTES = {
        DASHBOARD: '#dashboard',
        SETTINGS: '#settings'
    };

    const BUTTON_IDS = {
        SETTINGS: 'settingsBtn',
        LOGOUT: 'logoutBtn'
    };

    // Initialize Cordova
    document.addEventListener('deviceready', onDeviceReady, false);

    function onDeviceReady() {
        console.log('Running cordova-' + cordova.platformId + '@' + cordova.version);
        
        // CRITICAL: Signal to native that content is ready
        // This is used by MainActivity to know preload is complete
        signalNativeReady();
        
        // Check if NavigationPlugin is available
        if (window.NavigationPlugin) {
            console.log('✓ NavigationPlugin available');
        } else {
            console.log('✗ NavigationPlugin NOT available - checking NativeLogout');
        }
        
        // Check if NativeLogout interface is available (new preload architecture)
        if (window.NativeLogout) {
            console.log('✓ NativeLogout interface available');
        }
        
        initialize();
    }
    
    /**
     * Signal to native that WebView content is fully loaded
     * This is critical for the preload architecture
     */
    function signalNativeReady() {
        console.log('Signaling native that content is ready...');
        
        // Try multiple methods to ensure native gets the signal
        
        // Method 1: Call NativePreload.ready() if available
        if (window.NativePreload && typeof window.NativePreload.ready === 'function') {
            window.NativePreload.ready();
            console.log('✓ Called NativePreload.ready()');
        }
        
        // Method 2: Dispatch custom event for native to listen
        try {
            document.dispatchEvent(new CustomEvent('cordovaContentReady', {
                detail: { timestamp: Date.now() }
            }));
            console.log('✓ Dispatched cordovaContentReady event');
        } catch (e) {
            console.warn('Failed to dispatch event:', e);
        }
        
        // Method 3: Set global flag
        window.cordovaContentReady = true;
        console.log('✓ Set window.cordovaContentReady = true');
    }

    function initialize() {
        setupEventListeners();
        initNavigation();
    }

    function setupEventListeners() {
        attachButtonListener(BUTTON_IDS.SETTINGS, handleSettingsClick);
        attachButtonListener(BUTTON_IDS.LOGOUT, handleLogoutClick);
    }

    function attachButtonListener(buttonId, handler) {
        const button = document.getElementById(buttonId);
        
        if (!button) {
            console.error('Button not found: ' + buttonId);
            return;
        }

        button.addEventListener('click', handler);
        console.log('Event listener added: ' + buttonId);
    }

    function handleSettingsClick() {
        console.log('Settings button clicked');
        navigateTo(ROUTES.SETTINGS);
    }

    function handleLogoutClick() {
        console.log('Logout button clicked');
        
        // Try NativeLogout interface first (new preload architecture)
        if (window.NativeLogout && typeof window.NativeLogout.logout === 'function') {
            console.log('Calling NativeLogout.logout()...');
            window.NativeLogout.logout();
            return;
        }
        
        // Fallback to NavigationPlugin (old architecture)
        if (window.NavigationPlugin) {
            console.log('Calling NavigationPlugin.logout()...');
            NavigationPlugin.logout(
                function(result) {
                    console.log('✓ Logout successful:', result);
                },
                function(error) {
                    console.error('✗ Logout failed:', error);
                    alert('Logout failed: ' + error);
                }
            );
            return;
        }
        
        console.error('No logout interface available');
        alert('Logout not available');
    }

    function navigateTo(route) {
        console.log('Navigating to: ' + route);
        window.location.hash = route;
    }

    function initNavigation() {
        showPage();
        window.addEventListener('hashchange', showPage);
    }

    function showPage() {
        const currentHash = window.location.hash;
        
        console.log('Current hash: ' + currentHash);
        
        // Determine which page to show based on hash
        const isDashboard = !currentHash || currentHash === '' || currentHash === '#' || currentHash === '#dashboard';
        const isSettings = currentHash === '#settings';

        togglePageVisibility(PAGES.DASHBOARD, isDashboard);
        togglePageVisibility(PAGES.SETTINGS, isSettings);
    }

    function togglePageVisibility(pageId, isVisible) {
        const page = document.getElementById(pageId);
        
        if (!page) {
            console.error('Page not found: ' + pageId);
            return;
        }

        page.style.display = isVisible ? 'flex' : 'none';
        console.log((isVisible ? 'Showing' : 'Hiding') + ' page: ' + pageId);
    }

})();


