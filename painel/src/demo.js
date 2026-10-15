// Demonstração local: não acessa API, WhatsApp ou pagamentos reais.
export function createDemoStore(storage, clock = () => new Date()) {
  const key = 'artpizza-demo-v1'
  const copy = value => JSON.parse(JSON.stringify(value))
  const next = { NOVO: 'CONFIRMADO', CONFIRMADO: 'EM_PREPARO', EM_PREPARO: 'PRONTO', PRONTO: 'SAIU_PARA_ENTREGA', SAIU_PARA_ENTREGA: 'ENTREGUE' }
  let orders
  try { orders = JSON.parse(storage.getItem(key)); if (!Array.isArray(orders)) orders = null } catch { orders = null }
  const save = () => { try { storage.setItem(key, JSON.stringify(orders)) } catch { /* Navegação privada: permanece em memória. */ } }
  const make = (id, status = 'NOVO') => ({
    id, nomeCliente: `Cliente demonstração ${id}`, telefoneWhatsapp: '00000000000',
    status, formaPagamento: status === 'AGUARDANDO_PAGAMENTO' ? 'PIX' : 'DINHEIRO',
    tipoEntrega: 'ENTREGA', bairroEntrega: 'Bairro fictício',
    valorFrete: 5, valorTotal: 50.99, criadoEm: clock().toISOString(), pixCopiaCola: null,
    itens: [{tipo: 'PIZZA', nome: 'Grande', sabores: ['Calabresa', '4 Queijos'], quantidade: 1, precoUnitario: 45.99, precoTotal: 45.99}]
  })
  function reset() { orders = ['AGUARDANDO_PAGAMENTO', 'NOVO', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO'].map((s, i) => make(i + 1, s)); save() }
  if (!orders) reset()
  function update(id, status, payment = false) {
    const order = orders.find(o => o.id === Number(id))
    if (!order) throw new Error('Pedido não encontrado.')
    if (payment) {
      if (order.status !== 'AGUARDANDO_PAGAMENTO') throw new Error('O pedido não está aguardando pagamento.')
      order.status = 'NOVO'
    } else {
      if (next[order.status] !== status) throw new Error('Transição de status inválida.')
      order.status = status
    }
    save(); return copy(order)
  }
  function summary(date) {
    const localDate = value => { const d = new Date(value); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
    const rows = orders.filter(o => ['CONFIRMADO','EM_PREPARO','PRONTO','SAIU_PARA_ENTREGA','ENTREGUE'].includes(o.status) && localDate(o.criadoEm) === date)
    const total = rows.reduce((sum,o) => sum + Math.round(o.valorTotal * 100), 0) / 100
    const frete = rows.reduce((sum,o) => sum + Math.round(o.valorFrete * 100), 0) / 100
    return {data: date, quantidadePedidos: rows.length, valorTotal: total, valorFrete: frete, valorItens: Math.round((total-frete)*100)/100, ticketMedio: rows.length ? Math.round(total/rows.length*100)/100 : 0,
      porFormaPagamento: ['DINHEIRO','CARTAO','PIX'].map(forma => ({formaPagamento: forma, quantidade: rows.filter(o => o.formaPagamento === forma).length, valorTotal: rows.filter(o => o.formaPagamento === forma).reduce((s,o) => s+Math.round(o.valorTotal*100),0)/100}))}
  }
  return {
    reset,
    create() { const id = Math.max(0,...orders.map(o=>o.id)) + 1; orders.push(make(id)); save(); return copy(orders.at(-1)) },
    async request(path, options = {}) {
      if (path.startsWith('/api/faturamento?')) return summary(new URLSearchParams(path.split('?')[1]).get('data'))
      const payment = path.match(/^\/api\/pedidos\/(\d+)\/confirmar-pagamento$/)
      if (payment) return update(payment[1], 'NOVO', true)
      const status = path.match(/^\/api\/pedidos\/(\d+)\/status$/)
      if (status) return update(status[1], JSON.parse(options.body).status)
      if (path.startsWith('/api/pedidos?')) return copy(orders.filter(o => !['ENTREGUE','CANCELADO'].includes(o.status)))
      throw new Error('Operação indisponível na demonstração.')
    }
  }
}
