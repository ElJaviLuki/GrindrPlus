package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField

class ExpiringPhotos: Hook("Expiring photos",
    "Allow unlimited photo viewing") {
    private val expiringImageBody = "com.grindrapp.android.model.ExpiringImageBody"
    private val expiringImageBodyUiData = "com.grindrapp.android.ui.chat.model.BodyUiData\$ExpiringImageBodyUiData"

    override fun init() {
        findClass(expiringImageBodyUiData)
            ?.hook("hasViewsRemaining", HookStage.AFTER) { param ->
                param.setResult(true)
            }

        findClass(expiringImageBody)
            ?.hook("getDuration", HookStage.AFTER) { param ->
                param.setResult(Long.MAX_VALUE)
            }

        findClass(expiringImageBody)
            ?.hook("getViewsRemaining", HookStage.AFTER) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        findClass(expiringImageBody)
            ?.hook("getUrl", HookStage.AFTER) { param ->
                val mediaId = getObjectField(param.thisObject(), "mediaId") as Long
                val url = getObjectField(param.thisObject(), "url")?.toString()

                if (url != null) {
                    if (context.database.getPhoto(mediaId) == null) {
                        context.database.addPhoto(mediaId, url)
                    }
                } else {
                    param.setResult(context.database.getPhoto(mediaId))
                }
            }
    }
}