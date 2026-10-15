# Laboratório: pagamento fictício → pedido → notificação SMTP de teste

Este sandbox é um **simulador local**, sem Mercado Pago, Stripe, banco ou adquirente. Não aceita dados de cartão, não move dinheiro e não envia mensagens à internet. A demo do GitHub Pages continua isolada no navegador; os endpoints abaixo pertencem ao backend.

## Executar

Requer Docker Compose. Mantenha os repositórios atualizados em pastas irmãs `automatizacao-artpizza` e `notification-api`, ou ajuste `NOTIFICATION_SOURCE` no `.env`. Copie `.env.example` para `.env` e preencha DB_PASSWORD, ADMIN_USERNAME/PASSWORD, NOTIFICATION_ADMIN_USERNAME/PASSWORD, NOTIFICATION_INTEGRATION_PASSWORD, RABBITMQ_PASSWORD e PAGAMENTO_SANDBOX_SECRET. As senhas Notification precisam de pelo menos 16 caracteres; o segredo sandbox, 32. A conta de integração é `artpizza`, separada dos administradores.

```bash
docker compose -f compose.integration.yaml up --build -d
```

Art Pizza: `http://localhost:8081`; Notification: `http://localhost:8082/swagger-ui.html`; Mailpit: `http://localhost:8025`. Somente portas loopback são publicadas. Bancos e broker usam volumes persistentes. Não use `down -v` se precisar manter os dados.

## Fluxo

1. Consulte `/api/cardapio/produtos`. Crie `POST /api/pedidos` com um produto válido, `formaPagamento: "PIX"`, `tipoEntrega: "RETIRADA"`, telefone e nome fictícios. Acrescente `emailNotificacao: "customer@example.invalid"` para optar pelas notificações. Sem e-mail não há evento de notificação.
2. Com HTTP Basic do administrador Art Pizza, crie `POST /api/pagamentos/sandbox` com `{"pedidoId": ID}`. A cobrança nasce PENDING, com valor calculado no servidor e moeda BRL. Repetir retorna a mesma cobrança.
3. Envie `POST /api/pagamentos/sandbox/simular`, também como administrador:

   ```json
   {"eventId":"evento-unico","paymentId":"UUID-DA-COBRANCA","pedidoId":1,"amount":30.00,"currency":"BRL","status":"APPROVED"}
   ```

   Use o valor exato retornado na cobrança. APPROVED libera o pedido; DECLINED não libera. Repetir o mesmo evento não confirma duas vezes. A mesma identidade com outro conteúdo, moeda/valor incorretos, mudança de resultado final e aprovação de pedido cancelado são rejeitados. Uma cobrança recusada é terminal; para outro ensaio crie um novo pedido.
4. Pedido e evento de notificação são persistidos na mesma transação. O worker envia o evento por HTTP autenticado à Notification API com `Idempotency-Key: artpizza-UUID`. Falhas mantêm o evento na outbox, com retentativa crescente limitada a uma hora.
5. A Notification API persiste o trabalho e usa RabbitMQ → consumidor → SMTP Mailpit. Consulte notificações e `/attempts` com suas credenciais administrativas. A conta Art Pizza só cria notificações e consulta canais.

O SMTP recebe uma mensagem CRIADO e outra PAGAMENTO_CONFIRMADO. Mudanças posteriores de status também geram eventos. A outbox registra aceitação pela API; o resultado SMTP fica no histórico da Notification API.

## Webhook assinado

`POST /webhook/pagamento-sandbox` só existe quando PAGAMENTO_SANDBOX_ENABLED=true. Use o mesmo JSON, `X-Sandbox-Timestamp` (Unix em segundos, dentro de cinco minutos) e `X-Sandbox-Signature` (HMAC-SHA256 hexadecimal de `timestamp + "." + corpo JSON exato`, usando PAGAMENTO_SANDBOX_SECRET). Sem segredo de pelo menos 32 caracteres ou assinatura válida, o webhook é recusado. Esse contrato pertence ao simulador, não a um provedor externo. O endpoint administrativo `/simular` aplica as mesmas regras sem exigir assinatura.

## Verificação ponta a ponta

Com os serviços iniciados e as variáveis do `.env` exportadas no terminal:

```bash
python scripts/verify-integration.py
```

Use volumes novos e descartáveis: o teste exige exatamente duas mensagens na caixa Mailpit. Ele cria um pedido fictício, repete a aprovação, confere o status, aguarda duas notificações SENT, verifica uma tentativa SMTP por notificação e confirma 401 na listagem anônima. Não limpa dados existentes.

## Garantias e limites

Locks e restrições únicas serializam confirmações concorrentes. O processamento é pelo menos uma vez; a chave idempotente evita duplicar cadastros quando a resposta HTTP se perde. Uma queda entre aceitação SMTP e commit pode duplicar e-mail, conforme documentado na Notification API. Não há promessa de exatamente uma entrega ao provedor.

Para um provedor real de sandbox, ainda será necessário implementar seu contrato oficial de cobrança, consulta e assinatura. As regras de valor, moeda, pedido, idempotência e concorrência já estão separadas no serviço. Nenhuma compatibilidade com um provedor real é alegada nesta entrega.
