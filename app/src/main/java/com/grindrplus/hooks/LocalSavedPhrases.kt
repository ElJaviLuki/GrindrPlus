package com.grindrplus.hooks

import com.grindrplus.core.ModContext
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import java.lang.reflect.Proxy
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.lang.reflect.Constructor

class LocalSavedPhrases: Hook("Local saved phrases",
    "Save unlimited phrases locally") {
    private val phrasesRestService = "u4.k"
    private val createSuccessResult = "h9.a\$b"
    private val retrofit = "retrofit2.Retrofit"
    private val chatRestService = "com.grindrapp.android.chat.api.ChatRestService"
    private val addSavedPhraseResponse = "com.grindrapp.android.chat.api.model.AddSavedPhraseResponse"
    private val phrasesResponse = "com.grindrapp.android.model.PhrasesResponse"
    private val phraseModel = "com.grindrapp.android.persistence.model.Phrase"

override fun init() {
    val chatRestServiceClass = findClass(chatRestService)
    val createSuccess = findClass(createSuccessResult)?.constructors?.firstOrNull()
    val phrasesRestServiceClass = findClass(phrasesRestService)

    findClass(retrofit)?.hook("create", HookStage.AFTER) { param ->
        val service = param.getResult() ?: return@hook
        param.setResult(
            when {
                chatRestServiceClass?.isAssignableFrom(service.javaClass) == true ->
                    createChatRestServiceProxy(context, service, createSuccess!!)
                phrasesRestServiceClass?.isAssignableFrom(service.javaClass) == true ->
                    createPhrasesRestServiceProxy(context, service, createSuccess!!)
                else -> service
            }
        )
    }
}

    fun createChatRestServiceProxy(context: ModContext,
                                   originalService: Any,
                                   createSuccess: Constructor<*>): Any {
        val invocationHandler = Proxy.getInvocationHandler(originalService)
        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(findClass(chatRestService))
        ) { proxy, method, args ->
            when (method.name) {
                "w" -> { // Annotated with @POST("v3/me/prefs/phrases")
                    val phrase = getObjectField(args[0], "phrase") as String
                    val currentPhrases = context.database.getPhraseList()
                    var index = context.database.getCurrentPhraseIndex() + 1
                    while (currentPhrases.any { it.get("phraseId") == index }) index++
                    context.database.addPhrase(index, phrase, 0, System.currentTimeMillis())
                    val response = findClass(addSavedPhraseResponse)?.constructors?.first()
                        ?.newInstance(index.toString())
                    createSuccess.newInstance(response)
                }
                "o" -> { // Annotated with @DELETE("v3/me/prefs/phrases/{id}")
                    val index = context.database.getCurrentPhraseIndex()
                    context.database.deletePhrase(index)
                    createSuccess.newInstance(Unit)
                }
                "C" -> { // Annotated with @POST("v4/phrases/frequency/{id}")
                    val index = context.database.getCurrentPhraseIndex()
                    val phrase = context.database.getPhrase(index)
                    context.database.updatePhrase(index,
                        phrase.getAsString("text"),
                        phrase.getAsInteger("frequency") + 1,
                        System.currentTimeMillis()
                    )
                    createSuccess.newInstance(Unit)
                }
                else -> invocationHandler.invoke(proxy, method, args)
            }
        }
    }

    fun createPhrasesRestServiceProxy(context: ModContext,
                                      originalService: Any,
                                      createSuccess: Constructor<*>): Any {
        val invocationHandler = Proxy.getInvocationHandler(originalService)
        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(findClass(phrasesRestService))
        ) { proxy, method, args ->
            when (method.name) {
                "a" -> { // Annotated with @GET("v3/me/prefs")
                    val currentPhrases = context.database.getPhraseList()
                    val phrases = currentPhrases.associateWith { phrase ->
                        val text = phrase.getAsString("text")
                        val timestamp = phrase.getAsLong("timestamp")
                        val frequency = phrase.getAsInteger("frequency")
                        context.loadClass(phraseModel)?.constructors?.first()?.newInstance(
                            phrase.getAsString("phraseId"), text, timestamp, frequency
                        )
                    }
                    val phrasesResponse = findClass(phrasesResponse)
                        ?.constructors?.find { it.parameterTypes.size == 1 }?.newInstance(phrases)
                    createSuccess.newInstance(phrasesResponse)
                }
                else -> invocationHandler.invoke(proxy, method, args)
            }
        }
    }
}
