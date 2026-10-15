alter table pedido add column email_notificacao varchar(120);
create table notification_outbox (
 id varchar(36) primary key, pedido_id bigint not null references pedido(id),
 event_type varchar(40) not null, recipient varchar(120) not null,
 title varchar(120) not null, message varchar(1000) not null,
 created_at timestamp(6) not null, next_attempt_at timestamp(6) not null,
 attempts integer not null default 0, delivered_at timestamp(6), last_error varchar(40),
 unique(pedido_id,event_type)
);
create index idx_notification_outbox_due on notification_outbox(delivered_at,next_attempt_at);
