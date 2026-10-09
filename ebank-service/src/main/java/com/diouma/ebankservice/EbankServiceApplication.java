package com.diouma.ebankservice;

import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.service.EbankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
@EnableFeignClients
public class EbankServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(EbankServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EbankServiceApplication.class, args);
    }

    /**
     * Creates a few accounts on startup so the documented endpoints return
     * something on a fresh clone.
     *
     * <p>Deterministic on purpose: the README shows real output, and a reader
     * comparing it against their own run should see the same shape.
     *
     * <p>Seeding needs customer-service, which may not have started yet. That
     * failure is reported, not swallowed: an empty account list with no
     * explanation is what makes a demo look broken.
     */
    @Bean
    @ConditionalOnProperty(name = "demo.seed-accounts", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedDemoAccounts(EbankService ebankService) {
        return args -> {
            // Idempotent: the database now persists across restarts, so re-seeding
            // would pile up duplicate accounts on every boot. Seed only when empty.
            if (!ebankService.getAllBankAccounts().isEmpty()) {
                return;
            }
            try {
                for (long customerId = 1; customerId <= 3; customerId++) {
                    ebankService.createAccount(AccountType.CURRENT_ACCOUNT,
                            BigDecimal.valueOf(5_000 * customerId), customerId);
                    ebankService.createAccount(AccountType.SAVING_ACCOUNT,
                            BigDecimal.valueOf(1_200 * customerId), customerId);
                }
                log.info("Seeded 6 demo accounts for customers 1 to 3");
            } catch (Exception exception) {
                log.warn("Demo accounts not seeded ({}). customer-service is probably not up yet; "
                        + "accounts can still be created through POST /accounts once it is.",
                        exception.getMessage());
            }
        };
    }

}
