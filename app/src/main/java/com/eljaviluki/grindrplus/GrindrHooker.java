package com.eljaviluki.grindrplus;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GrindrHooker implements IXposedHookLoadPackage {
    static final XC_MethodReplacement RETURN_TRUE = XC_MethodReplacement.returnConstant(true);
    static final XC_MethodReplacement RETURN_FALSE = XC_MethodReplacement.returnConstant(false);
    static final XC_MethodReplacement RETURN_INTEGER_MAX_VALUE = XC_MethodReplacement.returnConstant(Integer.MAX_VALUE);
    static final String GRINDR_PKG = "com.grindrapp.android";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(GRINDR_PKG)) return;

        {
            /*
                Grant all the Grindr features (except disabling screenshots).
                A few more changes may be needed to use all the features.
            */
            Class<?> class_Feature = findClassIfExists(GRINDR_PKG + ".model.Feature", lpparam.classLoader);

            if(class_Feature != null){
                /*
                    "Grant a feature" Callback:
                        The callback will check if this.name != "DisableScreenshot" (the only feature I'm not interested in at this moment)
                        and then return true.
                */
                XC_MethodHook grantedCallback = new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        //Equivalent:   if(this.name.equals("DisableScreenshot"))
                        if (((String) getObjectField(param.thisObject, "name")).equals("DisableScreenshot")){
                            return false;
                        }

                        return true;
                    }
                };

                //This one just reverses the result of the previous one. (!false -> true)
                XC_MethodHook notGrantedCallback = new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        //Equivalent:   return !this.isGranted();
                        return !((boolean) callMethod(param.thisObject, "isGranted"));
                    }
                };
                

                // .method public final isGranted()Z, hook with 'grantedCallback'
                findAndHookMethod(class_Feature, "isGranted", grantedCallback);

                // .method public final isNotGranted()Z, hook with 'notGrantedCallback'
                findAndHookMethod(class_Feature, "isNotGranted", notGrantedCallback);


                Class<?> class_IUserSession = findClass(GRINDR_PKG + ".storage.IUserSession", lpparam.classLoader);
                // .method public final isGranted(Lcom/grindrapp/android/storage/IUserSession;)Z, hook with 'grantedCallback'
                findAndHookMethod(class_Feature, "isGranted", class_IUserSession, grantedCallback);

                // .method public final isNotGranted(Lcom/grindrapp/android/storage/IUserSession;)Z, hook with 'notGrantedCallback'
                findAndHookMethod(class_Feature, "isNotGranted", class_IUserSession, notGrantedCallback);
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
            hookUserSessionImpl(class_UserSession);

            Class<?> class_UserSession2 = findClassIfExists(GRINDR_PKG + ".storage.aj", lpparam.classLoader);
            hookUserSessionImpl(class_UserSession2);
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

        {
            //Find Kotlin utils
            Class<?> class_Continuation = findClassIfExists("kotlin.coroutines.Continuation", lpparam.classLoader);
            Class<?> class_Boxing = findClassIfExists("kotlin.coroutines.jvm.internal.Boxing", lpparam.classLoader);

            if(class_Continuation != null && class_Boxing != null){
                Object returnWrappedTrue = callStaticMethod(class_Boxing, "boxBoolean", true);

                {
                    /*
                        Allow videocalls on empty chats: Grindr checks that both users have chatted with each other
                        (both must have sent at least one message to the other) in order to allow videocalls.

                        This hook allows the user to bypass this restriction.
                    */
                    try{
                        Class<?> class_ChatRepo = findClass(GRINDR_PKG + ".persistence.repository.ChatRepo", lpparam.classLoader);

                        findAndHookMethod(class_ChatRepo, "checkMessageForVideoCall", String.class, class_Continuation,
                                XC_MethodReplacement.returnConstant(returnWrappedTrue));
                    }catch(Exception e){
                        XposedBridge.log(e);
                    }
                }
            }else{
                XposedBridge.log("GRINDR - Class kotlin.coroutines.Continuation or kotlin.coroutines.jvm.internal.Boxing not found.");
            }

            {
                /*
                    Hook in class .experiment.Experiments
                    public final fun uncheckedIsEnabled(expMgr: com.grindrapp.android.base.experiment.IExperimentsManager): kotlin.Boolean
                        Make it return true

                    This allows to use SOME (not all of them) hidden features that Grindr developers have not yet made public or they are just testing.
                */
                try{
                    Class<?> class_Experiments = findClass(GRINDR_PKG + ".experiment.Experiments", lpparam.classLoader);
                    Class<?> class_IExperimentsManager = findClass(GRINDR_PKG + ".base.g.b", lpparam.classLoader);

                    findAndHookMethod(class_Experiments, "a", class_IExperimentsManager, RETURN_TRUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }
        }

    }

    private void hookUserSessionImpl(Class<?> UserSessionImpl) {
        if(UserSessionImpl != null){
            /*
                Hook:   .method public final isFree()Z
                    Make it return false.
                    Some features will not work if set to true.
            */
            try{
                findAndHookMethod(UserSessionImpl, "i", RETURN_FALSE);
            }catch(Exception e){
                XposedBridge.log(e);
            }

            /*
                Hook:   .method public final isNoXtraUpsell()Z
                    Make it return a constant value.
                    Not sure what this is for.
            */
            try{
                findAndHookMethod(UserSessionImpl, "j", RETURN_FALSE);
            }catch(Exception e){
                XposedBridge.log(e);
            }

            /*
                Hook:   .method public final isXtra()Z
                    Make it return a constant value.
            */
            try{
                findAndHookMethod(UserSessionImpl, "k", RETURN_TRUE);
            }catch(Exception e){
                XposedBridge.log(e);
            }

            /*
                Hook:   .method public final isUnlimited()Z
                    Make it return a constant value.
            */
            try{
                findAndHookMethod(UserSessionImpl, "l", RETURN_TRUE);
            }catch(Exception e) {
                XposedBridge.log(e);
            }
        }else{
            XposedBridge.log("GRINDR - Implementation of UserSession not found.");
        }
    }
}