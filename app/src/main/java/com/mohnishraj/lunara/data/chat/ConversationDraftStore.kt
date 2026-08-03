package com.mohnishraj.lunara.data.chat

import android.content.Context

class ConversationDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("lunara_conversation_drafts", Context.MODE_PRIVATE)

    fun load(conversationId: String): String = preferences.getString(key(conversationId), "").orEmpty()

    fun save(conversationId: String, value: String) {
        val clean = value.take(4000)
        if (clean.isBlank()) {
            clear(conversationId)
        } else {
            preferences.edit().putString(key(conversationId), clean).apply()
        }
    }

    fun clear(conversationId: String) {
        preferences.edit().remove(key(conversationId)).apply()
    }

    fun all(): Map<String, String> = preferences.all.mapNotNull { (rawKey, value) ->
        val text = value as? String ?: return@mapNotNull null
        rawKey.removePrefix(PREFIX).takeIf { rawKey.startsWith(PREFIX) }?.let { it to text }
    }.toMap()

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    private fun key(conversationId: String): String = "$PREFIX$conversationId"

    private companion object {
        const val PREFIX = "draft."
    }
}
