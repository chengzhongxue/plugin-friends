package la.moony.friends.service;

import reactor.core.publisher.Mono;

public interface FriendPostService {

    Mono<Void> synchronizationFriend();

}
