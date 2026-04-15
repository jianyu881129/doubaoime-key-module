package com.example.doubaoimekey;

import android.view.KeyEvent;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedHook implements IXposedHookLoadPackage {
    
    private static final String TARGET_PACKAGE = "com.bytedance.android.doubaoime";
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }
        
        XposedBridge.log("DoubaoIMEKeyModule: Hooking into " + TARGET_PACKAGE);
        
        hookKeyEventDispatch(lpparam);
        hookInputMethodService(lpparam);
    }
    
    private void hookKeyEventDispatch(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "dispatchKeyEvent",
                KeyEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        KeyEvent event = (KeyEvent) param.args[0];
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            handleKeyDown(event, param);
                        }
                    }
                }
            );
            
            XposedBridge.log("DoubaoIMEKeyModule: Hooked Activity.dispatchKeyEvent");
        } catch (Throwable t) {
            XposedBridge.log("DoubaoIMEKeyModule: Failed to hook Activity.dispatchKeyEvent: " + t.getMessage());
        }
    }
    
    private void hookInputMethodService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inputMethodServiceClass = XposedHelpers.findClass(
                "android.inputmethodservice.InputMethodService",
                lpparam.classLoader
            );
            
            XposedHelpers.findAndHookMethod(
                inputMethodServiceClass,
                "onKeyDown",
                int.class,
                KeyEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int keyCode = (int) param.args[0];
                        KeyEvent event = (KeyEvent) param.args[1];
                        
                        if (handleKeyCode(keyCode, event, param)) {
                            param.setResult(true);
                        }
                    }
                }
            );
            
            XposedBridge.log("DoubaoIMEKeyModule: Hooked InputMethodService.onKeyDown");
        } catch (Throwable t) {
            XposedBridge.log("DoubaoIMEKeyModule: Failed to hook InputMethodService: " + t.getMessage());
        }
    }
    
    private void handleKeyDown(KeyEvent event, XC_MethodHook.MethodHookParam param) {
        int keyCode = event.getKeyCode();
        
        if (event.getRepeatCount() > 0) {
            handleKeyCode(keyCode, event, param);
        }
    }
    
    private boolean handleKeyCode(int keyCode, KeyEvent event, XC_MethodHook.MethodHookParam param) {
        Object inputConnection = getInputConnection(param);
        if (inputConnection == null) {
            return false;
        }
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_Z:
                XposedBridge.log("DoubaoIMEKeyModule: Z key pressed - Select All");
                performSelectAll(inputConnection);
                return true;
                
            case KeyEvent.KEYCODE_X:
                XposedBridge.log("DoubaoIMEKeyModule: X key pressed - Cut");
                performCut(inputConnection);
                return true;
                
            case KeyEvent.KEYCODE_C:
                XposedBridge.log("DoubaoIMEKeyModule: C key pressed - Copy");
                performCopy(inputConnection);
                return true;
                
            case KeyEvent.KEYCODE_V:
                XposedBridge.log("DoubaoIMEKeyModule: V key pressed - Paste");
                performPaste(inputConnection);
                return true;
        }
        
        return false;
    }
    
    private Object getInputConnection(XC_MethodHook.MethodHookParam param) {
        try {
            Object currentFocus = XposedHelpers.callMethod(param.thisObject, "getCurrentFocus");
            if (currentFocus != null) {
                return XposedHelpers.callMethod(currentFocus, "getInputConnection");
            }
        } catch (Throwable t) {
            // Ignore
        }
        return null;
    }
    
    private void performSelectAll(Object inputConnection) {
        try {
            XposedHelpers.callMethod(inputConnection, "performContextMenuAction", 
                android.R.id.selectAll);
        } catch (Throwable t) {
            XposedBridge.log("DoubaoIMEKeyModule: Failed to select all: " + t.getMessage());
        }
    }
    
    private void performCut(Object inputConnection) {
        try {
            performSelectAll(inputConnection);
            XposedHelpers.callMethod(inputConnection, "performContextMenuAction",
                android.R.id.cut);
        } catch (Throwable t) {
            XposedBridge.log("DoubaoIMEKeyModule: Failed to cut: " + t.getMessage());
        }
    }
    
    private void performCopy(Object inputConnection) {
        try {
            performSelectAll(inputConnection);
            XposedHelpers.callMethod(inputConnection, "performContextMenuAction",
                android.R.id.copy);
        } catch (Throwable t) {
            XposedBridge.log("DoubaoIMEKeyModule: Failed to copy: " + t.getMessage());
        }
    }
    
    private void performPaste(Object inputConnection) {
        try {
            XposedHelpers.callMethod(inputConnection, "performContextMenuAction",
                android.R.id.paste);
        } catch (Throwable t) {
            XposedBridge.log("DoubaoIMEKeyModule: Failed to paste: " + t.getMessage());
        }
    }
}
