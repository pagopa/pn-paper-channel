package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.exception.PnZipException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    private ZipUtils() {}

    public static byte[] extractPdfFromZip(byte[] zipData) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipData);
             ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream)) {

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith(".pdf")) {
                    return readZipEntry(zipInputStream);
                }
            }
        } catch (IOException e) {
            throw new PnZipException("Failed to extract PDF from ZIP", e);
        }

        throw new PnZipException("PDF not found in the ZIP file");
    }

    private static byte[] readZipEntry(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
