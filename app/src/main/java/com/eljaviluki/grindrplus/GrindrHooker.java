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

        String className;

        className = GRINDR_PKG + ".model.Feature";
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isGranted", RETURN_TRUE);
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isNotGranted", RETURN_FALSE);

        className = GRINDR_PKG + ".model.BaseUserSession";
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isFreeUser", RETURN_FALSE);
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isPaidUser", RETURN_TRUE);
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isXtra", RETURN_TRUE);
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isNoXtraUpsell", RETURN_TRUE); //Not sure about what's this.
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isUnlimited", RETURN_TRUE);
    }
}