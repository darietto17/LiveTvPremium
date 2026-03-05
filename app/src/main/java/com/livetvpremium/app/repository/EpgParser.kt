package com.livetvpremium.app.repository

import com.livetvpremium.app.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EpgParser {

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Parses an XMLTV stream keeping only programmes within a relevant time window
     * (from 1 hour ago to 24 hours ahead) to minimize memory usage.
     * Descriptions are truncated to save memory.
     */
    suspend fun parse(inputStream: InputStream): Map<String, List<EpgProgram>> = withContext(Dispatchers.IO) {
        val programs = mutableMapOf<String, MutableList<EpgProgram>>()

        val now = System.currentTimeMillis()
        val windowStart = now - 1 * 60 * 60 * 1000L      // 1 hour ago
        val windowEnd   = now + 24 * 60 * 60 * 1000L      // 24 hours ahead

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var currentChannelId = ""
        var currentStart = 0L
        var currentStop = 0L
        var currentTitle = ""
        var currentSubtitle = ""
        var inProgramme = false
        var skipProgramme = false  // skip programmes outside window
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    if (tagName == "programme") {
                        inProgramme = true
                        currentChannelId = parser.getAttributeValue(null, "channel") ?: ""
                        val startStr = parser.getAttributeValue(null, "start") ?: ""
                        val stopStr = parser.getAttributeValue(null, "stop") ?: ""
                        currentStart = parseDate(startStr)
                        currentStop = parseDate(stopStr)

                        // Skip programmes completely outside the time window
                        skipProgramme = currentStop < windowStart || currentStart > windowEnd

                        currentTitle = ""
                        currentSubtitle = ""
                    } else if (inProgramme && !skipProgramme) {
                        currentTag = tagName
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inProgramme && !skipProgramme) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> currentTitle = text
                            "sub-title" -> currentSubtitle = text
                            // Skip desc - not displayed in UI, saves a lot of memory
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && inProgramme) {
                        if (!skipProgramme && currentChannelId.isNotEmpty() && currentTitle.isNotEmpty()) {
                            val program = EpgProgram(
                                channelId = currentChannelId,
                                title = currentTitle,
                                subtitle = currentSubtitle,
                                description = "", // Skip to save memory
                                startTime = currentStart,
                                stopTime = currentStop
                            )
                            programs.getOrPut(currentChannelId) { mutableListOf() }.add(program)
                        }
                        inProgramme = false
                        skipProgramme = false
                        currentTag = ""
                    } else if (inProgramme) {
                        currentTag = ""
                    }
                }
            }
            eventType = parser.next()
        }

        // Sort each channel's programs by start time
        programs.mapValues { (_, list) -> list.sortedBy { it.startTime } }
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            dateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
