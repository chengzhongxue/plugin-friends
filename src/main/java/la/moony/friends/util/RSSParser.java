package la.moony.friends.util;

import javax.xml.parsers.*;
import la.moony.friends.extension.FriendPost;
import org.w3c.dom.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSSParser {

    public Map<String,Object> data(String rssUrl) throws Exception {
        Map<String,Object> map = new HashMap<>();
        // 创建DocumentBuilder对象
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 读取XML文件
        URL url = new URL(rssUrl);
        URLConnection connection = url.openConnection();
        InputStream inputStream = connection.getInputStream();
        Document document = builder.parse(inputStream);

        // 根节点为<channel>标签
        Element channelElement = (Element)document.getElementsByTagName("channel").item(0);
        String author = getTextValue(channelElement, "title");
        String channelLink = getTextValue(channelElement, "link");
        String channelDescription = getTextValue(channelElement, "description");
        map.put("channelLink",channelLink);
        // 遍历<item>标签获取每条信息
        NodeList itemElements = channelElement.getElementsByTagName("item");
        List<FriendPost> friendPostList = new ArrayList<>();
        for (int i=0; i < itemElements.getLength(); i++) {
            Element itemElement = (Element)itemElements.item(i);

            String title = getTextValue(itemElement, "title");
            String link = getTextValue(itemElement, "link");
            String pubDate = getTextValue(itemElement, "pubDate");
            String description = getTextValue(itemElement, "description");
            FriendPost friendPost = new FriendPost();
            friendPost.setSpec(new FriendPost.Spec());
            friendPost.getSpec().setUrl(channelLink);
            friendPost.getSpec().setTitle(title);
            friendPost.getSpec().setAuthor(author);
            friendPost.getSpec().setLink(link);
            friendPost.getSpec().setPubDate(new Date());
            friendPost.getSpec().setDescription(description);
            friendPostList.add(friendPost);
        }

        map.put("friendPostList", friendPostList);

        return map;
    }

    private static String getTextValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName).item(0).getChildNodes();
        StringBuilder text = new StringBuilder();
        for (int j=0; j < nodeList.getLength(); j++) {
            if (nodeList.item(j).getNodeType() == Node.TEXT_NODE || nodeList.item(j).getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(nodeList.item(j).getTextContent());
            }
        }
        return text.toString().trim();
    }
}