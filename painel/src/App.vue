<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import TopBar from './components/TopBar.vue'
import KanbanBoard from './components/KanbanBoard.vue'
import FaturamentoPanel from './components/FaturamentoPanel.vue'
import { listarPedidosAtivos, atualizarStatusPedido, confirmarPagamentoPedido, buscarFaturamento } from './api.js'
import { tocarAlertaNovoPedido } from './sound.js'
import { demoMode, demo, setCredentials, clearCredentials } from './api.js'
const authenticated = ref(demoMode)
const username = ref('')
const password = ref('')
const loginError = ref('')
const loginBusy = ref(false)
async function login() {
  loginBusy.value = true; loginError.value = ''
  setCredentials(username.value, password.value)
  try {
    await listarPedidosAtivos()
    password.value = ''; authenticated.value = true
    await buscarPedidos(); await carregarFaturamento()
  } catch (e) { clearCredentials(); loginError.value = e.message }
  finally { loginBusy.value = false }
}
function logout() { clearCredentials(); authenticated.value = false; pedidos.value = []; resumoFaturamento.value = null }
async function demoAction(reset = false) { reset ? demo.reset() : demo.create(); await buscarPedidos(); await carregarFaturamento() }

const pedidos = ref([])
const idsNovos = ref(new Set())
const somAtivo = ref(false)
const erro = ref(null)
const abaAtiva = ref('pedidos')
const resumoFaturamento = ref(null)
const carregandoFaturamento = ref(false)
const dataFaturamento = ref(new Date().toLocaleDateString('en-CA'))

let idsConhecidos = new Set()
let temporizador = null

async function buscarPedidos() {
  try {
    const resultado = await listarPedidosAtivos()
    erro.value = null

    const idsAtuais = new Set(resultado.map((p) => p.id))
    const novos = new Set([...idsAtuais].filter((id) => !idsConhecidos.has(id)))

    // Só toca alerta e destaca "novo" a partir da segunda busca — na primeira
    // carga, tudo que já existe não deve ser tratado como pedido recém-chegado.
    if (idsConhecidos.size > 0 && novos.size > 0) {
      idsNovos.value = novos
      if (somAtivo.value) tocarAlertaNovoPedido()
      setTimeout(() => {
        idsNovos.value = new Set()
      }, 3500)
    }

    idsConhecidos = idsAtuais
    pedidos.value = resultado
  } catch (e) {
    erro.value = e.message
  }
}

async function avancarStatus(id, novoStatus) {
  try {
    const pedido = pedidos.value.find((pedido) => pedido.id === id)
    if (pedido?.status === 'AGUARDANDO_PAGAMENTO' && novoStatus === 'NOVO') {
      await confirmarPagamentoPedido(id)
    } else {
      await atualizarStatusPedido(id, novoStatus)
    }
    await buscarPedidos()
    await carregarFaturamento()
  } catch (e) {
    erro.value = e.message
  }
}

async function carregarFaturamento() {
  carregandoFaturamento.value = true
  try {
    resumoFaturamento.value = await buscarFaturamento(dataFaturamento.value)
    erro.value = null
  } catch (e) {
    erro.value = e.message
  } finally {
    carregandoFaturamento.value = false
  }
}

async function mudarAba(aba) {
  abaAtiva.value = aba
  if (aba === 'faturamento') await carregarFaturamento()
}

onMounted(() => {
  if (authenticated.value) { buscarPedidos(); carregarFaturamento() }
  temporizador = setInterval(() => { if (authenticated.value) buscarPedidos() }, 5000)
})

onUnmounted(() => {
  if (temporizador) clearInterval(temporizador)
})
</script>

<template>
  <section v-if="!authenticated" class="login-page">
    <form @submit.prevent="login">
      <p class="login-eyebrow">ART PIZZA · OPERAÇÃO</p>
      <h1>Entre no painel</h1>
      <p>Pedidos, produção e vendas em um só lugar.</p>
      <label for="username">Usuário</label><input id="username" v-model="username" autocomplete="username" required />
      <label for="password">Senha</label><input id="password" v-model="password" type="password" autocomplete="current-password" required />
      <p v-if="loginError" role="alert">{{ loginError }}</p>
      <button :disabled="loginBusy">{{ loginBusy ? 'Entrando…' : 'Entrar' }}</button>
    </form>
  </section>
  <template v-else>
  <div v-if="demoMode" class="demo-banner">
    <div><strong>Demonstração interativa</strong><span>Dados fictícios salvos apenas neste navegador. Nenhum pedido, mensagem ou pagamento real.</span></div>
    <div class="demo-actions"><button @click="demoAction()">+ Simular pedido</button><button @click="demoAction(true)">Reiniciar demo</button><a href="https://github.com/FernandoTorresD97/automatizacao-artpizza" target="_blank" rel="noopener">Ver código ↗</a></div>
  </div>
  <div v-else class="session-bar"><span>Sessão administrativa</span><button @click="logout">Sair</button></div>
  <TopBar
    :som-ativo="somAtivo"
    :total-pedidos="pedidos.length"
    :aba-ativa="abaAtiva"
    @alternar-som="somAtivo = !somAtivo"
    @mudar-aba="mudarAba"
  />

  <p v-if="erro" class="aviso-erro">Não foi possível atualizar os pedidos: {{ erro }}</p>

  <KanbanBoard v-if="abaAtiva === 'pedidos'" :pedidos="pedidos" :ids-novos="idsNovos" @avancar="avancarStatus" />
  <FaturamentoPanel
    v-else
    :resumo="resumoFaturamento"
    :data="dataFaturamento"
    :carregando="carregandoFaturamento"
    @mudar-data="dataFaturamento = $event"
    @atualizar="carregarFaturamento"
  />
  </template>
</template>

<style scoped>
.aviso-erro {
  margin: 0;
  padding: 0.6rem 1.75rem;
  background: rgba(217, 106, 74, 0.15);
  color: var(--erro);
  font-size: 0.85rem;
  border-bottom: 1px solid var(--linha);
}
</style>
