package com.artpizza.service;

import com.artpizza.dto.PedidoRequestDTO;
import com.artpizza.exception.RecursoNaoEncontradoException;
import com.artpizza.exception.RegraNegocioException;
import com.artpizza.model.*;
import com.artpizza.pagamento.PixService;
import com.artpizza.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PedidoService {

    // Status a partir dos quais ainda dá pra cancelar um pedido.
    private static final Set<StatusPedido> CANCELAVEL_ATE = Set.of(
            StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.NOVO,
            StatusPedido.CONFIRMADO, StatusPedido.EM_PREPARO
    );
    private static final Map<StatusPedido, StatusPedido> PROXIMO_STATUS = Map.of(
            StatusPedido.NOVO, StatusPedido.CONFIRMADO,
            StatusPedido.CONFIRMADO, StatusPedido.EM_PREPARO,
            StatusPedido.EM_PREPARO, StatusPedido.PRONTO,
            StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA,
            StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.ENTREGUE
    );

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final TamanhoRepository tamanhoRepository;
    private final SaborRepository saborRepository;
    private final ProdutoRepository produtoRepository;
    private final FreteService freteService;
    private final PixService pixService;

    @Transactional
    public Pedido criarPedido(PedidoRequestDTO dto) {
        Cliente cliente = clienteRepository.findByTelefoneWhatsapp(dto.getTelefoneWhatsapp())
                .orElseGet(() -> clienteRepository.save(
                        Cliente.builder()
                                .nome(dto.getNomeCliente())
                                .telefoneWhatsapp(dto.getTelefoneWhatsapp())
                                .build()));

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .tipoEntrega(dto.getTipoEntrega())
                .formaPagamento(dto.getFormaPagamento())
                .status(dto.getFormaPagamento() == FormaPagamento.PIX
                        ? StatusPedido.AGUARDANDO_PAGAMENTO
                        : StatusPedido.NOVO)
                .build();

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal totalItens = BigDecimal.ZERO;

        for (PedidoRequestDTO.ItemPedidoRequestDTO itemDto : dto.getItens()) {
            ItemPedido item = montarItem(pedido, itemDto);
            itens.add(item);
            totalItens = totalItens.add(item.getPrecoTotal());
        }
        pedido.setItens(itens);

        BigDecimal valorFrete = BigDecimal.ZERO;
        if (dto.getTipoEntrega() == TipoEntrega.ENTREGA) {
            if (dto.getEndereco() == null) {
                throw new RegraNegocioException("Endereço é obrigatório quando o tipo de entrega é ENTREGA.");
            }
            Endereco endereco = enderecoRepository.save(
                    Endereco.builder()
                            .cliente(cliente)
                            .bairro(dto.getEndereco().getBairro())
                            .rua(dto.getEndereco().getRua())
                            .numero(dto.getEndereco().getNumero())
                            .complemento(dto.getEndereco().getComplemento())
                            .build());
            pedido.setEndereco(endereco);
            valorFrete = freteService.calcularFrete(dto.getEndereco().getBairro());
        }
        pedido.setValorFrete(valorFrete);
        pedido.setValorTotal(totalItens.add(valorFrete));

        pedido = pedidoRepository.save(pedido);

        if (dto.getFormaPagamento() == FormaPagamento.PIX) {
            String txid = "PED" + pedido.getId();
            pedido.setPixTxId(txid);
            pedido.setPixCopiaCola(pixService.gerarPayload(pedido.getValorTotal(), txid));
            pedido = pedidoRepository.save(pedido);
        }

        return pedido;
    }

    private ItemPedido montarItem(Pedido pedido, PedidoRequestDTO.ItemPedidoRequestDTO itemDto) {
        int quantidade = itemDto.getQuantidade() == null ? 1 : itemDto.getQuantidade();
        if (quantidade < 1) {
            throw new RegraNegocioException("A quantidade de um item precisa ser pelo menos 1.");
        }

        if (itemDto.getTipo() == TipoItem.PRODUTO) {
            return montarItemProduto(pedido, itemDto, quantidade);
        }
        return montarItemPizza(pedido, itemDto, quantidade);
    }

    private ItemPedido montarItemPizza(Pedido pedido, PedidoRequestDTO.ItemPedidoRequestDTO itemDto, int quantidade) {
        if (itemDto.getTamanhoId() == null || itemDto.getSaboresIds() == null || itemDto.getSaboresIds().isEmpty()) {
            throw new RegraNegocioException("Um item do tipo PIZZA precisa de tamanho e pelo menos um sabor.");
        }

        Tamanho tamanho = tamanhoRepository.findById(itemDto.getTamanhoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tamanho não encontrado: " + itemDto.getTamanhoId()));

        List<Sabor> sabores = saborRepository.findAllById(itemDto.getSaboresIds());
        if (sabores.size() != itemDto.getSaboresIds().size()) {
            throw new RecursoNaoEncontradoException("Um ou mais sabores informados não existem.");
        }
        if (sabores.size() > tamanho.getMaxSabores()) {
            throw new RegraNegocioException(
                    "O tamanho '" + tamanho.getNome() + "' aceita no máximo " + tamanho.getMaxSabores()
                            + " sabor(es), mas foram enviados " + sabores.size() + ".");
        }
        boolean algumIndisponivel = sabores.stream().anyMatch(s -> !Boolean.TRUE.equals(s.getDisponivel()));
        if (algumIndisponivel) {
            throw new RegraNegocioException("Um dos sabores escolhidos não está disponível no momento.");
        }

        BigDecimal adicionalSabores = sabores.stream()
                .map(Sabor::getPrecoAdicional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ItemPedido.builder()
                .pedido(pedido)
                .tipo(TipoItem.PIZZA)
                .tamanho(tamanho)
                .sabores(sabores)
                .quantidade(quantidade)
                .observacao(itemDto.getObservacao())
                .precoUnitario(tamanho.getPrecoBase().add(adicionalSabores))
                .build();
    }

    private ItemPedido montarItemProduto(Pedido pedido, PedidoRequestDTO.ItemPedidoRequestDTO itemDto, int quantidade) {
        if (itemDto.getProdutoId() == null) {
            throw new RegraNegocioException("Um item do tipo PRODUTO precisa informar o produto.");
        }
        Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + itemDto.getProdutoId()));
        if (!Boolean.TRUE.equals(produto.getDisponivel())) {
            throw new RegraNegocioException("O produto '" + produto.getNome() + "' não está disponível no momento.");
        }

        return ItemPedido.builder()
                .pedido(pedido)
                .tipo(TipoItem.PRODUTO)
                .produto(produto)
                .quantidade(quantidade)
                .observacao(itemDto.getObservacao())
                .precoUnitario(produto.getPreco())
                .build();
    }

    public List<Pedido> listarPorStatus(List<StatusPedido> status) {
        return pedidoRepository.findByStatusInOrderByCriadoEmAsc(status);
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    @Transactional
    public Pedido atualizarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);

        if (novoStatus == StatusPedido.CANCELADO) {
            if (!CANCELAVEL_ATE.contains(pedido.getStatus())) {
                throw new RegraNegocioException(
                        "Pedido no status " + pedido.getStatus() + " não pode mais ser cancelado.");
            }
        } else if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("Use a confirmação de pagamento para liberar um pedido PIX.");
        } else if (PROXIMO_STATUS.get(pedido.getStatus()) != novoStatus) {
            throw new RegraNegocioException("Transição inválida: " + pedido.getStatus() + " -> " + novoStatus + ".");
        }

        pedido.setStatus(novoStatus);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

    // Confirma manualmente que o Pix caiu (ou que o pagamento foi recebido de
    // outra forma) e libera o pedido pra cozinha. Ver com.artpizza.pagamento.PixService
    // para o porquê disso não ser automático nesta versão.
    @Transactional
    public Pedido confirmarPagamento(Long id) {
        Pedido pedido = buscarPorId(id);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException(
                    "Pedido #" + id + " não está aguardando pagamento (status atual: " + pedido.getStatus() + ").");
        }
        pedido.setStatus(StatusPedido.NOVO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido confirmarPagamentoPorTxid(String txid) {
        Pedido pedido = pedidoRepository.findByPixTxId(txid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum pedido encontrado para o txid " + txid));
        return confirmarPagamento(pedido.getId());
    }
}
