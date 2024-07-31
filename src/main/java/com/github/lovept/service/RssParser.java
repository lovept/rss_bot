package com.github.lovept.service;

import com.github.lovept.entity.RssItem;

import java.io.InputStream;
import java.util.List;

/**
 * @author lovept
 * @date 2024/7/23 20:50
 * @description rss parser interface
 */
public interface RssParser {
    List<RssItem> parse(InputStream is, Integer sourceId);
}