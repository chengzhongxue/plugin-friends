package la.moony.friends.util;

import javax.xml.parsers.*;
import la.moony.friends.extension.FriendPost;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RSSParser {

    public Map<String,Object> data(String rssUrl){
        Map<String,Object> map = new HashMap<>();

        // 创建DocumentBuilder对象
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 读取XML文件
        log.error("订阅rss链接 {} ", rssUrl);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            URL url = new URL(rssUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == 200) {
                InputStream inputStream = connection.getInputStream();
                Document document = builder.parse(inputStream);
                NodeList channel = document.getElementsByTagName("channel");
                NodeList feed = document.getElementsByTagName("feed");
                if (channel.getLength()>0){
                    channelElement(map,document);
                }else if(feed.getLength()>0){
                    feedElement(map,document);
                }else {
                    log.error("{} 当前链接不是W3C标准规则", rssUrl);
                }
            }else {
                log.error("{} 订阅rss链接访问失败", rssUrl);
            }
        } catch (Exception  e) {
            throw new RuntimeException(e);
        }


        return map;
    }

    private static void feedElement(Map<String,Object> map,Document document ) {
        List<FriendPost> friendPostList = new ArrayList<>();
        Element feedlElement = (Element)document.getElementsByTagName("feed").item(0);
        String author = getTextValue(feedlElement, "title");
        String channelLink = getTextValue(feedlElement, "id");
        String channelDescription = getTextValue(feedlElement, "subtitle");
        map.put("channelLink",channelLink);
        map.put("author",author);
        map.put("channelDescription",channelDescription);
        // 遍历<item>标签获取每条信息
        NodeList itemElements = feedlElement.getElementsByTagName("entry");

        for (int i=0; i < itemElements.getLength(); i++) {
            Element itemElement = (Element)itemElements.item(i);

            String title = getTextValue(itemElement, "title");
            String link = getTextValue(itemElement, "id");
            String published = getTextValue(itemElement, "published");
            // 将字符串时间转换为Instant对象
            Instant instant = Instant.parse(published);
            String description = null;
            String content = getTextValue(itemElement, "content");
            String summary = getTextValue(itemElement, "summary");
            if (summary!=null){
                description = summary;
            }else if (content!=null){
                description = content;
            }
            FriendPost friendPost = new FriendPost();
            friendPost.setSpec(new FriendPost.Spec());
            friendPost.getSpec().setUrl(channelLink);
            friendPost.getSpec().setTitle(title);
            friendPost.getSpec().setAuthor(author);
            friendPost.getSpec().setLink(link);
            friendPost.getSpec().setPubDate(instant);
            friendPost.getSpec().setDescription(description);
            friendPostList.add(friendPost);
        }
        map.put("friendPostList", friendPostList);

    }



    // 根节点为<channel>标签
    private static void channelElement(Map<String,Object> map,Document document ) {
        List<FriendPost> friendPostList = new ArrayList<>();
        Element channelElement = (Element)document.getElementsByTagName("channel").item(0);
        String author = getTextValue(channelElement, "title");
        String channelLink = getTextValue(channelElement, "link");
        String channelDescription = getTextValue(channelElement, "description");
        map.put("channelLink",channelLink);
        map.put("author",author);
        map.put("channelDescription",channelDescription);
        // 遍历<item>标签获取每条信息
        NodeList itemElements = channelElement.getElementsByTagName("item");

        for (int i=0; i < itemElements.getLength(); i++) {
            Element itemElement = (Element)itemElements.item(i);

            String title = getTextValue(itemElement, "title");
            String link = getTextValue(itemElement, "link");
            String pubDate = getTextValue(itemElement, "pubDate");
            SimpleDateFormat  format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            Date date = null;
            try {
                date = format.parse(pubDate);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            String description = getTextValue(itemElement, "description");
            FriendPost friendPost = new FriendPost();
            friendPost.setSpec(new FriendPost.Spec());
            friendPost.getSpec().setUrl(channelLink);
            friendPost.getSpec().setTitle(title);
            friendPost.getSpec().setAuthor(author);
            friendPost.getSpec().setLink(link);
            friendPost.getSpec().setPubDate(date.toInstant());
            friendPost.getSpec().setDescription(description);
            friendPostList.add(friendPost);
        }
        map.put("friendPostList", friendPostList);
    }

    private static String getTextValue(Element element, String tagName) {
        NodeList elementsByTagName = element.getElementsByTagName(tagName);
        if (elementsByTagName.getLength()>0){
            NodeList nodeList = elementsByTagName.item(0).getChildNodes();
            StringBuilder text = new StringBuilder();
            for (int j=0; j < nodeList.getLength(); j++) {
                if (nodeList.item(j).getNodeType() == Node.TEXT_NODE || nodeList.item(j).getNodeType() == Node.CDATA_SECTION_NODE) {
                    text.append(nodeList.item(j).getTextContent());
                }
            }
            return text.toString().trim();
        }else {
            return null;
        }
    }
}