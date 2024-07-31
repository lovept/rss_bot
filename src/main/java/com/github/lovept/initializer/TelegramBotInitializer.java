package com.github.lovept.initializer;

import com.github.lovept.config.TelegramBotConfig;
import com.github.lovept.handler.TelegramUpdateHandler;
import com.github.lovept.mapper.RssSourceMapper;
import com.github.lovept.mapper.UserMapper;
import com.github.lovept.mapper.UserSubscriptionMapper;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author lovept
 * @date 2024/7/23 14:29
 * @description Bot initialization
 */
@Slf4j
@Configuration
public class TelegramBotInitializer {

    @Resource
    private TelegramBotConfig config;

    private TelegramBot telegramBot;

    @Resource
    private RssSourceMapper rssSourceMapper;

    @Resource
    private UserMapper userMapper;


    @Resource
    private UserSubscriptionMapper subscriptionMapper;


    @PostConstruct
    private void init() {
        telegramBot = new TelegramBot(config.getToken());
        telegramBot.setUpdatesListener(this::handleUpdates, this::handleError);
    }


    @Bean
    public TelegramBot telegramBot() {
        return telegramBot;
    }

    private int handleUpdates(List<Update> updates) {
        updates.forEach(update -> new TelegramUpdateHandler(telegramBot, update, rssSourceMapper, userMapper
                , subscriptionMapper).handler());
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void handleError(TelegramException e) {
        if (e.response() != null) {
            log.error("Error code: {}, Description: {}", e.response().errorCode(), e.response().description());
        } else {
            log.error(e.getMessage());
        }
    }
}
