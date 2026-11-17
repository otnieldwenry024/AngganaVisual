"""Jalankan terhadap server AV_DB_URL=database uji baru (schema.sql + seed_test_db.sql).
AV_TEST_URL default http://localhost:8081. Pengujian membuat akun dan pesanan uji.
Jangan arahkan ke database kelompok. Tidak mengubah akun admin atau layanan.
"""
import http.cookiejar, json, os, urllib.request, urllib.parse, urllib.error
from datetime import date, timedelta
from uuid import uuid4
BASE = os.environ.get('AV_TEST_URL', 'http://localhost:8081')
class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.csrf = ''
    def request(self, path, form=None, csrf=True, origin=True, expected=200):
        headers = {}
        if form is not None:
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
            if origin: headers['Origin'] = BASE
            if csrf: headers['X-CSRF-Token'] = self.csrf
        req = urllib.request.Request(BASE + path, data=urllib.parse.urlencode(form).encode() if form is not None else None, headers=headers)
        try:
            with self.opener.open(req) as r: status, raw = r.status, r.read()
        except urllib.error.HTTPError as e: status, raw = e.code, e.read()
        assert status == expected, (path, status, expected, raw)
        return json.loads(raw) if raw and raw.startswith((b'{', b'[')) else raw
    def login(self, username, password):
        result = self.request('/api/login', dict(username=username, password=password))
        self.csrf = result['csrf']
        return result
anon, admin, one, two = [Client() for _ in range(4)]
assert b'Selamat datang' in anon.request('/')
anon.request('/api/data', expected=401)
assert b'Selamat datang' in anon.request('/app')  # redirected to login
anon.request('/api/login', dict(username='x', password='x'), origin=False, expected=403)
anon.request('/api/login', dict(username='testadmin', password='wrong'), expected=401)
assert admin.login('testadmin', 'AdminTest123!')['role'] == 'admin'
admin.request('/api/clients', dict(name='No csrf', contact='1'), csrf=False, expected=403)
admin.request('/src/database/KoneksiDB.java', expected=404)
suffix = uuid4().hex[:8]
for client, name in [(one, 'one'), (two, 'two')]:
    username = name + suffix
    anon.request('/api/register', dict(name='Klien ' + name, contact='08123456789', username=username, password='TestPass123!'), expected=201)
    assert client.login(username, 'TestPass123!')['role'] == 'klien'
    client.request('/api/clients', dict(name='Forbidden', contact='1'), expected=403)
anon.request('/api/register', dict(name='Duplicate', contact='1', username='one'+suffix, password='TestPass123!'), expected=400)
form = dict(service='L-TEST', date=str(date.today()+timedelta(days=5)), deadline=str(date.today()+timedelta(days=10)), location='Lokasi <script>uji</script>', needs='Dokumentasi acara', price='1', client='forged-client')
one.request('/api/orders', dict(form, deadline=str(date.today())), expected=400)
order_id = one.request('/api/orders', form, expected=201)['id']
orders = one.request('/api/data')['orders']
order = next(o for o in orders if o['id'] == order_id)
assert order['total'] == 1500000 and order['client'] == 'Klien one'
assert two.request('/api/data')['orders'] == []
one.request('/api/status', dict(id=order_id, status='Selesai'), expected=403)
one.request('/api/payments', dict(id=order_id, amount='1'), expected=403)
one.request('/api/result', dict(id=order_id, link='https://example.com'), expected=403)
admin.request('/api/status', dict(id=order_id, status='Bad'), expected=400)
admin.request('/api/status', dict(id=order_id, status='Editing'))
admin.request('/api/payments', dict(id=order_id, amount='-1'), expected=400)
admin.request('/api/payments', dict(id=order_id, amount='1500001'), expected=400)
admin.request('/api/payments', dict(id=order_id, amount='500000'))
admin.request('/api/result', dict(id=order_id, link='javascript:alert(1)'), expected=400)
admin.request('/api/result', dict(id=order_id, link='https://drive.google.com/'))
order = next(o for o in one.request('/api/data')['orders'] if o['id'] == order_id)
assert order['status'] == 'Editing' and order['remaining'] == 1000000 and len(order['payments']) == 1
admin.request('/api/clients', dict(name='Klien manual', contact='08123'))
client = next(c for c in admin.request('/api/data')['clients'] if c['name'] == 'Klien manual')
admin.request('/api/orders', dict(form, client=client['id'], price='2500000'), expected=201)
admin.request('/api/payments', dict(id=order_id, amount='1000000'))
order = next(o for o in one.request('/api/data')['orders'] if o['id'] == order_id)
assert order['remaining'] == 0 and order['paid'] == 1500000
one.request('/api/logout', {})
one.request('/api/me', expected=401)
one.request('/api/data', expected=401)
print('Integration passed: login, registration/hash, roles, ownership, server pricing, dates, payments, results, logout, private routes.')
