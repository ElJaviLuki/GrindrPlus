package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField

class UnlimitedProfiles: Hook("Unlimited profiles",
    "Allow unlimited profiles") {
    private var boostedProfilesList = emptyList<String>()
    private val serverDrivenCascadeCachedState =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCacheState"
    private val serverDrivenCascadeCachedProfile =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCachedProfile"
    private val inAccessibleProfileManager =
        "com.grindrapp.android.profile.experiments.InaccessibleProfileManager"

    override fun init() {
        findClass(inAccessibleProfileManager)
            ?.hook("a", HookStage.AFTER) { param ->
                param.setResult(true)
            }

        findClass(serverDrivenCascadeCachedState)
            ?.hook("getItems", HookStage.AFTER) { param ->
                val items = (param.getResult() as List<*>).filter {
                    (it?.javaClass?.name ?: "") == serverDrivenCascadeCachedProfile
                }

                items.forEach {
                    if (getObjectField(it, "isBoosting") as Boolean) {
                        boostedProfilesList += callMethod(it, "getProfileId") as String
                    }
                }

                param.setResult(items)
            }

        findClass(serverDrivenCascadeCachedProfile)
            ?.hook("getUpsellType", HookStage.AFTER) { param ->
                param.setResult(null)
            }
    }
}