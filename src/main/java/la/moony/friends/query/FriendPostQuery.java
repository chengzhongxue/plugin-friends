package la.moony.friends.query;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import la.moony.friends.extension.FriendPost;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.server.ServerRequest;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springframework.data.domain.Sort.Order.desc;
import static run.halo.app.extension.index.query.Queries.*;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;

public class FriendPostQuery extends SortableRequest {

    public FriendPostQuery(ServerRequest request) {
        super(request.exchange());
    }
    @Nullable
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Nullable
    public String getLinkName() {
        return queryParams.getFirst("linkName");
    }


    public ListOptions toListOptions() {
        var builder = ListOptions.builder(super.toListOptions());

        Optional.ofNullable(getKeyword())
            .filter(StringUtils::isNotBlank)
            .ifPresent(keyword -> builder.andQuery(or(
                contains("spec.author", keyword),
                contains("spec.authorUrl", keyword),
                contains("spec.title", keyword)
            )));

        Optional.ofNullable(getLinkName())
            .filter(StringUtils::isNotBlank)
            .ifPresent(linkName -> builder.andQuery(equal("spec.linkName", linkName)));

        return builder.build();
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
        var sort = SortResolver.defaultInstance.resolve(exchange).and(defaultSort());
        return PageRequestImpl.of(getPage(), getSize(), sort);
    }

    public static Sort defaultSort() {
        return Sort.by(desc("spec.pubDate"));
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(sortParameter())
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("linkName")
                .description("CronFriendPost filtered by linkName.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("keyword")
                .description("CronFriendPost filtered by keyword.")
                .implementation(String.class)
                .required(false));
    }
}