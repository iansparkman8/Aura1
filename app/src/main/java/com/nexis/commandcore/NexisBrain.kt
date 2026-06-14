package com.nexis.commandcore

object NexisBrain {
    fun coach(input: String, vaultSize: Int): NexisCoachResponse {
        val clean = input.trim()
        val lower = clean.lowercase()
        val type = when {
            lower.contains("fun ") || lower.contains("class ") || lower.contains("kotlin") || lower.contains("gradle") || lower.contains("xml") || lower.contains("manifest") -> NexisEntryType.CODE
            lower.contains("panic") || lower.contains("overwhelmed") || lower.contains("stuck") || lower.contains("tired") || lower.contains("stress") -> NexisEntryType.GROUNDING
            lower.contains("build") || lower.contains("github") || lower.contains("apk") || lower.contains("release") -> NexisEntryType.PLAN
            lower.contains("win") || lower.contains("done") || lower.contains("finished") -> NexisEntryType.WIN
            lower.contains("idea") || lower.contains("what if") || lower.contains("app") -> NexisEntryType.IDEA
            else -> NexisEntryType.NOTE
        }

        return when (type) {
            NexisEntryType.CODE -> NexisCoachResponse(
                headline = "Code captured. Nexis will keep it review-only.",
                nextStep = "Paste this into the Build Lab, label the target file, then run the GitHub Actions debug APK build. No auto-execution, no hidden changes.",
                entryType = type,
                momentum = (65 + vaultSize).coerceAtMost(99)
            )
            NexisEntryType.GROUNDING -> NexisCoachResponse(
                headline = "Pause the storm. One move wins the next minute.",
                nextStep = "Put both feet down, breathe once, then write only the next physical action. Nexis is built to shrink chaos into one step.",
                entryType = type,
                momentum = 42
            )
            NexisEntryType.PLAN -> NexisCoachResponse(
                headline = "Build lane detected.",
                nextStep = "Turn this into a 3-step ticket: files to edit, command to run, expected APK artifact. Then ship one clean commit.",
                entryType = type,
                momentum = (70 + vaultSize).coerceAtMost(100)
            )
            NexisEntryType.WIN -> NexisCoachResponse(
                headline = "Win logged. Do not skip the proof.",
                nextStep = "Save the screenshot, APK link, commit hash, or exact result. Wins become fuel when they are visible.",
                entryType = type,
                momentum = 88
            )
            NexisEntryType.IDEA -> NexisCoachResponse(
                headline = "Idea caught before it vanished.",
                nextStep = "Name the user, the pain, and the wow moment. If it cannot be explained in one sentence, simplify it.",
                entryType = type,
                momentum = (60 + vaultSize).coerceAtMost(95)
            )
            NexisEntryType.NOTE -> NexisCoachResponse(
                headline = "Stored as a working note.",
                nextStep = "Attach this to a project, person, or decision so it does not become clutter.",
                entryType = type,
                momentum = 50
            )
        }
    }

    fun titleFor(input: String): String {
        val oneLine = input.trim().lineSequence().firstOrNull().orEmpty()
        val compact = oneLine.replace(Regex("\\s+"), " ").trim()
        return when {
            compact.isBlank() -> "Untitled capture"
            compact.length <= 52 -> compact
            else -> compact.take(49).trimEnd() + "..."
        }
    }
}
