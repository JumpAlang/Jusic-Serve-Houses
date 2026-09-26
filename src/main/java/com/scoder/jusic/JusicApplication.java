package com.scoder.jusic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author H
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
//@Controller
public class JusicApplication {

    public static void main(String[] args) {
        // Preserve standard Spring Boot command-line configuration. The old
        // hard-coded --debug argument discarded every operator supplied limit
        // or lifecycle override.
        SpringApplication.run(JusicApplication.class, args);
    }

//    @GetMapping("/")
//    public String index(){
//        return "index.html";
//    }
}
