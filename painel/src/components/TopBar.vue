<script setup>
defineProps({
  somAtivo: { type: Boolean, required: true },
  totalPedidos: { type: Number, required: true },
  abaAtiva: { type: String, required: true }
})

defineEmits(['alternar-som', 'mudar-aba'])
</script>

<template>
  <header class="topo">
    <div class="marca">
      <span class="marca-selo">A</span>
      <span class="marca-nome">ART PIZZA</span>
      <span class="marca-sub">Painel de pedidos</span>
    </div>

    <div class="topo-acoes">
      <nav class="abas" aria-label="Seções do painel">
        <button :class="{ ativa: abaAtiva === 'pedidos' }" @click="$emit('mudar-aba', 'pedidos')">Pedidos</button>
        <button :class="{ ativa: abaAtiva === 'faturamento' }" @click="$emit('mudar-aba', 'faturamento')">Faturamento</button>
      </nav>
      <span class="contagem">{{ totalPedidos }} pedido{{ totalPedidos === 1 ? '' : 's' }} em andamento</span>
      <button class="botao-som" :class="{ ativo: somAtivo }" @click="$emit('alternar-som')">
        {{ somAtivo ? 'Som ligado' : 'Som desligado' }}
      </button>
    </div>
  </header>
</template>

<style scoped>
.topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.1rem 1.75rem;
  border-bottom: 1px solid var(--linha);
  flex-wrap: wrap;
}

.marca {
  display: flex;
  align-items: baseline;
  gap: 0.65rem;
}

.marca-selo {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 2px solid var(--dourado);
  color: var(--dourado);
  font-family: var(--fonte-marca);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
}

.marca-nome {
  font-family: var(--fonte-marca);
  font-size: 1.35rem;
  letter-spacing: 0.04em;
  color: var(--branco);
}

.marca-sub {
  color: var(--cinza-texto);
  font-size: 0.85rem;
}

.topo-acoes {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.abas {
  display: flex;
  gap: 0.3rem;
  padding: 0.2rem;
  border: 1px solid var(--linha);
  border-radius: 5px;
}

.abas button {
  border: 0;
  border-radius: 3px;
  padding: 0.4rem 0.65rem;
  color: var(--cinza-texto);
  background: transparent;
  cursor: pointer;
  font-size: 0.8rem;
}

.abas button.ativa {
  background: var(--dourado);
  color: var(--preto);
  font-weight: 700;
}

.contagem {
  color: var(--cinza-texto);
  font-size: 0.9rem;
}

.botao-som {
  background: transparent;
  border: 1px solid var(--linha);
  color: var(--branco);
  padding: 0.5rem 0.9rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.botao-som.ativo {
  border-color: var(--dourado);
  color: var(--dourado);
}

.botao-som:hover {
  border-color: var(--dourado-claro);
  color: var(--dourado-claro);
}
</style>
