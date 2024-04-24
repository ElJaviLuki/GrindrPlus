package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField

class UnlimitedAlbums: Hook("Unlimited albums",
    "Allow to be able to view unlimited albums") {
    private val albumModel = "com.grindrapp.android.model.Album"
    private val albumContent = "com.grindrapp.android.model.AlbumContent"

    override fun init() {
        val albumModelClass = findClass(albumModel)

        findClass(albumContent)
            ?.hook("getRemainingViews", HookStage.AFTER) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        albumModelClass?.hook(
            "getAlbumViewable", HookStage.AFTER) { param ->
                param.setResult(true)
            }

        albumModelClass?.hook(
            "getContent", HookStage.AFTER) { param ->
                val contentList = param.getResult() as List<*>
                val albumId = getObjectField(param.thisObject(), "albumId") as Long

                /**
                 * The first step is to find out if we're dealing with cover
                 * images or not. If we are, we don't have to do anything.
                 */
                if (contentList.isNotEmpty()) {
                    /**
                     * Covers usually have only one image, so we can just
                     * detect if it's a cover or not by checking the size of
                     * the list.
                     */
                    if (contentList.size == 1) {
                        return@hook // We're dealing with a cover image
                    }

                    /**
                     * At this point, we know we're dealing with an actual
                     * album. We can now proceed to save the contents if
                     * we haven't already.
                     */
                    if (context.database.getContentIdListByAlbumId(albumId).isNotEmpty()) {
                        /**
                         * It's possible that the album has been viewed before
                         * but the user added more images to it. In this case,
                         * we should update the album contents.
                         */
                        deleteContentsByList(albumId)
                    }
                    saveContentsByList(albumId, contentList)
                } else {
                    /**
                     * If the content is null, it means that the album is
                     * empty and we could try to retrieve the contents again.
                     */
                    if (context.database.getContentIdListByAlbumId(albumId).isNotEmpty()) {
                        val newContentsList = mutableListOf<Any>()
                        val savedContentsList = context.database.getContentIdListByAlbumId(albumId)
                        for (element in savedContentsList) {
                            val savedContent = context.database.getAlbumContent(element)
                            if (savedContent != null) {
                                loadClass(albumContent)?.constructors?.first()?.newInstance(
                                    savedContent.getAsLong("contentId"),
                                    savedContent.getAsString("contentType"),
                                    savedContent.getAsString("url"),
                                    savedContent.getAsBoolean("isProcessing"),
                                    savedContent.getAsString("thumbUrl"),
                                    savedContent.getAsString("coverUrl"),
                                    savedContent.getAsInteger("remainingViews")
                                )?.let { newContentsList.add(it) }
                            }
                        }

                        //setObjectField(param.thisObject(), "contentCount", newContentsList.size)
                        setObjectField(param.thisObject(), "content", newContentsList)

                        param.setResult(newContentsList)
                    }
                }
            }
    }

    private fun deleteContentsByList(albumId: Long) {
        val contents = context.database.getContentIdListByAlbumId(albumId)
        for (element in contents) {
            context.database.deleteAlbumContent(element)
        }
    }

    private fun saveContentsByList(albumId: Long, content: Any) {
        if (content is List<*>) {
            for (element in content) {
                if (getObjectField(element, "url") == null) {
                    continue // Skip invalid / empty content
                }
                context.database.addAlbumContent(
                    getObjectField(element, "contentId") as Long,
                    albumId,
                    getObjectField(element, "contentType") as String,
                    getObjectField(element, "url") as String,
                    getObjectField(element, "isProcessing") as Boolean,
                    getObjectField(element, "thumbUrl") as String,
                    getObjectField(element, "coverUrl") as String,
                    getObjectField(element, "remainingViews") as Int,
                )
            }
        }
    }

}