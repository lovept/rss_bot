package com.github.lovept.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author lovept
 * @date 2024/7/23 14:30
 * @description Bot Configuration
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bot")
public class TelegramBotConfig {
    private String token;
    private String cron;
}