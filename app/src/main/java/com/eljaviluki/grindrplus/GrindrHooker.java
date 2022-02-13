package com.eljaviluki.grindrplus;

import static de.robv.android.xposed.XposedHelpers.*;

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
            Class<?> class_Feature = findClass(GRINDR_PKG + ".model.Feature", lpparam.classLoader);
            findAndHookMethod(class_Feature, "isGranted", RETURN_TRUE);
            findAndHookMethod(class_Feature, "isNotGranted", RETURN_FALSE);
        }

        /*
            Allow Fake GPS in order to fake location.

            WARNING: Abusing this feature may result in a permanent ban on your Grindr account.
         */
        {
            Class<?> class_Location = findClass("android.location.Location", lpparam.classLoader);
            findAndHookMethod(class_Location, "isFromMockProvider", RETURN_FALSE);
        }

        /*
            Allow videocalls on empty chats: Grindr checks that both users have chatted with each other
            (both must have sent at least one message to the other) in order to allow videocalls.

            This hook allows the user to bypass this restriction.
        */
        {
            Class<?> class_ChatRepo = findClass(GRINDR_PKG + ".persistence.repository.ChatRepo", lpparam.classLoader);
            Class<?> class_Continuation = findClass("kotlin.coroutines.Continuation", lpparam.classLoader);
            Class<?> class_Boxing = findClass("kotlin.coroutines.jvm.internal.Boxing", lpparam.classLoader);

            Object returnWrappedTrue = callStaticMethod(class_Boxing, "boxBoolean", true);
            findAndHookMethod(class_ChatRepo, "checkMessageForVideoCall", String.class, class_Continuation,
                    XC_MethodReplacement.returnConstant(returnWrappedTrue));
        }
    }
}