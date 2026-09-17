package com.artpizza.whatsapp;

import com.artpizza.dto.PedidoRequestDTO;
import com.artpizza.exception.RegraNegocioException;
import com.artpizza.model.*;
import com.artpizza.repository.*;
import com.artpizza.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Toda a lógica de conversa do bot vive aqui. Cada chamada a
 * processarMensagem recebe o telefone de quem mandou a mensagem e o texto,
 * avança a SessaoConversa correspondente um passo, e devolve o texto de
 * resposta — o chamador (webhook real ou simulador) só decide pra onde
 * mandar essa resposta.
 *
 * O pedido pode ter vários itens (várias pizzas e/ou produtos avulsos como
 * bebida): cada item fechado vai pra SessaoConversa.itens, e o bot pergunta
 * "quer mais alguma coisa?" até o cliente decidir fechar o pedido.
 */
@Service
@RequiredArgsConstructor
public class ConversaService {

    private static final Set<String> PALAVRAS_REINICIO =
            Set.of("oi", "ola", "menu", "inicio", "comecar");

    private final SessaoConversaRepository sessaoRepository;
    private final TamanhoRepository tamanhoRepository;
    private final SaborRepository saborRepository;
    private final ProdutoRepository produtoRepository;
    private final RegiaoFreteRepository regiaoFreteRepository;
    private final PedidoService pedidoService;
    private final MensagemWhatsAppProcessadaRepository mensagemProcessadaRepository;

    @Transactional
    public String processarMensagem(String telefone, String nomePerfil, String textoOriginal) {
        return processar(telefone, nomePerfil, textoOriginal);
    }

    /** Retorna vazio quando a Meta reenviou uma mensagem já concluída. */
    @Transactional
    public Optional<String> processarMensagemWhatsApp(String idMensagem, String telefone, String nomePerfil,
                                                       String textoOriginal) {
        if (idMensagem == null || idMensagem.isBlank() || mensagemProcessadaRepository.existsById(idMensagem)) {
            return Optional.empty();
        }
        // O flush faz a restrição de unicidade valer antes de criar um pedido.
        mensagemProcessadaRepository.saveAndFlush(new MensagemWhatsAppProcessada(idMensagem));
        return Optional.of(processar(telefone, nomePerfil, textoOriginal));
    }

    private String processar(String telefone, String nomePerfil, String textoOriginal) {
        String texto = textoOriginal == null ? "" : textoOriginal.trim();

        SessaoConversa sessao = sessaoRepository.findByTelefoneWhatsapp(telefone)
                .orElseGet(() -> SessaoConversa.builder()
                        .telefoneWhatsapp(telefone)
                        .nomeCliente(nomePerfil != null && !nomePerfil.isBlank() ? nomePerfil : "Cliente")
                        .build());

        String textoNormalizado = normalizar(texto);

        if (PALAVRAS_REINICIO.contains(textoNormalizado)) {
            sessao.reiniciar();
            salvar(sessao);
            return mensagemBoasVindas(sessao);
        }
        if (textoNormalizado.equals("cancelar")) {
            sessao.reiniciar();
            salvar(sessao);
            return "Pedido cancelado. 👍\n\n" + mensagemMenu();
        }

        String resposta = switch (sessao.getEstado()) {
            case MENU -> tratarMenu(sessao, textoNormalizado);
            case ESCOLHENDO_TAMANHO -> tratarTamanho(sessao, textoNormalizado);
            case ESCOLHENDO_SABORES -> tratarSabores(sessao, textoNormalizado);
            case OBSERVACAO -> tratarObservacao(sessao, texto);
            case PERGUNTANDO_MAIS_ITEM -> tratarMaisItem(sessao, textoNormalizado);
            case ESCOLHENDO_PRODUTO -> tratarProduto(sessao, textoNormalizado);
            case ESCOLHENDO_QUANTIDADE_PRODUTO -> tratarQuantidadeProduto(sessao, textoNormalizado);
            case TIPO_ENTREGA -> tratarTipoEntrega(sessao, textoNormalizado);
            case ENDERECO_BAIRRO -> tratarBairro(sessao, texto);
            case ENDERECO_RUA -> tratarRua(sessao, texto);
            case ENDERECO_NUMERO -> tratarNumero(sessao, texto);
            case FORMA_PAGAMENTO -> tratarFormaPagamento(sessao, textoNormalizado);
            case CONFIRMACAO -> tratarConfirmacao(sessao, texto);
        };

        salvar(sessao);
        return resposta;
    }

    // ---- MENU ----------------------------------------------------------

    private String tratarMenu(SessaoConversa sessao, String texto) {
        return switch (texto) {
            case "1", "fazer pedido", "pedido" -> {
                sessao.setEstado(EstadoConversa.ESCOLHENDO_TAMANHO);
                yield "Show, " + sessao.getNomeCliente() + "! 🍕\n\n" + listarTamanhosTexto();
            }
            case "2", "cardapio" -> mensagemCardapio() + "\n\n" + mensagemMenu();
            case "3", "atendente" -> "Já chamei um atendente humano pra você, só um instante! 🙋";
            default -> "Não entendi. " + mensagemMenu();
        };
    }

    // ---- TAMANHO / SABORES (monta uma pizza) ------------------------------

    private String tratarTamanho(SessaoConversa sessao, String texto) {
        List<Tamanho> tamanhos = tamanhoRepository.findByAtivoTrue();
        Optional<Integer> opcao = parseOpcao(texto);

        if (opcao.isEmpty() || opcao.get() < 1 || opcao.get() > tamanhos.size()) {
            return "Não entendi o tamanho. Manda só o número:\n\n" + listarTamanhosTexto();
        }

        Tamanho escolhido = tamanhos.get(opcao.get() - 1);
        sessao.setTamanhoId(escolhido.getId());
        sessao.getSaboresIds().clear();
        sessao.setEstado(EstadoConversa.ESCOLHENDO_SABORES);
        return "Fechado, " + escolhido.getNome() + "! 🍕\n\n" + listarSaboresTexto(escolhido);
    }

    private String tratarSabores(SessaoConversa sessao, String texto) {
        Tamanho tamanho = tamanhoRepository.findById(sessao.getTamanhoId()).orElseThrow();
        List<Sabor> disponiveis = saborRepository.findByDisponivelTrue();

        List<Integer> opcoes = Arrays.stream(texto.split("[,\\s]+"))
                .filter(s -> !s.isBlank())
                .map(this::parseOpcao)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();

        if (opcoes.isEmpty() || opcoes.stream().anyMatch(i -> i < 1 || i > disponiveis.size())) {
            return "Não entendi os sabores. Manda os números separados por vírgula (ex: 1,3):\n\n"
                    + listarSaboresTexto(tamanho);
        }
        if (opcoes.size() > tamanho.getMaxSabores()) {
            return "O tamanho " + tamanho.getNome() + " aceita no máximo " + tamanho.getMaxSabores()
                    + " sabor(es). Manda de novo:\n\n" + listarSaboresTexto(tamanho);
        }

        List<Long> saboresIds = opcoes.stream().map(i -> disponiveis.get(i - 1).getId()).toList();
        sessao.getSaboresIds().clear();
        sessao.getSaboresIds().addAll(saboresIds);
        sessao.setEstado(EstadoConversa.OBSERVACAO);
        return "Deseja adicionar alguma observação? (ex: \"sem cebola\")\n\nSe não, manda *não*.";
    }

    private String tratarObservacao(SessaoConversa sessao, String texto) {
        String normalizado = normalizar(texto);
        String observacao = normalizado.equals("nao") ? null : texto;

        SessaoConversaItem item = SessaoConversaItem.builder()
                .sessao(sessao)
                .tipo(TipoItem.PIZZA)
                .tamanhoId(sessao.getTamanhoId())
                .saboresIds(new ArrayList<>(sessao.getSaboresIds()))
                .quantidade(1)
                .observacao(observacao)
                .build();
        sessao.getItens().add(item);

        sessao.setTamanhoId(null);
        sessao.getSaboresIds().clear();
        sessao.setEstado(EstadoConversa.PERGUNTANDO_MAIS_ITEM);

        return "Adicionado: " + resumoItemTexto(item) + " ✅\n\n" + mensagemMaisItem();
    }

    // ---- MAIS ITENS (pizza extra, produto avulso, ou fechar) --------------

    private String tratarMaisItem(SessaoConversa sessao, String texto) {
        return switch (texto) {
            case "1", "pizza", "mais uma pizza" -> {
                sessao.setEstado(EstadoConversa.ESCOLHENDO_TAMANHO);
                yield listarTamanhosTexto();
            }
            case "2", "bebida", "produto", "bebida/sobremesa" -> {
                sessao.setEstado(EstadoConversa.ESCOLHENDO_PRODUTO);
                yield listarProdutosTexto();
            }
            case "3", "nao", "finalizar", "fechar", "fechar pedido" -> {
                sessao.setEstado(EstadoConversa.TIPO_ENTREGA);
                yield "Como você quer receber?\n\n1️⃣ Retirada no balcão\n2️⃣ Entrega";
            }
            default -> "Não entendi. " + mensagemMaisItem();
        };
    }

    private String tratarProduto(SessaoConversa sessao, String texto) {
        List<Produto> disponiveis = produtoRepository.findByDisponivelTrue();
        Optional<Integer> opcao = parseOpcao(texto);

        if (opcao.isEmpty() || opcao.get() < 1 || opcao.get() > disponiveis.size()) {
            return "Não entendi. Manda só o número do produto:\n\n" + listarProdutosTexto();
        }

        Produto escolhido = disponiveis.get(opcao.get() - 1);
        sessao.setProdutoIdAtual(escolhido.getId());
        sessao.setEstado(EstadoConversa.ESCOLHENDO_QUANTIDADE_PRODUTO);
        return "Quantas unidades de " + escolhido.getNome() + " você quer?";
    }

    private String tratarQuantidadeProduto(SessaoConversa sessao, String texto) {
        Optional<Integer> quantidade = parseOpcao(texto);
        if (quantidade.isEmpty() || quantidade.get() < 1) {
            return "Manda um número válido de unidades (ex: 2).";
        }

        Produto produto = produtoRepository.findById(sessao.getProdutoIdAtual()).orElseThrow();
        SessaoConversaItem item = SessaoConversaItem.builder()
                .sessao(sessao)
                .tipo(TipoItem.PRODUTO)
                .produtoId(produto.getId())
                .quantidade(quantidade.get())
                .build();
        sessao.getItens().add(item);
        sessao.setProdutoIdAtual(null);
        sessao.setEstado(EstadoConversa.PERGUNTANDO_MAIS_ITEM);

        return "Adicionado: " + resumoItemTexto(item) + " ✅\n\n" + mensagemMaisItem();
    }

    // ---- ENTREGA ---------------------------------------------------------

    private String tratarTipoEntrega(SessaoConversa sessao, String texto) {
        return switch (texto) {
            case "1", "retirada" -> {
                sessao.setTipoEntrega(TipoEntrega.RETIRADA);
                sessao.setEstado(EstadoConversa.FORMA_PAGAMENTO);
                yield mensagemFormaPagamento();
            }
            case "2", "entrega" -> {
                sessao.setTipoEntrega(TipoEntrega.ENTREGA);
                sessao.setEstado(EstadoConversa.ENDERECO_BAIRRO);
                yield "Qual o seu bairro?";
            }
            default -> "Não entendi. Manda 1 pra retirada ou 2 pra entrega.";
        };
    }

    private String tratarBairro(SessaoConversa sessao, String texto) {
        boolean atendido = regiaoFreteRepository.findByBairroIgnoreCaseAndAtivoTrue(texto).isPresent();
        if (!atendido) {
            String bairrosAtendidos = regiaoFreteRepository.findAll().stream()
                    .filter(RegiaoFrete::getAtivo)
                    .map(RegiaoFrete::getBairro)
                    .collect(Collectors.joining(", "));
            return "Ainda não entregamos nesse bairro. Bairros atendidos: " + bairrosAtendidos
                    + ".\n\nQual o seu bairro?";
        }
        sessao.setBairro(texto);
        sessao.setEstado(EstadoConversa.ENDERECO_RUA);
        return "Qual sua rua?";
    }

    private String tratarRua(SessaoConversa sessao, String texto) {
        sessao.setRua(texto);
        sessao.setEstado(EstadoConversa.ENDERECO_NUMERO);
        return "Número da residência?";
    }

    private String tratarNumero(SessaoConversa sessao, String texto) {
        sessao.setNumero(texto);
        sessao.setEstado(EstadoConversa.FORMA_PAGAMENTO);
        return mensagemFormaPagamento();
    }

    // ---- PAGAMENTO -------------------------------------------------------

    private String tratarFormaPagamento(SessaoConversa sessao, String texto) {
        FormaPagamento forma = switch (texto) {
            case "1", "dinheiro" -> FormaPagamento.DINHEIRO;
            case "2", "cartao" -> FormaPagamento.CARTAO;
            case "3", "pix" -> FormaPagamento.PIX;
            default -> null;
        };
        if (forma == null) {
            return "Não entendi. " + mensagemFormaPagamento();
        }
        sessao.setFormaPagamento(forma);
        sessao.setEstado(EstadoConversa.CONFIRMACAO);
        return montarResumo(sessao) + "\n\nConfirma o pedido? 1️⃣ Sim 2️⃣ Cancelar";
    }

    // ---- CONFIRMACAO -----------------------------------------------------

    private String tratarConfirmacao(SessaoConversa sessao, String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.equals("2") || normalizado.equals("nao")) {
            sessao.reiniciar();
            return "Pedido cancelado. 👍\n\n" + mensagemMenu();
        }
        if (!normalizado.equals("1") && !normalizado.equals("sim")) {
            return "Não entendi. Confirma o pedido? 1️⃣ Sim 2️⃣ Cancelar";
        }

        try {
            Pedido pedido = pedidoService.criarPedido(construirRequisicao(sessao));
            sessao.reiniciar();
            return "🔔 Pedido #" + pedido.getId() + " confirmado!\n\n"
                    + "Total: " + formatarPreco(pedido.getValorTotal()) + "\n"
                    + mensagemPosPedido(pedido);
        } catch (RegraNegocioException e) {
            sessao.reiniciar();
            return "Não consegui fechar o pedido: " + e.getMessage() + "\n\n" + mensagemMenu();
        }
    }

    private String mensagemPosPedido(Pedido pedido) {
        if (pedido.getFormaPagamento() == FormaPagamento.PIX) {
            return "Pra confirmar, paga com Pix *copia e cola* abaixo. Assim que cair, já colocamos na produção:\n\n"
                    + pedido.getPixCopiaCola();
        }
        return "Seu pedido já está sendo preparado. Te aviso quando sair! 🍕";
    }

    private PedidoRequestDTO construirRequisicao(SessaoConversa sessao) {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setNomeCliente(sessao.getNomeCliente());
        dto.setTelefoneWhatsapp(sessao.getTelefoneWhatsapp());
        dto.setTipoEntrega(sessao.getTipoEntrega());
        dto.setFormaPagamento(sessao.getFormaPagamento());
        dto.setItens(sessao.getItens().stream().map(this::converterItem).collect(Collectors.toList()));

        if (sessao.getTipoEntrega() == TipoEntrega.ENTREGA) {
            PedidoRequestDTO.EnderecoRequestDTO endereco = new PedidoRequestDTO.EnderecoRequestDTO();
            endereco.setBairro(sessao.getBairro());
            endereco.setRua(sessao.getRua());
            endereco.setNumero(sessao.getNumero());
            dto.setEndereco(endereco);
        }
        return dto;
    }

    private PedidoRequestDTO.ItemPedidoRequestDTO converterItem(SessaoConversaItem item) {
        PedidoRequestDTO.ItemPedidoRequestDTO dto = new PedidoRequestDTO.ItemPedidoRequestDTO();
        dto.setTipo(item.getTipo());
        dto.setTamanhoId(item.getTamanhoId());
        dto.setSaboresIds(item.getSaboresIds());
        dto.setProdutoId(item.getProdutoId());
        dto.setQuantidade(item.getQuantidade());
        dto.setObservacao(item.getObservacao());
        return dto;
    }

    // ---- Textos auxiliares -------------------------------------------

    private String mensagemBoasVindas(SessaoConversa sessao) {
        return "Olá, " + sessao.getNomeCliente() + "! 👋\nSeja bem-vindo à *Art Pizza* 🍕 (Cruz das Almas)\n\n"
                + mensagemMenu();
    }

    private String mensagemMenu() {
        return "1️⃣ Fazer pedido\n2️⃣ Ver cardápio\n3️⃣ Falar com atendente";
    }

    private String mensagemMaisItem() {
        return "Quer adicionar mais alguma coisa?\n\n1️⃣ Mais uma pizza\n2️⃣ Bebida/sobremesa\n3️⃣ Não, fechar pedido";
    }

    private String mensagemCardapio() {
        StringBuilder texto = new StringBuilder("📋 *Cardápio*\n\n");
        tamanhoRepository.findByAtivoTrue().forEach(t ->
                texto.append(t.getNome()).append(" - ").append(formatarPreco(t.getPrecoBase()))
                        .append(" (até ").append(t.getMaxSabores()).append(" sabor(es))\n"));
        texto.append("\nSabores:\n");
        saborRepository.findByDisponivelTrue().forEach(s ->
                texto.append("• ").append(s.getNome())
                        .append(s.getPrecoAdicional().signum() > 0
                                ? " (+" + formatarPreco(s.getPrecoAdicional()) + ")" : "")
                        .append(s.getDescricao() != null && !s.getDescricao().isBlank()
                                ? " — " + s.getDescricao() : "")
                        .append("\n"));
        texto.append("\nBebidas e sobremesas:\n");
        produtoRepository.findByDisponivelTrue().forEach(p ->
                texto.append("• ").append(p.getNome()).append(" - ").append(formatarPreco(p.getPreco())).append("\n"));
        return texto.toString();
    }

    private String listarTamanhosTexto() {
        StringBuilder texto = new StringBuilder("Escolha o tamanho:\n\n");
        List<Tamanho> tamanhos = tamanhoRepository.findByAtivoTrue();
        for (int i = 0; i < tamanhos.size(); i++) {
            Tamanho t = tamanhos.get(i);
            texto.append(i + 1).append("️⃣ ").append(t.getNome())
                    .append(" - ").append(formatarPreco(t.getPrecoBase()))
                    .append(" (até ").append(t.getMaxSabores()).append(" sabor(es))\n");
        }
        return texto.toString();
    }

    private String listarSaboresTexto(Tamanho tamanho) {
        StringBuilder texto = new StringBuilder(
                "Escolha até " + tamanho.getMaxSabores() + " sabor(es) (números separados por vírgula):\n\n");
        List<Sabor> sabores = saborRepository.findByDisponivelTrue();
        for (int i = 0; i < sabores.size(); i++) {
            Sabor s = sabores.get(i);
            texto.append(i + 1).append("️⃣ ").append(s.getNome())
                    .append(s.getPrecoAdicional().signum() > 0
                            ? " (+" + formatarPreco(s.getPrecoAdicional()) + ")" : "")
                    .append("\n");
        }
        return texto.toString();
    }

    private String listarProdutosTexto() {
        StringBuilder texto = new StringBuilder("Escolha o produto:\n\n");
        List<Produto> produtos = produtoRepository.findByDisponivelTrue();
        for (int i = 0; i < produtos.size(); i++) {
            Produto p = produtos.get(i);
            texto.append(i + 1).append("️⃣ ").append(p.getNome())
                    .append(" - ").append(formatarPreco(p.getPreco())).append("\n");
        }
        return texto.toString();
    }

    private String mensagemFormaPagamento() {
        return "Como vai ser o pagamento?\n\n1️⃣ Dinheiro\n2️⃣ Cartão\n3️⃣ PIX";
    }

    private String resumoItemTexto(SessaoConversaItem item) {
        if (item.getTipo() == TipoItem.PIZZA) {
            Tamanho tamanho = tamanhoRepository.findById(item.getTamanhoId()).orElseThrow();
            List<String> sabores = saborRepository.findAllById(item.getSaboresIds()).stream()
                    .map(Sabor::getNome).toList();
            return tamanho.getNome() + " — " + String.join(" + ", sabores);
        }
        Produto produto = produtoRepository.findById(item.getProdutoId()).orElseThrow();
        return item.getQuantidade() + "x " + produto.getNome();
    }

    private String montarResumo(SessaoConversa sessao) {
        StringBuilder texto = new StringBuilder("📝 *Resumo do pedido*\n\n");
        BigDecimal totalItens = BigDecimal.ZERO;

        for (SessaoConversaItem item : sessao.getItens()) {
            BigDecimal precoUnitario = precoUnitarioItem(item);
            BigDecimal precoLinha = precoUnitario.multiply(BigDecimal.valueOf(item.getQuantidade()));
            totalItens = totalItens.add(precoLinha);

            texto.append(resumoItemTexto(item));
            if (item.getObservacao() != null) {
                texto.append(" (obs: ").append(item.getObservacao()).append(")");
            }
            texto.append(" — ").append(formatarPreco(precoLinha)).append("\n");
        }
        texto.append("\n");

        BigDecimal frete = BigDecimal.ZERO;
        if (sessao.getTipoEntrega() == TipoEntrega.ENTREGA) {
            frete = regiaoFreteRepository.findByBairroIgnoreCaseAndAtivoTrue(sessao.getBairro())
                    .map(RegiaoFrete::getValorFrete).orElse(BigDecimal.ZERO);
            texto.append("Entrega: ").append(sessao.getRua()).append(", ").append(sessao.getNumero())
                    .append(" — ").append(sessao.getBairro()).append("\n");
        } else {
            texto.append("Retirada no balcão\n");
        }

        texto.append("Pagamento: ").append(descreverFormaPagamento(sessao.getFormaPagamento())).append("\n\n");
        if (frete.signum() > 0) {
            texto.append("Frete: ").append(formatarPreco(frete)).append("\n");
        }
        texto.append("*Total: ").append(formatarPreco(totalItens.add(frete))).append("*");
        return texto.toString();
    }

    private BigDecimal precoUnitarioItem(SessaoConversaItem item) {
        if (item.getTipo() == TipoItem.PIZZA) {
            Tamanho tamanho = tamanhoRepository.findById(item.getTamanhoId()).orElseThrow();
            BigDecimal adicionalSabores = saborRepository.findAllById(item.getSaboresIds()).stream()
                    .map(Sabor::getPrecoAdicional).reduce(BigDecimal.ZERO, BigDecimal::add);
            return tamanho.getPrecoBase().add(adicionalSabores);
        }
        return produtoRepository.findById(item.getProdutoId()).orElseThrow().getPreco();
    }

    private String descreverFormaPagamento(FormaPagamento forma) {
        return switch (forma) {
            case DINHEIRO -> "Dinheiro";
            case CARTAO -> "Cartão";
            case PIX -> "PIX";
        };
    }

    private Optional<Integer> parseOpcao(String texto) {
        try {
            return Optional.of(Integer.parseInt(texto.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String normalizar(String texto) {
        String semAcento = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.of("pt", "BR")).trim();
    }

    private String formatarPreco(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        return formato.format(valor);
    }

    private void salvar(SessaoConversa sessao) {
        sessao.setAtualizadoEm(LocalDateTime.now());
        sessaoRepository.save(sessao);
    }
}
