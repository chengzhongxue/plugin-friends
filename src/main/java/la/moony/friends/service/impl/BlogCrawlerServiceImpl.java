package la.moony.friends.service.impl;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.rest.FriendPostController;
import la.moony.friends.service.BlogCrawlerService;
import la.moony.friends.util.OkHttpUtil;
import la.moony.friends.vo.RSSInfo;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Component
public class BlogCrawlerServiceImpl implements BlogCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(FriendPostController.class);

    private static final OkHttpClient client = OkHttpUtil.getUnsafeOkHttpClient();

    @Override
    public RSSInfo getRSSInfoByRSSAddress(String rssAddress, int postsLimit) {

        int postCount = 0;
        Request request = new Request.Builder()
            .url(rssAddress)
            .build();

        Call call = client.newCall(request);

        Response response = null;
        ResponseBody responseBody = null;
        InputStream inputStream = null;

        // 创建DocumentBuilder对象
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            response = call.execute();
            responseBody = response.body();
            inputStream = responseBody.byteStream();

            RSSInfo rssInfo = new RSSInfo();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            NodeList channel = document.getElementsByTagName("channel");
            NodeList feed = document.getElementsByTagName("feed");
            if (channel.getLength()>0){
                channelElement(rssInfo,document,postsLimit);
            }else if(feed.getLength()>0){
                feedElement(rssInfo,document,postsLimit);
            }else {
                log.error("{} 当前链接不是W3C标准规则", rssAddress);
                return null;
            }
            return rssInfo;
        } catch (Exception e) {
            log.error("error in crawling blog", e);
            return null;
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
                if (null != responseBody) {
                    responseBody.close();
                }
                if (null != response) {
                    response.close();
                }
            } catch (Exception e) {
                log.info(e.getMessage(), e);
            }
        }



    }

    private static void feedElement(RSSInfo rssInfo,Document document,int postsLimit ) {
        List<FriendPost> friendPostList = new ArrayList<>();
        Element feedlElement = (Element)document.getElementsByTagName("feed").item(0);
        String author = getTextValue(feedlElement, "title");
        String channelLink = getTextValue(feedlElement, "id");
        String channelDescription = getTextValue(feedlElement, "subtitle");
        rssInfo.setBlogTitle(author);
        rssInfo.setBlogAddress(channelLink);
        rssInfo.setBlogDescription(channelDescription);
        // 遍历<item>标签获取每条信息
        NodeList itemElements = feedlElement.getElementsByTagName("entry");
        int postCount = 0;
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
            postCount++;
            if (postCount >= postsLimit) {
                break;
            }
        }
        rssInfo.setBlogPosts(friendPostList);

    }



    // 根节点为<channel>标签
    private static void channelElement(RSSInfo rssInfo,Document document,int postsLimit ) {
        List<FriendPost> friendPostList = new ArrayList<>();
        Element channelElement = (Element)document.getElementsByTagName("channel").item(0);
        String author = getTextValue(channelElement, "title");
        String channelLink = getTextValue(channelElement, "link");
        String channelDescription = getTextValue(channelElement, "description");
        rssInfo.setBlogTitle(author);
        rssInfo.setBlogAddress(channelLink);
        rssInfo.setBlogDescription(channelDescription);
        // 遍历<item>标签获取每条信息
        NodeList itemElements = channelElement.getElementsByTagName("item");
        int postCount = 0;
        for (int i=0; i < itemElements.getLength(); i++) {
            Element itemElement = (Element)itemElements.item(i);

            String title = getTextValue(itemElement, "title");
            String link = getTextValue(itemElement, "link");
            String pubDate = getTextValue(itemElement, "pubDate");
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
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
            postCount++;
            if (postCount >= postsLimit) {
                break;
            }
        }
        rssInfo.setBlogPosts(friendPostList);
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
