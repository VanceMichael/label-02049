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
document.addEventListener('click', () => {
    document.querySelectorAll('.custom-select.open').forEach(cs => cs.classList.remove('open'));
});

// ===== State =====
const STATUS_MAP = { 0: '未开始', 1: '售票中', 2: '已结束', 3: '已下架' };
const STATUS_BADGE = { 0: 'badge-warning', 1: 'badge-success', 2: 'badge-muted', 3: 'badge-danger' };
const ORDER_STATUS_MAP = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' };
const ORDER_BADGE = { 0: 'badge-warning', 1: 'badge-success', 2: 'badge-muted', 3: 'badge-info', 4: 'badge-info' };

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
    initCustomSelects();
    if (localStorage.getItem('admin_token')) showAdmin();
    setupNav();
});

function setupNav() {
    document.querySelectorAll('.sidebar-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = link.dataset.page;
            goToPage(page);
        });
    });
}

function goToPage(page, filters = {}) {
    document.querySelectorAll('.admin-page').forEach(p => p.classList.remove('active'));
    document.getElementById(`page-${page}`).classList.add('active');
    document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));
    const activeLink = document.querySelector(`.sidebar-link[data-page="${page}"]`);
    if (activeLink) activeLink.classList.add('active');

    if (page === 'concerts') loadConcerts(1);
    if (page === 'orders') {
        // 如果带了筛选条件（如已支付订单 status=1），先设置筛选框
        const statusSelect = document.getElementById('filter-order-status');
        if (filters.status !== undefined && statusSelect) {
            statusSelect.value = filters.status;
        } else if (statusSelect) {
            statusSelect.value = '';
        }
        loadAdminOrders(1);
    }
    if (page === 'users') loadUsers(1);
    if (page === 'dashboard') loadDashboard();
    setTimeout(() => lucide.createIcons(), 50);
}

// ===== Custom Confirm =====
let _confirmResolve = null;

function showConfirm(message, title = '确认操作', okText = '确定删除') {
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
    const res = await api.login({ username: form.username.value, password: form.password.value });
    if (res.code === 200) {
        if (res.data.role !== 1) { showToast('仅管理员可登录后台', 'error'); return; }
        localStorage.setItem('admin_token', res.data.token);
        localStorage.setItem('admin_user', JSON.stringify(res.data));
        showAdmin();
        showToast('登录成功', 'success');
    } else {
        showToast(res.message, 'error');
    }
}

function showAdmin() {
    document.getElementById('login-page').style.display = 'none';
    document.getElementById('admin-layout').style.display = 'flex';
    const user = JSON.parse(localStorage.getItem('admin_user') || '{}');
    document.getElementById('admin-name').textContent = user.username || 'Admin';
    loadDashboard();
    setTimeout(() => lucide.createIcons(), 100);
}

function logout() {
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_user');
    location.reload();
}

// ===== Dashboard =====
async function loadDashboard() {
    const [concerts, orders, users, paid] = await Promise.all([
        api.getConcerts({ page: 1, size: 1 }),
        api.getOrders({ page: 1, size: 1 }),
        api.getUsers({ page: 1, size: 1 }),
        api.getOrders({ page: 1, size: 1, status: 1 }),
    ]);
    document.getElementById('stat-concerts').textContent = concerts.data?.total || 0;
    document.getElementById('stat-orders').textContent = orders.data?.total || 0;
    document.getElementById('stat-users').textContent = users.data?.total || 0;
    document.getElementById('stat-paid').textContent = paid.data?.total || 0;
}

// ===== Concerts =====
async function loadConcerts(page = 1) {
    const res = await api.getConcerts({ page, size: 10 });
    if (res.code !== 200) return;
    document.getElementById('concert-table-body').innerHTML = res.data.records.map(c => `
        <tr>
            <td>${c.id}</td>
            <td style="font-weight:600;">${c.title}</td>
            <td>${c.artist}</td>
            <td>${c.city}</td>
            <td>${c.venue}</td>
            <td>${formatDate(c.showTime)}</td>
            <td><span class="status-badge ${STATUS_BADGE[c.status]}">${STATUS_MAP[c.status]}</span></td>
            <td class="actions">
                <button class="btn btn-outline btn-sm" onclick="editConcert(${c.id})"><i data-lucide="pencil" style="width:14px;height:14px;"></i></button>
                <button class="btn btn-danger btn-sm" onclick="deleteConcert(${c.id})"><i data-lucide="trash-2" style="width:14px;height:14px;"></i></button>
            </td>
        </tr>
    `).join('');
    renderPagination('concert-pagination', page, res.data.pages, 'loadConcerts');
    lucide.createIcons();
}

function showConcertForm(concert = null) {
    const form = document.getElementById('concert-form');
    form.reset();
    document.getElementById('concert-id').value = '';
    setCustomSelectValue('concert-status-select', '0');
    document.getElementById('concert-form-title').textContent = concert ? '编辑演唱会' : '新增演唱会';
    document.getElementById('tier-section').style.display = concert ? 'block' : 'none';
    if (concert) {
        document.getElementById('concert-id').value = concert.id;
        form.title.value = concert.title;
        form.artist.value = concert.artist;
        form.city.value = concert.city;
        form.venue.value = concert.venue;
        form.showTime.value = concert.showTime ? concert.showTime.substring(0, 16) : '';
        setCustomSelectValue('concert-status-select', String(concert.status));
        form.posterUrl.value = concert.posterUrl || '';
        form.description.value = concert.description || '';
        document.getElementById('tier-concert-id').value = concert.id;
        loadTiers(concert.id);
    }
    showModal('concert');
}

async function editConcert(id) {
    const res = await api.getConcert(id);
    if (res.code === 200) showConcertForm(res.data);
}

async function saveConcert(e) {
    e.preventDefault();
    const form = e.target;
    const id = document.getElementById('concert-id').value;
    const data = {
        title: form.title.value, artist: form.artist.value,
        city: form.city.value, venue: form.venue.value,
        showTime: form.showTime.value + ':00',
        status: parseInt(getCustomSelectValue('concert-status-select')),
        posterUrl: form.posterUrl.value || null,
        description: form.description.value || null,
    };
    const res = id ? await api.updateConcert(id, data) : await api.createConcert(data);
    if (res.code === 200) {
        showToast(id ? '更新成功' : '创建成功', 'success');
        if (!id && res.data?.id) { editConcert(res.data.id); }
        else { closeModal('concert'); }
        loadConcerts(1);
    } else { showToast(res.message, 'error'); }
}

async function deleteConcert(id) {
    if (!await showConfirm('删除后将无法恢复，确定要删除此演唱会吗？', '删除演唱会', '确定删除')) return;
    const res = await api.deleteConcert(id);
    if (res.code === 200) { showToast('删除成功', 'success'); loadConcerts(1); }
    else showToast(res.message, 'error');
}

// ===== Ticket Tiers =====
async function loadTiers(concertId) {
    const res = await api.getTicketTiers(concertId);
    if (res.code !== 200) return;
    document.getElementById('tier-list').innerHTML = res.data.map(t => `
        <div class="tier-item">
            <div class="tier-info">
                <strong>${t.tierName}</strong>
                <span>¥${t.price}</span>
                <span>库存: ${t.availableStock}/${t.totalStock}</span>
            </div>
            <button class="btn btn-danger btn-sm" onclick="deleteTier(${t.id}, ${concertId})">
                <i data-lucide="trash-2" style="width:14px;height:14px;"></i>
            </button>
        </div>
    `).join('') || '<p style="color:var(--text-muted);font-size:0.85rem;">暂无票档</p>';
    lucide.createIcons();
}

async function saveTier(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
        concertId: parseInt(document.getElementById('tier-concert-id').value),
        tierName: form.tierName.value,
        price: parseFloat(form.price.value),
        totalStock: parseInt(form.totalStock.value),
    };
    const res = await api.createTicketTier(data);
    if (res.code === 200) {
        showToast('票档添加成功', 'success');
        form.reset();
        document.getElementById('tier-concert-id').value = data.concertId;
        loadTiers(data.concertId);
    } else { showToast(res.message, 'error'); }
}

async function deleteTier(id, concertId) {
    if (!await showConfirm('确定要删除此票档吗？', '删除票档', '确定删除')) return;
    const res = await api.deleteTicketTier(id);
    if (res.code === 200) { showToast('删除成功', 'success'); loadTiers(concertId); }
    else showToast(res.message, 'error');
}

// ===== Orders =====
async function loadAdminOrders(page = 1) {
    const params = { page, size: 10 };
    const orderNo = document.getElementById('filter-order-no').value;
    const status = getCustomSelectValue('filter-order-status');
    if (orderNo) params.orderNo = orderNo;
    if (status !== '') params.status = status;

    const res = await api.getOrders(params);
    if (res.code !== 200) return;
    document.getElementById('order-table-body').innerHTML = res.data.records.map(o => `
        <tr>
            <td style="font-family:monospace;font-size:0.82rem;">${o.orderNo}</td>
            <td>${o.userId}</td>
            <td>${o.concertId}</td>
            <td>${o.ticketTierId}</td>
            <td>${o.quantity}</td>
            <td style="color:var(--gold);font-weight:600;">¥${o.totalPrice}</td>
            <td><span class="status-badge ${ORDER_BADGE[o.status]}">${ORDER_STATUS_MAP[o.status]}</span></td>
            <td>${formatDate(o.createdAt)}</td>
            <td class="actions">
                ${o.status === 1 ? `
                    <button class="btn btn-success btn-sm" onclick="approveOrder(${o.id})" title="审核通过"><i data-lucide="check" style="width:14px;height:14px;"></i></button>
                    <button class="btn btn-danger btn-sm" onclick="rejectOrder(${o.id})" title="审核拒绝"><i data-lucide="x" style="width:14px;height:14px;"></i></button>
                ` : '—'}
            </td>
        </tr>
    `).join('');
    renderPagination('order-pagination', page, res.data.pages, 'loadAdminOrders');
    lucide.createIcons();
}

// ===== Order Audit =====
async function approveOrder(id) {
    if (!await showConfirm('确定审核通过此订单？', '审核订单', '确定通过')) return;
    const res = await api.approveOrder(id);
    if (res.code === 200) { showToast('审核通过', 'success'); loadAdminOrders(1); }
    else showToast(res.message, 'error');
}

async function rejectOrder(id) {
    if (!await showConfirm('拒绝后将自动退款，确定审核拒绝此订单？', '审核订单', '确定拒绝')) return;
    const res = await api.rejectOrder(id);
    if (res.code === 200) { showToast('审核拒绝，已退款', 'success'); loadAdminOrders(1); }
    else showToast(res.message, 'error');
}

// ===== Users =====
async function loadUsers(page = 1) {
    const keyword = document.getElementById('filter-user-keyword').value;
    const params = { page, size: 10 };
    if (keyword) params.keyword = keyword;

    const res = await api.getUsers(params);
    if (res.code !== 200) return;
    document.getElementById('user-table-body').innerHTML = res.data.records.map(u => `
        <tr>
            <td>${u.id}</td>
            <td style="font-weight:600;">${u.username}</td>
            <td>${u.email || '—'}</td>
            <td>${u.phone || '—'}</td>
            <td>${u.role === 1 ? '<span class="status-badge badge-info">管理员</span>' : '普通用户'}</td>
            <td>
                <span class="status-badge ${u.status === 0 ? 'badge-success' : 'badge-danger'}">
                    ${u.status === 0 ? '正常' : '禁用'}
                </span>
            </td>
            <td>${formatDate(u.createdAt)}</td>
            <td class="actions">
                ${u.role !== 1 ? `
                    <button class="btn ${u.status === 0 ? 'btn-danger' : 'btn-success'} btn-sm"
                            onclick="toggleUserStatus(${u.id}, ${u.status === 0 ? 1 : 0})">
                        ${u.status === 0 ? '禁用' : '启用'}
                    </button>
                ` : ''}
            </td>
        </tr>
    `).join('');
    renderPagination('user-pagination', page, res.data.pages, 'loadUsers');
}

async function toggleUserStatus(id, status) {
    const action = status === 1 ? '禁用' : '启用';
    if (!await showConfirm(`确定要${action}此用户吗？`, `${action}用户`, `确定${action}`)) return;
    const res = await api.updateUserStatus(id, status);
    if (res.code === 200) { showToast(`${action}成功`, 'success'); loadUsers(1); }
    else showToast(res.message, 'error');
}

// ===== Notifications =====
async function publishNotification(e) {
    e.preventDefault();
    const form = e.target;
    const res = await api.publishNotification({ title: form.title.value, content: form.content.value });
    if (res.code === 200) { showToast('公告发布成功', 'success'); form.reset(); }
    else showToast(res.message, 'error');
}

// ===== Pagination =====
function renderPagination(containerId, current, total, callbackName) {
    const container = document.getElementById(containerId);
    if (total <= 1) { container.innerHTML = ''; return; }
    let html = `<button ${current <= 1 ? 'disabled' : ''} onclick="${callbackName}(${current - 1})">‹ 上一页</button>`;
    for (let i = 1; i <= total; i++) {
        html += `<button class="${i === current ? 'active' : ''}" onclick="${callbackName}(${i})">${i}</button>`;
    }
    html += `<button ${current >= total ? 'disabled' : ''} onclick="${callbackName}(${current + 1})">下一页 ›</button>`;
    container.innerHTML = html;
}

// ===== Utils =====
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}
