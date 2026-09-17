<script setup>
import { computed } from 'vue'

const props = defineProps({
  resumo: { type: Object, default: null },
  data: { type: String, required: true },
  carregando: { type: Boolean, default: false }
})

defineEmits(['mudar-data', 'atualizar'])

const moeda = (valor) => Number(valor || 0).toLocaleString('pt-BR', {
  style: 'currency', currency: 'BRL'
})

const dataLegivel = computed(() => props.data.split('-').reverse().join('/'))
const nomeForma = { PIX: 'PIX', CARTAO: 'Cartão', DINHEIRO: 'Dinheiro' }
</script>

<template>
  <main class="faturamento">
    <section class="faturamento-cabecalho">
      <div>
        <p class="sobretitulo">Resumo financeiro</p>
        <h1>Faturamento da noite</h1>
        <p class="ajuda">Considera somente pedidos confirmados ou já em produção. PIX pendente e cancelados ficam de fora.</p>
      </div>
      <div class="filtro-data">
        <label for="data-faturamento">Data</label>
        <input id="data-faturamento" :value="data" type="date" @input="$emit('mudar-data', $event.target.value)" />
        <button @click="$emit('atualizar')">Atualizar</button>
      </div>
    </section>

    <p v-if="carregando" class="carregando">Atualizando faturamento…</p>
    <template v-else-if="resumo">
      <section class="kpis">
        <article class="kpi destaque"><span>Faturamento total</span><strong>{{ moeda(resumo.valorTotal) }}</strong><small>{{ resumo.quantidadePedidos }} pedido(s) faturado(s)</small></article>
        <article class="kpi"><span>Ticket médio</span><strong>{{ moeda(resumo.ticketMedio) }}</strong><small>por pedido confirmado</small></article>
        <article class="kpi"><span>Valor dos itens</span><strong>{{ moeda(resumo.valorItens) }}</strong><small>produtos e pizzas</small></article>
        <article class="kpi"><span>Frete arrecadado</span><strong>{{ moeda(resumo.valorFrete) }}</strong><small>entregas em {{ dataLegivel }}</small></article>
      </section>

      <section class="formas">
        <h2>Por forma de pagamento</h2>
        <div class="formas-lista">
          <article v-for="forma in resumo.porFormaPagamento" :key="forma.formaPagamento" class="forma">
            <span>{{ nomeForma[forma.formaPagamento] }}</span>
            <strong>{{ moeda(forma.valorTotal) }}</strong>
            <small>{{ forma.quantidade }} pedido(s)</small>
          </article>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.faturamento { padding: 2rem 1.75rem; max-width: 1120px; width: 100%; margin: 0 auto; }
.faturamento-cabecalho { display: flex; justify-content: space-between; gap: 1.5rem; align-items: end; margin-bottom: 2rem; flex-wrap: wrap; }
.sobretitulo { color: var(--dourado); text-transform: uppercase; letter-spacing: .1em; font-size: .72rem; font-weight: 700; margin: 0 0 .35rem; }
h1, h2 { font-family: var(--fonte-marca); margin: 0; }
h1 { font-size: 1.65rem; } h2 { font-size: 1rem; }
.ajuda { color: var(--cinza-texto); margin: .5rem 0 0; font-size: .9rem; max-width: 620px; }
.filtro-data { display: flex; align-items: end; gap: .5rem; } .filtro-data label { font-size: .75rem; color: var(--cinza-texto); display: flex; flex-direction: column; gap: .3rem; }
input, button { font: inherit; } input { background: var(--preto-card); color: var(--branco); border: 1px solid var(--linha); padding: .5rem; border-radius: 4px; }
button { background: var(--dourado); color: var(--preto); border: 0; border-radius: 4px; padding: .55rem .8rem; cursor: pointer; font-weight: 700; }
.kpis { display: grid; grid-template-columns: repeat(4, minmax(160px, 1fr)); gap: 1rem; }
.kpi, .forma { background: var(--preto-card); border: 1px solid var(--linha); border-radius: 5px; padding: 1rem; display: flex; flex-direction: column; gap: .4rem; }
.kpi.destaque { border-color: var(--dourado); } .kpi span, .forma span { color: var(--cinza-texto); font-size: .8rem; } .kpi strong { font-size: 1.35rem; color: var(--branco); } .kpi.destaque strong { color: var(--dourado); }
small { color: var(--cinza-texto); font-size: .76rem; }.formas { margin-top: 2rem; }.formas-lista { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-top: .8rem; }.forma strong { font-size: 1.1rem; }
.carregando { color: var(--cinza-texto); } @media (max-width: 760px) { .kpis, .formas-lista { grid-template-columns: 1fr 1fr; } } @media (max-width: 460px) { .kpis, .formas-lista { grid-template-columns: 1fr; } }
</style>
