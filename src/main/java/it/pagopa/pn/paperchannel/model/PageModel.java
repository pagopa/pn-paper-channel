package it.pagopa.pn.paperchannel.model;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


public class PageModel<T> extends PageImpl<T> {

    public static <A> PageModel<A> builder(List<A> content, Pageable pageable){
        int first = (int) Math.min(pageable.getOffset(), content.size());
        int last = Math.min(first + pageable.getPageSize(), content.size());
        return new PageModel<>(content.subList(first, last), pageable);
    }

    private PageModel(List<T> content, Pageable pageable) {
        super(content, pageable, content.size());
    }

    public <A> List<A> mapTo(Function<T, A> converter){
        return this.getContent().stream().map(converter).collect(Collectors.toList());
    }
}
