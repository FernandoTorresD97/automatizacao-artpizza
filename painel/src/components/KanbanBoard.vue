<script setup>
import { STATUS_QUADRO, PROXIMO_STATUS } from '../api.js'
import OrderCard from './OrderCard.vue'

const props = defineProps({
  pedidos: { type: Array, required: true },
  idsNovos: { type: Set, required: true }
})

defineEmits(['avancar'])

const rotuloProximo = {
  NOVO: 'Confirmar pagamento',
  CONFIRMADO: 'Confirmar',
  EM_PREPARO: 'Ir pro forno',
  PRONTO: 'Marcar pronto',
  SAIU_PARA_ENTREGA: 'Saiu p/ entrega',
  ENTREGUE: 'Marcar entregue'
}

function pedidosDoStatus(chave) {
  return props.pedidos.filter((p) => p.status === chave)
}
</script>

<template>
  <div class="quadro">
    <section v-for="coluna in STATUS_QUADRO" :key="coluna.chave" class="coluna">
      <header class="coluna-cabecalho">
        <h2>{{ coluna.titulo }}</h2>
        <span class="contador">{{ pedidosDoStatus(coluna.chave).length }}</span>
      </header>

      <div class="coluna-lista">
        <OrderCard
          v-for="pedido in pedidosDoStatus(coluna.chave)"
          :key="pedido.id"
          :pedido="pedido"
          :eh-novo="idsNovos.has(pedido.id)"
          :proximo-status-label="rotuloProximo[PROXIMO_STATUS[pedido.status]]"
          @avancar="(id) => $emit('avancar', id, PROXIMO_STATUS[pedido.status])"
        />

        <p v-if="pedidosDoStatus(coluna.chave).length === 0" class="coluna-vazia">Nada por aqui</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.quadro {
  flex: 1;
  display: flex;
  gap: 1.25rem;
  padding: 1.5rem 1.75rem 2rem;
  overflow-x: auto;
}

.coluna {
  flex: 1 0 260px;
  min-width: 260px;
  display: flex;
  flex-direction: column;
}

.coluna-cabecalho {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-bottom: 0.6rem;
  border-bottom: 2px solid var(--dourado);
  margin-bottom: 0.9rem;
}

.coluna-cabecalho h2 {
  font-family: var(--fonte-marca);
  font-size: 0.85rem;
  letter-spacing: 0.03em;
  margin: 0;
  color: var(--branco);
}

.contador {
  color: var(--preto);
  background: var(--dourado);
  border-radius: 10px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.05rem 0.45rem;
}

.coluna-lista {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
  overflow-y: auto;
}

.coluna-vazia {
  color: var(--cinza-texto);
  font-size: 0.82rem;
  font-style: italic;
}
</style>
