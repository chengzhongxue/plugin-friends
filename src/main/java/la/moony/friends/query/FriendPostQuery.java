package la.moony.friends.query;

import io.swagger.v3.oas.annotations.media.Schema;
import la.moony.friends.extension.FriendPost;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.QueryFactory;
import org.springframework.lang.Nullable;

import static java.util.Comparator.comparing;
import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.List;
import run.halo.app.extension.router.SortableRequest;
import run.halo.app.extension.router.selector.FieldSelector;
import java.util.Comparator;

public class FriendPostQuery extends SortableRequest {

    private final MultiValueMap<String, String> queryParams;

    public FriendPostQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
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


    public ListOptions toListOptions() {
        var listOptions =
            labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector());
        var query = all();
        String author = getAuthor();
        if (author != null){
            query = and(query, equal("spec.author", author));
        }

        String keyword = getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            query = and(query, QueryFactory.or(
                QueryFactory.contains("spec.author", keyword),
                QueryFactory.contains("spec.title", keyword),
                QueryFactory.contains("spec.description", keyword),
                QueryFactory.contains("spec.link", keyword)
            ));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return listOptions;
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

    public PageRequest toPageRequest() {
        var sort = getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("spec.pubDate").descending();
        }
        return PageRequestImpl.of(getPage(), getSize(), sort);
    }
}