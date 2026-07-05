package com.diouma.ebankservice.service;

import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.feign.CustomerRestClient;
import com.diouma.ebankservice.models.Customer;
import com.diouma.ebankservice.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class EbankService {
    private CustomerRestClient customerRestClient;
    private BankAccountRepository accountRepository;

    public EbankService(CustomerRestClient customerRestClient, BankAccountRepository accountRepository) {
        this.customerRestClient = customerRestClient;
        this.accountRepository = accountRepository;
    }

    public List<BankAccount> getAllBankAccounts(){
        return accountRepository.findAll();
    }

    public BankAccount getAccountById(String id){
        BankAccount bankAccount =  accountRepository.findById(id).orElseThrow(()-> new RuntimeException("Account not found"));
        bankAccount.setCustomer(customerRestClient.getCustomerById(bankAccount.getCustomerid()));
        return bankAccount;
    }

    public BankAccount save(BankAccount bankAccount) {
        // 1. On vérifie que le client existe (leve une erreur s'il n'existe pas)
        customerRestClient.getCustomerById(bankAccount.getCustomerid());
        // 2. On complete et on sauvegarde
        bankAccount.setId(UUID.randomUUID().toString());
        bankAccount.setCreatedAt(new Date());
        return accountRepository.save(bankAccount);
    }

}
