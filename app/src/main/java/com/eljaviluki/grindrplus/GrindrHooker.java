package com.eljaviluki.grindrplus;

import static de.robv.android.xposed.XposedHelpers.*;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GrindrHooker implements IXposedHookLoadPackage {
    static final XC_MethodReplacement RETURN_TRUE = XC_MethodReplacement.returnConstant(true);
    static final XC_MethodReplacement RETURN_FALSE = XC_MethodReplacement.returnConstant(false);
    static final XC_MethodReplacement RETURN_INTEGER_MAX_VALUE = XC_MethodReplacement.returnConstant(Integer.MAX_VALUE);
    static final String GRINDR_PKG = "com.grindrapp.android";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(GRINDR_PKG))
            return;

        /*
            Grant all the Grindr features (except disabling screenshots).
            A few more changes may be needed to use all the features.
        */
        {
            Class<?> class_Feature = findClass(GRINDR_PKG + ".model.Feature", lpparam.classLoader);

            /*
                Hook:   .method public final isGranted()Z
                    Hook it, the callback will check if this.name != "DisableScreenshot" and then return true.
            */
            findAndHookMethod(class_Feature, "isGranted", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    //Equivalent:   if(this.name.equals("DisableScreenshot"))
                    if (((String) getObjectField(param.thisObject, "name")).equals("DisableScreenshot")){
                        return false;
                    }

                    return true;
                }
            });

            /*
                Hook:   .method public final isNotGranted()Z
                    Make it return the opposite to isGranted().
            */
            findAndHookMethod(class_Feature, "isNotGranted", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    //Equivalent:   return !this.isGranted();
                    return !((boolean) callMethod(param.thisObject, "isGranted"));
                }
            });
        }

        // Unlimited expiring photos
        {
            /*
                Hack Lcom/grindrapp/android/model/ExpiringPhotoStatusResponse;->total:I to give unlimited expiring photos.
                Hack Lcom/grindrapp/android/model/ExpiringPhotoStatusResponse;->available:I to give unlimited expiring photos.
            */
            Class<?> class_ExpiringPhotoStatusResponse = findClass(GRINDR_PKG + ".model.ExpiringPhotoStatusResponse", lpparam.classLoader);

            /*
                Hook:   .method public final getTotal()I
                    Make it return a constant value.
            */
            findAndHookMethod(class_ExpiringPhotoStatusResponse, "getTotal", RETURN_INTEGER_MAX_VALUE);

            /*
                Hook:   .method public final getAvailable()I
                    Make it return a constant value.
            */
            findAndHookMethod(class_ExpiringPhotoStatusResponse, "getAvailable", RETURN_INTEGER_MAX_VALUE);
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