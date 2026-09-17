create table cliente (
    id bigserial primary key,
    nome varchar(255) not null,
    telefone_whatsapp varchar(255) not null unique,
    criado_em timestamp(6) not null
);

create table tamanho (
    id bigserial primary key,
    nome varchar(255) not null unique,
    preco_base numeric(38,2) not null,
    max_sabores integer not null,
    ativo boolean
);

create table sabor (
    id bigserial primary key,
    nome varchar(255) not null unique,
    preco_adicional numeric(38,2) not null,
    disponivel boolean
);

create table produto (
    id bigserial primary key,
    nome varchar(255) not null unique,
    preco numeric(38,2) not null,
    categoria varchar(255),
    disponivel boolean
);

create table regiao_frete (
    id bigserial primary key,
    bairro varchar(255) not null unique,
    valor_frete numeric(38,2) not null,
    ativo boolean
);

create table endereco (
    id bigserial primary key,
    cliente_id bigint not null references cliente(id),
    bairro varchar(255) not null,
    rua varchar(255) not null,
    numero varchar(255) not null,
    complemento varchar(255),
    latitude double precision,
    longitude double precision
);

create table pedido (
    id bigserial primary key,
    cliente_id bigint not null references cliente(id),
    tipo_entrega varchar(255) not null,
    endereco_id bigint references endereco(id),
    valor_frete numeric(38,2) not null,
    valor_total numeric(38,2) not null,
    forma_pagamento varchar(255) not null,
    status varchar(255) not null,
    criado_em timestamp(6) not null,
    atualizado_em timestamp(6),
    pix_txid varchar(255),
    pix_copia_cola text
);
create index idx_pedido_pix_txid on pedido(pix_txid);

create table item_pedido (
    id bigserial primary key,
    pedido_id bigint not null references pedido(id),
    tipo varchar(255) not null,
    tamanho_id bigint references tamanho(id),
    produto_id bigint references produto(id),
    quantidade integer not null,
    observacao varchar(255),
    preco_unitario numeric(38,2) not null
);

create table item_pedido_sabor (
    item_pedido_id bigint not null references item_pedido(id),
    sabor_id bigint not null references sabor(id)
);

create table sessao_conversa (
    id bigserial primary key,
    telefone_whatsapp varchar(255) not null unique,
    nome_cliente varchar(255),
    estado varchar(255) not null,
    tamanho_id bigint,
    observacao varchar(255),
    produto_id_atual bigint,
    tipo_entrega varchar(255),
    bairro varchar(255),
    rua varchar(255),
    numero varchar(255),
    forma_pagamento varchar(255),
    atualizado_em timestamp(6) not null
);

create table sessao_conversa_sabor (
    sessao_id bigint not null references sessao_conversa(id),
    sabor_id bigint
);

create table sessao_conversa_item (
    id bigserial primary key,
    sessao_id bigint not null references sessao_conversa(id),
    tipo varchar(255) not null,
    tamanho_id bigint,
    produto_id bigint,
    quantidade integer not null,
    observacao varchar(255)
);

create table sessao_conversa_item_sabor (
    item_id bigint not null references sessao_conversa_item(id),
    sabor_id bigint
);

create table mensagem_whatsapp_processada (
    id varchar(255) primary key,
    processada_em timestamp(6)
);
