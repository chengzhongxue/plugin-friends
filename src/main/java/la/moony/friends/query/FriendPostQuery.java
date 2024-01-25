package la.moony.friends.query;

import io.swagger.v3.oas.annotations.media.Schema;
import la.moony.friends.extension.FriendPost;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.router.IListRequest;
import org.springframework.lang.Nullable;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import static java.util.Comparator.comparing;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToPredicate;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import run.halo.app.extension.Extension;
import java.util.Comparator;

public class FriendPostQuery extends IListRequest.QueryListRequest {
        private final ServerWebExchange exchange;

        public FriendPostQuery(ServerWebExchange exchange) {
            super(exchange.getRequest().getQueryParams());
            this.exchange = exchange;
        }

        @Schema(description = "Keyword to search friendPost under the friendPost")
        public String getKeyword() {
            return queryParams.getFirst("keyword");
        }

        @Nullable
        @Schema()
        public String getAuthor() {
            return StringUtils.defaultIfBlank(queryParams.getFirst("author"), null);
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


        public Predicate<FriendPost> toPredicate() {
            Predicate<FriendPost> predicate = friendPost -> {return true;};
            Predicate<FriendPost> keywordPredicate = friend -> {
                var keyword = getKeyword();
                if (StringUtils.isBlank(keyword)) {
                    return true;
                }
                String keywordToSearch = keyword.trim().toLowerCase();
                return StringUtils.containsAnyIgnoreCase(friend.getSpec().getAuthor(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getTitle(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getDescription(),
                    keywordToSearch)
                    || StringUtils.containsAnyIgnoreCase(friend.getSpec().getLink(), keywordToSearch);
            };

            String author = getAuthor();
            if (author != null){
                predicate = predicate.and(friendPost -> author.equals(friendPost.getSpec().getAuthor()));
            }

            Predicate<Extension> labelAndFieldSelectorToPredicate =
                labelAndFieldSelectorToPredicate(getLabelSelector(), getFieldSelector());
            return predicate.and(keywordPredicate).and(labelAndFieldSelectorToPredicate);
        }

        public Comparator<FriendPost> toComparator() {
            List<Comparator<FriendPost>> comparators = new ArrayList<>();
            var sort = getSort();
            var ctOrder = sort.getOrderFor("pubDate");
            if (ctOrder != null) {
                Comparator<FriendPost> comparator =
                    comparing(friendPost -> friendPost.getSpec().getPubDate());
                if (ctOrder.isDescending()) {
                    comparator = comparator.reversed();
                }
                comparators.add(comparator);
            }
            Comparator<FriendPost> comparator =
                comparing(friendPost -> friendPost.getSpec().getPubDate());
            comparators.add(comparator.reversed());
            return comparators.stream()
                .reduce(Comparator::thenComparing)
                .orElse(null);
        }


        public static <E extends Extension> Comparator<E> compareName(boolean asc) {
            var comparator = Comparator.<E, String>comparing(e -> e.getMetadata().getName());
            return asc ? comparator : comparator.reversed();
        }
    }