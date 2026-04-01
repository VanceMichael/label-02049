// ===== Custom Select Component =====
function initCustomSelects(root = document) {
    root.querySelectorAll('.custom-select').forEach(cs => {
        if (cs._csInit) return;
        cs._csInit = true;
        const trigger = cs.querySelector('.custom-select-trigger');
        const dropdown = cs.querySelector('.custom-select-dropdown');
        trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            document.querySelectorAll('.custom-select.open').forEach(o => { if (o !== cs) o.classList.remove('open'); });
            cs.classList.toggle('open');
        });
        dropdown.addEventListener('click', (e) => {
            const opt = e.target.closest('.custom-select-option');
            if (!opt) return;
            dropdown.querySelectorAll('.custom-select-option').forEach(o => o.classList.remove('selected'));
            opt.classList.add('selected');
            cs.dataset.value = opt.dataset.value;
            const text = trigger.querySelector('.trigger-text');
            text.textContent = opt.textContent;
            text.classList.toggle('placeholder', opt.dataset.value === '');
            cs.classList.remove('open');
            cs.dispatchEvent(new Event('change'));
        });
    });
}
function getCustomSelectValue(id) {
    const el = document.getElementById(id);
    return el ? (el.dataset.value || '') : '';
}
function setCustomSelectValue(id, value) {
    const cs = document.getElementById(id);
    if (!cs) return;
    cs.dataset.value = value;
    const opt = cs.querySelector(`.custom-select-option[data-value="${value}"]`);
    cs.querySelectorAll('.custom-select-option').forEach(o => o.classList.remove('selected'));
    if (opt) {
        opt.classList.add('selected');
        const text = cs.querySelector('.trigger-text');
        text.textContent = opt.textContent;
        text.classList.toggle('placeholder', value === '');
    }
}
function addCustomSelectOption(id, value, label) {
    const cs = document.getElementById(id);
    if (!cs) return;
    const dropdown = cs.querySelector('.custom-select-dropdown');
    if (dropdown.querySelector(`.custom-select-option[data-value="${value}"]`)) return;
    const opt = document.createElement('div');
    opt.className = 'custom-select-option';
    opt.dataset.value = value;
    opt.textContent = label;
    dropdown.appendChild(opt);
}
document.addEventListener('click', (e) => {
    document.querySelectorAll('.custom-select.open').forEach(cs => cs.classList.remove('open'));
    const panel = document.getElementById('notification-panel');
    const bell = document.getElementById('notification-bell');
    if (panel && panel.classList.contains('active') && !panel.contains(e.target) && !bell.contains(e.target)) {
        panel.classList.remove('active');
    }
});

// ===== 加密工具（AES-GCM，Web Crypto API）=====
const CRYPTO_KEY_MATERIAL = 'ConcertGo2024SecretKey!';

async function deriveAESKey() {
    const enc = new TextEncoder();
    const keyMaterial = await crypto.subtle.importKey('raw', enc.encode(CRYPTO_KEY_MATERIAL), 'PBKDF2', false, ['deriveKey']);
    return crypto.subtle.deriveKey(
        { name: 'PBKDF2', salt: enc.encode('concert-salt'), iterations: 100000, hash: 'SHA-256' },
        keyMaterial,
        { name: 'AES-GCM', length: 256 },
        false,
        ['encrypt', 'decrypt']
    );
}

async function encryptToken(token) {
    const key = await deriveAESKey();
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const enc = new TextEncoder();
    const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, enc.encode(token));
    // 拼接 iv + ciphertext，base64 编码
    const combined = new Uint8Array(iv.length + ciphertext.byteLength);
    combined.set(iv);
    combined.set(new Uint8Array(ciphertext), iv.length);
    return btoa(String.fromCharCode(...combined));
}

async function decryptToken(encrypted) {
    const key = await deriveAESKey();
    const combined = Uint8Array.from(atob(encrypted), c => c.charCodeAt(0));
    const iv = combined.slice(0, 12);
    const ciphertext = combined.slice(12);
    const decrypted = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, ciphertext);
    return new TextDecoder().decode(decrypted);
}

// ===== 状态管理 =====
let currentPage = 'home';
let concertPage = 1;
let orderPage = 1;
let ws = null;

const STATUS_MAP = { 0: '未开始', 1: '售票中', 2: '已结束', 3: '已下架' };
const STATUS_ICON = { 0: 'clock', 1: 'zap', 2: 'check-circle', 3: 'x-circle' };
const ORDER_STATUS_MAP = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' };

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', () => {
    initCustomSelects();
    checkAuth();
    loadConcerts();
    setupNavigation();
});

function checkAuth() {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (token && user) {
        showLoggedIn(user);
        connectWebSocket(user.userId);
        loadUnreadCount();
        return;
    }
    // 记住密码免登录：解密 AES 加密的7天长令牌
    tryRememberLogin();
}

async function tryRememberLogin() {
    const rememberToken = localStorage.getItem('remember_token');
    const rememberExpire = localStorage.getItem('remember_expire');
    if (!rememberToken || !rememberExpire || Date.now() >= parseInt(rememberExpire)) return;
    try {
        const token = await decryptToken(rememberToken);
        // 验证令牌是否仍有效（调用一个需要认证的接口）
        localStorage.setItem('token', token);
        const res = await api.getMyOrders({ page: 1, size: 1 });
        if (res.code === 401) {
            // 令牌已过期，清除
            localStorage.removeItem('token');
            localStorage.removeItem('remember_token');
            localStorage.removeItem('remember_expire');
            return;
        }
        // 解析 JWT payload 获取用户信息
        const payload = JSON.parse(atob(token.split('.')[1]));
        const userData = { userId: parseInt(payload.sub), role: payload.role, username: payload.username || 'User' };
        localStorage.setItem('user', JSON.stringify(userData));
        showLoggedIn(userData);
        connectWebSocket(userData.userId);
        loadUnreadCount();
    } catch (e) {
        localStorage.removeItem('remember_token');
        localStorage.removeItem('remember_expire');
    }
}

function showLoggedIn(user) {
    document.getElementById('auth-buttons').style.display = 'none';
    document.getElementById('user-info').style.display = 'flex';
    document.getElementById('username-display').textContent = user.username;
    document.getElementById('nav-orders').style.display = 'inline-flex';
    document.getElementById('notification-bell').style.display = 'flex';
}

function setupNavigation() {
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(link.dataset.page);
        });
    });
    document.getElementById('notification-bell').addEventListener('click', toggleNotificationPanel);
    // 搜索框回车
    document.getElementById('search-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') searchConcerts();
    });
}

function navigateTo(page) {
    currentPage = page;
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(`page-${page}`).classList.add('active');
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    const activeLink = document.querySelector(`.nav-link[data-page="${page}"]`);
    if (activeLink) activeLink.classList.add('active');
    if (page === 'orders') loadOrders();
    window.scrollTo({ top: 0, behavior: 'smooth' });
    // 重新渲染 lucide 图标
    setTimeout(() => lucide.createIcons(), 50);
}

// ===== Toast =====
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transform = 'translateX(30px)'; }, 3000);
    setTimeout(() => toast.remove(), 3400);
}

// ===== Custom Confirm =====
let _confirmResolve = null;

function showConfirm(message, title = '确认操作', okText = '确定') {
    document.getElementById('confirm-title').textContent = title;
    document.getElementById('confirm-message').textContent = message;
    document.getElementById('confirm-ok-btn').textContent = okText;
    document.getElementById('modal-confirm').classList.add('active');
    return new Promise(resolve => { _confirmResolve = resolve; });
}

function resolveConfirm(result) {
    document.getElementById('modal-confirm').classList.remove('active');
    if (_confirmResolve) { _confirmResolve(result); _confirmResolve = null; }
}

// ===== Modal =====
function showModal(name) {
    document.getElementById(`modal-${name}`).classList.add('active');
    setTimeout(() => lucide.createIcons(), 50);
}
function closeModal(name) { document.getElementById(`modal-${name}`).classList.remove('active'); }

// ===== Auth =====
async function handleLogin(e) {
    e.preventDefault();
    const form = e.target;
    const btn = document.getElementById('login-btn');
    btn.disabled = true; btn.textContent = '登录中...';

    const rememberMeCheckbox = document.getElementById('remember-me');
    const rememberMe = rememberMeCheckbox ? rememberMeCheckbox.checked : false;
    const res = await api.login({ username: form.username.value, password: form.password.value, rememberMe });
    if (res.code === 200) {
        localStorage.setItem('token', res.data.token);
        localStorage.setItem('user', JSON.stringify(res.data));
        if (rememberMe && res.data.rememberToken) {
            // AES-GCM 加密存储7天长令牌
            try {
                const encrypted = await encryptToken(res.data.rememberToken);
                localStorage.setItem('remember_token', encrypted);
                localStorage.setItem('remember_expire', Date.now() + 7 * 24 * 3600 * 1000);
            } catch (err) {
                console.warn('加密存储失败:', err);
            }
        }
        showToast('登录成功', 'success');
        closeModal('login');
        showLoggedIn(res.data);
        connectWebSocket(res.data.userId);
        loadUnreadCount();
        lucide.createIcons();
    } else {
        showToast(res.message, 'error');
    }
    btn.disabled = false; btn.textContent = '登 录';
}

async function handleRegister(e) {
    e.preventDefault();
    const form = e.target;
    const btn = document.getElementById('register-btn');
    btn.disabled = true; btn.textContent = '注册中...';

    const res = await api.register({
        username: form.username.value, password: form.password.value,
        email: form.email.value || null, phone: form.phone.value || null
    });
    if (res.code === 200) {
        showToast('注册成功！', 'success');
        closeModal('register'); showModal('login');
    } else {
        showToast(res.message, 'error');
    }
    btn.disabled = false; btn.textContent = '注 册';
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('remember_token');
    localStorage.removeItem('remember_expire');
    if (ws) ws.close();
    location.reload();
}

// ===== Concerts =====
async function loadConcerts(page = 1) {
    const isPageSwitch = concertPage !== page && concertPage > 0;
    concertPage = page;
    const keyword = document.getElementById('search-input').value;
    const city = getCustomSelectValue('city-filter');
    const res = await api.getConcerts({ page, size: 9, keyword, city, status: '', excludeOffShelf: true });
    if (res.code === 200) {
        renderConcerts(res.data.records);
        renderPagination('concert-pagination', page, res.data.pages, loadConcerts);
        extractCities(res.data.records);
        if (isPageSwitch) {
            const title = document.querySelector('#page-home .section-title');
            if (title) {
                const top = title.getBoundingClientRect().top + window.scrollY - 88;
                window.scrollTo({ top, behavior: 'smooth' });
            }
        }
    }
}

function searchConcerts() { loadConcerts(1); }

function renderConcerts(concerts) {
    const grid = document.getElementById('concert-grid');
    if (!concerts || concerts.length === 0) {
        grid.innerHTML = `<div class="empty-state"><i data-lucide="music"></i><p>暂无演唱会信息</p></div>`;
        lucide.createIcons();
        return;
    }
    grid.innerHTML = concerts.map((c, i) => `
        <div class="concert-card" onclick="viewConcert(${c.id})" style="animation-delay:${i * 0.06}s">
            <div class="concert-poster">
                ${c.posterUrl
                    ? `<img src="${c.posterUrl}" alt="${c.title}" onerror="this.parentElement.innerHTML='<div class=poster-placeholder><i data-lucide=music></i></div>'">`
                    : '<div class="poster-placeholder"><i data-lucide="music"></i></div>'}
            </div>
            <div class="concert-info">
                <h3>${c.title}</h3>
                <div class="concert-meta">
                    <span><i data-lucide="mic-2"></i>${c.artist}</span>
                    <span><i data-lucide="map-pin"></i>${c.city} · ${c.venue}</span>
                    <span><i data-lucide="calendar"></i>${formatDate(c.showTime)}</span>
                </div>
                <span class="concert-status status-${c.status}">
                    <i data-lucide="${STATUS_ICON[c.status]}" style="width:12px;height:12px;"></i>
                    ${STATUS_MAP[c.status]}
                </span>
            </div>
        </div>
    `).join('');
    lucide.createIcons();
}

function extractCities(concerts) {
    const cs = document.getElementById('city-filter');
    const dropdown = cs.querySelector('.custom-select-dropdown');
    const existing = new Set([...dropdown.querySelectorAll('.custom-select-option')].map(o => o.dataset.value));
    concerts.forEach(c => {
        if (c.city && !existing.has(c.city)) {
            addCustomSelectOption('city-filter', c.city, c.city);
            existing.add(c.city);
        }
    });
    initCustomSelects();
}

// ===== Concert Detail =====
async function viewConcert(id) {
    navigateTo('detail');
    const [concertRes, tierRes] = await Promise.all([api.getConcert(id), api.getTicketTiers(id)]);
    if (concertRes.code !== 200) { showToast(concertRes.message, 'error'); return; }
    const c = concertRes.data;
    const tiers = tierRes.code === 200 ? tierRes.data : [];

    document.getElementById('concert-detail').innerHTML = `
        <button class="back-btn" onclick="navigateTo('home')">
            <i data-lucide="arrow-left"></i> 返回演出列表
        </button>
        <div class="detail-header">
            <div class="detail-poster">
                ${c.posterUrl
                    ? `<img src="${c.posterUrl}" alt="${c.title}" onerror="this.parentElement.innerHTML='<i data-lucide=music></i>'">`
                    : '<i data-lucide="music"></i>'}
            </div>
            <div class="detail-info">
                <span class="concert-status status-${c.status}">
                    <i data-lucide="${STATUS_ICON[c.status]}" style="width:12px;height:12px;"></i>
                    ${STATUS_MAP[c.status]}
                </span>
                <h1>${c.title}</h1>
                <div class="detail-meta">
                    <div class="detail-meta-item"><i data-lucide="mic-2"></i> ${c.artist}</div>
                    <div class="detail-meta-item"><i data-lucide="map-pin"></i> ${c.city} · ${c.venue}</div>
                    <div class="detail-meta-item"><i data-lucide="calendar"></i> ${formatDate(c.showTime)}</div>
                </div>
            </div>
        </div>
        ${c.description ? `<div class="detail-desc"><h3>演出介绍</h3><p>${c.description}</p></div>` : ''}
        <h3 class="tier-section-title">选择票档</h3>
        <div class="tier-list">
            ${tiers.length === 0 ? '<div class="empty-state"><p>暂无票档信息</p></div>' :
              tiers.map(t => `
                <div class="tier-card" id="tier-${t.id}">
                    <div class="tier-left">
                        <h4>${t.tierName}</h4>
                        <span class="stock">剩余 ${t.availableStock} / ${t.totalStock}</span>
                    </div>
                    <div class="tier-right">
                        <span class="tier-price">¥${t.price}<small>/张</small></span>
                        <div class="quantity-control">
                            <button onclick="changeQty(${t.id}, -1)">−</button>
                            <span id="qty-${t.id}">1</span>
                            <button onclick="changeQty(${t.id}, 1)">+</button>
                        </div>
                        <button class="btn btn-primary" onclick="grabTicket(${t.id})"
                                ${c.status !== 1 || t.availableStock <= 0 ? 'disabled' : ''}>
                            ${t.availableStock <= 0 ? '已售罄' : '立即抢票'}
                        </button>
                    </div>
                </div>
              `).join('')}
        </div>
    `;
    lucide.createIcons();
}

function changeQty(tierId, delta) {
    const el = document.getElementById(`qty-${tierId}`);
    let qty = parseInt(el.textContent) + delta;
    el.textContent = Math.max(1, Math.min(4, qty));
}

async function grabTicket(ticketTierId) {
    if (!localStorage.getItem('token')) { showModal('login'); return; }
    const qty = parseInt(document.getElementById(`qty-${ticketTierId}`).textContent);
    const res = await api.grabTicket({ ticketTierId, quantity: qty });
    if (res.code === 200) {
        showToast('抢票成功！请在15分钟内完成支付', 'success');
        navigateTo('orders'); loadOrders();
    } else {
        showToast(res.message, 'error');
    }
}

// ===== Orders =====
async function loadOrders(page = 1) {
    orderPage = page;
    const res = await api.getMyOrders({ page, size: 10 });
    if (res.code === 200) {
        renderOrders(res.data.records);
        renderPagination('order-pagination', page, res.data.pages, loadOrders);
    }
}

function renderOrders(orders) {
    const list = document.getElementById('order-list');
    if (!orders || orders.length === 0) {
        list.innerHTML = '<div class="empty-state"><i data-lucide="ticket"></i><p>暂无订单</p></div>';
        lucide.createIcons(); return;
    }
    list.innerHTML = orders.map(o => `
        <div class="order-card">
            <div class="order-header">
                <span class="order-no">NO. ${o.orderNo}</span>
                <span class="order-status order-status-${o.status}">${ORDER_STATUS_MAP[o.status]}</span>
            </div>
            <div class="order-body">
                <div class="order-detail">
                    <div>票档ID: ${o.ticketTierId} × ${o.quantity}张</div>
                    <div>总价: <strong>¥${o.totalPrice}</strong></div>
                    <div>创建时间: ${formatDate(o.createdAt)}</div>
                    ${o.status === 0 ? `<div style="color:var(--warning)">⏱ 请在 ${formatDate(o.expireAt)} 前完成支付</div>` : ''}
                </div>
                <div class="order-actions">
                    ${o.status === 0 ? `
                        <button class="btn btn-primary btn-sm" onclick="payOrder(${o.id})">立即支付</button>
                        <button class="btn btn-ghost btn-sm" onclick="cancelOrder(${o.id})">取消</button>
                    ` : ''}
                    ${o.status === 1 ? `
                        <button class="btn btn-warning btn-sm" onclick="refundOrder(${o.id})">退款</button>
                    ` : ''}
                </div>
            </div>
        </div>
    `).join('');
}

let _suppressWsToast = false;

async function payOrder(id) {
    const res = await api.payOrder(id);
    if (res.code === 200) { _suppressWsToast = true; setTimeout(() => _suppressWsToast = false, 2000); showToast('支付成功', 'success'); loadOrders(orderPage); }
    else showToast(res.message, 'error');
}
async function cancelOrder(id) {
    if (!await showConfirm('确定取消此订单？', '取消订单', '确定取消')) return;
    const res = await api.cancelOrder(id);
    if (res.code === 200) { _suppressWsToast = true; setTimeout(() => _suppressWsToast = false, 2000); showToast('订单已取消', 'success'); loadOrders(orderPage); }
    else showToast(res.message, 'error');
}
async function refundOrder(id) {
    if (!await showConfirm('退款后将释放库存，确定申请退款？', '申请退款', '确定退款')) return;
    const res = await api.refundOrder(id);
    if (res.code === 200) { _suppressWsToast = true; setTimeout(() => _suppressWsToast = false, 2000); showToast('退款成功', 'success'); loadOrders(orderPage); }
    else showToast(res.message, 'error');
}

// ===== Notifications =====
function toggleNotificationPanel() {
    const panel = document.getElementById('notification-panel');
    panel.classList.toggle('active');
    if (panel.classList.contains('active')) loadNotifications();
}

async function loadNotifications() {
    const res = await api.getNotifications({ page: 1, size: 20 });
    if (res.code !== 200) return;
    const list = document.getElementById('notification-list');
    if (!res.data.records || res.data.records.length === 0) {
        list.innerHTML = '<div class="empty-state" style="padding:40px;"><p>暂无通知</p></div>';
        return;
    }
    list.innerHTML = res.data.records.map(n => `
        <div class="notification-item ${n.isRead === 0 ? 'unread' : ''}" onclick="readNotification(${n.id})">
            <div class="n-title">${n.title}</div>
            <div class="n-content">${n.content}</div>
            <div class="n-time">${formatDate(n.createdAt)}</div>
        </div>
    `).join('');
}

async function readNotification(id) { await api.markRead(id); loadNotifications(); loadUnreadCount(); }
async function markAllRead() { await api.markAllRead(); loadNotifications(); loadUnreadCount(); }

async function loadUnreadCount() {
    const res = await api.getUnreadCount();
    if (res.code === 200) {
        const badge = document.getElementById('unread-badge');
        if (res.data > 0) {
            badge.textContent = res.data > 99 ? '99+' : res.data;
            badge.style.display = 'flex';
        } else { badge.style.display = 'none'; }
    }
}

// ===== WebSocket =====
let wsRetryCount = 0;
const WS_MAX_RETRIES = 5;

function connectWebSocket(userId) {
    if (ws) ws.close();
    wsRetryCount = 0;
    doConnectWebSocket(userId);
}
function doConnectWebSocket(userId) {
    try {
        ws = new WebSocket(`${WS_BASE}/${userId}`);
        ws.onopen = () => { wsRetryCount = 0; };
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (!_suppressWsToast) showToast(data.title || data.content, 'info');
                loadUnreadCount();
            } catch (e) { if (!_suppressWsToast) showToast(event.data, 'info'); }
        };
        ws.onclose = () => {
            if (localStorage.getItem('token') && wsRetryCount < WS_MAX_RETRIES) {
                wsRetryCount++;
                setTimeout(() => doConnectWebSocket(userId), 5000 * wsRetryCount);
            }
        };
        ws.onerror = () => {};
    } catch (e) { console.error('WebSocket连接失败:', e); }
}

// ===== Pagination =====
function renderPagination(containerId, current, total, callback) {
    const container = document.getElementById(containerId);
    if (total <= 1) { container.innerHTML = ''; return; }
    let html = `<button ${current <= 1 ? 'disabled' : ''} onclick="(${callback.name})(${current - 1})">‹ 上一页</button>`;
    for (let i = 1; i <= total; i++) {
        if (total > 7 && i > 3 && i < total - 2 && Math.abs(i - current) > 1) {
            if (i === 4 || i === total - 3) html += '<button disabled>…</button>';
            continue;
        }
        html += `<button class="${i === current ? 'active' : ''}" onclick="(${callback.name})(${i})">${i}</button>`;
    }
    html += `<button ${current >= total ? 'disabled' : ''} onclick="(${callback.name})(${current + 1})">下一页 ›</button>`;
    container.innerHTML = html;
}

// ===== Utils =====
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}
