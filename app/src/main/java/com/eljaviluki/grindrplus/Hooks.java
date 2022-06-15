package com.eljaviluki.grindrplus;

import static com.eljaviluki.grindrplus.Constants.Returns.RETURN_FALSE;
import static com.eljaviluki.grindrplus.Constants.Returns.RETURN_INTEGER_MAX_VALUE;
import static com.eljaviluki.grindrplus.Constants.Returns.RETURN_TRUE;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getLongField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;

import com.eljaviluki.grindrplus.Obfuscation.GApp;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class Hooks {
    /**
       Allow screenshots in all the views of the application (including expiring photos, albums, etc.)

       Inspired in the project https://github.com/veeti/DisableFlagSecure
       Credit and thanks to @veeti!
    */
    public static void allowScreenshotsHook(){
        findAndHookMethod(Window.class, "setFlags", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Integer flags = (Integer) param.args[0];
                flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
                param.args[0] = flags;
            }
        });
    }

    /**
        Add extra profile fields with more information:
            - Profile ID
            - Last seen (exact date and time)
    */
    public static void addExtraProfileFields() {
        Class<?> class_ProfileFieldsView = findClass(GApp.ui.profileV2.ProfileFieldsView, Hooker.pkgParam.classLoader);
        Class<?> class_Profile = findClass(GApp.persistence.model.Profile, Hooker.pkgParam.classLoader);
        Class<?> class_ExtendedProfileFieldView = findClass(GApp.view.ExtendedProfileFieldView, Hooker.pkgParam.classLoader);
        Class<?> class_R_color = findClass(GApp.R.color, Hooker.pkgParam.classLoader);
        Class<?> class_Styles = findClass(GApp.utils.Styles, Hooker.pkgParam.classLoader);

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

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                fieldsViewInstance = param.thisObject;
                context = callMethod(fieldsViewInstance, "getContext"); //Call this.getContext()

                labelColorId = getLabelColorId();

                //Get profile instance in the 1st parameter
                Object profile = param.args[0];

                addProfileFieldUi("Profile ID", (CharSequence) getObjectField(profile, "profileId"));
                addProfileFieldUi("Last Seen", Utils.toReadableDate(getLongField(profile, "seen")));

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
    }

    /**
        Hook these methods in all the classes that implement IUserSession.
            isFree()Z (return false)
            isNoXtraUpsell()Z (return false)
            isXtra()Z to give Xtra account features.
            isUnlimited()Z to give Unlimited account features.
    */
    public static void hookUserSessionImpl() {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(findClass(GApp.storage.UserSession, Hooker.pkgParam.classLoader));
        classes.add(findClass(GApp.storage.UserSession2, Hooker.pkgParam.classLoader));

        //Apply the hook to all the classes using lambda expressions
        Class<?> class_Feature = findClass(GApp.model.Feature, Hooker.pkgParam.classLoader);
        classes.forEach(class_ -> {
            findAndHookMethod(class_, GApp.storage.IUserSession_.hasFeature_feature, class_Feature, RETURN_TRUE);

            findAndHookMethod(class_, GApp.storage.IUserSession_.isFree, RETURN_FALSE);
            findAndHookMethod(class_, GApp.storage.IUserSession_.isNoXtraUpsell, RETURN_FALSE); //Not sure what is this for
            findAndHookMethod(class_, GApp.storage.IUserSession_.isXtra, RETURN_TRUE);
            findAndHookMethod(class_, GApp.storage.IUserSession_.isUnlimited, RETURN_TRUE);
        });
    }

    public static void unlimitedExpiringPhotos() {
        Class<?> class_ExpiringPhotoStatusResponse = findClass(GApp.model.ExpiringPhotoStatusResponse, Hooker.pkgParam.classLoader);
        findAndHookMethod(class_ExpiringPhotoStatusResponse, GApp.model.ExpiringPhotoStatusResponse_.getTotal, RETURN_INTEGER_MAX_VALUE);
        findAndHookMethod(class_ExpiringPhotoStatusResponse, GApp.model.ExpiringPhotoStatusResponse_.getAvailable, RETURN_INTEGER_MAX_VALUE);
    }

    /**
        Grant all the Grindr features (except disabling screenshots).
        A few more changes may be needed to use all the features.
    */
    public static void hookFeatureGranting() {
        Class<?> class_Feature = findClass(GApp.model.Feature, Hooker.pkgParam.classLoader);
        findAndHookMethod(class_Feature, GApp.model.Feature_.isGranted, RETURN_TRUE);
        findAndHookMethod(class_Feature, GApp.model.Feature_.isNotGranted, RETURN_FALSE);


        Class<?> class_IUserSession = findClass(GApp.storage.IUserSession, Hooker.pkgParam.classLoader);
        findAndHookMethod(class_Feature, GApp.model.Feature_.isGranted, class_IUserSession, RETURN_TRUE);
        findAndHookMethod(class_Feature, GApp.model.Feature_.isNotGranted, class_IUserSession, RETURN_FALSE);
    }

    /**
     Allow to use SOME (not all of them) hidden features that Grindr developers have not yet made public
     or they are just testing.
     */
    public static void allowSomeExperiments() {
        Class<?> class_Experiments = findClass(GApp.experiment.Experiments, Hooker.pkgParam.classLoader);
        Class<?> class_IExperimentsManager = findClass(GApp.base.Experiment.IExperimentManager, Hooker.pkgParam.classLoader);
        findAndHookMethod(class_Experiments, GApp.experiment.Experiments_.uncheckedIsEnabled_expMgr, class_IExperimentsManager, RETURN_TRUE);
    }

    /**
        Allow videocalls on empty chats: Grindr checks that both users have chatted with each other
        (both must have sent at least one message to the other) in order to allow videocalls.

        This hook allows the user to bypass this restriction.
    */
    public static void allowVideocallsOnEmptyChats() {
        //Find Kotlin utils
        Class<?> class_Continuation = findClass("kotlin.coroutines.Continuation", Hooker.pkgParam.classLoader);
        Class<?> class_Boxing = findClass("kotlin.coroutines.jvm.internal.Boxing", Hooker.pkgParam.classLoader);

        Class<?> class_ChatRepo = findClass(GApp.persistence.repository.ChatRepo, Hooker.pkgParam.classLoader);

        Object returnWrappedTrue = callStaticMethod(class_Boxing, "boxBoolean", true);
        findAndHookMethod(class_ChatRepo, GApp.persistence.repository.ChatRepo_.checkMessageForVideoCall, String.class, class_Continuation, XC_MethodReplacement.returnConstant(returnWrappedTrue));
    }

    /**
        Allow Fake GPS in order to fake location.

        WARNING: Abusing this feature may result in a permanent ban on your Grindr account.
    */
    public static void allowMockProvider() {
        Class<?> class_Location = findClass("android.location.Location", Hooker.pkgParam.classLoader);
        findAndHookMethod(class_Location, "isFromMockProvider", RETURN_FALSE);
    }
}
