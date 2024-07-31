package com.github.lovept.factory;

import com.github.lovept.service.RssParser;
import com.github.lovept.service.impl.DefaultRssParser;
import com.github.lovept.service.impl.SouthPlusRssParser;

/**
 * @author rss格式化工厂
 */
public class RssParserFactory {
    public static RssParser getParser(String sourceUrl) {
        if (sourceUrl.contains("www.summer-plus.net")) {
            return new SouthPlusRssParser();
        } else {
            return new DefaultRssParser();
        }
    }
}
