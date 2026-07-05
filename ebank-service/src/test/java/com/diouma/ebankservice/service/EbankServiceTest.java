package com.diouma.ebankservice.service;

import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.feign.CustomerRestClient;
import com.diouma.ebankservice.models.Customer;
import com.diouma.ebankservice.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // 1. on active Mockito pour cette classe de test
class EbankServiceTest {

    @Mock // 2. une doublure du client réseau (ne partira PAS sur le réseau)
    private CustomerRestClient customerRestClient;

    @Mock // 3. une doublure de la base de données
    private BankAccountRepository accountRepository;

    @InjectMocks // 4. le VRAI EbankService, mais on lui injecte les 2 doublures
    private EbankService ebankService;

    @Test
    void save_doit_poser_un_id_et_une_date() {
        // ---------- GIVEN : on prépare le décor ----------
        // On donne son texte à la doublure customerRestClient :
        // "si on te demande n'importe quel client, réponds ce faux Diouma"
        Customer fauxClient = Customer.builder().id(1L).name("Diouma").email("diouma@gmail.com").build();
        when(customerRestClient.getCustomerById(anyLong())).thenReturn(fauxClient);

        // La doublure du repository : "quand on te demande de sauver un compte,
        // fais semblant et renvoie exactement le compte qu'on t'a donné"
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Le compte qu'on veut sauver : il n'a NI id NI date pour l'instant
        BankAccount compteAvant = BankAccount.builder()
                .type("CURRENT-ACCOUNT")
                .balance(5000)
                .customerid(1)
                .build();

        // ---------- WHEN : on déclenche l'action testée ----------
        BankAccount compteApres = ebankService.save(compteAvant);

        // ---------- THEN : on vérifie que save() a fait son travail ----------
        assertThat(compteApres.getId()).isNotNull();        // un id a bien été posé (ligne 36 du service)
        assertThat(compteApres.getCreatedAt()).isNotNull(); // une date a bien été posée (ligne 37)
        assertThat(compteApres.getCustomerid()).isEqualTo(1);
    }
    @Test
    public void getAllBankAccounts(){
        List<BankAccount> bankAccounts = new ArrayList<>();
        BankAccount bankAccount1 = BankAccount.builder()
                .customerid(1)
                .balance(2000)
                .type("SAVING-ACCOUNT")
                .build();
        BankAccount bankAccount2 = BankAccount.builder()
                .customerid(3)
                .balance(50000)
                .type("CURRENT-ACCOUNT")
                .build();
        BankAccount bankAccount3 = BankAccount.builder()
                .customerid(2)
                .balance(4000)
                .type("SAVING-ACCOUNT")
                .build();

        bankAccounts.add(bankAccount2);
        bankAccounts.add(bankAccount3);
        bankAccounts.add(bankAccount1);

        when(accountRepository.findAll()).thenReturn(bankAccounts);

        List<BankAccount> bankAccountList = ebankService.getAllBankAccounts();
        assertThat(bankAccountList).isNotNull();
        assertThat(bankAccountList).hasSize(3);
    }

    @Test
    void getAccountById_doit_lever_une_erreur_si_le_compte_nexiste_pas() {
        // ---------- GIVEN : le mannequin "base de données" ne trouve AUCUN compte ----------
        // Optional.empty() = sa façon de dire "je n'ai rien sous cet id"
        when(accountRepository.findById("id-bidon")).thenReturn(Optional.empty());

        // ---------- WHEN + THEN : appeler la méthode DOIT lever une erreur ----------
        // assertThatThrownBy : "j'affirme que ce bout de code va exploser"
        assertThatThrownBy(() -> ebankService.getAccountById("id-bidon"))
                .isInstanceOf(RuntimeException.class)   // ...avec le bon TYPE d'erreur
                .hasMessage("Account not found");        // ...et le bon MESSAGE
    }
}
