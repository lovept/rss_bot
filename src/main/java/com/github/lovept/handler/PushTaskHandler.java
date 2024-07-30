package com.github.lovept.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.lovept.entity.RssItem;
import com.github.lovept.entity.RssSource;
import com.github.lovept.entity.UserSubscription;
import com.github.lovept.mapper.RssItemMapper;
import com.github.lovept.mapper.RssSourceMapper;
import com.github.lovept.mapper.UserSubscriptionMapper;
import com.github.lovept.utils.HttpUtil;
import com.github.lovept.utils.RssUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


/**
 * @author lovept
 * @date 2024/7/23 14:30
 * @description push processing
 */
@Log4j2
@Component
public class PushTaskHandler {


    @Resource
    private RssSourceMapper rssSourceMapper;

    @Resource
    private UserSubscriptionMapper userSubscriptionMapper;

    @Resource
    private RssItemMapper rssItemMapper;

    @Resource
    private TelegramBot bot;

    public void pushMessage() {
        List<RssSource> sourceList = rssSourceMapper.selectList(null);
        sourceList.parallelStream().forEach(this::processSource);
    }

    private void processSource(RssSource source) {
        List<RssItem> rssItemList = fetchRssItems(source);
        if (rssItemList == null) {
            return;
        }

        List<UserSubscription> subscriptionList = fetchUserSubscriptions(source);
        subscriptionList.forEach(subscription -> processSubscription(subscription, rssItemList, source.getId()));

        rssItemList.forEach(this::saveRssItem);
    }

    private List<RssItem> fetchRssItems(RssSource source) {
        HttpURLConnection connection = HttpUtil.getHttpURLConnection(source.getSourceUrl());
        try {
            return RssUtil.buildRssItem(connection.getInputStream(), source.getId());
        } catch (IOException e) {
            log.error("Error fetching RSS items {}: {}", source.getSourceUrl(), e.getMessage());
            return null;
        }
    }

    private List<UserSubscription> fetchUserSubscriptions(RssSource source) {
        QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("source_id", source.getId());
        return userSubscriptionMapper.selectList(queryWrapper);
    }

    private void processSubscription(UserSubscription subscription, List<RssItem> rssItemList, Integer sourceId) {
        Date notifiedAt = subscription.getNotifiedAt();
        List<RssItem> items = rssItemList.stream()
                .filter(item -> item.getPubDate() != null && item.getPubDate().after(notifiedAt))
                .sorted(Comparator.comparing(RssItem::getPubDate))
                .toList();

        items.forEach(item -> sendMessage(subscription.getTelegramId(), formatMessage(item)));

        updateSubscriptionNotifiedAt(subscription, sourceId);
    }

    private String formatMessage(RssItem item) {
        return "[" + item.getTitle() + "](" + item.getLink() + ")";
    }

    private void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text).parseMode(ParseMode.Markdown);
        bot.execute(sendMessage);
    }

    private void updateSubscriptionNotifiedAt(UserSubscription subscription, Integer sourceId) {
        subscription.setNotifiedAt(new Date());
        QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("source_id", sourceId);
        queryWrapper.eq("telegram_id", subscription.getTelegramId());
        userSubscriptionMapper.update(subscription, queryWrapper);
    }

    private void saveRssItem(RssItem item) {
        QueryWrapper<RssItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("link", item.getLink());
        queryWrapper.eq("title", item.getTitle());
        RssItem existingItem = rssItemMapper.selectOne(queryWrapper);
        if (existingItem == null) {
            rssItemMapper.insert(item);
        }
    }

}
