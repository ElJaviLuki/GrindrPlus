package com.grindrplus.core

import Database
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.utils.HookManager
import dalvik.system.DexClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class ModContext(val androidContext: Context, val dexClassLoader: DexClassLoader) {
    var mainActivity: Any? = null
    var currentActivity: Activity? = null
    var appDataDir = GrindrPlus.loadPackageParam.appInfo.dataDir
    var pkgVersionName = androidContext.packageManager
        .getPackageInfo(androidContext.packageName, 0).versionName
    val hooks = HookManager(this)
    val logger = Logger(appDataDir + "/grindrplus.log")
    val database = Database(androidContext,
        appDataDir + "/grindrplus.db")
    var coroutineScope = CoroutineScope(Dispatchers.IO)

    fun runOnUiThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        } else {
            Handler(Looper.getMainLooper()).post {
                runCatching(block).onFailure {
                    logger.log("Failed to" +
                            " run on UI thread: ${it.message}")
                }
            }
            mainActivity?.let {
                (it as? Activity)?.runOnUiThread(block)
            }
        }
    }

    fun executeAsync(block: suspend ModContext.() -> Unit) {
        coroutineScope.launch {
            runCatching {
                block()
            }.onFailure {
                logger.log("Failed to" +
                        " execute async block: ${it.message}")
            }
        }
    }

    fun showToast(type: Int, message: String) {
        runOnUiThread {
            Toast.makeText(androidContext, message, type).show()
        }
    }

    fun softRestartApp() {
        val intent: Intent? = androidContext.packageManager
            .getLaunchIntentForPackage(androidContext.packageName)
        intent?.let {
            androidContext.startActivity(
                Intent.makeRestartActivityTask(it.component)
            )
        }
        exitProcess(1) // Exit the current process
    }

    fun loadClass(name: String): Class<*>? {
        return try {
            androidContext.classLoader.loadClass(name)
        } catch (e: ClassNotFoundException) {
            logger.log("Failed to load class: $name")
            null
        }
    }
}