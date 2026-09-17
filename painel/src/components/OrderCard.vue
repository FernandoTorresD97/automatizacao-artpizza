<script setup>
import { computed } from 'vue'

const props = defineProps({
  pedido: { type: Object, required: true },
  ehNovo: { type: Boolean, default: false },
  proximoStatusLabel: { type: String, default: '' }
})

defineEmits(['avancar'])

const iconePagamento = computed(() => {
  return { DINHEIRO: '💵', CARTAO: '💳', PIX: '📱' }[props.pedido.formaPagamento] || ''
})

const icioneEntrega = computed(() => (props.pedido.tipoEntrega === 'ENTREGA' ? '🛵' : '🚶'))

const resumoItens = computed(() =>
  props.pedido.itens
    .map((item) =>
      item.tipo === 'PIZZA'
        ? `${item.nome} — ${item.sabores.join(' + ')}`
        : `${item.quantidade}x ${item.nome}`
    )
    .join('; ')
)

const horario = computed(() => {
  const data = new Date(props.pedido.criadoEm)
  return data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
})

const valorFormatado = computed(() =>
  props.pedido.valorTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
)
</script>

<template>
  <article class="card" :class="{ novo: ehNovo }">
    <div class="card-topo">
      <span class="numero">#{{ pedido.id }}</span>
      <span class="horario">{{ horario }}</span>
    </div>

    <p class="cliente">{{ pedido.nomeCliente }}</p>
    <p class="itens">{{ resumoItens }}</p>

    <div class="rodape">
      <span class="entrega" :title="pedido.tipoEntrega === 'ENTREGA' ? pedido.bairroEntrega : 'Retirada'">
        {{ icioneEntrega }} {{ pedido.tipoEntrega === 'ENTREGA' ? pedido.bairroEntrega : 'Retirada' }}
      </span>
      <span class="pagamento">{{ iconePagamento }} {{ valorFormatado }}</span>
    </div>

    <button v-if="proximoStatusLabel" class="botao-avancar" @click="$emit('avancar', pedido.id)">
      {{ proximoStatusLabel }} →
    </button>
  </article>
</template>

<style scoped>
.card {
  background: var(--preto-card);
  border-left: 3px solid var(--linha);
  border-radius: 3px;
  padding: 0.85rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.card.novo {
  animation: piscar 1.1s ease-in-out 3;
}

@keyframes piscar {
  0%,
  100% {
    border-left-color: var(--linha);
    background: var(--preto-card);
  }
  50% {
    border-left-color: var(--dourado);
    background: var(--preto-hover);
  }
}

.card-topo {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.numero {
  font-family: var(--fonte-marca);
  font-size: 0.95rem;
  color: var(--dourado);
}

.horario {
  color: var(--cinza-texto);
  font-size: 0.75rem;
}

.cliente {
  margin: 0;
  font-weight: 600;
  font-size: 0.95rem;
}

.itens {
  margin: 0;
  color: var(--cinza-texto);
  font-size: 0.85rem;
  line-height: 1.4;
}

.rodape {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: var(--branco);
  margin-top: 0.2rem;
}

.botao-avancar {
  margin-top: 0.4rem;
  background: transparent;
  border: 1px solid var(--dourado);
  color: var(--dourado);
  padding: 0.4rem 0.6rem;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 600;
  align-self: flex-start;
  transition: background 0.15s ease, color 0.15s ease;
}

.botao-avancar:hover {
  background: var(--dourado);
  color: var(--preto);
}
</style>
