alter table sabor add column descricao varchar(500);

update sabor set descricao = case nome
  when 'Calabresa' then 'Calabresa, cebola e orégano'
  when 'Baiana' then 'Calabresa picante, pimenta e pimentão'
  when '4 Queijos' then 'Mussarela, parmesão, provolone e gorgonzola'
  when 'Portuguesa' then 'Presunto, ovo, cebola, ervilha e azeitona'
  when 'Frango com Catupiry' then 'Frango desfiado, catupiry e milho'
  when 'Lombinho' then 'Lombinho, cream cheese e cebola caramelizada'
  when 'Moda da Casa' then 'Calabresa, bacon, catupiry e milho'
  when 'Bacon' then 'Bacon crocante, mussarela e cheddar'
  when 'Milho Verde' then 'Milho, mussarela e parmesão'
  when 'Italiana' then 'Salame italiano, rúcula, tomate seco e parmesão'
  when 'Paraguaia' then 'Carne moída, cheddar, cebola e pimenta'
  when 'Rua da Mata' then 'Nata, frango, bacon e cebola'
  when 'IncoPop' then 'Presunto, palmito e mussarela'
  when 'Areal' then 'Carne de sol, queijo coalho e cebola'
  when 'Brigadeiro' then 'Brigadeiro e granulado de chocolate'
  when 'Romeu e Julieta' then 'Queijo e goiabada'
  when 'Doce de Leite com Paçoca' then 'Doce de leite finalizado com farofa de paçoca'
end;
