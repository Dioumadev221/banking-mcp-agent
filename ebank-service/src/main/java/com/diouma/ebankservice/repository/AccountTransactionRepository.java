package com.diouma.ebankservice.repository;

import com.diouma.ebankservice.entities.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    /** An account's statement: its movements, most recent first. */
    List<AccountTransaction> findByAccountIdOrderByCreatedAtDesc(String accountId);
}
