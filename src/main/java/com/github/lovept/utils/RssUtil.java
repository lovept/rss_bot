package com.github.lovept.utils;

import com.github.lovept.entity.RssItem;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;


/**
 * @author lovept
 * @date 2024/7/23 20:50
 */
public class RssUtil {

    public static SyndFeed buildSyndFeed(InputStream is) {
        SyndFeed feed;
        try {
            feed = new SyndFeedInput().build(new XmlReader(is));
        } catch (FeedException | IOException e) {
            throw new RuntimeException(e);
        }
        return feed;
    }

    public static SyndFeed buildSyndFeed(String url) {
        HttpURLConnection con = HttpUtil.getHttpURLConnection(url);
        SyndFeed feed;
        try (InputStream is = con.getInputStream()) {
            feed = new SyndFeedInput().build(new XmlReader(is));
        } catch (FeedException | IOException e) {
            throw new RuntimeException(e);
        }
        return feed;
    }


    public static List<RssItem> buildRssItem(InputStream is, Integer sourceId) {
        SyndFeed feed = buildSyndFeed(is);
        return feed.getEntries()
                .stream()
                .map(entry -> RssItem.builder()
                        .sourceId(sourceId)
                        .title(entry.getTitle().replaceAll("\\[", "").replaceAll("]", ""))
                        .link(entry.getLink())
                        .pubDate(entry.getPublishedDate() == null ? entry.getUpdatedDate() : entry.getPublishedDate())
                        .description(entry.getDescription() == null ? "" : entry.getDescription().getValue())
                        //.guid(entry.getGenerator())
                        .build())
                .toList();
    }
}
