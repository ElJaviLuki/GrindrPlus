package com.eljaviluki.grindrplus;

import de.robv.android.xposed.XC_MethodReplacement;

public class Constants {
    public static final String GRINDR_PKG = "com.grindrapp.android";

    public static class Returns {
        static final XC_MethodReplacement RETURN_TRUE = XC_MethodReplacement.returnConstant(true);
        static final XC_MethodReplacement RETURN_FALSE = XC_MethodReplacement.returnConstant(false);
        static final XC_MethodReplacement RETURN_INTEGER_MAX_VALUE = XC_MethodReplacement.returnConstant(Integer.MAX_VALUE);
    }
}
