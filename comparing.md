# Cordova WebView Preloading – Architecture Decision

## Why we should **NOT** choose  
**Singleton CordovaWebView / SystemWebView (detach & attach across Activities)**

- **Not supported by Cordova**
  - Cordova assumes **1 Activity ↔ 1 WebView**
  - No official documentation or examples for reusing a WebView across multiple Activities  
  - Official docs show WebView is owned by a single Activity:  
    https://cordova.apache.org/docs/en/latest/guide/platforms/android/webview.html

- **High plugin risk**
  - Plugins (Camera, Permissions, File chooser, InAppBrowser) depend on a **stable Activity**
  - Detaching WebView can break:
    - permission callbacks
    - `onActivityResult`

- **Complex lifecycle management**
  - Requires manual forwarding of:
    - `onResume / onPause`
    - `onActivityResult`
    - `onRequestPermissionsResult`
  - Hard to guarantee correctness when Android recreates Activities

- **WebView was not designed to move**
  - Internally bound to window, input system, and rendering surface
  - Can cause white screens, keyboard issues, and device-specific bugs

- **Hard to maintain**
  - Issues often appear only on some devices (Samsung, Xiaomi, Oppo)
  - Cordova or Android upgrades may break the setup

- **High engineering cost**
  - More custom code
  - More testing
  - Higher long-term maintenance risk

---

## Why we SHOULD choose  
**Preload CordovaActivity and reuse it (singleTask / singleTop)**

- **Officially supported by Cordova**
  - This is the intended architecture
  - Used by Ionic and most production Cordova apps

- **Stable plugin behavior**
  - Plugins are initialized once
  - Activity lifecycle is handled automatically

- **Best performance with lowest risk**
  - WebView, JS engine, and plugins load only once
  - Navigation via JS (no reload)

- **Simple and reliable**
  - No attach/detach logic
  - No custom lifecycle or permission handling

- **Clear ownership**
  - One Activity owns the WebView
  - Web handles navigation, native handles system features

---

## Official Cordova position (summary)

- Cordova documentation **does not provide any pattern** for sharing one WebView across multiple Activities
- CordovaActivity exists specifically to:
  - manage WebView lifecycle
  - manage plugin lifecycle
  - keep Activity ↔ WebView binding stable

Source:  
https://cordova.apache.org/docs/en/latest/guide/platforms/android/webview.html

---

## Final one-line summary

> Cordova is designed for **one Activity owning one WebView and its plugins**.  
> Reusing a singleton WebView across Activities is unsupported and high-risk.  
> Preloading a single CordovaActivity delivers better performance with far less risk.
