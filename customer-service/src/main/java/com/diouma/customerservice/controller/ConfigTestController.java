package com.diouma.customerservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigTestController {

    // Cette valeur va être injectée DEPUIS le config server
    @Value("${isepat.dev.contact.prenom}")
    private String prenom;

    @GetMapping("/config-test")
    public String test() {
        return "Prénom récupéré depuis le config server : " + prenom;
    }
}