package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class EnableUnlimited: Hook("Enable unlimited",
    "Enable Grindr Unlimited features") {
    private val userSession = "com.grindrapp.android.storage.b"

    override fun init() {
        val userSessionClass = findClass(userSession)

        userSessionClass?.hook( // hasFeature()
            "x", HookStage.AFTER) { param ->
                val disallowedFeatures = setOf("DisableScreenshot")
                param.setResult(param.arg(0) !in disallowedFeatures)
            }

        userSessionClass?.hook( // isNoXtraUpsell()
            "i", HookStage.AFTER) { param ->
                param.setResult(true)
            }

        userSessionClass?.hook( // isNoPlusUpsell()
            "D", HookStage.AFTER) { param ->
            param.setResult(true)
        }

        userSessionClass?.hook( // isFree()
            "t", HookStage.BEFORE) { param ->
                param.setResult(false)
            }

        userSessionClass?.hook( // isXtra()
            "r", HookStage.AFTER) { param ->
                param.setResult(false)
            }

        userSessionClass?.hook( // isPlus()
            "B", HookStage.AFTER) { param ->
                param.setResult(false)
            }

        userSessionClass?.hook( // isUnlimited()
            "A", HookStage.BEFORE) { param ->
                param.setResult(true)
            }
    }
}