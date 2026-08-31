package com.example.util

import com.example.data.model.LyricLine
import java.util.regex.Pattern

object LyricsParser {
    private val LRC_LINE_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")

    fun parse(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()
        val result = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val matcher = LRC_LINE_PATTERN.matcher(trimmed)
            if (matcher.matches()) {
                try {
                    val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                    val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                    val millisFraction = matcher.group(3)
                    val millis = when (millisFraction?.length) {
                        1 -> millisFraction.toLong() * 100
                        2 -> millisFraction.toLong() * 10
                        3 -> millisFraction.toLong()
                        else -> 0L
                    }
                    val timestampMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                    val text = matcher.group(4)?.trim().orEmpty()

                    // Filter out meta tags like [ti:Title] if empty text or header
                    if (text.isNotEmpty() || trimmed.contains("]")) {
                        result.add(LyricLine(timestampMs = timestampMs, text = text))
                    }
                } catch (e: Exception) {
                    // Ignore malformed timestamp line
                }
            } else if (!trimmed.startsWith("[ti:") && !trimmed.startsWith("[ar:") && !trimmed.startsWith("[al:")) {
                // If line has no timestamp but is plain text
                result.add(LyricLine(timestampMs = 0L, text = trimmed))
            }
        }

        return result.sortedBy { it.timestampMs }
    }

    fun findActiveIndex(lyrics: List<LyricLine>, currentPositionMs: Long, offsetMs: Long = 0L): Int {
        if (lyrics.isEmpty()) return -1
        val adjustedPosition = (currentPositionMs + offsetMs).coerceAtLeast(0L)

        for (i in lyrics.indices.reversed()) {
            if (adjustedPosition >= lyrics[i].timestampMs) {
                return i
            }
        }
        return 0
    }

    fun formatToLrc(lines: List<LyricLine>): String {
        return lines.joinToString("\n") { line ->
            val totalSeconds = line.timestampMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (line.timestampMs % 1000) / 10
            "[%02d:%02d.%02d]%s".format(minutes, seconds, millis, line.text)
        }
    }

    fun formatTimestamp(timestampMs: Long): String {
        val totalSeconds = (timestampMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = (timestampMs % 1000) / 10
        return "%02d:%02d.%02d".format(minutes, seconds, millis)
    }
}
