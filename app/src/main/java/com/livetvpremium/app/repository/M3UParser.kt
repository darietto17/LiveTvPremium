package com.livetvpremium.app.repository

import com.livetvpremium.app.model.M3UItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M3UParser {
    private val groupTitleRegex = "group-title=\"([^\"]*)\"".toRegex()
    private val tvgIdRegex = "tvg-id=\"([^\"]*)\"".toRegex()
    private val tvgLogoRegex = "tvg-logo=\"([^\"]*)\"".toRegex()

    suspend fun parse(inputStream: java.io.InputStream): List<M3UItem> = withContext(Dispatchers.Default) {
        val items = mutableListOf<M3UItem>()
        
        var name = ""
        var groupTitle = ""
        var tvgId = ""
        var tvgLogo = ""
        
        inputStream.bufferedReader().useLines { lines ->
            for (lineRef in lines) {
                val line = lineRef.trim()
                if (line.isEmpty()) continue
                
                if (line.startsWith("#EXTINF:")) {
                    // Extract parameters using pre-compiled regex
                    groupTitle = groupTitleRegex.find(line)?.groupValues?.get(1) ?: ""
                    tvgId = tvgIdRegex.find(line)?.groupValues?.get(1) ?: ""
                    tvgLogo = tvgLogoRegex.find(line)?.groupValues?.get(1) ?: ""
                    
                    // Extract name (after the comma)
                    val commaIndex = line.lastIndexOf(',')
                    name = if (commaIndex != -1) line.substring(commaIndex + 1).trim() else ""
                } else if (line.startsWith("#")) {
                    continue
                } else {
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
        }
        items
    }
}
