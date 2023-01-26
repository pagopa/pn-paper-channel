package it.pagopa.pn.paperchannel.middleware.msclient.common;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;

public abstract class BaseClient extends CommonBaseClient {


    protected BaseClient( ){
    }

//    protected WebClient initWebClient(WebClient.Builder builder){
//
//        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
//                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS)));
//
//        return super.enrichBuilder(builder)
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .build();
//    }
}
