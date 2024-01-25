package la.moony.friends.service;

import la.moony.friends.vo.RSSInfo;

public interface BlogCrawlerService {

    RSSInfo getRSSInfoByRSSAddress(String rssAddress, int postsLimit);
}
