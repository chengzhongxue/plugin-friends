package la.moony.friends.service;

import reactor.core.publisher.Mono;
import java.util.Map;

public interface FriendPostService {

    Mono<Void> synchronizationFriend();

    Mono<Map<String,Object>> yearlyPublishData(String friendName);

}
