package it.pagopa.pn.paperchannel.middleware.msclient.common;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.api.DeduplicatesAddressServiceApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.api.PaperAddressesApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.api.PaperMessagesApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.api.PaperRequestMetadataPatchApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.api.AddressApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.NotificationReworkApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.PaperTrackerTrackingApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.api.CoveragePrivateApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.ApiClient;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage_reactive.api.FileUploadApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig extends CommonBaseClient {

    @Bean
    public FileDownloadApi getSafeStorageClient (PnPaperChannelConfig pnPaperChannelConfig){

        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientSafeStorageBasepath());

        return new FileDownloadApi(newApiClient);
    }

    @Bean
    public FileUploadApi fileUploadApi (PnPaperChannelConfig pnPaperChannelConfig){

        var newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage_reactive.ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientSafeStorageBasepath());

        return new FileUploadApi(newApiClient);
    }

    @Bean
    public DeduplicatesAddressServiceApi getAddressManagerClient (PnPaperChannelConfig pnPaperChannelConfig){

        it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientAddressManagerBasepath());


        return new DeduplicatesAddressServiceApi(newApiClient);

    }

    @Bean
    public AddressApi getAddressApi(PnPaperChannelConfig pnPaperChannelConfig){

        it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.ApiClient(super.initWebClient(it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientNationalRegistriesBasepath());
        return new AddressApi(newApiClient);

    }

    @Bean
    public PaperMessagesApi getExternalChannelAPI(PnPaperChannelConfig pnPaperChannelConfig){

        it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.ApiClient(super.initWebClient(it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientExternalChannelBasepath());
        return new PaperMessagesApi(newApiClient);

    }

    @Bean
    public PaperTrackerTrackingApi getPaperTrackerEventApi(PnPaperChannelConfig pnPaperChannelConfig){

        it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.ApiClient(super.initWebClient(it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientPaperTrackerBasepath());
        return new PaperTrackerTrackingApi(newApiClient);

    }

    @Bean
    public NotificationReworkApi getNotificationReworkApi(PnPaperChannelConfig pnPaperChannelConfig){
        it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.ApiClient(super.initWebClient(it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientPaperTrackerBasepath());
        return new NotificationReworkApi(newApiClient);

    }

    @Bean
    public PaperRequestMetadataPatchApi patchRequestMetadata(PnPaperChannelConfig pnPaperChannelConfig){
        it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.ApiClient(super.initWebClient(it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientExternalChannelBasepath());
        return new PaperRequestMetadataPatchApi(newApiClient);
    }

    @Bean
    public CoveragePrivateApi getCoveragePrivateApi(PnPaperChannelConfig pnPaperChannelConfig){

        it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.ApiClient(super.initWebClient(it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientRaddAltBasepath());
        return new CoveragePrivateApi(newApiClient);
    }

    @Bean
    public PaperAddressesApi getPaperAddressesApi(PnPaperChannelConfig pnPaperChannelConfig){
        it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.ApiClient newApiClient = new it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.ApiClient(super.initWebClient(it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientDataVaultBasepath());
        return new PaperAddressesApi(newApiClient);
    }
}
