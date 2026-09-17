const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const ADMIN_USERNAME = import.meta.env.VITE_ADMIN_USERNAME
const ADMIN_PASSWORD = import.meta.env.VITE_ADMIN_PASSWORD

function authHeader() {
  if (!ADMIN_USERNAME || !ADMIN_PASSWORD) return {}
  return { Authorization: `Basic ${btoa(`${ADMIN_USERNAME}:${ADMIN_PASSWORD}`)}` }
}

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...authHeader(), ...options.headers },
    ...options
  })

  if (!response.ok) {
    const corpo = await response.json().catch(() => null)
    throw new Error(corpo?.erro || `Erro ${response.status} ao chamar ${path}`)
  }

  if (response.status === 204) return null
  return response.json()
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
