/**
 * Custom Navigation Plugin JavaScript Interface
 * Provides easy access to native navigation from JavaScript
 */
var NavigationPlugin = {
    /**
     * Logout and navigate to LoginActivity
     * @param {Function} success - Success callback
     * @param {Function} error - Error callback
     */
    logout: function(success, error) {
        cordova.exec(
            success || function() { console.log('Logout successful'); },
            error || function(err) { console.error('Logout failed:', err); },
            'NavigationPlugin',
            'logout',
            []
        );
    },

    /**
     * Navigate to LoginActivity
     * @param {Function} success - Success callback
     * @param {Function} error - Error callback
     */
    navigateToLogin: function(success, error) {
        cordova.exec(
            success || function() { console.log('Navigation successful'); },
            error || function(err) { console.error('Navigation failed:', err); },
            'NavigationPlugin',
            'navigateToLogin',
            []
        );
    },

    /**
     * Open settings
     * @param {Object} options - Settings options
     * @param {Function} success - Success callback
     * @param {Function} error - Error callback
     */
    openSettings: function(options, success, error) {
        cordova.exec(
            success || function() { console.log('Settings opened'); },
            error || function(err) { console.error('Settings failed:', err); },
            'NavigationPlugin',
            'openSettings',
            [options || {}]
        );
    }
};

// Make it globally available
window.NavigationPlugin = NavigationPlugin;
