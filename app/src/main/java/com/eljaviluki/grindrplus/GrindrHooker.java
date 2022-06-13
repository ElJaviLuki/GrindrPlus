package com.eljaviluki.grindrplus;

import static com.eljaviluki.grindrplus.Constants.GRINDR_PKG;
import static com.eljaviluki.grindrplus.Constants.Returns.RETURN_FALSE;
import static com.eljaviluki.grindrplus.Constants.Returns.RETURN_INTEGER_MAX_VALUE;
import static com.eljaviluki.grindrplus.Constants.Returns.RETURN_TRUE;
import static com.eljaviluki.grindrplus.Obfuscation.*;
import static de.robv.android.xposed.XposedHelpers.*;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GrindrHooker implements IXposedHookLoadPackage {


    Class<?> class_Feature;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(GRINDR_PKG)) return;

        class_Feature = findClassIfExists(GApp.model.Feature, lpparam.classLoader);
        {
            /*
                Grant all the Grindr features (except disabling screenshots).
                A few more changes may be needed to use all the features.
            */
            if(class_Feature != null){
                findAndHookMethod(class_Feature, GApp.model.Feature_.isGranted, RETURN_TRUE);
                findAndHookMethod(class_Feature, GApp.model.Feature_.isNotGranted, RETURN_FALSE);


                Class<?> class_IUserSession = findClass(GApp.storage.IUserSession, lpparam.classLoader);
                findAndHookMethod(class_Feature, GApp.model.Feature_.isGranted, class_IUserSession, RETURN_TRUE);
                findAndHookMethod(class_Feature, GApp.model.Feature_.isNotGranted, class_IUserSession, RETURN_FALSE);
            }else{
                XposedBridge.log("GRINDR - Class " + GRINDR_PKG + ".model.Feature not found.");
            }

        }

        /*
           Allow screenshots in all the views of the application (including expiring photos, albums, etc.)

           Inspired in the project https://github.com/veeti/DisableFlagSecure
           Credit and thanks to @veeti!
        */
        {
            findAndHookMethod(Window.class, "setFlags", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Integer flags = (Integer) param.args[0];
                    flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
                    param.args[0] = flags;
                }
            });
        }

        // Unlimited expiring photos
        {
            Class<?> class_ExpiringPhotoStatusResponse = findClassIfExists(GApp.model.ExpiringPhotoStatusResponse, lpparam.classLoader);

            if(class_ExpiringPhotoStatusResponse != null){
                try{
                    findAndHookMethod(class_ExpiringPhotoStatusResponse, GApp.model.ExpiringPhotoStatusResponse_.getTotal, RETURN_INTEGER_MAX_VALUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }

                try{
                    findAndHookMethod(class_ExpiringPhotoStatusResponse, GApp.model.ExpiringPhotoStatusResponse_.getAvailable, RETURN_INTEGER_MAX_VALUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }else{
                XposedBridge.log("GRINDR - Class " + GRINDR_PKG + ".model.ExpiringPhotoStatusResponse not found.");
            }
        }

        /*
            Add extra profile fields with more information:
                - Profile ID
                - Last seen (exact date and time)
        */
        try{
            Class<?> class_ProfileFieldsView = findClassIfExists(GApp.ui.profileV2.ProfileFieldsView, lpparam.classLoader);
            Class<?> class_Profile = findClassIfExists(GApp.persistence.model.Profile, lpparam.classLoader);
            Class<?> class_ExtendedProfileFieldView = findClassIfExists(GApp.view.ExtendedProfileFieldView, lpparam.classLoader);
            Class<?> class_R_color = findClassIfExists(GApp.R.color, lpparam.classLoader);
            Class<?> class_Styles = findClassIfExists(GApp.utils.Styles, lpparam.classLoader);

            findAndHookMethod(class_ProfileFieldsView, "setProfile", class_Profile, new XC_MethodHook() {
                Object fieldsViewInstance;
                Object context;

                int labelColorId; //Label color cannot be assigned when the program has just been launched, since the data to be used is not created at this point.
                final int valueColorId = getStaticIntField(class_R_color, GApp.R.color_.grindr_pure_white); //R.color.grindr_pure_white

                private int getLabelColorId(){
                    Object stylesSingleton = getStaticObjectField(class_Styles, GApp.utils.Styles_.INSTANCE);

                    //Some color field reference (maybe 'pureWhite', not sure)
                    return (int) callMethod(stylesSingleton, GApp.utils.Styles_._maybe_pureWhite);
                }

                private String toReadableDate(long timestamp){
                    return SimpleDateFormat.getDateTimeInstance().format(new Date(timestamp));
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    fieldsViewInstance = param.thisObject;
                    context = callMethod(fieldsViewInstance, "getContext"); //Call this.getContext()

                    labelColorId = getLabelColorId();

                    //Get profile instance in the 1st parameter
                    Object profile = param.args[0];

                    addProfileFieldUi("Profile ID", (CharSequence) getObjectField(profile, "profileId"));
                    addProfileFieldUi("Last Seen", toReadableDate(getLongField(profile, "seen")));

                    //.setVisibility() of param.thisObject to always VISIBLE (otherwise if the profile has no fields, the additional ones will not be shown)
                    callMethod(param.thisObject, "setVisibility", View.VISIBLE);
                }

                private void addProfileFieldUi(CharSequence label, CharSequence value) {
                    Object extendedProfileFieldView = newInstance(class_ExtendedProfileFieldView, context);

                    callMethod(extendedProfileFieldView, GApp.view.ExtendedProfileFieldView_.setLabel, label, labelColorId);
                    callMethod(extendedProfileFieldView, GApp.view.ExtendedProfileFieldView_.setValue, value, valueColorId);

                    //From View.setContentDescription(...)
                    callMethod(extendedProfileFieldView, "setContentDescription", value);

                    //(ProfileFieldsView).addView(Landroid/view/View;)V
                    callMethod(fieldsViewInstance, "addView", extendedProfileFieldView);
                }
            });
        }catch (Exception e){
            XposedBridge.log(e);
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
            Class<?> class_UserSession = findClassIfExists(GApp.storage.UserSession, lpparam.classLoader);
            hookUserSessionImpl(class_UserSession);

            Class<?> class_UserSession2 = findClassIfExists(GApp.storage.UserSession2, lpparam.classLoader);
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
                        Class<?> class_ChatRepo = findClass(GApp.persistence.repository.ChatRepo, lpparam.classLoader);

                        findAndHookMethod(class_ChatRepo, GApp.persistence.repository.ChatRepo_.checkMessageForVideoCall, String.class, class_Continuation,
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
                    Allow to use SOME (not all of them) hidden features that Grindr developers have not yet made public or they are just testing.
                */
                try{
                    Class<?> class_Experiments = findClass(GApp.experiment.Experiments, lpparam.classLoader);
                    Class<?> class_IExperimentsManager = findClass(GApp.base.Experiment.IExperimentManager, lpparam.classLoader);

                    findAndHookMethod(class_Experiments, GApp.experiment.Experiments_.uncheckedIsEnabled_expMgr, class_IExperimentsManager, RETURN_TRUE);
                }catch(Exception e){
                    XposedBridge.log(e);
                }
            }
        }

    }

    private void hookUserSessionImpl(Class<?> UserSessionImpl) {
        if(UserSessionImpl != null){
            try{
                findAndHookMethod(UserSessionImpl, GApp.storage.IUserSession_.hasFeature_feature, class_Feature, RETURN_TRUE);
            }catch(Exception e){
                XposedBridge.log(e);
            }

            try{
                findAndHookMethod(UserSessionImpl, GApp.storage.IUserSession_.isFree, RETURN_FALSE);
            }catch(Exception e){
                XposedBridge.log(e);
            }

            //Not sure what is this for
            try{
                findAndHookMethod(UserSessionImpl, GApp.storage.IUserSession_.isNoXtraUpsell, RETURN_FALSE);
            }catch(Exception e){
                XposedBridge.log(e);
            }

            try{
                findAndHookMethod(UserSessionImpl, GApp.storage.IUserSession_.isXtra, RETURN_TRUE);
            }catch(Exception e){
                XposedBridge.log(e);
            }

            try{
                findAndHookMethod(UserSessionImpl, GApp.storage.IUserSession_.isUnlimited, RETURN_TRUE);
            }catch(Exception e) {
                XposedBridge.log(e);
            }
        }else{
            XposedBridge.log("GRINDR - Implementation of UserSession not found.");
        }
    }
}