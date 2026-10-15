import { createDemoStore } from './demo.js'
export const demoMode = import.meta.env.VITE_DEMO_MODE === 'true'
export const demo = demoMode ? createDemoStore(window.localStorage) : null
const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
let authorization = ''
export function setCredentials(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  authorization = `Basic ${btoa(String.fromCharCode(...bytes))}`
}
export function clearCredentials() { authorization = '' }
async function request(path, options = {}) {
  if (demo) return demo.request(path, options)
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(authorization ? { Authorization: authorization } : {}), ...options.headers }
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.erro || (response.status === 401 ? 'Usuário ou senha inválidos.' : `Erro ${response.status} ao chamar ${path}`))
  }
  return response.status === 204 ? null : response.json()
}

// Status que compõem o quadro ativo do painel, na ordem das colunas.
export const STATUS_QUADRO = [
  { chave: 'AGUARDANDO_PAGAMENTO', titulo: 'Aguard. pagamento' },
  { chave: 'NOVO', titulo: 'Novo' },
  { chave: 'CONFIRMADO', titulo: 'Confirmado' },
  { chave: 'EM_PREPARO', titulo: 'Em preparo' },
  { chave: 'PRONTO', titulo: 'Pronto' },
  { chave: 'SAIU_PARA_ENTREGA', titulo: 'Saiu p/ entrega' }
]

// Próximo status sugerido ao clicar em "avançar" num card.
export const PROXIMO_STATUS = {
  AGUARDANDO_PAGAMENTO: 'NOVO',
  NOVO: 'CONFIRMADO',
  CONFIRMADO: 'EM_PREPARO',
  EM_PREPARO: 'PRONTO',
  PRONTO: 'SAIU_PARA_ENTREGA',
  SAIU_PARA_ENTREGA: 'ENTREGUE'
}

export function listarPedidosAtivos() {
  const status = STATUS_QUADRO.map((s) => s.chave).join(',')
  return request(`/api/pedidos?status=${status}`)
}

export function atualizarStatusPedido(id, status) {
  return request(`/api/pedidos/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status })
  })
}

export function confirmarPagamentoPedido(id) {
  return request(`/api/pedidos/${id}/confirmar-pagamento`, { method: 'PATCH' })
}

export function buscarFaturamento(data) {
  return request(`/api/faturamento?data=${data}`)
}
