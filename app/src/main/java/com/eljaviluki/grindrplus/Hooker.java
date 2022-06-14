package com.eljaviluki.grindrplus;

import static com.eljaviluki.grindrplus.Constants.*;
import static de.robv.android.xposed.XposedHelpers.*;

import android.app.Application;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hooker implements IXposedHookLoadPackage {

    public static XC_LoadPackage.LoadPackageParam pkgParam;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(GRINDR_PKG)) return;

        pkgParam = lpparam;
        findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.xLog("Application.onCreate()");

                Hooks.hookFeatureGranting();
                Hooks.allowScreenshotsHook();
                //Hooks.unlimitedExpiringPhotos();
                Hooks.addExtraProfileFields();
                Hooks.hookUserSessionImpl();
                Hooks.allowMockProvider();
                Hooks.allowVideocallsOnEmptyChats();
                Hooks.allowSomeExperiments();
            }
        });


    }
}