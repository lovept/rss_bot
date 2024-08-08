package com.github.lovept.factory;

import com.github.lovept.service.RssParser;
import com.github.lovept.service.impl.DefaultRssParser;
import com.github.lovept.service.impl.NodeSeekRssParser;
import com.github.lovept.service.impl.PoJie52RssParser;
import com.github.lovept.service.impl.SouthPlusRssParser;

/**
 * @author rss格式化工厂
 */
public class RssParserFactory {
    public static RssParser getParser(String sourceUrl) {
        if (sourceUrl.contains("www.summer-plus.net")) {
            return new SouthPlusRssParser();
        } else if (sourceUrl.contains("www.nodeseek.com")) {
            return new NodeSeekRssParser();
        }else if (sourceUrl.contains("www.52pojie.cn")) {
            return new PoJie52RssParser();
        }else {
            return new DefaultRssParser();
        }
    }
}
