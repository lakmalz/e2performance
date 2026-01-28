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
        DASHBOARD: '',
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
        initialize();
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
        
        if (isAndroidBridgeAvailable()) {
            console.log('Calling AndroidBridge.logout()');
            AndroidBridge.logout();
        } else {
            console.warn('AndroidBridge not available');
            alert('Logout functionality - AndroidBridge not available');
        }
    }

    function isAndroidBridgeAvailable() {
        return typeof AndroidBridge !== 'undefined';
    }

    function navigateTo(route) {
        window.location.hash = route;
    }

    function initNavigation() {
        showPage();
        window.addEventListener('hashchange', showPage);
    }

    function showPage() {
        const currentHash = window.location.hash;
        const isSettingsPage = currentHash === ROUTES.SETTINGS;
        
        console.log('Current hash: ' + currentHash);

        togglePageVisibility(PAGES.DASHBOARD, !isSettingsPage);
        togglePageVisibility(PAGES.SETTINGS, isSettingsPage);
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


