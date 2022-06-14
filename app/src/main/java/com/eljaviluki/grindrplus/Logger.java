package com.eljaviluki.grindrplus;


import android.util.Log;

import de.robv.android.xposed.XposedBridge;

public class Logger {
    public static final String TAG = "GrindrPlus";

    public static void xLog(String msg){
        XposedBridge.log(TAG + ": " + msg);
    }

    public static void log(String msg){
        Log.i(TAG, msg);
    }
}
