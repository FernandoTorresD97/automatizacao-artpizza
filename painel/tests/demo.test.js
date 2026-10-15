import test from 'node:test'
import assert from 'node:assert/strict'
import { createDemoStore } from '../src/demo.js'
const now = () => new Date('2026-10-15T12:00:00Z')
function storage() { const data = new Map(); return {getItem:k=>data.get(k),setItem:(k,v)=>data.set(k,v)} }
test('estado da demonstração persiste e reinicia sem backend', async()=>{
 const s=storage(); const demo=createDemoStore(s,now); assert.equal((await demo.request('/api/pedidos?')).length,5)
 demo.create(); assert.equal((await createDemoStore(s,now).request('/api/pedidos?')).length,6)
 demo.reset(); assert.equal((await demo.request('/api/pedidos?')).length,5)
})
test('PIX não pode avançar sem confirmação e confirmação repetida falha',async()=>{
 const demo=createDemoStore(storage(),now)
 await assert.rejects(demo.request('/api/pedidos/1/status',{body:JSON.stringify({status:'NOVO'})}))
 await demo.request('/api/pedidos/1/confirmar-pagamento')
 await assert.rejects(demo.request('/api/pedidos/1/confirmar-pagamento'))
})
test('resumo exclui pedidos novos e PIX pendente; data sem vendas retorna zero',async()=>{
 const demo=createDemoStore(storage(),now)
 const result=await demo.request('/api/faturamento?data=2026-10-15')
 assert.equal(result.quantidadePedidos,3);assert.equal(result.valorTotal,152.97);assert.equal(result.valorFrete,15)
 assert.equal((await demo.request('/api/faturamento?data=2026-10-14')).valorTotal,0)
})
test('demo não permite pular etapas e remove entregues do quadro',async()=>{
 const demo=createDemoStore(storage(),now)
 await assert.rejects(demo.request('/api/pedidos/2/status',{body:'{"status":"ENTREGUE"}'}))
 for (const status of ['CONFIRMADO','EM_PREPARO','PRONTO','SAIU_PARA_ENTREGA','ENTREGUE']) await demo.request('/api/pedidos/2/status',{body:JSON.stringify({status})})
 assert.equal((await demo.request('/api/pedidos?')).some(o=>o.id===2),false)
})
