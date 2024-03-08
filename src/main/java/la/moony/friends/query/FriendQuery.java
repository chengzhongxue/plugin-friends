package la.moony.friends.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import la.moony.friends.extension.Friend;
import org.springframework.data.domain.Sort;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.QueryFactory;
import static java.util.Comparator.comparing;
import org.springframework.lang.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import run.halo.app.extension.router.SortableRequest;
import run.halo.app.extension.router.selector.FieldSelector;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

public class FriendQuery extends SortableRequest {
    private final MultiValueMap<String, String> queryParams;

    public FriendQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
    }

    @Schema()
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Schema()
    public Boolean getSelfSubmitted() {
        String selfSubmitted = queryParams.getFirst("selfSubmitted");
        if (selfSubmitted.equals("true")){
            return true;
        }
        return false;
    }

    @Nullable
    public Friend.Spec.SubmittedType getSubmittedType() {
        String submittedType = queryParams.getFirst("submittedType");
        return Friend.Spec.SubmittedType.from(submittedType);
    }

    @Nullable
    public Friend.Status.StatusType getStatusType() {
        String statusType = queryParams.getFirst("statusType");
        return Friend.Status.StatusType.from(statusType);
    }


    public ListOptions toListOptions() {
        var listOptions =
            labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector());
        var query = all();
        if (StringUtils.isNotBlank(queryParams.getFirst("selfSubmitted"))) {
            Boolean selfSubmitted = getSelfSubmitted();
            query = and(query, equal("spec.selfSubmitted", getSelfSubmitted().toString()));
        }

        Friend.Status.StatusType statusType = getStatusType();
        if (statusType != null){
            query = and(query, equal("status.statusType", statusType.name()));
        }

        Friend.Spec.SubmittedType submittedType = getSubmittedType();
        if (submittedType != null){
            query = and(query, equal("spec.submittedType", submittedType.name()));
        }

        String keyword = getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            query = and(query, QueryFactory.or(
                QueryFactory.contains("spec.displayName", keyword),
                QueryFactory.contains("spec.description", keyword),
                QueryFactory.contains("spec.rssUrl", keyword),
                QueryFactory.contains("spec.link", keyword)
            ));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return listOptions;
    }

    public Comparator<Friend> toComparator() {
        List<Comparator<Friend>> comparators = new ArrayList<>();
        var sort = getSort();
        var ctOrder = sort.getOrderFor("creationTimestamp");
        if (ctOrder != null) {
            Comparator<Friend> comparator =
                comparing(friend -> friend.getMetadata().getCreationTimestamp());
            if (ctOrder.isDescending()) {
                comparator = comparator.reversed();
            }
            comparators.add(comparator);
        }
        var utOrder = sort.getOrderFor("updateTime");
        if (utOrder != null){
            Comparator<Friend> comparator =
                comparing(link -> {
                        if (link.getSpec().getUpdateTime()!=null){
                            return link.getSpec().getUpdateTime();
                        }
                        return link.getMetadata().getCreationTimestamp();
                    }
                );
            if (utOrder.isDescending()) {
                comparator = comparator.reversed();
            }
            comparators.add(comparator);
        }
        Comparator<Friend> comparator =
            comparing(friend -> friend.getMetadata().getCreationTimestamp());
        comparators.add(comparator.reversed());
        return comparators.stream()
            .reduce(Comparator::thenComparing)
            .orElse(null);
    }

    public PageRequest toPageRequest() {
        var sort = getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("metadata.creationTimestamp").descending();
        }
        return PageRequestImpl.of(getPage(), getSize(), sort);
    }
}