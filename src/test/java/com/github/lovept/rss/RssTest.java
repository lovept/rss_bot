package com.github.lovept.rss;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author lovept
 * @date 2024/7/27 14:04
 * @description TODO
 */
public class RssTest {
    @Test
    public void t1() {
        //String url = "https://stackoverflow.com/feeds/tag?tagnames=rome";
        //String url = "https://sspai.com/feed";
        //String url = "https://rss.nodeseek.com/";
        //String url = "https://audiences.me/torrentrss.php?rows=10&tea20=1&torrent_type=1&rsskey=9c7bc8944767bb440e31b99fb8ef006d";
        //String url = "https://www.raycast.com/store/feed.atom";
        //String url = "https://www.cnblogs.com/1399z3blog/rss";
        //String url = "https://www.v2ex.com/feed/macos.xml";
        //SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URI(url).toURL()));
        //
        //System.out.println(feed.getTitle());



        String url = "https://www.v2ex.com/feed/macos.xml";
        try {
            HttpURLConnection httpcon = (HttpURLConnection) new URI(url).toURL().openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");

            try (InputStream is = httpcon.getInputStream()) {
                SyndFeed feed = new SyndFeedInput().build(new XmlReader(is));
                System.out.println(feed.getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void t2() {
        String filePath = "path/to/your/file.xml";
        try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(is));
            System.out.println(feed.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void t3() {
        //// 使用 lambda 表达式实现函数式接口
        //AsyncTaskExecutorService executorService = (rssSource) -> CompletableFuture.runAsync(() -> {
        //    try {
        //        String url = rssSource.getSourceUrl();
        //        HttpURLConnection cnn = (HttpURLConnection) new URI(url).toURL().openConnection();
        //
        //        try (InputStream is = cnn.getInputStream()) {
        //            SyndFeed feed = new SyndFeedInput().build(new XmlReader(is));
        //            System.out.println(feed.getTitle());
        //        }
        //    } catch (Exception ignored) {
        //    }
        //});
        //
        //// 创建一个 RssSource 实例
        //RssSource rssSource = RssSource.builder()
        //        .sourceUrl("https://www.v2ex.com/feed/macos.xml")
        //        .build();
        //
        //// 调用异步方法
        //executorService.asyncUpdateMessage(rssSource);
    }
}
