package it.pagopa.pn.paperchannel.middleware.queue.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Data
@Builder
public class OcrInputPayload {
    public static final String VERSION_1_0_0 = "1.0.0";

    @Builder.Default
    private String version = VERSION_1_0_0;
    private CommandType commandType;
    private String commandId;
    private DataDto data;

    @Data
    @Builder
    public static class DataDto {
        private ProductType productType;
        private DocumentType documentType;
        private UnifiedDeliveryDriver unifiedDeliveryDriver;
        private DetailsDto details;

        @Data
        @Builder
        public static class DetailsDto {

            private String deliveryDetailCode;
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            private Instant notificationDate;
            private String registeredLetterCode;
            private DeliveryFailureCause deliveryFailureCause;
            private String attachment;

            public enum DeliveryFailureCause {
                M01, M02, M03, M04,
                M05, M06, M07, M08, M09
            }
        }

        public enum ProductType { AR }
        public enum DocumentType {
            AR,
            Plico
        }
        public enum UnifiedDeliveryDriver {
            Fulmine,
            Poste,
            Sailpost,
            PostAndService
        }
    }

    public enum CommandType {
        postal;
    }
}