package it.pagopa.pn.paperchannel.model;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.function.Function;


public class PageModel<T> extends PageImpl<T> {

    public static <A> PageModel<A> builder(List<A> content, Pageable pageable){
        int first = (int) Math.min(pageable.getOffset(), content.size());
        int last = Math.min(first + pageable.getPageSize(), content.size());
        int size = content.size();
        return new PageModel<>(content.subList(first, last), pageable, size);
    }

    private PageModel(List<T> content, Pageable pageable, long size) {
        super(content, pageable, size);
    }

    public <A> List<A> mapTo(Function<T, A> converter){
        return this.getContent().stream().map(converter).toList();
    }
}
