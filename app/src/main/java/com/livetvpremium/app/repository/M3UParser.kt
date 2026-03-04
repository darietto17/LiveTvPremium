package com.livetvpremium.app.repository

import com.livetvpremium.app.model.M3UItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M3UParser {
    suspend fun parse(content: String): List<M3UItem> = withContext(Dispatchers.Default) {
        val items = mutableListOf<M3UItem>()
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        var name = ""
        var groupTitle = ""
        var tvgId = ""
        var tvgLogo = ""
        
        for (line in lines) {
            if (line.startsWith("#EXTINF:")) {
                // Extract parameters
                groupTitle = extractAttribute(line, "group-title")
                tvgId = extractAttribute(line, "tvg-id")
                tvgLogo = extractAttribute(line, "tvg-logo")
                
                // Extract name (after the comma)
                val commaIndex = line.lastIndexOf(',')
                name = if (commaIndex != -1) line.substring(commaIndex + 1).trim() else ""
            } else if (line.startsWith("#")) {
                // Ignore other comments/directives like #EXTM3U or plain # COMMENTS
                continue
            } else if (line.isNotEmpty()) {
                // This is the URL
                val url = line
                if (url.isNotEmpty() && name.isNotEmpty()) {
                    items.add(
                        M3UItem(
                            name = name,
                            groupTitle = groupTitle,
                            tvgId = tvgId,
                            tvgLogo = tvgLogo,
                            url = url
                        )
                    )
                }
                // Reset for next item
                name = ""
                groupTitle = ""
                tvgId = ""
                tvgLogo = ""
            }
        }
        items
    }

    private fun extractAttribute(line: String, attribute: String): String {
        val regex = "$attribute=\"([^\"]*)\"".toRegex()
        val matchResult = regex.find(line)
        return matchResult?.groupValues?.get(1) ?: ""
    }
}
