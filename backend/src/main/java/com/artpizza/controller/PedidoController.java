package com.artpizza.controller;

import com.artpizza.dto.AtualizarStatusDTO;
import com.artpizza.dto.PedidoRequestDTO;
import com.artpizza.dto.PedidoResponseDTO;
import com.artpizza.model.Pedido;
import com.artpizza.model.StatusPedido;
import com.artpizza.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    // Status que o painel mostra por padrão quando nenhum filtro é passado
    // (esconde ENTREGUE/CANCELADO do quadro ativo).
    private static final List<StatusPedido> STATUS_ATIVOS = List.of(
            StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.NOVO, StatusPedido.CONFIRMADO,
            StatusPedido.EM_PREPARO, StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA
    );

    private final PedidoService pedidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponseDTO criar(@Valid @RequestBody PedidoRequestDTO dto) {
        Pedido pedido = pedidoService.criarPedido(dto);
        return PedidoResponseDTO.from(pedido);
    }

    @GetMapping("/{id}")
    public PedidoResponseDTO buscar(@PathVariable Long id) {
        return PedidoResponseDTO.from(pedidoService.buscarPorId(id));
    }

    // Alimenta o painel administrativo. Ex: GET /api/pedidos?status=EM_PREPARO,PRONTO
    @GetMapping
    public List<PedidoResponseDTO> listar(@RequestParam(required = false) String status) {
        List<StatusPedido> filtro = status == null
                ? STATUS_ATIVOS
                : Arrays.stream(status.split(",")).map(StatusPedido::valueOf).collect(Collectors.toList());

        return pedidoService.listarPorStatus(filtro).stream()
                .map(PedidoResponseDTO::from)
                .collect(Collectors.toList());
    }

    @PatchMapping("/{id}/status")
    public PedidoResponseDTO atualizarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusDTO dto) {
        Pedido pedido = pedidoService.atualizarStatus(id, dto.getStatus());
        return PedidoResponseDTO.from(pedido);
    }

    // Usado pelo painel quando o operador confere que o Pix caiu na conta.
    @PatchMapping("/{id}/confirmar-pagamento")
    public PedidoResponseDTO confirmarPagamento(@PathVariable Long id) {
        Pedido pedido = pedidoService.confirmarPagamento(id);
        return PedidoResponseDTO.from(pedido);
    }
}
