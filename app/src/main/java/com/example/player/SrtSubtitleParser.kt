package com.example.player

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction


data class SrtCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class ExternalSubtitleTrack(
    val id: String,
    val displayName: String,
    val uri: Uri,
    val cues: List<SrtCue>
)

object SrtSubtitleParser {

    /** Supports HH:MM:SS,mmm, MM:SS,mmm, dot milliseconds, and flexible spacing. */
    private val TIME_LINE_REGEX = Regex(
        """(\d{1,2}\s*:\s*)?(\d{1,2})\s*:\s*(\d{2})\s*[,.]\s*(\d{1,3})\s*-->\s*(\d{1,2}\s*:\s*)?(\d{1,2})\s*:\s*(\d{2})\s*[,.]\s*(\d{1,3})"""
    )
    private val HTML_TAG_REGEX = Regex("""<[^>]*>""")
    private val WINDOWS_1252: Charset = Charset.forName("windows-1252")

    suspend fun loadFromUri(context: Context, uri: Uri): ExternalSubtitleTrack? =
        withContext(Dispatchers.IO) {
            val fileName = resolveFileName(context, uri)
            val bytes = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes() }
                ?: return@withContext null
            val cues = parse(decodeSubtitleBytes(bytes))
            ExternalSubtitleTrack(
                id = uri.toString(),
                displayName = fileName,
                uri = uri,
                cues = cues
            )
        }

    /**
     * Decodes common subtitle exports without assuming UTF-8. Bangla subtitle files are often
     * UTF-16LE exports and contain NUL bytes, which previously made the parser return zero cues.
     */
    fun decodeSubtitleBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        val (payload, charset) = when {
            bytes.startsWith(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) -> {
                bytes.copyOfRange(3, bytes.size) to Charsets.UTF_8
            }
            bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xFE.toByte())) -> {
                bytes.copyOfRange(2, bytes.size) to Charsets.UTF_16LE
            }
            bytes.startsWith(byteArrayOf(0xFE.toByte(), 0xFF.toByte())) -> {
                bytes.copyOfRange(2, bytes.size) to Charsets.UTF_16BE
            }
            containsFrequentNullBytes(bytes) -> bytes to Charsets.UTF_16LE
            else -> bytes to null
        }

        val decoded = if (charset != null) {
            String(payload, charset)
        } else {
            decodeStrict(payload, Charsets.UTF_8)
                ?: decodeStrict(payload, WINDOWS_1252)
                ?: decodeStrict(payload, Charsets.ISO_8859_1)
                ?: String(payload, Charsets.UTF_8)
        }
        return normalizeText(decoded)
    }

    fun parse(bytes: ByteArray): List<SrtCue> = parse(decodeSubtitleBytes(bytes))

    /** Parses cue blocks as well as files that omit blank lines between cues. */
    fun parse(rawContent: String): List<SrtCue> {
        val lines = normalizeText(rawContent).lines()
        if (lines.isEmpty()) return emptyList()

        val cues = ArrayList<SrtCue>()
        var index = 0
        while (index < lines.size) {
            val timeMatch = TIME_LINE_REGEX.find(lines[index])
            if (timeMatch == null) {
                index++
                continue
            }

            val startMs = toMillis(
                timeMatch.groupValues[1],
                timeMatch.groupValues[2],
                timeMatch.groupValues[3],
                timeMatch.groupValues[4]
            )
            val endMs = toMillis(
                timeMatch.groupValues[5],
                timeMatch.groupValues[6],
                timeMatch.groupValues[7],
                timeMatch.groupValues[8]
            )
            index++

            val textLines = mutableListOf<String>()
            while (index < lines.size) {
                val line = lines[index]
                val nextLineStartsCue = line.trim().toIntOrNull() != null &&
                    index + 1 < lines.size &&
                    TIME_LINE_REGEX.containsMatchIn(lines[index + 1])
                if (line.isBlank() || TIME_LINE_REGEX.containsMatchIn(line) || nextLineStartsCue) break
                textLines += line.trimEnd()
                index++
            }

            val text = textLines.joinToString("\n")
                .replace(HTML_TAG_REGEX, "")
                .trim()
            if (endMs > startMs && text.isNotEmpty()) {
                cues += SrtCue(startMs = startMs, endMs = endMs, text = text)
            }
        }

        return cues.sortedBy { it.startMs }
    }

    fun findActiveCues(cues: List<SrtCue>, positionMs: Long): String {
        if (cues.isEmpty()) return ""
        var low = 0
        var high = cues.lastIndex
        var candidateIndex = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (cues[mid].startMs <= positionMs) {
                candidateIndex = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        if (candidateIndex < 0) return ""

        val active = mutableListOf<String>()
        var index = candidateIndex
        while (index >= 0 && cues[index].startMs <= positionMs) {
            if (positionMs <= cues[index].endMs) {
                active.add(0, cues[index].text)
            }
            index--
        }
        return active.joinToString("\n")
    }

    private fun toMillis(hours: String, minutes: String, seconds: String, milliseconds: String): Long {
        val hourValue = hours.replace(":", "").trim().toLongOrNull() ?: 0L
        val minuteValue = minutes.trim().toLongOrNull() ?: 0L
        val secondValue = seconds.trim().toLongOrNull() ?: 0L
        val millisecondValue = milliseconds.trim().padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return ((hourValue * 3600L) + (minuteValue * 60L) + secondValue) * 1000L + millisecondValue
    }

    private fun normalizeText(rawText: String): String = rawText
        .replace("\uFEFF", "")
        .replace("\u0000", "")
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    private fun decodeStrict(bytes: ByteArray, charset: Charset): String? = runCatching {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()

    private fun containsFrequentNullBytes(bytes: ByteArray): Boolean {
        val sampleSize = minOf(bytes.size, 100)
        if (sampleSize < 4) return false
        val nullCount = bytes.take(sampleSize).count { it == 0.toByte() }
        return nullCount >= 2 && nullCount * 4 >= sampleSize
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun resolveFileName(context: Context, uri: Uri): String {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Subtitle.srt"
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                }
            }
        }
        return name
    }
}
