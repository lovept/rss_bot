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
public class PushTaskHandler{


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
        sourceList.forEach(x -> {
            // 获取rssItem
            HttpURLConnection connection = HttpUtil.getHttpURLConnection(x.getSourceUrl());
            List<RssItem> rssItemList;
            try {
                rssItemList = RssUtil.buildRssItem(connection.getInputStream(), x.getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 查询订阅了此源的用户
            QueryWrapper<UserSubscription> userSubscriptionQueryWrapper = new QueryWrapper<>();
            userSubscriptionQueryWrapper.eq("source_id", x.getId());
            List<UserSubscription> subscriptionList = userSubscriptionMapper.selectList(userSubscriptionQueryWrapper);
            // 根据用户的推送时间,推送rssItem
            subscriptionList.forEach(y -> {
                // 根据用户的最后推送时间 推送信息
                Date notifiedAt = y.getNotifiedAt();
                List<RssItem> items = rssItemList.stream()
                        .filter(z -> z.getPubDate().after(notifiedAt))
                        .sorted(Comparator.comparing(RssItem::getPubDate))
                        .toList();
                items.forEach(i -> {

                    String sendText = "[" + i.getTitle() + "]" + "(" + i.getLink() + ")";
                    sendMessage(y.getTelegramId(), sendText);
                });
                y.setNotifiedAt(new Date());
                QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("source_id", x.getId());
                queryWrapper.eq("telegram_id", y.getTelegramId());
                userSubscriptionMapper.update(y,queryWrapper);
            });

            rssItemList.forEach(this::saveRssItem);

        });
    }


    private void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text)
                .parseMode(ParseMode.Markdown);
        bot.execute(sendMessage);
    }


    private void saveRssItem(RssItem item) {
        QueryWrapper<RssItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("link", item.getLink());
        queryWrapper.eq("title", item.getTitle());
        RssItem rssItem = rssItemMapper.selectOne(queryWrapper);
        if (rssItem != null) {
            return;
        }
        rssItemMapper.insert(item);
    }


}
