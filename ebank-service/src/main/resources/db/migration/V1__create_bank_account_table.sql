-- The schema is owned by Flyway and validated by Hibernate at startup
-- (spring.jpa.hibernate.ddl-auto=validate). Each column type mirrors what the
-- BankAccount entity maps to on PostgreSQL; any drift fails the build.
--
--   id          String (app-generated UUID)      -> varchar(255), primary key
--   balance     BigDecimal (never null)          -> numeric(19,4) not null
--   type        AccountType @Enumerated(STRING)  -> varchar(255)
--   created_at  Instant                          -> timestamp with time zone
--   customer_id long (primitive, never null)     -> bigint not null
--
-- Money is numeric(19,4), never a floating-point type: a double cannot hold
-- 0.10 exactly, and rounding error has no place in an account balance. The Java
-- side matches with BigDecimal.
--
-- The @Transient customer field is deliberately absent: it is fetched from
-- customer-service on read and never stored here.

create table bank_account (
    id          varchar(255)             not null primary key,
    balance     numeric(19, 4)           not null,
    type        varchar(255),
    created_at  timestamp with time zone,
    customer_id bigint                    not null
);
