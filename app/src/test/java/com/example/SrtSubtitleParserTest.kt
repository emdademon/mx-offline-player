package com.example

import com.example.player.SrtSubtitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtSubtitleParserTest {

    @Test
    fun `parses UTF-8 BOM and CRLF srt cues accurately`() {
        val sampleSrt = "\uFEFF1\r\n00:00:01,500 --> 00:00:04,250\r\n<i>Hello world!</i>\r\nSecond line\r\n\r\n" +
            "2\r\n00:01:02.100 --> 00:01:05.000\r\nNext cue"

        val cues = SrtSubtitleParser.parse(sampleSrt)
        assertEquals(2, cues.size)
        assertEquals(1500L, cues[0].startMs)
        assertEquals(4250L, cues[0].endMs)
        assertEquals("Hello world!\nSecond line", cues[0].text)
        assertEquals(62100L, cues[1].startMs)
        assertEquals(65000L, cues[1].endMs)
        assertEquals("Next cue", cues[1].text)
    }

    @Test
    fun `decodes UTF-16 BOMs and BOM-less UTF-16LE with Bangla text`() {
        val srt = "1\r\n00:00:01,000 --> 00:00:03,500\r\nবাংলা subtitle\uFEFF\u0000"
        val utf16Le = srt.toByteArray(Charsets.UTF_16LE)
        val utf16Be = srt.toByteArray(Charsets.UTF_16BE)

        assertEquals("বাংলা subtitle", SrtSubtitleParser.parse(utf16Le).single().text)
        assertEquals("বাংলা subtitle", SrtSubtitleParser.parse(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + utf16Le).single().text)
        assertEquals("বাংলা subtitle", SrtSubtitleParser.parse(byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + utf16Be).single().text)
    }

    @Test
    fun `supports optional hours flexible spacing and one to three millisecond digits`() {
        val cues = SrtSubtitleParser.parse(
            """
            1
            01:02,7   -->   01:05.25
            Minute cue

            2
            1 : 02 : 03,004 --> 1:02:04,005
            Hour cue
            """.trimIndent()
        )

        assertEquals(2, cues.size)
        assertEquals(62_700L, cues[0].startMs)
        assertEquals(65_250L, cues[0].endMs)
        assertEquals(3_723_004L, cues[1].startMs)
        assertEquals(3_724_005L, cues[1].endMs)
    }

    @Test
    fun `falls back to Windows 1252 and strips every BOM and NUL character`() {
        val bytes = "1\r\n00:00:00,000 --> 00:00:01,000\r\nCaf\u00E9\uFEFF".toByteArray(Charsets.ISO_8859_1)
        val decoded = SrtSubtitleParser.decodeSubtitleBytes(bytes + byteArrayOf(0))

        assertEquals("1\n00:00:00,000 --> 00:00:01,000\nCafé", decoded)
    }

    @Test
    fun `binary search finds active cue at exact timestamps`() {
        val cues = SrtSubtitleParser.parse(
            """
            1
            00:00:02,000 --> 00:00:05,000
            First line

            2
            00:00:08,000 --> 00:00:12,000
            Second line
            """.trimIndent()
        )

        assertEquals("", SrtSubtitleParser.findActiveCues(cues, 1500L))
        assertEquals("First line", SrtSubtitleParser.findActiveCues(cues, 2000L))
        assertEquals("First line", SrtSubtitleParser.findActiveCues(cues, 3500L))
        assertEquals("", SrtSubtitleParser.findActiveCues(cues, 6000L))
        assertEquals("Second line", SrtSubtitleParser.findActiveCues(cues, 10000L))
        assertTrue(SrtSubtitleParser.findActiveCues(cues, 15000L).isEmpty())
    }
}
