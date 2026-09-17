insert into tamanho (nome, preco_base, max_sabores, ativo) values
  ('Pequena', 15.99, 1, true), ('Média', 25.99, 2, true),
  ('Grande', 45.99, 3, true), ('Família', 69.99, 4, true)
on conflict (nome) do update set preco_base = excluded.preco_base, max_sabores = excluded.max_sabores, ativo = excluded.ativo;

insert into sabor (nome, preco_adicional, disponivel) values
  ('Calabresa', 0, true), ('Baiana', 0, true), ('4 Queijos', 0, true), ('Portuguesa', 0, true),
  ('Frango com Catupiry', 0, true), ('Lombinho', 0, true), ('Moda da Casa', 0, true), ('Bacon', 0, true),
  ('Milho Verde', 0, true), ('Italiana', 0, true), ('Paraguaia', 0, true), ('Rua da Mata', 0, true),
  ('IncoPop', 0, true), ('Areal', 0, true), ('Brigadeiro', 0, true), ('Romeu e Julieta', 0, true),
  ('Doce de Leite com Paçoca', 0, true)
on conflict (nome) do update set preco_adicional = excluded.preco_adicional, disponivel = excluded.disponivel;

insert into produto (nome, preco, categoria, disponivel) values
  ('Batata Frita Tradicional', 21.90, 'PETISCO', true), ('Batata Frita Turbinada', 31.90, 'PETISCO', true),
  ('Isca de Frango + Batata', 30.00, 'PETISCO', true), ('Surpresa de Queijo', 30.00, 'PETISCO', true),
  ('Filé com Fritas', 40.00, 'PETISCO', true), ('Coxinha da Asa + Batata', 30.00, 'PETISCO', true),
  ('Espetinho de Carne', 15.00, 'PETISCO', true), ('Picanha na Chapa à Moda da Casa', 80.00, 'PETISCO', true),
  ('Contra Filé na Chapa', 70.00, 'PETISCO', true), ('Refrigerante 1L', 10.00, 'BEBIDA', true),
  ('Refrigerante Lata', 6.00, 'BEBIDA', true), ('Refrigerante Litrinho', 6.00, 'BEBIDA', true),
  ('Heineken Long Neck', 10.00, 'BEBIDA', true), ('Heineken 0 Álcool Long Neck', 12.00, 'BEBIDA', true),
  ('Cerveja Lata', 6.00, 'BEBIDA', true), ('Suco', 10.00, 'BEBIDA', true), ('Água sem Gás', 4.00, 'BEBIDA', true),
  ('Água com Gás', 5.00, 'BEBIDA', true)
on conflict (nome) do update set preco = excluded.preco, categoria = excluded.categoria, disponivel = excluded.disponivel;
