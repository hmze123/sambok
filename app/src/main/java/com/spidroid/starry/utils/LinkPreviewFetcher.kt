// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/utils/LinkPreviewFetcher.kt
package com.spidroid.starry.utils

import com.spidroid.starry.models.PostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

object LinkPreviewFetcher {

    // الدالة الآن هي suspend function وتعمل مع Coroutines
    // وتُرجع كائن Result الذي يحتوي إما على النجاح أو الفشل
    suspend fun fetch(url: String): Result<PostModel.LinkPreview> {
        // التبديل إلى IO dispatcher المناسب لعمليات الشبكة
        return withContext(Dispatchers.IO) {
            try {
                // الاتصال بالرابط باستخدام Jsoup مع تحديد user agent و timeout
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                    .timeout(10000)
                    .get()

                // إنشاء كائن LinkPreview للاحتفاظ بالبيانات
                val preview = PostModel.LinkPreview()
                preview.url = url

                // محاولة الحصول على وسوم Open Graph (og) أولاً لأنها أكثر موثوقية
                val title = getMetaTag(doc, "og:title") ?: doc.title()
                val description = getMetaTag(doc, "og:description") ?: getMetaTag(doc, "description")
                val imageUrl = getMetaTag(doc, "og:image") ?: doc.select("img").first()?.absUrl("src")
                val siteName = getMetaTag(doc, "og:site_name")

                preview.title = title
                preview.description = description
                preview.imageUrl = imageUrl
                preview.siteName = siteName

                // إذا لم يتم العثور على عنوان، اعتبر العملية فاشلة
                if (title.isNullOrBlank()) {
                    Result.failure(Exception("Could not find a title for the link."))
                } else {
                    Result.success(preview)
                }

            } catch (e: IOException) {
                // معالجة أخطاء الشبكة أو التحليل
                Result.failure(e)
            } catch (e: Exception) {
                // معالجة أي أخطاء أخرى محتملة
                Result.failure(e)
            }
        }
    }

    // دالة مساعدة لاستخراج المحتوى من وسم meta
    private fun getMetaTag(doc: Document, attr: String): String? {
        // تبحث عن وسوم meta التي تحتوي على "name" أو "property" مطابق للمفتاح
        val elements = doc.select("meta[name=$attr], meta[property=$attr]")
        return elements.first()?.attr("content")?.takeIf { it.isNotEmpty() }
    }
}