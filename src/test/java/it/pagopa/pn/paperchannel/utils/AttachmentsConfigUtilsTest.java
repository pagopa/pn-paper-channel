package it.pagopa.pn.paperchannel.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentsConfigUtilsTest {

    @ParameterizedTest
    @CsvSource(value = {
            "safestorage://fileKey.pdf?docTag=DOCUMNET, fileKey.pdf", //WithSafeStoragePrefixAndDocTag
            "safestorage://fileKey.pdf, fileKey.pdf", //WithSafeStoragePrefix
            "safestorage://fileKey.pdf?docTag=DOCUMNET&another=value, fileKey.pdf", //WithSafeStoragePrefixAndMoreQueryParams
            "f24set://123456789/1?docTag=F_24&another=value, f24set://123456789/1", //WithF24prefix
            "fileKey.pdf, fileKey.pdf", //WithStringAlreadyCleaned
            "NULL, NULL", //WithStringAlreadyCleaned
    }, nullValues = {"NULL"})
    void cleanFileKeyTest(String fileKey, String expected) {
        var result = AttachmentsConfigUtils.cleanFileKey(fileKey);
        assertThat(result).isEqualTo(expected);
    }


    @ParameterizedTest
    @CsvSource(value = {
            "safestorage://fileKey.pdf?docTag=DOCUMNET, safestorage://fileKey.pdf", //WithSafeStoragePrefixAndDocTag
            "safestorage://fileKey.pdf, safestorage://fileKey.pdf", //WithSafeStoragePrefix
            "safestorage://fileKey.pdf?docTag=DOCUMNET&another=value, safestorage://fileKey.pdf", //WithSafeStoragePrefixAndMoreQueryParams
            "f24set://123456789/1?docTag=F_24&another=value, f24set://123456789/1", //WithF24prefix
            "fileKey.pdf, fileKey.pdf", //WithStringAlreadyCleaned
            "NULL, NULL", //WithStringAlreadyCleaned
    }, nullValues = {"NULL"})
    void cleanFileKeyNoSafestorageTest(String fileKey, String expected) {
        var result = AttachmentsConfigUtils.cleanFileKey(fileKey, false);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "safestorage://fileKey.pdf?docTag=DOCUMENT, DOCUMENT", //WithDocTag
            "safestorage://fileKey.pdf?docTag=DOCUMENT&another=value, DOCUMENT", //WithDocTagAndOtherParams
            "f24set://UPJX-QGLV-YTQJ-202401-P-1/0?cost=100&docTag=DOCUMNET, DOCUMNET", //WithDocTagInF24Url
            "safestorage://fileKey.pdf, NULL", //WithoutDocTag
            "fileKey.pdf, NULL", //WithStringAlreadyCleaned
            "safestorage://fileKey.pdf?another=value, NULL", //WithoutDocTagButWithAQueryParam
            "NULL, NULL" //with fileKey null
    }, nullValues = {"NULL"})
    void getDocTagFromFileKey(String fileKey, String expected) {
        var result = AttachmentsConfigUtils.getDocTagFromFileKey(fileKey);
        assertThat(result).isEqualTo(expected);
    }

}
