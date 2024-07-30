package com.github.lovept.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.lovept.entity.RssSource;
import com.github.lovept.entity.User;
import com.github.lovept.entity.UserSubscription;
import com.github.lovept.mapper.RssSourceMapper;
import com.github.lovept.mapper.UserMapper;
import com.github.lovept.mapper.UserSubscriptionMapper;
import com.github.lovept.service.BotCommand;
import com.github.lovept.utils.RssUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.rometools.rome.feed.synd.SyndFeed;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lovept
 * @date 2024/7/23 19:47
 * @description dialog builder
 */
public class TelegramUpdateHandler {

    private final TelegramBot bot;
    private final Update update;
    private final Map<String, BotCommand> commandMap = new HashMap<>();

    private final RssSourceMapper rssSourceMapper;

    private final UserMapper userMapper;

    private final UserSubscriptionMapper userSubscriptionMapper;


    public TelegramUpdateHandler(TelegramBot bot, Update update, RssSourceMapper rssSourceMapper
            , UserMapper userMapper, UserSubscriptionMapper userSubscriptionMapper) {
        this.bot = bot;
        this.update = update;
        this.rssSourceMapper = rssSourceMapper;
        this.userMapper = userMapper;
        this.userSubscriptionMapper = userSubscriptionMapper;
        initializeCommands();
    }


    public void handler() {
        long chatId = update.message().chat().id();
        String commandText = update.message().text();


        String regex = "/(\\S+)(\\s(https://\\S+))?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(commandText);

        if (matcher.find()) {
            BotCommand command = commandMap.getOrDefault(matcher.group(1), commandMap.get("start"));
            command.accept(bot, update);
        } else {
            sendMessage(chatId, "There is no such command.", true);
        }

    }

    private void initializeCommands() {
        commandMap.put("start", (bot, update) -> startCommand());
        commandMap.put("list", (bot, update) -> listCommand());
        commandMap.put("sub", (bot, update) -> subCommand());
        commandMap.put("unsub", (bot, update) -> unsubCommand());
        commandMap.put("help", (bot, update) -> helpCommand());
    }


    private void startCommand() {
        long chatId = update.message().chat().id();

        String firstName = update.message().from().firstName();
        String lastName = update.message().from().lastName();
        String username = (firstName == null ? "" : firstName) + (lastName == null ? "" : lastName);

        User user = User.builder()
                .telegramId(chatId)
                .username(username)
                .build();

        userMapper.insertOrUpdate(user);
        sendMessage(chatId, "You're already registered.", true);
    }


    private void listCommand() {
        long chatId = update.message().chat().id();
        User user = fetchUser(chatId);
        if (user == null) {
            sendMessage(chatId, "You're not registered.", false);
            return;
        }

        List<UserSubscription> userSubscriptions = fetchUserSubscriptions(chatId);

        // 获取所有的 sourceId
        List<Integer> sourceIds = userSubscriptions.stream()
                .map(UserSubscription::getSourceId)
                .collect(Collectors.toList());
        if (sourceIds.isEmpty()) {
            sendMessage(chatId, "You haven't subscribed to anything yet.", false);
            return;
        }
        // 查询所有的订阅源信息
        List<RssSource> rssSourceList = fetchRssSources(sourceIds);

        String subscriptionNames = rssSourceList.stream()
                .map(rssSource -> "\\[`" + rssSource.getId() + "`] " + "[" + rssSource.getSourceName() + "]" + "(" + rssSource.getSourceUrl() + ")")
                .collect(Collectors.joining("\n"));

        sendMessage(chatId, subscriptionNames, true);
    }


    private void subCommand() {
        long chatId = update.message().chat().id();
        User user = fetchUser(chatId);

        if (user == null) {
            sendMessage(chatId, "You haven't subscribed to anything yet.", false);
            return;
        }

        String commandText = update.message().text();

        String regex = "/(\\S+)\\s(https://\\S+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(commandText);

        if (!matcher.find()) {
            sendMessage(chatId, "There's something wrong with the path you entered.", true);
            return;
        }

        String sourceUrl = matcher.group(2);
        RssSource source = fetchRssSource(sourceUrl);

        if (source != null) {
            processUserSubscription(user, source);
            sendMessage(chatId, "This site is subscribed!", true);
            return;
        }

        saveFeedInfo(sourceUrl, chatId);

    }


    private void unsubCommand() {
        long chatId = update.message().chat().id();
        // 查询该用户的订阅
        String commandText = update.message().text();

        String regex = "/unsub\\s+(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(commandText);

        if (matcher.matches()) {
            String sourceId = matcher.group(1);
            // 删除用户订阅
            QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("source_id", sourceId);
            queryWrapper.eq("telegram_id", chatId);
            UserSubscription userSubscription = userSubscriptionMapper.selectOne(queryWrapper);

            if (userSubscription == null) {
                sendMessage(chatId, "You didn't subscribe to this.", false);
                return;
            }
            userSubscriptionMapper.delete(queryWrapper);
            sendMessage(chatId, "Subscription deleted.", true);
        } else {
            sendMessage(chatId, "This command is wrong.", true);
        }

    }

    private void helpCommand() {
        long chatId = update.message().chat().id();

        String helpResult = """
                /sub - add subscription -> [/sub https://example.com]
                /list - get a list of my subscriptions
                /unsub - unsubscribe -> [/unsub 1]
                /help - get this help""";

        sendMessage(chatId, helpResult, true);
    }



    private void sendMessage(long chatId, String text, boolean replyToMessage) {
        SendMessage sendMessage = new SendMessage(chatId, text).parseMode(ParseMode.Markdown);
        if (replyToMessage) {
            sendMessage.replyToMessageId(update.message().messageId());
        }
        bot.execute(sendMessage);
    }



    private void processUserSubscription(User user, RssSource source) {
        UserSubscription subscription = fetchUserSubscription(user.getTelegramId(), source.getId());

        if (subscription != null) {
            return;
        }

        UserSubscription userSubscription = UserSubscription.builder()
                .telegramId(user.getTelegramId())
                .sourceId(source.getId())
                .subscribedAt(new Date())
                .notifiedAt(getDefaultDate())
                .build();

        userSubscriptionMapper.insert(userSubscription);
    }


    private void saveFeedInfo(String url, long chatId) {
        SyndFeed feed = RssUtil.buildSyndFeed(url);
        RssSource rssSource = RssSource.builder()
                .sourceUrl(url)
                .sourceName(feed.getTitle())
                .description(feed.getDescription())
                .lastChecked(new Date())
                .build();
        rssSourceMapper.insert(rssSource);

        UserSubscription userSubscription = UserSubscription.builder()
                .telegramId(chatId)
                .sourceId(rssSource.getId())
                .subscribedAt(new Date())
                .notifiedAt(getDefaultDate())
                .build();
        userSubscriptionMapper.insert(userSubscription);
        sendMessage(chatId, "Subscription added successfully!", true);
    }


    private Date getDefaultDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        return calendar.getTime();
    }


    private void defaultCommand() {

        //long chatId = update.message().chat().id();
        //String text = update.message().text();
        //String caption = update.message().caption();
        //MessageOrigin messageOrigin = update.message().forwardOrigin();
        //
        //if (messageOrigin instanceof MessageOriginHiddenUser) {
        //    MessageOriginHiddenUser origin = (MessageOriginHiddenUser) update.message().forwardOrigin();
        //    if (origin != null) {
        //        String forwardedSenderName = origin.senderUserName();
        //        text = "Forwarded from: " + forwardedSenderName + "\n" + (text == null ? "" : text) + (caption == null ? "" : caption);
        //    }
        //}
        //// 接收消息 todo 视频
        //memo.setContent(text == null ? "" : text);
        //log.info("Text saved to database successfully.");
        //
        //
        //
        //PhotoSize[] photos = update.message().photo();
        //
        //if (photos != null) {
        //    PhotoSize largestPhoto = Arrays.stream(photos)
        //            .max(Comparator.comparingLong(PhotoSize::fileSize))
        //            .orElse(null);
        //
        //    // 4.返回成功的消息
        //    if (largestPhoto != null) {
        //        TelegramImageDownloader imageDownloader = new TelegramImageDownloader(bot);
        //        resource.setSize(Math.toIntExact(largestPhoto.fileSize()));
        //        imageDownloader.download(resource, largestPhoto.fileId());
        //        resourceMapper.insert(resource);
        //        log.info("Image saved to database successfully.");
        //    }
        //}
        //
        //
        //SendMessage sendMessage = new SendMessage(chatId, "\uD83C\uDF89 memos/" + memo.getId())
        //        .parseMode(ParseMode.Markdown)
        //        .replyToMessageId(update.message().messageId());
        //
        //SendResponse response = bot.execute(sendMessage);
    }


    private List<UserSubscription> fetchUserSubscriptions(long chatId) {
        QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("telegram_id", chatId);
        return userSubscriptionMapper.selectList(queryWrapper);
    }

    private UserSubscription fetchUserSubscription(long chatId, Integer sourceId) {
        QueryWrapper<UserSubscription> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("telegram_id", chatId);
        queryWrapper.eq("source_id", sourceId);
        return userSubscriptionMapper.selectOne(queryWrapper);
    }

    private List<RssSource> fetchRssSources(List<Integer> sourceIds) {
        QueryWrapper<RssSource> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", sourceIds);
        return rssSourceMapper.selectList(queryWrapper);
    }


    private User fetchUser(long chatId) {
        // 查询用户表是否注册
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("telegram_id", chatId);

        return userMapper.selectOne(userQueryWrapper);
    }


    private RssSource fetchRssSource(String sourceUrl) {
        QueryWrapper<RssSource> rssSourceQueryWrapper = new QueryWrapper<>();
        rssSourceQueryWrapper.eq("source_url", sourceUrl);
        return rssSourceMapper.selectOne(rssSourceQueryWrapper);
    }

}
