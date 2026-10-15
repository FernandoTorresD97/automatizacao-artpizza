"""Teste real do laboratório Compose. Usa apenas contas e destinatário fictícios."""
import base64, json, os, time, urllib.request, urllib.error, uuid

def request(base, path, method='GET', body=None, credentials=None, headers=None):
    head = {'Content-Type': 'application/json', **(headers or {})}
    if credentials:
        head['Authorization'] = 'Basic ' + base64.b64encode((':'.join(credentials)).encode()).decode()
    data = None if body is None else json.dumps(body).encode()
    with urllib.request.urlopen(urllib.request.Request(base + path, data=data, headers=head, method=method), timeout=15) as response:
        raw = response.read()
        return json.loads(raw) if raw else None

art = 'http://127.0.0.1:8081'
notification = 'http://127.0.0.1:8082'
admin = (os.environ['ADMIN_USERNAME'], os.environ['ADMIN_PASSWORD'])
notification_admin = (os.environ.get('NOTIFICATION_ADMIN_USERNAME', 'notification-admin'), os.environ['NOTIFICATION_ADMIN_PASSWORD'])
for base, path in [(art, '/api/cardapio/produtos'), (notification, '/api/v1/channels')]:
    for attempt in range(120):
        try:
            request(base, path)
            break
        except (OSError, urllib.error.HTTPError):
            time.sleep(2)
    else:
        raise AssertionError('Serviço não iniciou: ' + base)

product = request(art, '/api/cardapio/produtos')[0]
order = request(art, '/api/pedidos', 'POST', {
    'nomeCliente': 'Cliente fictício CI', 'telefoneWhatsapp': '5500000000000',
    'emailNotificacao': 'customer@example.invalid', 'tipoEntrega': 'RETIRADA', 'formaPagamento': 'PIX',
    'itens': [{'tipo': 'PRODUTO', 'produtoId': product['id'], 'quantidade': 1}]})
charge = request(art, '/api/pagamentos/sandbox', 'POST', {'pedidoId': order['id']}, admin)
event = {'eventId': str(uuid.uuid4()), 'paymentId': charge['id'], 'pedidoId': order['id'],
         'amount': charge['amount'], 'currency': 'BRL', 'status': 'APPROVED'}
request(art, '/api/pagamentos/sandbox/simular', 'POST', event, admin)
request(art, '/api/pagamentos/sandbox/simular', 'POST', event, admin)
assert request(art, '/api/pedidos/' + str(order['id']), credentials=admin)['status'] == 'NOVO'

for attempt in range(60):
    page = request(notification, '/api/v1/notifications?size=100', credentials=notification_admin)
    delivered = [n for n in page['content'] if n['title'] == 'Art Pizza · Pedido #' + str(order['id']) and n['status'] == 'SENT']
    if len(delivered) == 2:
        break
    time.sleep(2)
else:
    raise AssertionError('As duas notificações não foram entregues ao SMTP de teste')
for item in delivered:
    attempts = request(notification, '/api/v1/notifications/' + str(item['id']) + '/attempts', credentials=notification_admin)
    assert len(attempts) == 1 and attempts[0]['outcome'] == 'SMTP_ACCEPTED'
assert request('http://127.0.0.1:8025', '/api/v1/messages')['total'] == 2
try:
    request(notification, '/api/v1/notifications')
    raise AssertionError('Listagem anônima deveria ser bloqueada')
except urllib.error.HTTPError as error:
    assert error.code == 401
print('OK: pagamento fictício repetido → pedido confirmado uma vez → outbox → API autenticada → RabbitMQ → 2 mensagens Mailpit, sem duplicação.')
