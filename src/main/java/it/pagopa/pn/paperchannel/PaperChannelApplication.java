package it.pagopa.pn.paperchannel;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Slf4j
@EnableScheduling
public class PaperChannelApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PaperChannelApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        app.run(args);
    }


    @RestController
    public static class HomeController {

        @GetMapping("")
        public String home() {
            return "Ok";
        }
    }

}
