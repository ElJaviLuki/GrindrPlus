package com.eljaviluki.grindrplus

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import android.view.View
import android.view.Window
import de.robv.android.xposed.XC_MethodHook
import android.view.WindowManager
import com.eljaviluki.grindrplus.Obfuscation.GApp.ui.profileV2
import com.eljaviluki.grindrplus.Obfuscation.GApp.view
import com.eljaviluki.grindrplus.Obfuscation.GApp.utils
import com.eljaviluki.grindrplus.Obfuscation.GApp.R.color_
import com.eljaviluki.grindrplus.Obfuscation.GApp.utils.Styles_
import com.eljaviluki.grindrplus.Obfuscation.GApp.view.ExtendedProfileFieldView_
import com.eljaviluki.grindrplus.Obfuscation.GApp.storage
import com.eljaviluki.grindrplus.Obfuscation.GApp.storage.IUserSession_
import com.eljaviluki.grindrplus.Obfuscation.GApp.model.ExpiringPhotoStatusResponse_
import com.eljaviluki.grindrplus.Obfuscation.GApp.model.Feature_
import com.eljaviluki.grindrplus.Obfuscation.GApp.experiment
import com.eljaviluki.grindrplus.Obfuscation.GApp.base.Experiment
import com.eljaviluki.grindrplus.Obfuscation.GApp.experiment.Experiments_
import com.eljaviluki.grindrplus.Obfuscation.GApp.persistence.repository
import com.eljaviluki.grindrplus.Obfuscation.GApp.persistence.repository.ChatRepo_
import com.eljaviluki.grindrplus.Obfuscation.GApp.persistence

object Hooks {
    /**
     * Allow screenshots in all the views of the application (including expiring photos, albums, etc.)
     *
     * Inspired in the project https://github.com/veeti/DisableFlagSecure
     * Credit and thanks to @veeti!
     */
    fun allowScreenshotsHook() {
        XposedHelpers.findAndHookMethod(
            Window::class.java,
            "setFlags",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    var flags = param.args[0] as Int
                    flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                    param.args[0] = flags
                }
            })
    }

    /**
     * Add extra profile fields with more information:
     * - Profile ID
     * - Last seen (exact date and time)
     */
    fun addExtraProfileFields() {
        val class_ProfileFieldsView = XposedHelpers.findClass(
            profileV2.ProfileFieldsView,
            Hooker.pkgParam!!.classLoader
        )
        val class_Profile = XposedHelpers.findClass(
            persistence.model.Profile,
            Hooker.pkgParam!!.classLoader
        )
        val class_ExtendedProfileFieldView = XposedHelpers.findClass(
            view.ExtendedProfileFieldView,
            Hooker.pkgParam!!.classLoader
        )
        val class_R_color = XposedHelpers.findClass(
            Obfuscation.GApp.R.color,
            Hooker.pkgParam!!.classLoader
        )
        val class_Styles =
            XposedHelpers.findClass(utils.Styles, Hooker.pkgParam!!.classLoader)
        XposedHelpers.findAndHookMethod(
            class_ProfileFieldsView,
            "setProfile",
            class_Profile,
            object : XC_MethodHook() {
                var fieldsViewInstance: Any? = null
                var context: Any? = null
                var labelColorId //Label color cannot be assigned when the program has just been launched, since the data to be used is not created at this point.
                        = 0
                val valueColorId = XposedHelpers.getStaticIntField(
                    class_R_color,
                    color_.grindr_pure_white
                ) //R.color.grindr_pure_white

                private fun obtainLabelColorId(): Int {
                    val stylesSingleton =
                        XposedHelpers.getStaticObjectField(class_Styles, Styles_.INSTANCE)

                    //Some color field reference (maybe 'pureWhite', not sure)
                    return XposedHelpers.callMethod(
                        stylesSingleton,
                        Styles_._maybe_pureWhite
                    ) as Int
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    fieldsViewInstance = param.thisObject
                    context = XposedHelpers.callMethod(
                        fieldsViewInstance,
                        "getContext"
                    ) //Call this.getContext()
                    labelColorId = obtainLabelColorId()

                    //Get profile instance in the 1st parameter
                    val profile = param.args[0]
                    addProfileFieldUi(
                        "Profile ID",
                        XposedHelpers.getObjectField(profile, "profileId") as CharSequence
                    )
                    addProfileFieldUi(
                        "Last Seen",
                        Utils.toReadableDate(XposedHelpers.getLongField(profile, "seen"))
                    )

                    //.setVisibility() of param.thisObject to always VISIBLE (otherwise if the profile has no fields, the additional ones will not be shown)
                    XposedHelpers.callMethod(param.thisObject, "setVisibility", View.VISIBLE)
                }

                private fun addProfileFieldUi(label: CharSequence, value: CharSequence?) {
                    val extendedProfileFieldView =
                        XposedHelpers.newInstance(class_ExtendedProfileFieldView, context)
                    XposedHelpers.callMethod(
                        extendedProfileFieldView,
                        ExtendedProfileFieldView_.setLabel,
                        label,
                        labelColorId
                    )
                    XposedHelpers.callMethod(
                        extendedProfileFieldView,
                        ExtendedProfileFieldView_.setValue,
                        value,
                        valueColorId
                    )

                    //From View.setContentDescription(...)
                    XposedHelpers.callMethod(
                        extendedProfileFieldView,
                        "setContentDescription",
                        value
                    )

                    //(ProfileFieldsView).addView(Landroid/view/View;)V
                    XposedHelpers.callMethod(
                        fieldsViewInstance,
                        "addView",
                        extendedProfileFieldView
                    )
                }
            })
    }

    /**
     * Hook these methods in all the classes that implement IUserSession.
     * isFree()Z (return false)
     * isNoXtraUpsell()Z (return false)
     * isXtra()Z to give Xtra account features.
     * isUnlimited()Z to give Unlimited account features.
     */
    fun hookUserSessionImpl() {
        val classes = listOf(
            XposedHelpers.findClass(
            storage.UserSession,
            Hooker.pkgParam!!.classLoader
        ),
            XposedHelpers.findClass(
            storage.UserSession2,
            Hooker.pkgParam!!.classLoader
        ))
        

        //Apply the hook to all the classes using lambda expressions
        val class_Feature = XposedHelpers.findClass(
            Obfuscation.GApp.model.Feature,
            Hooker.pkgParam!!.classLoader
        )

        classes.forEach {
            XposedHelpers.findAndHookMethod(
                it,
                IUserSession_.hasFeature_feature,
                class_Feature,
                Constants.Returns.RETURN_TRUE
            )
            XposedHelpers.findAndHookMethod(
                it,
                IUserSession_.isFree,
                Constants.Returns.RETURN_FALSE
            )
            XposedHelpers.findAndHookMethod(
                it,
                IUserSession_.isNoXtraUpsell,
                Constants.Returns.RETURN_FALSE
            ) //Not sure what is this for
            XposedHelpers.findAndHookMethod(
                it,
                IUserSession_.isXtra,
                Constants.Returns.RETURN_TRUE
            )
            XposedHelpers.findAndHookMethod(
                it,
                IUserSession_.isUnlimited,
                Constants.Returns.RETURN_TRUE
            )
        }
    }

    fun unlimitedExpiringPhotos() {
        val class_ExpiringPhotoStatusResponse = XposedHelpers.findClass(
            Obfuscation.GApp.model.ExpiringPhotoStatusResponse,
            Hooker.pkgParam!!.classLoader
        )
        XposedHelpers.findAndHookMethod(
            class_ExpiringPhotoStatusResponse,
            ExpiringPhotoStatusResponse_.getTotal,
            Constants.Returns.RETURN_INTEGER_MAX_VALUE
        )
        XposedHelpers.findAndHookMethod(
            class_ExpiringPhotoStatusResponse,
            ExpiringPhotoStatusResponse_.getAvailable,
            Constants.Returns.RETURN_INTEGER_MAX_VALUE
        )
    }

    /**
     * Grant all the Grindr features (except disabling screenshots).
     * A few more changes may be needed to use all the features.
     */
    fun hookFeatureGranting() {
        val class_Feature = XposedHelpers.findClass(
            Obfuscation.GApp.model.Feature,
            Hooker.pkgParam!!.classLoader
        )
        XposedHelpers.findAndHookMethod(
            class_Feature,
            Feature_.isGranted,
            Constants.Returns.RETURN_TRUE
        )
        XposedHelpers.findAndHookMethod(
            class_Feature,
            Feature_.isNotGranted,
            Constants.Returns.RETURN_FALSE
        )
        val class_IUserSession =
            XposedHelpers.findClass(storage.IUserSession, Hooker.pkgParam!!.classLoader)
        XposedHelpers.findAndHookMethod(
            class_Feature,
            Feature_.isGranted,
            class_IUserSession,
            Constants.Returns.RETURN_TRUE
        )
        XposedHelpers.findAndHookMethod(
            class_Feature,
            Feature_.isNotGranted,
            class_IUserSession,
            Constants.Returns.RETURN_FALSE
        )
    }

    /**
     * Allow to use SOME (not all of them) hidden features that Grindr developers have not yet made public
     * or they are just testing.
     */
    fun allowSomeExperiments() {
        val class_Experiments =
            XposedHelpers.findClass(experiment.Experiments, Hooker.pkgParam!!.classLoader)
        val class_IExperimentsManager = XposedHelpers.findClass(
            Experiment.IExperimentManager,
            Hooker.pkgParam!!.classLoader
        )
        XposedHelpers.findAndHookMethod(
            class_Experiments,
            Experiments_.uncheckedIsEnabled_expMgr,
            class_IExperimentsManager,
            Constants.Returns.RETURN_TRUE
        )
    }

    /**
     * Allow videocalls on empty chats: Grindr checks that both users have chatted with each other
     * (both must have sent at least one message to the other) in order to allow videocalls.
     *
     * This hook allows the user to bypass this restriction.
     */
    fun allowVideocallsOnEmptyChats() {
        //Find Kotlin utils
        val class_Continuation = XposedHelpers.findClass(
            "kotlin.coroutines.Continuation",
            Hooker.pkgParam!!.classLoader
        )
        val class_Boxing = XposedHelpers.findClass(
            "kotlin.coroutines.jvm.internal.Boxing",
            Hooker.pkgParam!!.classLoader
        )
        val class_ChatRepo =
            XposedHelpers.findClass(repository.ChatRepo, Hooker.pkgParam!!.classLoader)
        val returnWrappedTrue = XposedHelpers.callStaticMethod(class_Boxing, "boxBoolean", true)
        XposedHelpers.findAndHookMethod(
            class_ChatRepo,
            ChatRepo_.checkMessageForVideoCall,
            String::class.java,
            class_Continuation,
            XC_MethodReplacement.returnConstant(returnWrappedTrue)
        )
    }

    /**
     * Allow Fake GPS in order to fake location.
     *
     * WARNING: Abusing this feature may result in a permanent ban on your Grindr account.
     */
    fun allowMockProvider() {
        val class_Location = XposedHelpers.findClass(
            "android.location.Location",
            Hooker.pkgParam!!.classLoader
        )
        XposedHelpers.findAndHookMethod(
            class_Location,
            "isFromMockProvider",
            Constants.Returns.RETURN_FALSE
        )
    }
}