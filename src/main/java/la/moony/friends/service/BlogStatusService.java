package la.moony.friends.service;

public interface BlogStatusService {

    boolean isStatusOkByName(String name);

    void detectBlogStatus();
}
