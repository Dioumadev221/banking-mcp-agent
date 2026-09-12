package com.diouma.customerservice;

import com.diouma.customerservice.entities.Customer;
import com.diouma.customerservice.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Locale;

@SpringBootApplication
public class CustomerServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    /**
     * Creates the customers the documented examples refer to.
     *
     * <p>Addresses use example.com, the domain reserved for documentation, so
     * demo data cannot accidentally point at a real mailbox.
     */
    @Bean
    @ConditionalOnProperty(name = "demo.seed-customers", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedDemoCustomers(CustomerService customerService) {
        return args -> {
            List<String> names = List.of("Diouma", "Seynabou", "Souleymane");
            names.forEach(name -> customerService.saveCustomer(Customer.builder()
                    .name(name)
                    .email(name.toLowerCase(Locale.ROOT) + "@example.com")
                    .build()));
            log.info("Seeded {} demo customers", names.size());
        };
    }

}
