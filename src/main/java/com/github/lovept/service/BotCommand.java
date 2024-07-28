package com.github.lovept.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

import java.util.function.BiConsumer;

/**
 * @author lovept
 * @date 2024/7/23 20:50
 * @description bot command interface
 */
@FunctionalInterface
public interface BotCommand extends BiConsumer<TelegramBot, Update> {
}
