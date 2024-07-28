package com.github.lovept.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;


/**
 * @author lovept
 * @date 2024/7/23 14:30
 * @description telegram image processing
 */
@Log4j2
public class TelegramImageHandler {

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    private static final String TELEGRAM_FILE_URL = "https://api.telegram.org/file/bot";

    private final TelegramBot bot;

    public TelegramImageHandler(TelegramBot bot) {
        this.bot = bot;
    }

    //public void download(Resource resource, String fileId) {
    //    try {
    //        // Step 1: Get file path from Telegram API
    //        String token = bot.getToken();
    //        String api = TELEGRAM_API_URL + token + "/getFile?file_id=" + fileId;
    //        String filePath = getFilePathFromTelegramApi(api);
    //        if (filePath == null) {
    //            System.out.println("Failed to get file path from Telegram API.");
    //            return;
    //        }
    //        resource.setFilename(filePath.substring(filePath.indexOf("/") + 1));
    //        if (filePath.endsWith("jpg") || filePath.endsWith("JPG") || filePath.endsWith("jpeg") || filePath.endsWith("JPEG")) {
    //            resource.setType("image/jpeg");
    //        }
    //        // Step 2: Download image from Telegram
    //        byte[] imageData = downloadImageFromTelegram(filePath, token);
    //        resource.setBlob(imageData);
    //
    //    } catch (Exception e) {
    //        log.error(e.getMessage());
    //    }
    //}


    private static String getFilePathFromTelegramApi(String api) throws Exception {

        URL url = new URI(api).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStream responseStream = new BufferedInputStream(connection.getInputStream());
        JsonObject jsonResponse = JsonParser.parseReader(new java.io.InputStreamReader(responseStream)).getAsJsonObject();
        connection.disconnect();

        if (jsonResponse.get("ok").getAsBoolean()) {
            return jsonResponse.getAsJsonObject("result").get("file_path").getAsString();
        } else {
            return null;
        }
    }

    private static byte[] downloadImageFromTelegram(String filePath, String token) throws Exception {
        String api = TELEGRAM_FILE_URL + token + "/" + filePath;
        URL url = new URI(api).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        connection.disconnect();

        return byteArrayOutputStream.toByteArray();
    }

}
