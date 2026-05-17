package com.nexus.iptv.update

/**
 * Sections extracted from a GitHub release's markdown body. Sections are
 * matched loosely on header text so any of `## Features`, `**Features**`,
 * `### New features`, etc. all funnel into [features]; same for changes
 * (matches "changes" / "improvements" / "updates") and fixes (matches
 * "fix" / "fixes" / "bug fixes").
 *
 * Anything that appears before the first recognized header — or in a body
 * that has no recognized headers at all — lands in [other], so the dialog
 * can show a fallback "What's new" block when notes are flat prose rather
 * than structured.
 */
data class ParsedReleaseNotes(
    val features: List<String>,
    val changes: List<String>,
    val fixes: List<String>,
    val other: String
) {
    val isEmpty: Boolean
        get() = features.isEmpty() && changes.isEmpty() && fixes.isEmpty() && other.isBlank()
}

object ReleaseNotesParser {
    private val markdownHeaderRegex = Regex("""^\s*#{1,6}\s*\*{0,2}\s*([^*].*?)\s*\*{0,2}\s*$""")
    private val boldHeaderRegex = Regex("""^\s*\*\*([^*]+?)\*\*\s*:?\s*$""")
    private val horizontalRuleRegex = Regex("""^\s*-{3,}\s*$""")

    private enum class Bucket { FEATURES, CHANGES, FIXES }

    fun parse(markdown: String?): ParsedReleaseNotes {
        if (markdown.isNullOrBlank()) {
            return ParsedReleaseNotes(emptyList(), emptyList(), emptyList(), "")
        }

        val features = mutableListOf<String>()
        val changes = mutableListOf<String>()
        val fixes = mutableListOf<String>()
        val otherLines = mutableListOf<String>()

        var currentBucket: Bucket? = null
        var seenAnyKnownHeader = false

        // Buffer for the bullet currently being assembled, so wrapped lines in the
        // source markdown (indented continuation text) join onto the previous bullet
        // instead of appearing as their own lowercase-leading entries in the UI.
        var pendingItem: StringBuilder? = null

        fun flushPending() {
            val sb = pendingItem ?: return
            val text = sb.toString().trim()
            pendingItem = null
            if (text.isEmpty()) return
            val capitalized = capitalizeFirstLetter(text)
            when (currentBucket) {
                Bucket.FEATURES -> features.add(capitalized)
                Bucket.CHANGES -> changes.add(capitalized)
                Bucket.FIXES -> fixes.add(capitalized)
                null -> otherLines.add(capitalized)
            }
        }

        for (rawLine in markdown.lineSequence()) {
            val line = rawLine.trimEnd()
            if (horizontalRuleRegex.matches(line)) continue

            val headerTitle = markdownHeaderRegex.matchEntire(line)?.groupValues?.get(1)
                ?: boldHeaderRegex.matchEntire(line)?.groupValues?.get(1)

            if (headerTitle != null) {
                flushPending()
                currentBucket = bucketFor(headerTitle)
                if (currentBucket != null) seenAnyKnownHeader = true
                continue
            }

            if (line.isBlank()) {
                flushPending()
                continue
            }

            val trimmedStart = line.trimStart()
            val isBulletStart = trimmedStart.startsWith("- ") ||
                trimmedStart.startsWith("* ") ||
                trimmedStart.startsWith("• ")
            val content = if (isBulletStart) {
                trimmedStart
                    .removePrefix("- ")
                    .removePrefix("* ")
                    .removePrefix("• ")
                    .trim()
            } else {
                trimmedStart.trim()
            }
            if (content.isEmpty()) continue

            if (isBulletStart) {
                flushPending()
                pendingItem = StringBuilder(content)
            } else {
                val existing = pendingItem
                if (existing != null) {
                    existing.append(' ').append(content)
                } else {
                    pendingItem = StringBuilder(content)
                }
            }
        }
        flushPending()

        return ParsedReleaseNotes(
            features = features,
            changes = changes,
            fixes = fixes,
            // If we recognized any structured section, drop unstructured prose
            // (typically header preamble like "## Compatibility note"). For
            // wholly-unstructured notes, fall back to showing it as the "Other"
            // / "What's new" block.
            other = if (seenAnyKnownHeader) "" else otherLines.joinToString("\n")
        )
    }

    private fun bucketFor(headerTitle: String): Bucket? {
        val title = headerTitle.lowercase()
        return when {
            "feature" in title || "new" in title -> Bucket.FEATURES
            "fix" in title || "bug" in title -> Bucket.FIXES
            "change" in title || "improvement" in title || "update" in title -> Bucket.CHANGES
            else -> null
        }
    }

    private fun capitalizeFirstLetter(text: String): String {
        if (text.isEmpty()) return text
        val first = text[0]
        if (!first.isLetter() || first.isUpperCase()) return text
        return first.uppercaseChar() + text.substring(1)
    }
}
