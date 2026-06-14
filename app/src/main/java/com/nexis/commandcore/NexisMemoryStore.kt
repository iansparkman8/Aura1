package com.nexis.commandcore

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class NexisMemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("nexis_memory", Context.MODE_PRIVATE)

    fun load(): List<NexisMemoryEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return seedEntries().also { saveAll(it) }
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(
                    NexisMemoryEntry(
                        id = obj.optLong("id"),
                        title = obj.optString("title"),
                        body = obj.optString("body"),
                        type = runCatching { NexisEntryType.valueOf(obj.optString("type")) }.getOrDefault(NexisEntryType.NOTE),
                        createdAt = obj.optLong("createdAt"),
                        pinned = obj.optBoolean("pinned", false)
                    )
                )
            }
        }.sortedWith(compareByDescending<NexisMemoryEntry> { it.pinned }.thenByDescending { it.createdAt })
    }

    fun add(title: String, body: String, type: NexisEntryType): List<NexisMemoryEntry> {
        val entry = NexisMemoryEntry(
            id = System.currentTimeMillis(),
            title = title.ifBlank { NexisBrain.titleFor(body) },
            body = body,
            type = type,
            createdAt = System.currentTimeMillis(),
            pinned = type == NexisEntryType.PLAN || type == NexisEntryType.CODE
        )
        val updated = (listOf(entry) + load()).distinctBy { it.id }.take(250)
        saveAll(updated)
        return updated
    }

    fun remove(id: Long): List<NexisMemoryEntry> {
        val updated = load().filterNot { it.id == id }
        saveAll(updated)
        return updated
    }

    fun clear(): List<NexisMemoryEntry> {
        saveAll(emptyList())
        return emptyList()
    }

    private fun saveAll(entries: List<NexisMemoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title)
                    .put("body", entry.body)
                    .put("type", entry.type.name)
                    .put("createdAt", entry.createdAt)
                    .put("pinned", entry.pinned)
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun seedEntries(): List<NexisMemoryEntry> {
        val now = System.currentTimeMillis()
        return listOf(
            NexisMemoryEntry(
                id = now - 3,
                title = "Nexis mission",
                body = "A living command center for Ian: capture ideas, protect focus, store code safely, and turn chaos into a next step.",
                type = NexisEntryType.PLAN,
                createdAt = now - 3,
                pinned = true
            ),
            NexisMemoryEntry(
                id = now - 2,
                title = "Safety contract",
                body = "No Accessibility Service, no screen scraping, no screenshots, no auto-running code, no hidden remote control. Every upgrade stays review-first.",
                type = NexisEntryType.CODE,
                createdAt = now - 2,
                pinned = true
            ),
            NexisMemoryEntry(
                id = now - 1,
                title = "Astound moment",
                body = "The avatar should feel alive: breathing aura, orbiting particles, expressive eyes, and a floating overlay that makes people say: wait, your phone has a companion?",
                type = NexisEntryType.IDEA,
                createdAt = now - 1,
                pinned = true
            )
        )
    }

    private companion object {
        const val KEY_ENTRIES = "entries_json"
    }
}
