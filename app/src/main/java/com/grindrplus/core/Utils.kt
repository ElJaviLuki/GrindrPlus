
package com.grindrplus.core

import android.content.Context
import android.content.Intent
import java.lang.reflect.Proxy
import kotlin.math.pow

object Utils {
    fun createServiceProxy(context: ModContext, originalService: Any, serviceClass: Class<*>,
                           blacklist: Array<String> = emptyArray()): Any {
        val invocationHandler = Proxy.getInvocationHandler(originalService)
        val createSuccess = context.loadClass("h9.a\$b")?.constructors?.firstOrNull()
        return Proxy.newProxyInstance(originalService.javaClass.classLoader,
            arrayOf(serviceClass)) { proxy, method, args ->
                if (blacklist.isEmpty()) {
                    createSuccess?.newInstance(Unit) ?: invocationHandler.invoke(proxy, method, args)
                } else {
                    if (method.name in blacklist) {
                        createSuccess?.newInstance(Unit) ?: invocationHandler.invoke(proxy, method, args)
                    } else {
                        invocationHandler.invoke(proxy, method, args)
                    }
                }
        }
    }

    fun openProfile(id: String, context: ModContext) {
        val profilesActivityClass = context.loadClass("com.grindrapp.android.ui.profileV2.ProfilesActivity")
        val profilesActivityInstance = profilesActivityClass?.getField("u0")?.get(null)
        val referrerTypeClass = context.loadClass("com.grindrapp.android.base.model.profile.ReferrerType")
        val referrerType = referrerTypeClass?.getField("NOTIFICATION")?.get(null)
        val profilesActivityInnerClass = context.loadClass("com.grindrapp.android.ui.profileV2.ProfilesActivity\$a")

        val method = profilesActivityInnerClass?.declaredMethods?.find {
            it.parameterTypes.size == 3 && it.parameterTypes[2] == referrerTypeClass
        }

        val intent = method?.invoke(profilesActivityInstance, context.androidContext, id, referrerType) as Intent?
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val generalDeepLinksClass = context.loadClass("com.grindrapp.android.deeplink.GeneralDeepLinks")
        val startActivityMethod = generalDeepLinksClass?.getDeclaredMethod(
            "safedk_Context_startActivity_97cb3195734cf5c9cc3418feeafa6dd6", Context::class.java, Intent::class.java
        )

        startActivityMethod?.invoke(null, context.androidContext, intent)
    }

    fun calculateBMI(isMetric: Boolean, weight: Double, height: Double): Double {
        return if (isMetric) {
            weight / (height / 100).pow(2)
        } else {
            703 * weight / height.pow(2)
        }
    }
}