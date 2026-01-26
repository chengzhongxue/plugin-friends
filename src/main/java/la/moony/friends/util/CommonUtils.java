package la.moony.friends.util;

import java.text.BreakIterator;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

public class CommonUtils {

    public static String parseAndTruncateHtml2Text(String html, int length) {
        if (StringUtils.isBlank(html)) {
            return "";
        }

        String text = Jsoup.parse(html).text();
        if (text.length() <= length) {
            return text;
        } else {
            BreakIterator bi = BreakIterator.getCharacterInstance();
            bi.setText(text);
            int bound = bi.first();
            int prevBound = bound;
            while (bound != BreakIterator.DONE && bound <= length) {
                prevBound = bound;
                bound = bi.next();
            }
            text = text.substring(0, prevBound);
            text += "...";
        }
        return text;
    }


}
