package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod

class ProfileViews: Hook(
    "Profile views",
    "Don't let others know you viewed their profile"
) {
    private val profileRestService = "com.grindrapp.android.api.ProfileRestService"
    private val blacklistedPaths = setOf(
        "v4/views/{profileId}",
        "v4/views"
    )

    override fun init() {
        val profileRestServiceClass = findClass(profileRestService) ?: return

        val methodBlacklist = profileRestServiceClass.declaredMethods
            .asSequence()
            .filter {
                it.annotations.any {
                    it.annotationClass.java.name == "retrofit2.http.POST"
                            && callMethod(it, "value") in blacklistedPaths
                }
            }
            .map { it.name }
            .toList()

        if (methodBlacklist.size != blacklistedPaths.size) {
            GrindrPlus.logger.log("ProfileViews: not all blacklisted paths were found! Open an issue on GitHub.")
        }

        findClass("retrofit2.Retrofit")
            ?.hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service != null && profileRestServiceClass.isAssignableFrom(service.javaClass)) {
                val proxy = createServiceProxy(service, profileRestServiceClass, methodBlacklist.toTypedArray())
                param.setResult(proxy)
            }
        }
    }
}
