package com.github.lovept.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.lovept.entity.RssItem;
import com.github.lovept.entity.RssSource;
import com.github.lovept.entity.UserSubscription;
import com.github.lovept.factory.RssParserFactory;
import com.github.lovept.kafka.KafkaProducerService;
import com.github.lovept.mapper.RssItemMapper;
import com.github.lovept.mapper.RssSourceMapper;
import com.github.lovept.mapper.UserSubscriptionMapper;
import com.github.lovept.service.RssParser;
import com.github.lovept.utils.RssUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author lovept
 * @date 2024/7/23 20:50
 * @description rss处理
 */
@Slf4j
@Service
public class RssHandler {

    @Resource
    private RssItemMapper rssItemMapper;

    @Resource
    private RssSourceMapper rssSourceMapper;

    @Resource
    private UserSubscriptionMapper userSubscriptionMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private KafkaProducerService kafkaProducerService;

    private final Map<Long, Lock> userLocks = new ConcurrentHashMap<>();

    public void pushMessages() {
        List<RssSource> sources = rssSourceMapper.selectList(null);
        sources.parallelStream().forEach(this::processRssSource);
    }


    private void processRssSource(RssSource source) {
        try (InputStream is = new URI(source.getSourceUrl()).toURL().openStream()) {
            RssParser parser = RssParserFactory.getParser(source.getSourceUrl());
            List<RssItem> items = parser.parse(is, source.getId());
            List<UserSubscription> subscriptionList = getUserSubscriptionsBySource(source);

            subscriptionList.forEach(subscription -> transactionTemplate.execute(status -> {
                processSubscription(subscription, items, source.getId());
                return null;
            }));

            items.forEach(this::saveOrUpdateRssItem);

        } catch (Exception e) {
            log.error("Error processing source {}: {}", source.getSourceUrl(), e.getMessage(), e);
        }
    }

    private void processSubscription(UserSubscription subscription, List<RssItem> rssItemList, Integer sourceId) {
        Lock userLock = userLocks.computeIfAbsent(subscription.getTelegramId(), k -> new ReentrantLock());
        userLock.lock();
        try {
            Date notifiedAt = subscription.getNotifiedAt();

            List<RssItem> items = new ArrayList<>(rssItemList.stream()
                    .filter(item -> (item.getPubDate() != null && item.getPubDate().after(notifiedAt))
                            || (item.getPubDate() == null && !rssItemExists(item)))
                    .sorted(Comparator.comparing(RssItem::getPubDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList());

            if (items.isEmpty()) {
                return;
            }

            Date latestPubDate = items.stream()
                    .map(RssItem::getPubDate)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .orElse(RssUtil.getDefaultDate());

            subscription.setNotifiedAt(latestPubDate);
            // 发送消息到Kafka
            items.forEach(item -> kafkaProducerService.sendMessage("rss_bot_topic", subscription.getTelegramId() + ":" + formatRssMessage(item)));
            updateUserSubscriptionNotifiedAt(subscription, sourceId);
        } finally {
            userLock.unlock();
            userLocks.remove(subscription.getTelegramId());
        }
    }


    private List<UserSubscription> getUserSubscriptionsBySource(RssSource source) {
        QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("source_id", source.getId());
        return userSubscriptionMapper.selectList(queryWrapper);
    }


    private void saveOrUpdateRssItem(RssItem item) {
        try {
            rssItemMapper.insert(item);
        } catch (Exception ignored) {
        }
    }


    private boolean rssItemExists(RssItem rssItem) {
        return getRssItemByLinkHash(rssItem.getLinkHash()) != null;
    }

    private RssItem getRssItemByLinkHash(String linkHash) {
        QueryWrapper<RssItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("link_hash", linkHash);
        return rssItemMapper.selectOne(queryWrapper);
    }

    private String formatRssMessage(RssItem item) {
        return "[" + item.getTitle() + "](" + item.getLink() + ")";
    }


    private void updateUserSubscriptionNotifiedAt(UserSubscription subscription, Integer sourceId) {
        QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("source_id", sourceId);
        queryWrapper.eq("telegram_id", subscription.getTelegramId());
        userSubscriptionMapper.update(subscription, queryWrapper);
    }
}