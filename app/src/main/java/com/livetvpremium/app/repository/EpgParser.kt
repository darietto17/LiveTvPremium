package com.livetvpremium.app.repository

import android.util.Xml
import com.livetvpremium.app.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EpgParser {

    suspend fun parse(inputStream: InputStream): List<EpgProgram> = withContext(Dispatchers.Default) {
        val programs = mutableListOf<EpgProgram>()
        
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            var currentProgram: EpgProgram? = null
            var channelId = ""
            var start = ""
            var stop = ""
            var title = ""
            var description = ""
            
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "programme") {
                            channelId = parser.getAttributeValue(null, "channel") ?: ""
                            start = parser.getAttributeValue(null, "start") ?: ""
                            stop = parser.getAttributeValue(null, "stop") ?: ""
                            title = ""
                            description = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "title" -> title = text
                                "desc" -> description = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme") {
                            programs.add(
                                EpgProgram(
                                    channelId = channelId,
                                    title = title,
                                    description = description,
                                    startTime = start,
                                    stopTime = stop
                                )
                            )
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        programs
    }

    // Helper to format XMLTV dates: 20240304201333 +0100
    fun parseEpgDate(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())
            format.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
