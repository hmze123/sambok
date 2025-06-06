package com.spidroid.starry.utils

import android.os.AsyncTask
import com.spidroid.starry.models.PostModel.LinkPreview
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

object LinkPreviewFetcher {
    fun fetch(url: String, callback: LinkPreviewCallback) {
        object : AsyncTask<Void?, Void?, LinkPreview?>() {
            override fun doInBackground(vararg voids: Void?): LinkPreview? {
                try {
                    val doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                        .timeout(10000)
                        .get()

                    val preview = LinkPreview()
                    preview.setUrl(url)

                    // Try Open Graph tags first
                    var title = getMetaTag(doc, "og:title")
                    if (title == null) title = doc.title()

                    var description = getMetaTag(doc, "og:description")
                    if (description == null) description = getMetaTag(doc, "description")

                    var imageUrl = getMetaTag(doc, "og:image")
                    if (imageUrl == null) {
                        val img = doc.select("img").first()
                        if (img != null) imageUrl = img.absUrl("src")
                    }

                    val siteName = getMetaTag(doc, "og:site_name")

                    preview.setTitle(title)
                    preview.setDescription(description)
                    preview.setImageUrl(imageUrl)
                    preview.setSiteName(siteName)

                    return preview
                } catch (e: IOException) {
                    return null
                }
            }

            fun getMetaTag(doc: Document, property: String?): String? {
                val meta = doc.select("meta[property~=" + property + "]")
                if (!meta.isEmpty()) {
                    return meta.first()!!.attr("content")
                }
                return null
            }

            override fun onPostExecute(preview: LinkPreview?) {
                if (preview != null) {
                    callback.onPreviewReceived(preview)
                } else {
                    callback.onError("Failed to fetch link preview")
                }
            }
        }.execute()
    }

    interface LinkPreviewCallback {
        fun onPreviewReceived(preview: LinkPreview?)
        fun onError(error: String?)
    }
}