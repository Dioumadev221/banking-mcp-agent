-- The schema is owned by Flyway and validated by Hibernate at startup
-- (spring.jpa.hibernate.ddl-auto=validate). Each column type mirrors what the
-- BankAccount entity maps to on PostgreSQL; any drift fails the build.
--
--   id          String (app-generated UUID)      -> varchar(255), primary key
--   balance     double (primitive, never null)   -> double precision not null
--   type        AccountType @Enumerated(STRING)  -> varchar(255)
--   created_at  Instant                          -> timestamp with time zone
--   customer_id long (primitive, never null)     -> bigint not null
--
-- The @Transient customer field is deliberately absent: it is fetched from
-- customer-service on read and never stored here.

create table bank_account (
    id          varchar(255)             not null primary key,
    balance     double precision         not null,
    type        varchar(255),
    created_at  timestamp with time zone,
    customer_id bigint                    not null
);
