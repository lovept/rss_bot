package com.github.lovept.utils;

import com.github.lovept.entity.RssItem;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Date;
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


    public static Date getDefaultDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        return calendar.getTime();
    }
}
