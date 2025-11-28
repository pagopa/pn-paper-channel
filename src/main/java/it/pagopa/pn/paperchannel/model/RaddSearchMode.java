package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.SearchModeDto;

public enum RaddSearchMode {
    OLD(null),
    LIGHT(SearchModeDto.LIGHT),
    COMPLETE(SearchModeDto.COMPLETE);

    private final SearchModeDto clientSearchMode;

    RaddSearchMode(SearchModeDto clientSearchMode) {
        this.clientSearchMode = clientSearchMode;
    }

    public SearchModeDto toClientSearchMode() {
        if(this.clientSearchMode == null) {
            throw new IllegalStateException(
                    "RaddSearchMode." + this.name() + " non è compatibile con il client HTTP"
            );
        }

        return this.clientSearchMode;
    }
}
