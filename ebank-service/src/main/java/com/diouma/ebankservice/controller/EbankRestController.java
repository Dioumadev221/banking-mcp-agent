package com.diouma.ebankservice.controller;

import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.service.EbankService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EbankRestController {
 private EbankService ebankService;

    public EbankRestController(EbankService ebankService) {
        this.ebankService = ebankService;
    }
    @GetMapping("/accounts")
    public List<BankAccount> getAllBankAccounts(){
        return ebankService.getAllBankAccounts();
    }
    @GetMapping("/accounts/{id}")
    public BankAccount getAccountById(@PathVariable String id){
        return ebankService.getAccountById(id);
    }
    @PostMapping("/accounts")
    public BankAccount save (@RequestBody BankAccount bankAccount){
        return ebankService.save(bankAccount);
    }
}
