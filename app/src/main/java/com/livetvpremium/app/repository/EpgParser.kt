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

    suspend fun parse(inputStream: InputStream): Map<String, List<EpgProgram>> = withContext(Dispatchers.Default) {
        val programs = mutableMapOf<String, MutableList<EpgProgram>>()

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
        var currentDesc = ""
        var inProgramme = false
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
                        currentTitle = ""
                        currentSubtitle = ""
                        currentDesc = ""
                    } else if (inProgramme) {
                        currentTag = tagName
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inProgramme) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> currentTitle = text
                            "sub-title" -> currentSubtitle = text
                            "desc" -> currentDesc = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && inProgramme) {
                        inProgramme = false
                        if (currentChannelId.isNotEmpty() && currentTitle.isNotEmpty()) {
                            val program = EpgProgram(
                                channelId = currentChannelId,
                                title = currentTitle,
                                subtitle = currentSubtitle,
                                description = currentDesc,
                                startTime = currentStart,
                                stopTime = currentStop
                            )
                            programs.getOrPut(currentChannelId) { mutableListOf() }.add(program)
                        }
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
            // Format: "20260306070300 +0000"
            dateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
