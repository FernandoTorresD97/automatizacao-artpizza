create table sandbox_payment (
 id varchar(36) primary key, pedido_id bigint not null unique references pedido(id),
 amount numeric(38,2) not null, currency varchar(3) not null,
 status varchar(20) not null, created_at timestamp(6) not null
);
create table sandbox_payment_event (
 id varchar(80) primary key, payment_id varchar(36) not null references sandbox_payment(id),
 payload_hash varchar(64) not null, received_at timestamp(6) not null
);
