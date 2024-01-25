package it.pagopa.pn.paperchannel.utils;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@Getter
public enum DematDocumentTypeEnum {
    DEMAT_AR("AR", Set.of("AR")),
    DEMAT_ARCAD("ARCAD", Set.of("ARCAD", "CAD")),
    DEMAT_23L("23L", Set.of("23L"));

    private final String alias;
    private final Set<String> documentTypes;

    DematDocumentTypeEnum(String alias, Set<String> documentTypes) {
        this.alias = alias;
        this.documentTypes = documentTypes;
    }

    /**
     * <p> This method retrieve an optional {@link DematDocumentTypeEnum} from a document type. </p>
     *
     * @param documentType  document type that must be translated
     * @return              optional {@link DematDocumentTypeEnum}
     * */
    public static Optional<DematDocumentTypeEnum> fromDocumentType(String documentType) {
        return Arrays.stream(DematDocumentTypeEnum.values())
                .filter(dematDocumentTypeEnum -> dematDocumentTypeEnum.getDocumentTypes().contains(documentType))
                .findFirst();
    }

    /**
     * <p>
     *     This method retrieve an alias from document type using {@link DematDocumentTypeEnum}.
     *     In this way it is possible to gather and retrieve common document type names from different sources.
     * </p>
     * <p>
     *    When {@link DematDocumentTypeEnum} is not found, the same document type is returned.
     *    <i>Example</i>: in certain cases ARCAD and CAD must be interchangeable
     * </p>
     *
     * @param documentType  document type that must be translated
     * @return              the alias or same parameter document type if not found
     * */
    public static String getAliasFromDocumentType(String documentType) {
        return fromDocumentType(documentType)
                .map(DematDocumentTypeEnum::getAlias)
                .orElse(documentType);
    }
}
