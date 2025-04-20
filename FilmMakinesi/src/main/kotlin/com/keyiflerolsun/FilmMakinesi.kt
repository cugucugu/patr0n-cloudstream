// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer


class FilmMakinesi : MainAPI() {
    override var mainUrl              = "https://filmmakinesi.de"
    override var name                 = "FilmMakinesi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    // ! CloudFlare bypass
    override var sequentialMainPage            = true
    override var sequentialMainPageDelay       = 50L
    override var sequentialMainPageScrollDelay = 50L

    override val mainPage = mainPageOf(
        "${mainUrl}/filmler/sayfa/"                                to "Son Filmler",
        "${mainUrl}/film-izle/olmeden-izlenmesi-gerekenler/sayfa/" to "Ölmeden İzle",
        "${mainUrl}/tur/aksiyon/film/sayfa/"                       to "Aksiyon",
        "${mainUrl}/tur/bilim-kurgu/film/sayfa/"                   to "Bilim Kurgu",
        "${mainUrl}/tur/macera/film/sayfa/"                        to "Macera",
        "${mainUrl}/tur/komedi/film/sayfa/"                        to "Komedi",
        "${mainUrl}/tur/romantik/film/sayfa/"                      to "Romantik",
        "${mainUrl}/tur/belgesel/film/sayfa/"                      to "Belgesel",
        "${mainUrl}/tur/fantastik/film/sayfa/"                     to "Fantastik",
        "${mainUrl}/tur/polisiye/film/sayfa/"                      to "Polisiye Suç",
        "${mainUrl}/tur/korku/film/sayfa/"                         to "Korku",
        "${mainUrl}/tur/animasyon/film/sayfa/"                     to "Animasyon",
        "${mainUrl}/tur/gizem/film/sayfa/"                         to "Gizem",
        "${mainUrl}/kanal/netflix/sayfa/"                          to "Netflix",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
val cleanedUrl = request.data.removeSuffix("/")
val url = if (page > 1) {
    "$cleanedUrl/$page"
} else {
    cleanedUrl.replace(Regex("/sayfa/?$"), "")
}

val document = app.get(url, headers = mapOf(
    "User-Agent" to USER_AGENT,
    "Referer" to mainUrl
)).document

    val home = document.select("div.film-list div.item-relative")
        .mapNotNull { it.toSearchResult() }

    Log.d("FLMM", "Toplam film: ${home.size}")
    return newHomePageResponse(request.name, home)
}

private fun Element.toSearchResult(): SearchResponse? {
    val aTag = selectFirst("a.item") ?: return null
    val title = aTag.attr("data-title").takeIf { it.isNotBlank() } ?: return null
    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(aTag.selectFirst("img")?.attr("src"))

    Log.d("FLMM", "Film: $title, Href: $href, Poster: $posterUrl")

    return newMovieSearchResponse(title, href, TvType.Movie) {
        this.posterUrl = posterUrl
    }
}
    private fun Element.toRecommendResult(): SearchResponse? {
        val title     = this.select("a").last()?.text() ?: return null
        val href      = fixUrlNull(this.select("a").last()?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}?s=${query}").document

        return document.select("div.film-list div.item-relative").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description     = document.select("div.info-description p").last()?.text()?.trim()
        val tags            = document.selectFirst("dt:contains(Tür:) + dd")?.text()?.split(", ")
        val rating          = document.selectFirst("dt:contains(IMDB Puanı:) + dd")?.text()?.trim()?.toRatingInt()
        val year            = document.selectFirst("dt:contains(Yapım Yılı:) + dd")?.text()?.trim()?.toIntOrNull()

        val durationElement = document.select("dt:contains(Film Süresi:) + dd time").attr("datetime")
        // ? ISO 8601 süre formatını ayrıştırma (örneğin "PT129M")
        val duration        = if (durationElement.startsWith("PT") && durationElement.endsWith("M")) {
            durationElement.drop(2).dropLast(1).toIntOrNull() ?: 0
        } else {
            0
        }

        val recommendations = document.select("div.film-list div.item-relative").mapNotNull { it.toRecommendResult() }
        val actors          = document.selectFirst("dt:contains(Oyuncular:) + dd")?.text()?.split(", ")?.map {
            Actor(it.trim())
        }

        val trailer         = fixUrlNull(document.selectXpath("//iframe[@title='Fragman']").attr("data-src"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }


    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("FLMM", "data » $data")
        val document      = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("data-src") ?: ""
        Log.d("FLMM", iframe)

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}
