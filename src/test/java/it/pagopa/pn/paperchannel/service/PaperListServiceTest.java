package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.CapDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class PaperListServiceTest extends BaseTest {

    @MockBean
    private CapDAO capDAO;
    @Autowired
    private PaperListService paperListService;


    @Test
    void testGetAllCap(){
        List<PnCap> capResponseDto= new ArrayList<>();
        PnCap cap = new PnCap();
        cap.setCap("21034");
        cap.setCity("Milano");
        cap.setAuthor(Const.PN_PAPER_CHANNEL);

        capResponseDto.add(cap);

        Mockito.when(capDAO.getAllCap(Mockito.any())).thenReturn(Mono.just(capResponseDto));

        CapResponseDto response = this.paperListService.getAllCap("").block();
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertEquals(1, response.getContent().size());

    }

}
