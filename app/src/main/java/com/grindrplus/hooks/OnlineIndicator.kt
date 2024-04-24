package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import kotlin.time.Duration.Companion.minutes

class OnlineIndicator: Hook("Online indicator",
    "Customize online indicator duration") {
    val utils = "ne.t0"

    override fun init() {
        findClass(utils) // shouldShowOnlineIndicator()
            ?.hook("a", HookStage.AFTER) { param ->
                param.setResult(
                    System.currentTimeMillis() - param.arg<Long>(0)
                                <= 3.minutes.inWholeMilliseconds)
            }
    }
}