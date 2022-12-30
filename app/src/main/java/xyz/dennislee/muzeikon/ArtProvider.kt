package xyz.dennislee.muzeikon

import android.net.Uri
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val host = "https://www.oca.org"
const val basePath = "/saints/lives/"
const val baseUrl = host + basePath
const val datePattern = "uuuu/MM/dd"

class ArtProvider : MuzeiArtProvider() {
    override fun isArtworkValid(artwork: Artwork): Boolean {
        val date = LocalDate.now()
        return date == artwork.dateAdded.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    override fun onLoadRequested(initial: Boolean) {
        if (lastAddedArtwork?.let { isArtworkValid(it) } == true) return
        onInvalidArtwork(Artwork(null))
    }

    override fun onInvalidArtwork(artwork: Artwork) {
        setArtwork(buildList {
            val dateFormatter = DateTimeFormatter.ofPattern(datePattern)

            val date = LocalDate.now()
            val dateFormatted = date.format(dateFormatter)

            val doc = Jsoup.connect("$baseUrl${date.format(dateFormatter)}").get()
            val figures = doc.select("figure.thumbnail")
            figures.forEach { figure -> add(createArtwork(figure, date)) }
        })
    }
}

private fun createArtwork(figure: Element, date: LocalDate): Artwork {
    val article = figure.siblingElements()

    val name = article.select("h2.name").text()
    val description = article.select("p.description").text()
    val dateString = date.toString()
    val persistentUri =
        Uri.parse(Regex("xsm").replace(figure.select("span > img[alt][src]").attr("src"), "lg"))
    val webUri = Uri.parse(host + article.select("div.content > a.button[href]")
        .attr("href") + "#content-header")
    return Artwork(name, description, dateString, persistentUri = persistentUri, webUri = webUri)
}