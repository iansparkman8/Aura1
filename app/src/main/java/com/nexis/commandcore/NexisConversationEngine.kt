package com.nexis.commandcore

enum class NexisChatMode(val label: String) {
    COMPANION("Companion"),
    BUILDER("Builder"),
    CODE_REVIEW("Code Review"),
    GROUNDING("Grounding"),
    STRATEGY("Strategy")
}

data class NexisChatMessage(
    val id: Long,
    val role: String,
    val text: String,
    val createdAt: Long,
    val mode: NexisChatMode
)

object NexisConversationEngine {
    fun detectMode(input: String): NexisChatMode {
        val lower = input.lowercase()
        return when {
            listOf("crash", "error", "stacktrace", "kotlin", "gradle", "manifest", "xml", "code").any { lower.contains(it) } -> NexisChatMode.CODE_REVIEW
            listOf("panic", "stress", "overwhelmed", "tired", "stuck").any { lower.contains(it) } -> NexisChatMode.GROUNDING
            listOf("strategy", "launch", "sell", "market", "plan").any { lower.contains(it) } -> NexisChatMode.STRATEGY
            listOf("build", "feature", "upgrade", "github", "apk").any { lower.contains(it) } -> NexisChatMode.BUILDER
            else -> NexisChatMode.COMPANION
        }
    }

    fun reply(input: String, mode: NexisChatMode, vaultSize: Int): String {
        val clean = input.trim().ifBlank { "Untitled thought" }
        return when (mode) {
            NexisChatMode.COMPANION -> """
                I caught it.

                Signal:
                "$clean"

                Clean next move:
                Name it, save it, and attach one physical action to it. Do not let it float around loose.

                Vault charge: $vaultSize saved sparks.
            """.trimIndent()

            NexisChatMode.BUILDER -> """
                Build mode active.

                Feature signal:
                "$clean"

                Ship path:
                1. Define the visible feature.
                2. Name the exact screen/file it touches.
                3. Build the smallest working slice.
                4. Run: gradle :app:assembleDebug --stacktrace
                5. Save the APK artifact path.

                Rule: giant vision, clean patch.
            """.trimIndent()

            NexisChatMode.CODE_REVIEW -> """
                Code Review mode active.

                Input:
                "$clean"

                Review checklist:
                1. Which file changes?
                2. Does it compile?
                3. Does it add permissions?
                4. Does it keep the safety contract?
                5. Does the APK build?

                Command:
                gradle :app:assembleDebug --stacktrace

                Review-first only. Nexis stores code ideas but does not execute them.
            """.trimIndent()

            NexisChatMode.GROUNDING -> """
                Grounding mode.

                First: feet down, shoulders loose, one breath.

                What I heard:
                "$clean"

                Next action:
                Pick one thing you can physically do in under two minutes. Do that. Then log the win.
            """.trimIndent()

            NexisChatMode.STRATEGY -> """
                Strategy mode.

                Goal:
                "$clean"

                Command frame:
                1. Outcome: what exists when done?
                2. Proof: APK, commit, screenshot, or working screen.
                3. Constraint: what must not break?
                4. First move: under 30 minutes.
                5. Build gate: assembleDebug passes.

                Recommendation: turn it into one ticket and ship the first visible slice.
            """.trimIndent()
        }
    }
}
