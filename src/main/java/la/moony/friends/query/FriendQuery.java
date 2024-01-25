package la.moony.friends.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import la.moony.friends.extension.Friend;
import org.springframework.data.domain.Sort;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.Extension;
import java.util.function.Predicate;
import static java.util.Comparator.comparing;
import org.springframework.lang.Nullable;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToPredicate;

public class FriendQuery extends IListRequest.QueryListRequest {
        private final ServerWebExchange exchange;

        public FriendQuery(ServerWebExchange exchange) {
            super(exchange.getRequest().getQueryParams());
            this.exchange = exchange;
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

        @Schema()
        public String getKeyword() {
            return queryParams.getFirst("keyword");
        }

        @ArraySchema(uniqueItems = true,
            arraySchema = @Schema(name = "sort",
                description = "Sort property and direction of the list result. Supported fields: "
                    + "creationTimestamp, priority"),
            schema = @Schema(description = "friend field,asc or field,desc",
                implementation = String.class,
                example = "creationTimestamp,desc"))
        public Sort getSort() {
            return SortResolver.defaultInstance.resolve(exchange);
        }


        public Predicate<Friend> toPredicate() {
            Predicate<Friend> predicate = friend -> {return true;};
            Predicate<Friend> keywordPredicate = friend -> {
                var keyword = getKeyword();
                if (StringUtils.isBlank(keyword)) {
                    return true;
                }
                String keywordToSearch = keyword.trim().toLowerCase();
                return StringUtils.containsAnyIgnoreCase(friend.getSpec().getDisplayName(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getDescription(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getRssUrl(), keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getLink(), keywordToSearch);
            };

            if (StringUtils.isNotEmpty(queryParams.getFirst("selfSubmitted"))){
                Boolean selfSubmitted = getSelfSubmitted();
                predicate = predicate.and(friend -> friend.getSpec().getSelfSubmitted().equals(selfSubmitted));
            }

            Friend.Status.StatusType statusType = getStatusType();
            if (statusType != null){
                predicate = predicate.and(friend -> {
                       if (friend.getStatus()!=null){
                           if (friend.getStatus().getStatusType()!=null){
                               return friend.getStatus().getStatusType().equals(statusType);
                           }
                       }
                       return false;
                    }
                );
            }

            Friend.Spec.SubmittedType submittedType = getSubmittedType();
            if (submittedType != null){
                predicate = predicate.and(friend -> {
                       if (friend.getSpec().getSubmittedType()!=null){
                           return friend.getSpec().getSubmittedType().equals(submittedType);
                       }
                       return false;
                    });
            }

            Predicate<Extension> labelAndFieldSelectorToPredicate =
                labelAndFieldSelectorToPredicate(getLabelSelector(), getFieldSelector());
            return predicate.and(keywordPredicate).and(labelAndFieldSelectorToPredicate);
        }

        public Comparator<Friend> toComparator() {
            List<Comparator<Friend>> comparators = new ArrayList<>();
            var sort = getSort();
            var ctOrder = sort.getOrderFor("creationTimestamp");
            if (ctOrder != null) {
                Comparator<Friend> comparator =
                    comparing(link -> link.getMetadata().getCreationTimestamp());
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
            comparators.add(compareCreationTimestamp(false));
            comparators.add(compareName(true));
            return comparators.stream()
                .reduce(Comparator::thenComparing)
                .orElse(null);
        }

        public static <E extends Extension> Comparator<E> compareCreationTimestamp(boolean asc) {
            var comparator =
                Comparator.<E, Instant>comparing(e -> e.getMetadata().getCreationTimestamp());
            return asc ? comparator : comparator.reversed();
        }

        public static <E extends Extension> Comparator<E> compareName(boolean asc) {
            var comparator = Comparator.<E, String>comparing(e -> e.getMetadata().getName());
            return asc ? comparator : comparator.reversed();
        }
    }