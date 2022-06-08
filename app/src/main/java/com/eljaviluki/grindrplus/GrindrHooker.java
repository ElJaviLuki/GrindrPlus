package com.eljaviluki.grindrplus;

import static de.robv.android.xposed.XposedHelpers.*;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GrindrHooker implements IXposedHookLoadPackage {
    static final XC_MethodReplacement RETURN_TRUE = XC_MethodReplacement.returnConstant(true);
    static final XC_MethodReplacement RETURN_FALSE = XC_MethodReplacement.returnConstant(false);
    static final XC_MethodReplacement RETURN_INTEGER_MAX_VALUE = XC_MethodReplacement.returnConstant(Integer.MAX_VALUE);
    static final String GRINDR_PKG = "com.grindrapp.android";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(GRINDR_PKG)) return;

        /*
            Grant all the Grindr features (except disabling screenshots).
            A few more changes may be needed to use all the features.
        */
        {
            Class<?> class_Feature = findClassIfExists(GRINDR_PKG + ".model.Feature", lpparam.classLoader);

            if(class_Feature != null){
                /*
                    Hook:   .method public final isGranted()Z
                        Hook it, the callback will check if this.name != "DisableScreenshot" and then return true.
                */
                try{
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
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final isNotGranted()Z
                        Make it return the opposite to isGranted().
                */
                try{
                    findAndHookMethod(class_Feature, "isNotGranted", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            //Equivalent:   return !this.isGranted();
                            return !((boolean) callMethod(param.thisObject, "isGranted"));
                        }
                    });
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }else{
                XposedBridge.log("GRINDR - Class " + GRINDR_PKG + ".model.Feature not found.");
            }

        }

        // Unlimited expiring photos
        {
            /*
                Hack Lcom/grindrapp/android/model/ExpiringPhotoStatusResponse;->total:I to give unlimited expiring photos.
                Hack Lcom/grindrapp/android/model/ExpiringPhotoStatusResponse;->available:I to give unlimited expiring photos.
            */
            Class<?> class_ExpiringPhotoStatusResponse = findClassIfExists(GRINDR_PKG + ".model.ExpiringPhotoStatusResponse", lpparam.classLoader);

            if(class_ExpiringPhotoStatusResponse != null){
                /*
                    Hook:   .method public final getTotal()I
                        Make it return a constant value.
                */
                try{
                    findAndHookMethod(class_ExpiringPhotoStatusResponse, "getTotal", RETURN_INTEGER_MAX_VALUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final getAvailable()I
                        Make it return a constant value.
                */
                try{
                    findAndHookMethod(class_ExpiringPhotoStatusResponse, "getAvailable", RETURN_INTEGER_MAX_VALUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }else{
                XposedBridge.log("GRINDR - Class " + GRINDR_PKG + ".model.ExpiringPhotoStatusResponse not found.");
            }
        }

        // Unlimited and Xtra account features
        {
            /*
                Hook these methods in all the classes that implement IUserSession.
                    isFree()Z (return false)
                    isNoXtraUpsell()Z (return false)
                    isXtra()Z to give Xtra account features.
                    isUnlimited()Z to give Unlimited account features.
            */

            Class<?> class_UserSession = findClassIfExists(GRINDR_PKG + ".storage.ai", lpparam.classLoader);
            if(class_UserSession != null){
                /*
                    Hook:   .method public final isFree()Z
                        Make it return false.
                        Some features will not work if set to true.
                */
                try{
                    findAndHookMethod(class_UserSession, "g", RETURN_FALSE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final isNoXtraUpsell()Z
                        Make it return a constant value.
                        Not sure what this is for.
                */
                try{
                    findAndHookMethod(class_UserSession, "h", RETURN_FALSE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final isXtra()Z
                        Make it return a constant value.
                */
                try{
                    findAndHookMethod(class_UserSession, "i", RETURN_TRUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final isUnlimited()Z
                        Make it return a constant value.
                */
                try{
                    findAndHookMethod(class_UserSession, "j", RETURN_TRUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }else{
                XposedBridge.log("GRINDR - Class " + GRINDR_PKG + ".storage.UserSession (obfuscated) not found.");
            }

            Class<?> class_UserSession2 = findClassIfExists(GRINDR_PKG + ".storage.aj", lpparam.classLoader);
            if(class_UserSession2 != null){
                /*
                    Hook:   .method public final isFree()Z
                        Make it return false.
                        Some features will not work if set to true.
                */
                try{
                    findAndHookMethod(class_UserSession2, "g", RETURN_FALSE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final isNoXtraUpsell()Z
                        Make it return a constant value.
                        Not sure what this is for.
                */
                try{
                    findAndHookMethod(class_UserSession2, "h", RETURN_FALSE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final isXtra()Z
                        Make it return a constant value.
                */
                try{
                    findAndHookMethod(class_UserSession2, "i", RETURN_TRUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                /*
                    Hook:   .method public final isUnlimited()Z
                        Make it return a constant value.
                */
                try{
                    findAndHookMethod(class_UserSession2, "j", RETURN_TRUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }else{
                XposedBridge.log("GRINDR - Class " + GRINDR_PKG + ".storage.UserSession2 (obfuscated) not found.");
            }
        }

        {
            Class<?> class_Location = findClassIfExists("android.location.Location", lpparam.classLoader);
            if(class_Location != null){
                /*
                    Allow Fake GPS in order to fake location.

                    WARNING: Abusing this feature may result in a permanent ban on your Grindr account.
                */
                try{
                    findAndHookMethod(class_Location, "isFromMockProvider", RETURN_FALSE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }else{
                XposedBridge.log("GRINDR - Class android.location.Location not found");
            }
        }

        /*
            Allow videocalls on empty chats: Grindr checks that both users have chatted with each other
            (both must have sent at least one message to the other) in order to allow videocalls.

            This hook allows the user to bypass this restriction.
        */
        {
            try{
                Class<?> class_ChatRepo = findClass(GRINDR_PKG + ".persistence.repository.ChatRepo", lpparam.classLoader);
                Class<?> class_Continuation = findClass("kotlin.coroutines.Continuation", lpparam.classLoader);
                Class<?> class_Boxing = findClass("kotlin.coroutines.jvm.internal.Boxing", lpparam.classLoader);

                Object returnWrappedTrue = callStaticMethod(class_Boxing, "boxBoolean", true);
                findAndHookMethod(class_ChatRepo, "checkMessageForVideoCall", String.class, class_Continuation,
                        XC_MethodReplacement.returnConstant(returnWrappedTrue));
            }catch(Exception e){
                XposedBridge.log(e);
            }

        }
    }
}