package it.pagopa.pn.paperchannel.service;

public interface NationalRegistryService {

    void finderAddressFromNationalRegistries(String requestId, String relatedRequestId, String fiscalCode,
                                             String personType, String iun, Integer attempt);
}
