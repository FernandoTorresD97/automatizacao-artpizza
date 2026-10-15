# Evolução do portfólio

## Etapa 1 — base verificável

- PostgreSQL e Flyway; execução Compose.
- Testes de integração para regras de negócio e acesso administrativo.
- Proteção contra atualizações concorrentes e tratamento HTTP 409.
- Login do painel em tempo de execução; nenhuma senha no build.
- Demonstração pública isolada com dados fictícios.

## Etapa 2 — identidade e rastreabilidade

Perfis de atendente/administrador, persistência de usuários, registro de alterações com autor e data e testes das permissões. Não implementar outro formato de token apenas para acrescentar uma tecnologia.

## Etapa 3 — pagamentos em sandbox

Implementado um simulador **local**, sem provedor: cobrança fictícia, aprovação/recusa, webhook HMAC, eventos idempotentes, validação de valor/moeda/pedido e testes de concorrência. Integração com um sandbox de provedor permanece pendente de conta e contrato escolhido.

Escolher um provedor conforme a conta disponível. Integrar cobrança e verificação autenticada do pagamento; conferir valor, moeda, identificação e pedido. Persistir eventos com identificação única e processar confirmação atomicamente. Testar evento repetido, evento concorrente, assinatura inválida, valor incorreto, indisponibilidade e pedido cancelado. Não movimentar dinheiro real em uma demonstração.

## Etapa 4 — Notification API

Implementada outbox transacional por pedido e evento, cliente HTTP com conta de integração, chave idempotente e retentativas. O laboratório Compose conecta os dois backends, RabbitMQ e Mailpit; a verificação ponta a ponta está no CI. Ativação em hospedagem pública ainda exige configuração de serviços e segredos.

Primeiro adicionar envio de teste, retentativas e histórico à Notification API. Depois integrar eventos de pedidos com RabbitMQ, tratamento de mensagens que esgotam tentativas e idempotência. Registrar as garantias e limitações de entrega; não prometer exatamente uma entrega ao provedor sem suporte dele.

## Etapa 5 — observabilidade e implantação real

Logs correlacionados, health checks, métricas e testes de falhas. Escolher hospedagem da API/banco com orçamento definido, HTTPS, segredos e dados de demonstração separados. A demo do GitHub Pages não substitui esse ambiente.

## Critério para citar no currículo

Só incluir funcionalidades implementadas, verificadas e disponíveis no repositório. Diferenciar demonstração, sandbox e operação real. Publicar resultados de testes e medições somente depois de executá-los.
