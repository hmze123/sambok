package com.spidroid.starry.utils;

import android.os.AsyncTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.spidroid.starry.models.PostModel.LinkPreview;
import java.io.IOException;

public class LinkPreviewFetcher {
    public interface LinkPreviewCallback {
        void onPreviewReceived(LinkPreview preview);
        void onError(String error);
    }

    public static void fetch(String url, LinkPreviewCallback callback) {
        new AsyncTask<Void, Void, LinkPreview>() {
            @Override
            protected LinkPreview doInBackground(Void... voids) {
                try {
                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                            .timeout(10000)
                            .get();

                    LinkPreview preview = new LinkPreview();
                    preview.setUrl(url);

                    // Try Open Graph tags first
                    String title = getMetaTag(doc, "og:title");
                    if (title == null) title = doc.title();
                    
                    String description = getMetaTag(doc, "og:description");
                    if (description == null) description = getMetaTag(doc, "description");
                    
                    String imageUrl = getMetaTag(doc, "og:image");
                    if (imageUrl == null) {
                        Element img = doc.select("img").first();
                        if (img != null) imageUrl = img.absUrl("src");
                    }

                    String siteName = getMetaTag(doc, "og:site_name");
                    
                    preview.setTitle(title);
                    preview.setDescription(description);
                    preview.setImageUrl(imageUrl);
                    preview.setSiteName(siteName);
                    
                    return preview;

                } catch (IOException e) {
                    return null;
                }
            }

            private String getMetaTag(Document doc, String property) {
                Elements meta = doc.select("meta[property~=" + property + "]");
                if (!meta.isEmpty()) {
                    return meta.first().attr("content");
                }
                return null;
            }

            @Override
            protected void onPostExecute(LinkPreview preview) {
                if (preview != null) {
                    callback.onPreviewReceived(preview);
                } else {
                    callback.onError("Failed to fetch link preview");
                }
            }
        }.execute();
    }
}