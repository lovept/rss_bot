package com.github.lovept.kafka;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author lovept
 * @date 2024/7/23 20:50
 * @description kafka消费者
 */
@Log4j2
@Service
public class KafkaConsumerService {

    private final TelegramBot bot;
    private final ScheduledExecutorService scheduler;
    private final BlockingQueue<String> messageQueue;

    public KafkaConsumerService(TelegramBot bot) {
        this.bot = bot;
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.messageQueue = new LinkedBlockingQueue<>();
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::sendMessages, 0, 1, TimeUnit.SECONDS);
    }


    @KafkaListener(topics = "rss_bot_topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessages(String message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessages() {
        for (int i = 0; i < 30; i++) {
            String message = messageQueue.poll();
            if (message == null) {
                break;
            }

            // 消息格式为 "chatId:message"
            String[] parts = message.split(":", 2);

            long chatId = Long.parseLong(parts[0].replaceAll("\"", ""));

            sendTelegramMessage(chatId, parts[1]);
        }
    }

    private void sendTelegramMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text)
                .parseMode(ParseMode.Markdown);
        bot.execute(sendMessage);
    }
}
