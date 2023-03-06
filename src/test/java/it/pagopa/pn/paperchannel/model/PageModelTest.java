package it.pagopa.pn.paperchannel.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Slf4j
class PageModelTest {



    @Test
    void testPagination(){

        List<String> data = List.of("1", "1", "1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1");
        log.info("Size list : {}", data.size());

        Pageable pageable = PageRequest.of(0, 10);

        PageModel<String> pageString = PageModel.builder(data, pageable);

        Assertions.assertEquals(pageable.getPageSize(), pageString.getContent().size());
        Assertions.assertEquals(data.size(), pageString.getTotalElements());
        Assertions.assertEquals(3, pageString.getTotalPages());



    }

}
