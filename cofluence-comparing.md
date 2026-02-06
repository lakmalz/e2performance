Sure 👍
Below is a **Confluence-ready Markdown** version you can paste directly into a Confluence page (it renders cleanly in the Markdown macro).

---

# Cordova WebView Architecture – Why Preloading `CordovaActivity` Is the Right Choice

This document explains **why a Singleton / detached CordovaWebView approach is risky** and **why preloading and reusing `CordovaActivity` is the recommended and stable solution**, based on Cordova’s internal design and real-world usage.

---

## Why *NOT* Choose

**Singleton CordovaWebView / SystemWebView Detached + Attached Manually**

### 1. Cordova Assumes Ownership of the Activity Lifecycle

Cordova is designed around a **tight 1:1 relationship**:

```
Activity ↔ CordovaWebView ↔ CordovaInterface
```

Cordova internally assumes:

* The `Activity` **creates** the WebView
* The `Activity` **owns** the WebView lifecycle
* The `Activity` **controls** pause, resume, destroy

When you detach the WebView from its Activity:

* Cordova lifecycle hooks are broken
* Internal state becomes inconsistent
* Cordova has no idea where it is running

---

### 2. Plugin Lifecycle Is Bound to the Activity

Cordova plugins are **not WebView-only objects**.

Plugins depend on:

* `Activity` context
* `CordovaInterface`
* Activity lifecycle callbacks:

  * `onPause`
  * `onResume`
  * `onDestroy`
  * `onActivityResult`
  * `onRequestPermissionsResult`

When you detach and reattach a WebView:

* Plugins keep references to the **old Activity**
* Callbacks stop working correctly
* Permission dialogs, intents, and results break
* Memory leaks become very likely

This leads to:

* Random crashes
* Plugins silently failing
* “Works on some devices, breaks on others”

---

### 3. WebView Was Never Designed to Move Between Activities

Android `WebView` is **stateful and fragile**.

Detaching a WebView causes:

* Rendering context loss
* JS engine instability
* GPU surface recreation issues

Common symptoms:

* White screens
* Black screens
* Random reloads
* ANRs during reattach
* Lost JS execution state

Cordova does **not** support this pattern officially.

---

### 4. Detach / Reattach Breaks Plugin Context

Plugins internally cache:

* Activity reference
* Window tokens
* Fragment managers
* Permission state

Once detached:

* Cached references become invalid
* Plugins may crash or hang
* JS ↔ Native bridge becomes unreliable

This often results in:

* Race conditions
* Deadlocks
* Hard-to-reproduce bugs

---

### 5. High Risk, High Complexity, Low Maintainability

A singleton WebView solution requires:

* Manual lifecycle forwarding
* Manual plugin state handling
* Custom attach/detach logic
* Careful threading management
* Deep Cordova internals knowledge

Even then:

* No official support
* No guarantees across OS versions
* Very hard to debug in production

---

## Why Choose

**Preload `CordovaActivity` and Reuse It**

### 1. Officially Supported by Cordova

This approach respects Cordova’s core assumption:

```
One Activity = One WebView = One Plugin Lifecycle
```

You are working **with the framework**, not against it.

---

### 2. Used by Ionic and Production Cordova Apps

Popular frameworks like **Ionic**:

* Use a single host `CordovaActivity`
* Hide / show it instead of recreating it
* Navigate using JS routing (`hash`, `history`, SPA)

This pattern has:

* Years of production validation
* Community support
* Predictable behavior

---

### 3. Stable Plugin Behavior

Because the Activity is never destroyed:

* Plugins remain initialized
* Native state is preserved
* Permissions and callbacks work reliably

No:

* Plugin reinitialization issues
* Lost context
* Broken callbacks

---

### 4. Much Simpler Implementation

With a preloaded `CordovaActivity`:

* No manual WebView attach/detach
* No custom lifecycle forwarding
* No plugin hacks
* No unsafe reflection or internal API usage

You rely on:

* Cordova’s lifecycle
* Android’s activity management
* Proven SPA navigation

---

### 5. Cordova Auto-Loads `launchUrl` Correctly

Cordova is designed to:

* Load `launchUrl` automatically in `onCreate`
* Manage `loadUrl()` internally
* Handle reloads safely

Using `loadUrl()` or reattaching WebViews manually:

* Bypasses Cordova safeguards
* Causes reloads and white screens
* Breaks routing consistency

With a preloaded activity:

* The WebView loads once
* Navigation is handled via JS (`window.location.hash`)
* Performance is stable and predictable

---

## Summary Comparison

| Aspect                | Singleton / Detached WebView | Preloaded CordovaActivity |
| --------------------- | ---------------------------- | ------------------------- |
| Cordova support       | ❌ Not supported              | ✅ Officially supported    |
| Plugin stability      | ❌ High risk                  | ✅ Stable                  |
| Lifecycle handling    | ❌ Manual & complex           | ✅ Automatic               |
| Risk of white screens | ❌ High                       | ✅ Very low                |
| Maintainability       | ❌ Poor                       | ✅ Excellent               |
| Production readiness  | ❌ Unsafe                     | ✅ Proven                  |

---

## Final Recommendation

**Do not use a Singleton or manually detached CordovaWebView.**
It introduces instability, plugin failures, and long-term maintenance risk.

**Use a preloaded `CordovaActivity` and reuse it.**
This aligns with Cordova’s design, matches community best practices, and delivers the best balance of **performance, stability, and simplicity**.

---

If you want, next I can:

* Add **architecture diagrams**
* Provide a **sample preload + reuse flow**
* Help turn this into a **design decision record (ADR)** for your team
