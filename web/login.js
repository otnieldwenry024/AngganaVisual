let mode = 'login';
const form = document.querySelector('#auth-form');
const message = document.querySelector('#auth-message');
const submit = document.querySelector('#submit-auth');
function setMode(next) {
  mode = next;
  const register = mode === 'register';
  document.querySelector('#register-fields').hidden = !register;
  for (const input of document.querySelectorAll('#register-fields input')) input.required = register;
  document.querySelector('#password').minLength = register ? 8 : 1;
  document.querySelector('#password').autocomplete = register ? 'new-password' : 'current-password';
  document.querySelector('#auth-title').textContent = register ? 'Cerita baru dimulai di sini.' : 'Selamat datang kembali.';
  document.querySelector('#auth-description').textContent = register ? 'Buat akun klien untuk memulai proyek pertama lu.' : 'Masuk untuk melanjutkan cerita dan proyek lu.';
  document.querySelector('#login-tab').classList.toggle('active', !register);
  document.querySelector('#register-tab').classList.toggle('active', register);
  document.querySelector('#login-tab').setAttribute('aria-pressed', String(!register));
  document.querySelector('#register-tab').setAttribute('aria-pressed', String(register));
  submit.textContent = register ? 'Buat akun klien ↗' : 'Masuk ke ruang proyek ↗';
  message.textContent = '';
}
document.querySelector('#login-tab').onclick = () => setMode('login');
document.querySelector('#register-tab').onclick = () => setMode('register');
document.querySelector('#toggle-password').onclick = event => {
  const input = document.querySelector('#password');
  input.type = input.type === 'password' ? 'text' : 'password';
  event.currentTarget.textContent = input.type === 'password' ? 'Lihat' : 'Sembunyikan';
  event.currentTarget.setAttribute('aria-label', input.type === 'password' ? 'Tampilkan password' : 'Sembunyikan password');
};
form.onsubmit = async event => {
  event.preventDefault();
  const label = submit.textContent;
  submit.disabled = true; submit.textContent = 'Memproses…'; message.textContent = ''; message.classList.remove('success');
  try {
    const response = await fetch(`/api/${mode}`, { method: 'POST', body: new URLSearchParams(new FormData(form)) });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Permintaan gagal.');
    if (mode === 'login') { location.replace('/app'); return; }
    setMode('login'); form.elements.password.value = ''; message.classList.add('success'); message.textContent = data.message;
  } catch (error) { message.textContent = error instanceof TypeError ? 'Tidak dapat terhubung ke server Java. Coba lagi.' : error.message; }
  finally { submit.disabled = false; if (mode === 'register') submit.textContent = label; else submit.textContent = 'Masuk ke ruang proyek ↗'; }
};
fetch('/api/me').then(response => { if (response.ok) location.replace('/app'); }).catch(() => {});
