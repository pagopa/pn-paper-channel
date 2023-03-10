package it.pagopa.pn.paperchannel.service;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.*;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PaperListServiceTest  extends BaseTest {

    @Mock
    private CapDAO capDAO;

    @Mock
    private PaperListService paperListService;

    //@Test
    void test(){
        List<PnCap> capResponseDto= new ArrayList<>();
        Mockito.when(capDAO.getAllCap(Mockito.any())).thenReturn(Mono.just(capResponseDto));
        assertNotNull(paperListService.getAllCap("abc" ));
    }

}
