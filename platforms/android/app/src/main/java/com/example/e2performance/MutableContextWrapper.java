package com.example.e2performance;

import android.content.Context;
import androidx.appcompat.view.ContextThemeWrapper;
import java.lang.reflect.Field;

/**
 * Custom ContextWrapper that allows RE-SETTING the base context
 * 
 * This uses reflection to bypass the "Base context already set" restriction
 * Critical for OPTION A: switch WebView from Application → Activity context
 */
public class MutableContextWrapper extends ContextThemeWrapper {
    
    public MutableContextWrapper(Context base) {
        super(base, 0);  // 0 = use default theme
    }
    
    /**
     * Force-set base context using reflection (bypasses "already set" check)
     * This allows switching context after the wrapper is created
     */
    public void setBaseContext(Context newBase) {
        try {
            Field field = ContextThemeWrapper.class.getSuperclass().getDeclaredField("mBase");
            field.setAccessible(true);
            field.set(this, newBase);
        } catch (Exception e) {
            // Fallback: try normal attachBaseContext (will throw if already set)
            try {
                attachBaseContext(newBase);
            } catch (IllegalStateException ise) {
                throw new RuntimeException("Failed to set base context via reflection", e);
            }
        }
    }
}

