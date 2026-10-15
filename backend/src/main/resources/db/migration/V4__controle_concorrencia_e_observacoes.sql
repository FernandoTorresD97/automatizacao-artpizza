alter table pedido add column version bigint not null default 0;
alter table item_pedido alter column observacao type varchar(500);
create index idx_pedido_status_criado_em on pedido(status, criado_em);
