package com.grindrplus

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.grindrplus.core.Config
import com.grindrplus.core.Constants.TARGET_GRINDR_PKG_VERSION_NAME
import com.grindrplus.core.ModContext
import com.grindrplus.utils.HookAdapter
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import dalvik.system.DexClassLoader
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class GrindrPlus(
    lpparam: LoadPackageParam,
    private val dexClassLoader: DexClassLoader) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: ModContext private set
        lateinit var classLoader: ClassLoader private set
        lateinit var loadPackageParam: LoadPackageParam private set
    }

    private fun hookMainActivity(methodName: String, stage: HookStage =
        HookStage.AFTER, block: Activity.(param: HookAdapter) -> Unit) {
        Activity::class.java.hook(methodName, stage) { param ->
            val activity = param.thisObject() as Activity
            if (!activity.packageName.equals(TARGET_GRINDR_PKG_VERSION_NAME)) return@hook
            block(activity, param)
        }
    }

    init {
        loadPackageParam = lpparam

        Application::class.java.hook("attach", HookStage.BEFORE) { param ->
            appContext = ModContext(
                androidContext = param.arg<Context>(0)
                    .also { classLoader = it.classLoader },
                dexClassLoader = dexClassLoader
            )

            if (appContext.pkgVersionName != BuildConfig.TARGET_GRINDR_VERSION) {
                appContext.logger.log("Installed Grindr ${appContext.pkgVersionName} mismatched" +
                        " with target version $TARGET_GRINDR_PKG_VERSION_NAME.")
                appContext.showToast(Toast.LENGTH_LONG, "Installed Grindr ${appContext
                    .pkgVersionName} mismatched" + "with target version $TARGET_GRINDR_PKG_VERSION_NAME.")
                return@hook
            }

            /**
             * Hacky but works for now. Keep the track of the current activity in
             * case we need to execute some UI-related code in the UI thread.
             */
            appContext.androidContext.classLoader.loadClass("android.app.Instrumentation")
                ?.hook("newActivity", HookStage.AFTER) { param ->
                    val resultActivity = param.getResult() as Activity
                    val excludedActivities = setOf("ChatRoomPhotosActivity",
                        "ProfilesActivity", "FullScreenExpiringImageActivity")

                    if (appContext.currentActivity?.javaClass?.simpleName == "ChatActivityV2" &&
                        resultActivity.javaClass.simpleName in excludedActivities) return@hook

                    appContext.currentActivity = param.getResult() as Activity
                }

            appContext.apply {
                runCatching {
                    measureTimeMillis {
                        runBlocking {
                            init(this)
                        }
                    }.also {
                        logger.log("Initialization completed in $it ms.")
                    }
                }.onFailure {
                    logger.log("Failed to initialize: ${it.message}")
                    showToast(Log.ERROR, "Failed to initialize: ${it.message}")
                }
            }
        }

        hookMainActivity("onCreate") { param ->
            appContext.mainActivity = this
        }
    }

    private fun init(scope: CoroutineScope) {
        with (appContext) {
            logger.log("Initializing GrindrPlus...")
            Config.initialize(appContext.androidContext)

            /**
             * Emergency reset of the database if the flag is set.
             */
            if ((Config.get("reset_database", false) as Boolean)) {
                appContext.logger.log("Resetting database...")
                appContext.database.deleteDatabase()
                Config.put("reset_database", false)
            }

            hooks.init()
        }
    }
}