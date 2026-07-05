package com.diouma.ebankservice.mcp;

import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.service.EbankService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class BankMcpTools {

    private final EbankService ebankService;

    public BankMcpTools(EbankService ebankService) {   // injection : tu connais !
        this.ebankService = ebankService;
    }

    @McpTool(name = "creerCompte",
            description = "Cree un compte bancaire pour un client existant")
    public BankAccount creerCompte(
            @McpToolParam(description = "Type: CURRENT-ACCOUNT ou SAVING-ACCOUNT", required = true) String type,
            @McpToolParam(description = "Solde initial du compte", required = true) double balance,
            @McpToolParam(description = "Id du client proprietaire", required = true) long customerId) {

        BankAccount compte = BankAccount.builder()
                .type(type)
                .balance(balance)
                .customerid(customerId)
                .build();

        return ebankService.save(compte);
    }
}
