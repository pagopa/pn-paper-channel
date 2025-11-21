package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.SearchModeDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;


class RaddSearchModeTest {
    @Test
    void toClientSearchModeReturnsLightForLightEnum() {
        Assertions.assertEquals(SearchModeDto.LIGHT, RaddSearchMode.LIGHT.toClientSearchMode());
    }

    @Test
    void toClientSearchModeReturnsCompleteForCompleteEnum() {
        Assertions.assertEquals(
                it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.SearchModeDto.COMPLETE,
                RaddSearchMode.COMPLETE.toClientSearchMode()
        );
    }

    @Test
    void toClientSearchModeThrowsExceptionForOldEnum() {
        assertThrows(
                IllegalStateException.class,
                RaddSearchMode.OLD::toClientSearchMode
        );
    }
}