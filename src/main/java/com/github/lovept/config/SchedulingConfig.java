package com.github.lovept.config;

import com.github.lovept.handler.RssHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;


/**
 * @author lovept
 * @date 2024/7/23 14:30
 * @description timed task
 */
@Log4j2
@Component
public class SchedulingConfig implements SchedulingConfigurer {

    private final TelegramBotConfig telegramBotConfig;

    private final RssHandler rssHandler;

    public SchedulingConfig(RssHandler rssHandler, TelegramBotConfig telegramBotConfig) {
        this.rssHandler = rssHandler;
        this.telegramBotConfig = telegramBotConfig;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(rssHandler::pushMessages, triggerContext -> {
            String cron = telegramBotConfig.getCron();
            Trigger trigger = new CronTrigger(cron);
            return trigger.nextExecution(triggerContext);
        });
    }
}