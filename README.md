# Automatização Art Pizza

Sistema de atendimento e gestão de pedidos para a **Art Pizza**, em Cruz das Almas/BA. Reúne uma API de pedidos, um fluxo conversacional para atendimento e um painel web para acompanhar a operação da pizzaria.

**Autor: Fernando Torres** · **Versão: 0.1.0** · **Estágio: desenvolvimento e demonstração local**

> O projeto permite demonstrar o fluxo de pedidos localmente. A publicação do código no GitHub não coloca a aplicação no ar e não significa que integrações bancárias, WhatsApp ou infraestrutura de produção estejam homologadas.

## Funcionalidades

- Cardápio com tamanhos, limites de sabores, descrições, petiscos e bebidas.
- Atendimento conversacional por etapas, com simulador HTTP e estrutura de integração com WhatsApp.
- Criação de pedidos, cálculo de itens e frete por bairro.
- Painel de acompanhamento por status, atualização periódica e aviso sonoro de novos pedidos.
- Confirmação administrativa de pagamento PIX por operação específica.
- Resumo diário de vendas com quantidade de pedidos, ticket médio, itens, frete e forma de pagamento.
- Validação de regras de negócio, autenticação administrativa HTTP Basic e verificação de assinatura dos webhooks.

### Tamanhos e regras do cardápio

| Tamanho | Preço base | Máximo de sabores |
| --- | ---: | ---: |
| Pequena | R$ 15,99 | 1 |
| Média | R$ 25,99 | 2 |
| Grande | R$ 45,99 | 3 |
| Família | R$ 69,99 | 4 |

O cadastro inicial contém 17 sabores e 18 produtos entre petiscos e bebidas. Os dados estão no seeder de desenvolvimento e nas migrações SQL; não há editor completo de cardápio no painel.

## Tecnologias e organização

Backend em **Java 21 / Spring Boot 3.3.4**, com Spring Data JPA, Spring Security, Maven e Flyway. Painel em **Vue 3 / Vite 5**. O perfil de desenvolvimento usa **H2 em memória**; o perfil de produção está configurado para **PostgreSQL**.

```text
backend/
  pom.xml
  src/main/java/com/artpizza/     # API, serviços, entidades e integrações
  src/main/resources/
    application.yml              # Perfis e variáveis de ambiente
    db/migration/                # Migrações e cardápio inicial
painel/
  src/                           # Componentes Vue e cliente HTTP
  .env.example                   # Exemplo exclusivamente local
  package.json
  package-lock.json
```

## Executar localmente

Pré-requisitos: JDK 21, Maven 3.9+, Node.js 20+ com npm e Git. Os comandos abaixo usam PowerShell, em dois terminais.

### 1. Backend

Na raiz do projeto:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn spring-boot:run
```

A API estará em `http://localhost:8080`. Apenas no perfil `dev`, as credenciais administrativas padrão são `admin` / `admin`. Não reutilize essas credenciais em produção.

**O H2 é temporário: os pedidos e conversas são perdidos ao reiniciar o backend.** O cardápio e os bairros de demonstração são carregados novamente. Nenhuma mensagem real é enviada por padrão.

### 2. Painel

Em outro terminal, a partir da raiz:

```powershell
cd painel
Copy-Item .env.example .env
npm ci
npm run dev
```

Abra `http://localhost:5173`. Se alterar o usuário ou a senha do backend, ajuste os valores correspondentes no `.env` e reinicie o Vite. Não versione o `.env`.

### 3. Simular uma conversa

Com o backend ativo:

```powershell
$mensagem = @{
  telefone = '5500000000000'
  nome = 'Cliente de demonstracao'
  texto = 'Oi'
} | ConvertTo-Json

Invoke-RestMethod -Uri 'http://localhost:8080/api/whatsapp-simulador/mensagem' `
  -Method Post -ContentType 'application/json' -Body $mensagem
```

Envie as próximas respostas no campo `texto`, mantendo o mesmo telefone, e siga as opções retornadas pelo bot. Use dados fictícios. O pedido aparecerá no painel quando o atendimento concluir a criação.

## Fluxo operacional e PIX

O fluxo principal é `NOVO → CONFIRMADO → EM_PREPARO → PRONTO → SAIU_PARA_ENTREGA → ENTREGUE`.

Pedidos PIX podem começar em `AGUARDANDO_PAGAMENTO`. Para liberá-los, use a confirmação de pagamento no painel, que chama `PATCH /api/pedidos/{id}/confirmar-pagamento` e move o pedido para `NOVO`. Não tente liberar um PIX pendente pela atualização genérica de status. Na operação real, confirme somente após conferir o recebimento.

A geração do código PIX depende de uma chave válida. O webhook de pagamento é uma estrutura genérica com assinatura, desativada por padrão: ainda precisa ser adaptado e homologado com um provedor. O simulador não comprova recebimento bancário.

## Como interpretar o faturamento

O resumo usa a **data de criação do pedido**, da meia-noite até a meia-noite seguinte, e inclui pedidos nos estados `CONFIRMADO`, `EM_PREPARO`, `PRONTO`, `SAIU_PARA_ENTREGA` e `ENTREGUE`. Pedidos novos, cancelados e aguardando pagamento não entram no cálculo.

O valor total inclui frete, exibido também separadamente. Trata-se de um indicador de vendas confirmadas, **não de lucro, conciliação bancária ou dinheiro efetivamente recebido**. Uma operação que atravessa a meia-noite aparece em duas datas; ainda não existe fechamento por turno.

## Principais endpoints

| Método | Rota | Uso |
| --- | --- | --- |
| GET | `/api/cardapio/tamanhos` | Tamanhos disponíveis |
| GET | `/api/cardapio/sabores` | Sabores e descrições |
| GET | `/api/cardapio/produtos` | Produtos adicionais |
| GET | `/api/frete?bairro=Centro` | Consulta de frete |
| POST | `/api/pedidos` | Criação de pedido |
| GET | `/api/pedidos` | Listagem administrativa |
| GET | `/api/pedidos/{id}` | Detalhes administrativos |
| PATCH | `/api/pedidos/{id}/status` | Alteração de status |
| PATCH | `/api/pedidos/{id}/confirmar-pagamento` | Confirmação administrativa PIX |
| GET | `/api/faturamento?data=AAAA-MM-DD` | Resumo administrativo |
| POST | `/api/whatsapp-simulador/mensagem` | Atendimento simulado |
| GET / POST | `/webhook/whatsapp` | Verificação e recebimento WhatsApp |
| POST | `/webhook/pagamento` | Recebimento de confirmação assinada, se habilitado |

As operações administrativas exigem HTTP Basic. Os DTOs em `backend/src/main/java/com/artpizza/dto` definem os contratos de pedidos.

## Configuração de integrações

Configure variáveis no ambiente do processo ou no gerenciador de segredos da hospedagem. O backend não carrega automaticamente um arquivo `.env`.

| Variáveis | Finalidade |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Selecionar explicitamente `dev` ou `prod` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Acesso administrativo |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão PostgreSQL no perfil `prod` |
| `WHATSAPP_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET` | Verificação e assinatura do webhook |
| `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID` | Credenciais da integração WhatsApp |
| `WHATSAPP_ENVIAR_MENSAGENS_REAIS` | Ativar envio real; padrão `false` |
| `PIX_CHAVE`, `PIX_NOME_RECEBEDOR`, `PIX_CIDADE` | Identificação do recebedor PIX |
| `PAGAMENTO_WEBHOOK_ENABLED`, `PAGAMENTO_WEBHOOK_SECRET` | Habilitação e assinatura do webhook de pagamento |
| `VITE_API_BASE_URL` | Endereço da API usado pelo painel |
| `VITE_ADMIN_USERNAME`, `VITE_ADMIN_PASSWORD` | Credenciais do painel de demonstração; não são segredos no navegador |

## Validação e build

```powershell
# Dentro de backend/
mvn test
mvn package

# Dentro de painel/
npm ci
npm run build
```

O JAR é gerado em `backend/target/` e os arquivos do painel em `painel/dist/`. Não são versionados. **A versão atual ainda não possui suíte de testes automatizados**; compilar com sucesso não garante todos os fluxos. Antes de operar, valide pedidos, limites de sabores, frete, pagamentos, cancelamentos, atualização do painel e totais com cenários controlados.

## Antes de usar em uma pizzaria

- Revisar e atualizar dependências: na preparação desta versão, `npm ci` reportou duas vulnerabilidades (uma moderada e uma alta). Os builds passaram, mas isso não elimina os alertas; consulte `npm audit` antes da implantação.
- Implementar login seguro no painel: variáveis `VITE_*` são incorporadas ao código acessível no navegador; a solução atual é apenas para demonstração local.
- Definir credenciais fortes e próprias, HTTPS, CORS para o domínio correto, proteção contra abuso e controle de acesso. O CORS atual permite `http://localhost:5173`.
- Desativar ou restringir o simulador público em produção.
- Provisionar PostgreSQL, testar migrações e compatibilidade do módulo Flyway/PostgreSQL, backups e recuperação. O perfil `prod` não foi homologado nesta entrega.
- Configurar e validar a integração oficial do WhatsApp e o provedor de pagamentos em ambiente de testes antes de enviar mensagens ou cobrar clientes reais.
- Conferir cardápio, bairros, preços, chave PIX e regras de retirada/entrega. Os bairros do perfil `dev` são exemplos.
- Implementar observabilidade e testes automatizados, além de procedimentos de privacidade e retenção de dados dos clientes.
- Evoluir atendimento humano, notificações de status, fechamento por turno e conciliação: não estão completos nesta versão.

## Autor e uso

**Fernando Torres** — autoria e responsável pelo projeto.

GitHub: [FernandoTorresD97](https://github.com/FernandoTorresD97)

Nenhuma licença de código aberto foi definida nesta versão. O repositório não concede, por si só, autorização de redistribuição ou uso comercial por terceiros.
