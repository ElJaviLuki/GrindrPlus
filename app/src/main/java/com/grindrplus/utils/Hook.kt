package com.grindrplus.utils

import com.grindrplus.core.Config
import com.grindrplus.core.ModContext

abstract class Hook(
    val hookName : String,
    val hookDesc : String = "",
) {
    lateinit var context: ModContext

    /**
     * Hook specific initialization.
     */
    open fun init() {}

    /**
     * Hook specific cleanup.
     */
    open fun cleanup() {}

    protected fun isHookEnabled(): Boolean {
        return Config.isHookEnabled(hookName)
    }

    protected fun findClass(name: String): Class<*>? {
        return try {
            context.loadClass(name)
        } catch (e: ClassNotFoundException) {
            context.logger.log("Failed to find class: $name")
            null
        }
    }

    protected fun loadClass(name: String): Class<*>? {
        return try {
            context.dexClassLoader.loadClass(name)
        } catch (e: ClassNotFoundException) {
            context.logger.log("Failed to load class: $name")
            null
        }
    }

    protected fun getResource(name: String, type: String): Int {
        return context.androidContext.resources.getIdentifier(
            name, type, context.androidContext.packageName)
    }
}