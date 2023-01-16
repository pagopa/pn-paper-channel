package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.model.DeliveryDriverFilter;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping(value = "/index")
public class IndexController {

    @Autowired
    private KmsEncryption kmsEncryption;

    @Autowired
    private PrepareAsyncServiceImpl prepareAsyncServiceImpl;

    @Autowired
    private AddressDAO addressDAO;

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @Autowired
    private PaperChannelService paperChannelService;


    @GetMapping(value = "/crypt")
//    public Mono<ResponseEntity<PnAddress>> encryption(){
    public Mono<ResponseEntity<String>> encryption(){
        //For testing encryption/decryption string value
        String value = "This is an encryption/decryption test!";
        String decrypted = kmsEncryption.decode(kmsEncryption.encode(value));
        return Mono.just("")
                .map(item -> ResponseEntity.ok().body(decrypted));

        //For testing encryption/decryption Entity
//        PnAddress pnAddress = new PnAddress();
//        pnAddress.setRequestId("12345");
//        pnAddress.setAddress("Road Acn");
//        pnAddress.setAddressRow2("Road AcnTech");
//        pnAddress.setCap("00100");
//        pnAddress.setCity("Rome");
//        pnAddress.setCountry("Italy");
//        pnAddress.setFullName("Pat");
//        pnAddress.setNameRow2("Pas");
//        pnAddress.setCity2("CZ");
//        return addressDAO.create(pnAddress)
//                .flatMap(entity -> addressDAO.findByRequestId(entity.getRequestId())
//                        .map(item -> item))
//                        .map(item ->  ResponseEntity.ok().body(item));
    }

    @GetMapping(value = "/recursive")
    public Mono<ResponseEntity<String>> recursive(){
        return prepareAsyncServiceImpl.getFileRecursive(3, "RETRY", new BigDecimal(1) )
                .map(item -> ResponseEntity.ok().body("RISPOSTA "));
    }

    @PostMapping(value = "/sendResult")
    public Mono<ResponseEntity<PnDeliveryRequest>> sendResult(@RequestBody SingleStatusUpdateDto singleStatusUpdateDtoRequest){
        return paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDtoRequest)
                .map(item -> {
                    log.info("SEND RESULT ENTITY: {}", item);
                    return ResponseEntity.ok().body(item);
                });
    }

    @PostMapping(value = "/addDeliveryDriver")
    public Mono<ResponseEntity<PnPaperDeliveryDriver>> addDeliveryDriver(@RequestBody PnPaperDeliveryDriver pnPaperDeliveryDriver){
        return paperChannelService.addDeliveryDriver(pnPaperDeliveryDriver)
                .map(item -> {
                    log.info("DELIVERY DRIVER ADDED : {}", item);
                    return ResponseEntity.ok().body(item);
                });
    }

    @GetMapping(value = "/takeDeliveryDriver")
    public Mono<ResponseEntity<PageableDeliveryDriverResponseDto>> takeDeliveryDriver(@RequestParam(name="page") String page,
            @RequestParam(name="size") String size,
            @RequestParam(required=false, name="startDate") Date startDate){
        return paperChannelService.takeDeliveryDriver(new DeliveryDriverFilter(Integer.valueOf(page), Integer.valueOf(size), false, startDate, null))
                .map(item -> {
                    log.info("DELIVERY DRIVER PAGE : {}", item);
                    return ResponseEntity.ok().body(item);
                });
    }
}
