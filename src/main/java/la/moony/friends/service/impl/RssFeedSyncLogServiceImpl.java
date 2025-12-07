package la.moony.friends.service.impl;

import la.moony.friends.ListedRssSyncLog;
import la.moony.friends.RssFeedSyncEvent;
import la.moony.friends.extension.CronFriendPost;
import la.moony.friends.extension.Link;
import la.moony.friends.extension.RssFeedSyncLog;
import la.moony.friends.query.RssSyncLogQuery;
import la.moony.friends.service.RssFeedSyncLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.isNull;

@Component
public class RssFeedSyncLogServiceImpl implements RssFeedSyncLogService {

    private final ReactiveExtensionClient client;

    private final ApplicationEventPublisher eventPublisher;

    public RssFeedSyncLogServiceImpl(ReactiveExtensionClient client,
        ApplicationEventPublisher eventPublisher) {
        this.client = client;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<ListResult<ListedRssSyncLog>> listRssSyncLog(RssSyncLogQuery query) {
        return buildListOptions(query)
            .flatMap(listOptions -> client.listBy(Link.class, listOptions,
                PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort())
            ))
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .map(this::getListedRssSyncLog)
                .flatMapSequential(Function.identity())
                .collectList()
                .map(listedApplications -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), listedApplications)
                )
                .defaultIfEmpty(ListResult.emptyResult())
            );
    }

    private Mono<ListedRssSyncLog> getListedRssSyncLog(Link link) {
        Assert.notNull(link, "The link must not be null.");
        var listedRssSyncLog = new ListedRssSyncLog().setLink(link);
        var logMono = client.fetch(RssFeedSyncLog.class,"sync-log-"+link.getMetadata().getName())
            .doOnNext(listedRssSyncLog::setLog);

        return Mono.when(logMono)
            .thenReturn(listedRssSyncLog);
    }

    Mono<ListOptions> buildListOptions(RssSyncLogQuery query) {
        return Mono.just(query.toListOptions());
    }

    public Mono<Void> publishRssFeedSyncEvent(String name) {
        CronFriendPost cron =  new CronFriendPost();
        CronFriendPost.CronSpec cronSpec = new CronFriendPost.CronSpec();
        cronSpec.setSuccessfulRetainLimit(5);
        cronSpec.setDisableSyncList(new ArrayList<>());
        cron.setSpec(cronSpec);
        return client.fetch(CronFriendPost.class, "cron-friends-default")
            .defaultIfEmpty(cron)
            .flatMap(cronFriendPost -> {
                var spec = cronFriendPost.getSpec();
                int successfulRetainLimit = spec.getSuccessfulRetainLimit();
                List<String> disableSyncList = spec.getDisableSyncList();
                int sum = successfulRetainLimit == 0 ? 5 : successfulRetainLimit;
                var listOptions = new ListOptions();
                FieldSelector fieldSelector = FieldSelector.of(isNull("metadata.deletionTimestamp"));
                if (!StringUtils.equals(name,"all")) {
                    fieldSelector =  fieldSelector.andQuery(equal("metadata.name",name));
                }
                listOptions.setFieldSelector(fieldSelector);
                Flux<Link> flux = Flux.range(1, Integer.MAX_VALUE)
                    .concatMap(page -> client.listBy(
                            Link.class,
                            listOptions,
                            PageRequestImpl.of(page, 50, Sort.by("metadata.creationTimestamp"))
                        )
                        .map(listResult -> listResult.get().collect(Collectors.toList()))
                    )
                    .takeWhile(list -> !list.isEmpty())
                    .flatMapIterable(list -> list);

                if (StringUtils.equals(name, "all")) {
                    flux = flux.delayElements(Duration.ofMillis(500));
                }
                return flux
                    .doOnNext(link -> eventPublisher.publishEvent(new RssFeedSyncEvent(this, link, sum, disableSyncList)))
                    .then();
            });
    }
}
