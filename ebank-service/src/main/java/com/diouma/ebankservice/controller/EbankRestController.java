package com.diouma.ebankservice.controller;

import com.diouma.ebankservice.entities.AccountTransaction;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.service.EbankService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EbankRestController {

    private final EbankService ebankService;

    public EbankRestController(EbankService ebankService) {
        this.ebankService = ebankService;
    }

    @GetMapping("/accounts")
    public List<BankAccount> getAllBankAccounts() {
        return ebankService.getAllBankAccounts();
    }

    @GetMapping("/accounts/{id}")
    public BankAccount getAccountById(@PathVariable String id) {
        return ebankService.getAccountById(id);
    }

    @GetMapping("/accounts/{id}/transactions")
    public List<AccountTransaction> getStatement(@PathVariable String id) {
        return ebankService.getStatement(id);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccount createAccount(@RequestBody CreateAccountRequest request) {
        return ebankService.createAccount(request.type(), request.balance(), request.customerId());
    }

    @PostMapping("/accounts/{id}/deposits")
    public BankAccount deposit(@PathVariable String id, @RequestBody AmountRequest request) {
        return ebankService.deposit(id, request.amount());
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public BankAccount withdraw(@PathVariable String id, @RequestBody AmountRequest request) {
        return ebankService.withdraw(id, request.amount());
    }

    @PostMapping("/transfers")
    public void transfer(@RequestBody TransferRequest request) {
        ebankService.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
    }
}
