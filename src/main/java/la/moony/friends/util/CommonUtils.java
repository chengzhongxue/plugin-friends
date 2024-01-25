package la.moony.friends.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CommonUtils {


    public static String getDomain(String address) {
        // scheme
        if (address.startsWith("https://")) {
            address = address.substring("https://".length());
        } else if (address.startsWith("http://")) {
            address = address.substring("http://".length());
        }

        // tail
        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return address;
    }


    public static String md5(String str) {
        StringBuilder md5 = new StringBuilder();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = messageDigest.digest();
            for (byte b : bytes) {
                md5.append(byteToHex(b));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5.toString();
    }

    private static String byteToHex(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() < 2) {
            hex = "0" + hex;
        }
        return hex;
    }

    public static String parseAndTruncateHtml2Text(String html, int length) {
        if (StringUtils.isBlank(html)) {
            return "";
        }

        String text = Jsoup.parse(html).text();
        if (text.length() <= length) {
            return text;
        } else {
            text = text.substring(0, length);
            text += "...";
        }
        return text;
    }


}
