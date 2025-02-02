package it.pagopa.pn.paperchannel.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Utility {
    private static final Pattern PATTERN_PREFIX_CLIENT_ID = Pattern.compile("^\\d{3}\\.");
    public static final String POSTMAN_REQUEST_PREFIX = "PST_ADDRESS_";
    public static final String NATIONAL_REGISTRIES_REQUEST_PREFIX = "NRG_ADDRESS_";

    private static final String COUNTRY_IT = "it";
    private static final String COUNTRY_ITALIA = "italia";
    private static final String COUNTRY_ITALY = "italy";

    private Utility() {
        throw new IllegalCallerException();
    }


    public static Mono<String> getFromContext(String key, String defaultValue){
        return Mono.deferContextual(ctx -> {
            String value = ctx.getOrDefault(key, defaultValue);
            if (value == null) return Mono.empty();
            return Mono.just(value);
        });
    }

    public static String getRequestIdWithParams(String requestId, String attempt, String clientId){
        String finalRequestId = requestId.concat(Const.RETRY).concat(attempt);
        if (StringUtils.isNotBlank(clientId))
            finalRequestId = clientId.concat(".").concat(finalRequestId);
        return finalRequestId;
    }

    public static String getRequestIdWithoutPrefixClientId(String requestId){
        Matcher matcher = PATTERN_PREFIX_CLIENT_ID.matcher(requestId);
        if (matcher.find()) {
            return requestId.substring(matcher.end());
        }
        return requestId;
    }

    public static String getClientIdFromRequestId(String requestId){
        Matcher matcher = PATTERN_PREFIX_CLIENT_ID.matcher(requestId);
        if (matcher.find()) return matcher.group(0).substring(0, matcher.group(0).length()-1);
        return null;
    }

    public static Integer toCentsFormat(BigDecimal value) {
        value = value.multiply(BigDecimal.valueOf(100));
        value = value.setScale(0, RoundingMode.HALF_UP);
        return value.intValue();
    }

    public static BigDecimal toBigDecimal(String value) throws ParseException {
        DecimalFormat fr = new DecimalFormat("#######.##");
        fr.setRoundingMode(RoundingMode.HALF_UP);
        fr.setParseBigDecimal(true);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        fr.setDecimalFormatSymbols(symbols);

        return (BigDecimal) fr.parse(value);
    }

    public static String convertToHash(String string) {
        if(string==null){
            string = "";
        }
        string = string.toLowerCase().replaceAll("\\s", "");
        return DigestUtils.sha256Hex(string);
    }

    public static <T> String objectToJson (T data){
       try{
           ObjectMapper objectMapper = new ObjectMapper()
                   .registerModule(new Jdk8Module())
                   .registerModule(new JavaTimeModule());
           objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
           return objectMapper.writeValueAsString(data);
       }
       catch (JsonProcessingException ex){
           log.warn("Error with mapping : {}", ex.getMessage(), ex);
           return null;
       }
    }

    public static <T> T jsonToObject(ObjectMapper objectMapper, String json, Class<T> tClass){
        try {

            return objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            log.error("Error with mapping : {}", e.getMessage(), e);
            return null;
        }
    }

    public static boolean isValidFromRegex(String value, String regex){
        boolean check = false;
        if (!StringUtils.isEmpty(value)) {
            value = value.replace(".", "");
            value = value.replace(",", "");
            value = value.replace("'", "");
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(value);
            check = (m.find() && m.group().equals(value));
        }
        return check;
    }

    public static boolean isValidCapFromRegex(String value, String regex){
        return isValidFromRegex(value, regex);
    }

    public static List<String> isValidCap(String capList) {
        String regex = Const.capRegex;
        List<String> capsFinded = new ArrayList<>();
        String[] cap = capList.split(",");
        if (cap != null && cap.length > 0) {
            for (String item : cap) {
                item = item.trim();
                if (StringUtils.isNotEmpty(item) && item.contains(".")) item = item.substring(0, item.indexOf("."));
                if (item.contains("-")) {
                    String[] range = item.trim().split("-");
                    if (!isValidCapFromRegex(range[0],regex) || !isValidCapFromRegex(range[1],regex)) {
                        log.info("Il cap non è conforme agli standard previsti.");
                        return null;
                    }
                    int low = Integer.parseInt(range[0]);
                    int high = Integer.parseInt(range[1]);
                    if (low < high) {
                        log.info("Trovato cap in questo range " + item);
                        for (int i = low; i <= high; i++){
                            String capFormatted = addZero(i);
                            capsFinded.add(capFormatted);
                        }
                    } else {
                        log.info("Intervallo errato.");
                        return null;
                    }
                } else {
                    if (!isValidCapFromRegex(item, regex)) return null;
                    capsFinded.add(item);
                }
            }
            Set<String> findedEquals = new HashSet<>(capsFinded);
            if (findedEquals.size() < capsFinded.size()) {
                log.info("Trovato cap duplicato.");
                return null;
            }
            return capsFinded;

        }

        return (isValidCapFromRegex(capList, regex)) ? List.of(capList.trim()) : null ;
    }

    public static String addZero (int i){
        return String.format("%05d", i);
    }

    public static Map<String, Boolean> requiredCostFSU(){
        Map<String, Boolean> map = new HashMap<>();

        map.put("99999-AR", false);
        map.put("99999-890", false);
        map.put("99999-RS", false);

        return map;
    }

    public static boolean isCallCenterEvoluto(String requestId) {
        return requestId.startsWith(Const.PREFIX_REQUEST_ID_SERVICE_DESK);
    }

    public static boolean isNotCallCenterEvoluto(String requestId) {
        return !isCallCenterEvoluto(requestId);
    }

    public static String buildNationalRegistriesCorrelationId(@NotNull String requestId) {
        return NATIONAL_REGISTRIES_REQUEST_PREFIX + requestId;
    }

    public static String buildPostmanAddressCorrelationId(@NotNull String requestId) {
        return POSTMAN_REQUEST_PREFIX + requestId;
    }

    public static void logAuditSuccessLogic(String message, PnDeliveryRequest deliveryRequest, PnLogAudit pnLogAudit){
        pnLogAudit.addsSuccessResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }

    public static void logAuditBeforeLogic(String message, PnDeliveryRequest deliveryRequest, PnLogAudit pnLogAudit){
        pnLogAudit.addsBeforeResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }

    public static void resolveAuditLogFromResponse(PnDeliveryRequest entity, String error, PnLogAudit pnLogAudit, String serviceName, String correlationId) {
        if (StringUtils.isEmpty(error)) {
            pnLogAudit.addsSuccessResolveService(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, correlationId = %s Response OK from %s service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            correlationId,
                            serviceName));
        } else {
            pnLogAudit.addsFailResolveService(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, correlationId = %s Response KO from %s service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            correlationId,
                            serviceName));
        }
    }

    public static boolean isNational(String country) {
       return StringUtils.isBlank(country) ||
                StringUtils.equalsIgnoreCase(country, COUNTRY_IT) ||
                StringUtils.equalsIgnoreCase(country, COUNTRY_ITALIA) ||
                StringUtils.equalsIgnoreCase(country, COUNTRY_ITALY);
    }

    /**
     * Retrieves a geokey based on the address provided.
     * <p>
     * If the address is located in the national territory, CAP is returned.
     * Otherwise, the country code of the address is returned.
     * </p>
     *
     * @param address The address object from which to extract the geographic key.
     * @return The postal code if the address is national, or the country code otherwise.
     */
    public static String getGeokey(Address address) {
        return isNational(address.getCountry()) ? address.getCap() : address.getCountry();
    }

    /**
     * Retrieves the request ID for the first attempt of a delivery request.
     * <p>
     * If the delivery request object has the {@code relatedRequestId} field populated,
     * it indicates that this is a second attempt. In this case, the value of
     * {@code relatedRequestId} contains the request ID of the first attempt.
     * </p>
     * <p>
     * If the {@code relatedRequestId} field is not populated, it indicates that
     * this is the first attempt, and the method returns the {@code requestId}
     * of the delivery request object.
     * </p>
     *
     * @param pnDeliveryRequest the delivery request object containing request information
     * @return the {@code relatedRequestId} if populated, indicating a second attempt;
     *         otherwise, the {@code requestId} of the first attempt
     */
    public static String getRequestIdFirstAttempt(@NotNull PnDeliveryRequest pnDeliveryRequest) {
        var relatedRequestId = pnDeliveryRequest.getRelatedRequestId();
        return StringUtils.isNotBlank(relatedRequestId) ? relatedRequestId : pnDeliveryRequest.getRequestId();
    }

}
