package it.pagopa.pn.paperchannel.utils;

import lombok.Getter;

import java.util.*;

@Getter
public enum DematDocumentTypeEnum {
    DEMAT_AR("AR", "AR"),
    DEMAT_ARCAD("ARCAD", "ARCAD"),
    DEMAT_CAD("CAD", "ARCAD"),
    DEMAT_23L("23L", "23L");

    private final String documentType;
    private final String alias;

    private final static String DEMAT_PREFIX = "DEMAT_";

    DematDocumentTypeEnum(String documentType, String alias) {
        this.documentType = documentType;
        this.alias = alias;
    }

    /**
     * <p> This method retrieve an optional {@link DematDocumentTypeEnum} from a document type. </p>
     *
     * @param documentType  document type that must be translated
     * @return              optional {@link DematDocumentTypeEnum}
     * */
    public static Optional<DematDocumentTypeEnum> fromDocumentType(String documentType) {
        DematDocumentTypeEnum dematDocumentTypeEnum;

        try {
            dematDocumentTypeEnum = DematDocumentTypeEnum.valueOf(DEMAT_PREFIX + documentType);
        } catch (IllegalArgumentException e) {
            dematDocumentTypeEnum = null;
        }

        return Optional.ofNullable(dematDocumentTypeEnum);
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
