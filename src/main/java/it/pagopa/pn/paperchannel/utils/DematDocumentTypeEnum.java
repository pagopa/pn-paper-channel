package it.pagopa.pn.paperchannel.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * <p>
 *     This enum gathers all demat document types in a type safe way providing some additional utilities:
 *     <ul>
 *         <li>Gather demat document types in a centralized enumeration</li>
 *         <li>Provide aliasing translation when need to refer to same or similar object from a business point of view</li>
 *         <li>Implement utility methods to retrieve correct value from raw string with fallbacks to avoid throwing exceptions</li>
 *     </ul>
 *
 *     Document type string values come from {@link Const} class to provide better code maintainability
 * </p>
 */
@Getter
@Slf4j
public enum DematDocumentTypeEnum {
    DEMAT_AR(Const.DEMAT_AR, Const.DEMAT_AR),
    DEMAT_ARCAD(Const.DEMAT_ARCAD, Const.DEMAT_ARCAD),
    DEMAT_CAD(Const.DEMAT_CAD, Const.DEMAT_ARCAD),
    DEMAT_23L(Const.DEMAT_23L, Const.DEMAT_23L),
    DEMAT_PLICO(Const.DEMAT_PLICO, Const.DEMAT_PLICO),
    DEMAT_INDAGINE(Const.DEMAT_INDAGINE, Const.DEMAT_INDAGINE);

    private final String documentType;
    private final String alias;

    private static final String DEMAT_PREFIX = "DEMAT_";

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
        DematDocumentTypeEnum dematDocumentTypeEnum = null;

        try {
            // use StringUtils#upperCase to avoid null pointer exceptions
            dematDocumentTypeEnum = DematDocumentTypeEnum.valueOf(DEMAT_PREFIX + StringUtils.upperCase(documentType));
        } catch (IllegalArgumentException e) {
            log.warn("Cannot find DematDocumentTypeEnum with name {}", documentType);
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
