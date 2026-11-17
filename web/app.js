let user, data = { orders: [], services: [], clients: [], statuses: [] }, selectedId = null, busy = false;
const $ = selector => document.querySelector(selector);
const currency = value => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(value);
const dateLabel = value => new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(`${value}T12:00:00`));
const escape = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
function safeLink(value) { try { const url = new URL(value); return url.protocol === 'https:' ? url.href : ''; } catch { return ''; } }
async function api(path, form) {
  let response;
  try {
    response = await fetch(path, form ? { method: 'POST', headers: { 'X-CSRF-Token': user?.csrf || '' }, body: new URLSearchParams(form) } : {});
  } catch { throw new Error('Koneksi ke server terputus. Pastikan server Java masih berjalan.'); }
  const body = await response.json();
  if (response.status === 401) { location.replace('/'); throw new Error('Sesi berakhir.'); }
  if (!response.ok) throw new Error(body.error || 'Permintaan gagal.');
  return body;
}
let toastTimer;
function toast(text) { $('#toast').textContent = text; $('#toast').hidden = false; clearTimeout(toastTimer); toastTimer = setTimeout(() => { $('#toast').hidden = true; }, 4500); }
function options(select, rows, label) {
  select.replaceChildren();
  for (const row of rows) select.add(new Option(label(row), row.id ?? row));
}
async function load() {
  $('#refresh').disabled = true;
  $('#data-message').textContent = '';
  try {
    const fresh = await api('/api/data');
    data = fresh; render();
    options($('#service-select'), data.services, s => `${s.name} · ${currency(s.price)}`);
    options($('#client-select'), data.clients, c => c.name);
    options($('#status-select'), data.statuses, s => s);
    updatePrice();
    return true;
  } catch (error) { $('#data-message').textContent = error.message; $('#row-count').textContent = 'Data belum dapat dimuat.'; return false; }
  finally { $('#refresh').disabled = false; }
}
function render() {
  $('#stat-total').textContent = data.orders.length;
  $('#stat-active').textContent = data.orders.filter(o => o.status !== 'Selesai').length;
  $('#stat-done').textContent = data.orders.filter(o => o.status === 'Selesai').length;
  $('#stat-balance').textContent = currency(data.orders.reduce((sum, order) => sum + order.remaining, 0));
  const search = $('#search').value.toLocaleLowerCase('id');
  const status = $('#filter').value;
  const visible = data.orders.filter(o => (!status || o.status === status) && [o.id, o.client, o.location].some(v => v.toLocaleLowerCase('id').includes(search)));
  $('#order-rows').innerHTML = visible.map(o => `<tr><td><strong>${escape(o.client)}</strong><small>${escape(o.id)} · ${escape(o.location)}</small></td><td><strong>${dateLabel(o.date)}</strong><small>Deadline ${dateLabel(o.deadline)}</small></td><td><span class="status-badge status-${escape(o.status)}">${escape(o.status)}</span></td><td><strong>${currency(o.total)}</strong><small>${o.remaining <= 0 ? '✓ Lunas' : `Sisa ${currency(o.remaining)}`}</small></td><td><button class="detail-button" data-order="${escape(o.id)}">Detail ↗</button></td></tr>`).join('');
  $('#empty-state').hidden = visible.length !== 0;
  $('#empty-state h3').textContent = data.orders.length ? 'Pesanan tidak ditemukan.' : 'Belum ada cerita di sini.';
  $('#empty-state p').textContent = data.orders.length ? 'Coba kata kunci lain atau pilih semua status.' : 'Buat pesanan pertama untuk memulai proyek bersama Anggana Visual.';
  $('#empty-create').hidden = data.orders.length > 0;
  $('#row-count').textContent = `${visible.length} dari ${data.orders.length} pesanan`;
}
function updatePrice() { const service = data.services.find(s => s.id === $('#service-select').value); $('#order-price').value = service?.price ?? ''; }
function localDate() { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
function newOrder() {
  if (!data.services.length) { toast('Layanan belum tersedia. Pastikan katalog layanan sudah diisi di database.'); return; }
  if (user.role === 'admin' && !data.clients.length) { $('#client-dialog').showModal(); toast('Tambahkan klien sebelum membuat pesanan.'); return; }
  $('#order-form').reset(); $('#order-form .form-message').textContent = '';
  $('#order-date').min = localDate(); $('#order-deadline').min = localDate();
  updatePrice(); $('#order-dialog').showModal();
}
function showDetails(id, open = true) {
  selectedId = id;
  const order = data.orders.find(o => o.id === id);
  if (!order) return;
  $('#detail-title').textContent = 'Detail pesanan.';
  const link = safeLink(order.result);
  $('#detail-content').innerHTML = `<dl><dt>ID pesanan</dt><dd>${escape(order.id)}</dd><dt>Klien</dt><dd>${escape(order.client)}</dd><dt>Jadwal</dt><dd>${dateLabel(order.date)}</dd><dt>Deadline</dt><dd>${dateLabel(order.deadline)}</dd><dt>Lokasi</dt><dd>${escape(order.location)}</dd><dt>Kebutuhan</dt><dd>${order.details.map(d => escape(d.needs)).join('\n')}</dd><dt>Pengerjaan</dt><dd>${escape(order.status)}</dd><dt>Total biaya</dt><dd>${currency(order.total)}</dd><dt>Sudah dibayar</dt><dd>${currency(order.paid)}</dd><dt>Sisa tagihan</dt><dd>${currency(order.remaining)}</dd><dt>Hasil akhir</dt><dd>${link ? `<a href="${escape(link)}" target="_blank" rel="noopener noreferrer">Buka hasil proyek ↗</a>` : 'Belum tersedia'}</dd></dl><h3>Riwayat pembayaran</h3>${order.payments.length ? `<ul class="payment-history">${order.payments.map(p => `<li>${dateLabel(p.date)} · ${currency(p.amount)} · ${escape(p.method)}</li>`).join('')}</ul>` : '<p class="muted">Belum ada pembayaran tercatat.</p>'}`;
  $('#admin-actions').hidden = user.role !== 'admin';
  $('#status-select').value = order.status;
  $('#payment-form').reset(); $('#payment-form').elements.amount.max = order.remaining.toFixed(2);
  $('#payment-form button').disabled = order.remaining <= 0;
  $('#payment-form').elements.amount.disabled = order.remaining <= 0;
  $('#result-form').elements.link.value = order.result || '';
  for (const message of document.querySelectorAll('#admin-actions .form-message')) message.textContent = '';
  if (open) $('#detail-dialog').showModal();
}
function submitForm(selector, endpoint, onSuccess, transform = value => value) {
  const form = $(selector);
  form.onsubmit = async event => {
    event.preventDefault();
    if (busy) return;
    busy = true;
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    const message = form.querySelector('.form-message'); message.textContent = '';
    try {
      const values = Object.fromEntries(new FormData(form));
      const result = await api(endpoint, transform(values));
      // Close the completed form even if refreshing the list encounters a connection error.
      onSuccess(result);
      await load();
      if ($('#detail-dialog').open && selectedId) showDetails(selectedId, false);
      toast(result.message);
    } catch (error) { message.textContent = error.message; }
    finally { busy = false; button.disabled = selector === '#payment-form' && (data.orders.find(o => o.id === selectedId)?.remaining ?? 0) <= 0; }
  };
}
// Dua URL, satu layout bersama. History menjaga navigasi Back/Forward tetap berfungsi.
function showPage(path, push = false) {
  const ordersPage = path === '/app/orders';
  if (push && location.pathname !== path) history.pushState({}, '', path);
  for (const id of ['#overview-banner', '#overview-stats', '#overview-next']) $(id).hidden = ordersPage;
  $('#orders-panel').hidden = !ordersPage;
  for (const [selector, active] of [['#nav-overview', !ordersPage], ['#nav-orders', ordersPage]]) {
    const link = $(selector);
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  }
  $('.breadcrumb').textContent = ordersPage ? '/ Daftar pesanan' : '/ Ringkasan';
  $('#greeting').textContent = ordersPage ? 'Daftar pesanan.' : `Halo, ${user.name.split(' ')[0]}.`;
  $('#page-description').textContent = ordersPage
    ? (user.role === 'admin' ? 'Kelola jadwal, tagihan, dan hasil seluruh pesanan.' : 'Semua pesanan dan perkembangan proyek lu ada di sini.')
    : (user.role === 'admin' ? 'Kelola pesanan dan proses kreatif tim dalam satu tempat.' : 'Pantau setiap proses proyek lu, dari ide sampai hasil akhir.');
  document.title = `${ordersPage ? 'Daftar Pesanan' : 'Ringkasan Proyek'} — Anggana Visual`;
  window.scrollTo({ top: 0, behavior: 'instant' });
}
function navigate(event) {
  // Tetap dukung buka tautan di tab baru menggunakan Cmd/Ctrl+klik.
  if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
  event.preventDefault();
  showPage(event.currentTarget.getAttribute('href'), true);
}
async function init() {
  try { user = await api('/api/me'); } catch { location.replace('/'); return; }
  document.body.hidden = false;
  const admin = user.role === 'admin';
  $('#sidebar-name').textContent = user.name;
  $('#sidebar-role').textContent = admin ? 'Administrator' : 'Akun klien';
  $('#role-label').textContent = admin ? 'Admin' : 'Klien';
  $('#avatar').textContent = user.name.split(/\s+/).slice(0, 2).map(n => n.charAt(0)).join('').toUpperCase();
  $('#today').textContent = new Intl.DateTimeFormat('id-ID', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date()).toUpperCase();
  showPage(location.pathname);
  $('#orders-title').textContent = admin ? 'Semua pesanan' : 'Pesanan lu';
  $('#client-field').hidden = !admin;
  $('#client-select').required = admin;
  $('#order-price').readOnly = !admin;
  if (admin) {
    $('#price-caption').textContent = 'kesepakatan (Rp)'; $('#price-help').textContent = 'Harga awal mengikuti katalog. Admin dapat menyesuaikan harga kesepakatan.';
    const button = document.createElement('button'); button.type = 'button'; button.className = 'add-client'; button.textContent = '+ Tambah klien';
    button.onclick = () => { $('#order-dialog').close(); $('#client-form').reset(); $('#client-form .form-message').textContent = ''; $('#client-dialog').showModal(); };
    $('#client-field').append(button);
  }
  for (const id of ['#new-order', '#nav-new', '#empty-create']) $(id).onclick = newOrder;
  for (const id of ['#nav-overview', '#nav-orders', '#view-orders']) $(id).onclick = navigate;
  window.addEventListener('popstate', () => showPage(location.pathname));
  $('#refresh').onclick = load; $('#search').oninput = render; $('#filter').onchange = render;
  $('#service-select').onchange = updatePrice;
  $('#order-date').onchange = () => { $('#order-deadline').min = $('#order-date').value || localDate(); };
  $('#order-rows').onclick = event => { const button = event.target.closest('[data-order]'); if (button) showDetails(button.dataset.order); };
  for (const button of document.querySelectorAll('.close-dialog')) button.onclick = () => { if (!busy) button.closest('dialog').close(); };
  for (const dialog of document.querySelectorAll('dialog')) dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
  $('#logout').onclick = async () => { try { await api('/api/logout', {}); location.replace('/'); } catch (error) { toast(error.message); } };
  submitForm('#order-form', '/api/orders', () => { $('#order-dialog').close(); showPage('/app/orders', true); });
  submitForm('#client-form', '/api/clients', () => $('#client-dialog').close());
  submitForm('#status-form', '/api/status', () => {}, value => ({ ...value, id: selectedId }));
  submitForm('#payment-form', '/api/payments', () => {}, value => ({ ...value, id: selectedId }));
  submitForm('#result-form', '/api/result', () => {}, value => ({ ...value, id: selectedId }));
  await load();
}
init();
