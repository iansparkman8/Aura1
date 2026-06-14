package com.nexis.commandcore

enum class NexisEntryType(val label: String) {
    IDEA("Idea"),
    CODE("Code"),
    PLAN("Plan"),
    GROUNDING("Grounding"),
    WIN("Win"),
    NOTE("Note")
}

data class NexisMemoryEntry(
    val id: Long,
    val title: String,
    val body: String,
    val type: NexisEntryType,
    val createdAt: Long,
    val pinned: Boolean = false
)

data class NexisCoachResponse(
    val headline: String,
    val nextStep: String,
    val entryType: NexisEntryType,
    val momentum: Int
)
