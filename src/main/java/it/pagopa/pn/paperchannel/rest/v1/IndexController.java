package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/index")
public class IndexController {

    @Autowired
    private KmsEncryption kmsEncryption;


    @GetMapping(value = "/crypt")
    public Mono<ResponseEntity<String>> encryption(){
        String crypted = kmsEncryption.encode("pippo");
        String decrypted = kmsEncryption.decode(crypted);

        return Mono.just("")
                .map(item -> ResponseEntity.ok().body(decrypted));
    }



}
