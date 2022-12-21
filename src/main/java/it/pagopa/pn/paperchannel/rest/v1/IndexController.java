package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/index")
public class IndexController {

    @Autowired
    private KmsEncryption kmsEncryption;

    @Autowired
    private PrepareAsyncServiceImpl prepareAsyncServiceImpl;

    @GetMapping(value = "/crypt")
    public Mono<ResponseEntity<String>> encryption(){
        String crypted = kmsEncryption.encode("pippo");
        String decrypted = kmsEncryption.decode(crypted);

        return Mono.just("")
                .map(item -> ResponseEntity.ok().body(decrypted));
    }

    @GetMapping(value = "/recursive")
    public Mono<ResponseEntity<String>> recursive(){

        return prepareAsyncServiceImpl.getFileRecursive(3, "RETRY", new BigDecimal(1) )
                .map(item -> ResponseEntity.ok().body("RISPOSTA "));

    }


}
