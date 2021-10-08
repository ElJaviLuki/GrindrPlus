package com.eljaviluki.grindrplus;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GrindrHooker implements IXposedHookLoadPackage {
    static final XC_MethodReplacement RETURN_TRUE = XC_MethodReplacement.returnConstant(true);
    static final XC_MethodReplacement RETURN_FALSE = XC_MethodReplacement.returnConstant(false);
    static final String GRINDR_PKG = "com.grindrapp.android";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(GRINDR_PKG))
            return;

        {
            Class<?> class_Feature = XposedHelpers.findClass(GRINDR_PKG + ".model.Feature", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(class_Feature, "isGranted", RETURN_TRUE);
            XposedHelpers.findAndHookMethod(class_Feature, "isNotGranted", RETURN_FALSE);
        }

        {
            Class<?> class_BaseUserSession = XposedHelpers.findClass(GRINDR_PKG + ".base.model.BaseUserSession", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(class_BaseUserSession, "isFreeUser", RETURN_FALSE);
            XposedHelpers.findAndHookMethod(class_BaseUserSession, "isPaidUser", RETURN_TRUE);
            XposedHelpers.findAndHookMethod(class_BaseUserSession, "isXtra", RETURN_TRUE);
            XposedHelpers.findAndHookMethod(class_BaseUserSession, "isNoXtraUpsell", RETURN_TRUE); //Not sure about what's this.
            XposedHelpers.findAndHookMethod(class_BaseUserSession, "isUnlimited", RETURN_TRUE);
        }
    }
}