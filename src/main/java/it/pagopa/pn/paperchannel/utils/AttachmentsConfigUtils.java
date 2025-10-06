package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.api.dto.events.ConfigTypeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.service.SafeStorageService.SAFESTORAGE_PREFIX;

public class AttachmentsConfigUtils {

    public static final String DELIMITER_PK = "##";

    public static final String ZIPCODE_PK_PREFIX = "ZIP";

    public static final String DOCTAG_QUERY_PARAM = "docTag";

    private AttachmentsConfigUtils() {}


    public static String buildPartitionKey(String configKey, String configType) {
        if(ConfigTypeEnum.ZIPCODE.name().equals(configType)) {
            return ZIPCODE_PK_PREFIX + DELIMITER_PK + configKey;
        }
        else if(configType != null) {
            return configType + DELIMITER_PK + configKey;
        }
        else {
            return configKey;
        }
    }

    public static String getDocTagFromFileKey(String fileKey) {
        if (fileKey == null) {
            return null;
        }
        var uri = URI.create(fileKey);
        var uriComponents = UriComponentsBuilder.fromUri(uri).build();
        var docTags = uriComponents.getQueryParams().get(DOCTAG_QUERY_PARAM);
        if(CollectionUtils.isEmpty(docTags)) {
            return null;
        }
        return docTags.get(0);
    }

    public static String cleanFileKey(String fileKey) {
        return cleanFileKey(fileKey, true);
    }

    public static String cleanFileKey(String fileKey, boolean cleanSafestoragePrefix) {
        if (fileKey == null) {
            return null;
        }

        StringBuilder fileKeyNew = new StringBuilder();

        if (cleanSafestoragePrefix && fileKey.contains(SAFESTORAGE_PREFIX)){
            //clean safestorage://
            fileKeyNew.append(fileKey.replace(SAFESTORAGE_PREFIX, ""));
        }
        else {
            fileKeyNew.append(fileKey);
        }

        var queryParamStartIndex = fileKeyNew.indexOf("?");
        if(queryParamStartIndex != -1) {
            //clean all query params
            fileKeyNew.delete(queryParamStartIndex, fileKeyNew.length());
        }

        return fileKeyNew.toString();
    }

    /**
     * Restituisce una lista contenente tutti gli allegati di una richiesta di consegna,
     * includendo sia quelli dell'array attachment che quelli dell'array removedAttachments.
     * <p>
     * La lista risultante è una nuova istanza di {@link ArrayList}, inizializzata con
     * gli allegati ottenuti tramite {@link PnDeliveryRequest#getAttachments()} e,
     * se presenti, arricchita con gli allegati rimossi tramite
     * {@link PnDeliveryRequest#getRemovedAttachments()}.
     * </p>
     *
     * @param pnDeliveryRequest la richiesta di consegna da cui recuperare gli allegati;
     *                          non deve essere {@code null}.
     * @return una nuova lista contenente sia gli allegati dell'array attachment sia quelli dell'array removedAttachments.
     *         Se {@code getRemovedAttachments()} restituisce {@code null} o una lista vuota,
     *         il risultato conterrà solo gli allegati dell'array attachments.
     * @throws NullPointerException se {@code getAttachments()} restituisce {@code null}.
     */

    public static List<PnAttachmentInfo> getAllAttachments(PnDeliveryRequest pnDeliveryRequest) {
        List<PnAttachmentInfo> attachments = pnDeliveryRequest.getAttachments();
        List<PnAttachmentInfo> removedAttachments = pnDeliveryRequest.getRemovedAttachments();

        List<PnAttachmentInfo> merged = new ArrayList<>(attachments);

        if (! CollectionUtils.isEmpty(removedAttachments)) {
            merged.addAll(removedAttachments);
        }

        return merged;
    }

}
