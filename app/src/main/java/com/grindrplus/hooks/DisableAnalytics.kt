package com.grindrplus.hooks

import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class DisableAnalytics: Hook(
    "Disable analytics",
    "Disable Grindr analytics (data collection)"
) {
    private val retrofit = "retrofit2.Retrofit"
    private val analyticsRestService = "v5.b"

    override fun init() {
        val analyticsRestServiceClass = findClass(analyticsRestService) ?: return

        findClass(retrofit)?.hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service != null && analyticsRestServiceClass.isAssignableFrom(service.javaClass)) {
                val proxy = createServiceProxy(service, analyticsRestServiceClass)
                param.setResult(proxy)
            }
        }
    }
}