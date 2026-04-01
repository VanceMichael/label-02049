const API_BASE = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : '/api';

async function request(url, options = {}) {
    const token = localStorage.getItem('admin_token');
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const config = { headers, ...options };
    if (config.body && typeof config.body === 'object') {
        config.body = JSON.stringify(config.body);
    }

    try {
        const response = await fetch(`${API_BASE}${url}`, config);
        const data = await response.json();
        if (data.code === 401) {
            if (token) {
                localStorage.removeItem('admin_token');
                localStorage.removeItem('admin_user');
                location.reload();
            }
            return data;
        }
        return data;
    } catch (error) {
        console.error('请求失败:', error);
        return { code: 500, message: '网络请求失败' };
    }
}

const api = {
    login: (data) => request('/auth/login', { method: 'POST', body: data }),

    // Concert
    getConcerts: (params) => request(`/concert/list?${new URLSearchParams(params)}`),
    getConcert: (id) => request(`/concert/${id}`),
    createConcert: (data) => request('/concert', { method: 'POST', body: data }),
    updateConcert: (id, data) => request(`/concert/${id}`, { method: 'PUT', body: data }),
    deleteConcert: (id) => request(`/concert/${id}`, { method: 'DELETE' }),

    // Ticket Tier
    getTicketTiers: (concertId) => request(`/ticket-tier/concert/${concertId}`),
    createTicketTier: (data) => request('/ticket-tier', { method: 'POST', body: data }),
    deleteTicketTier: (id) => request(`/ticket-tier/${id}`, { method: 'DELETE' }),

    // Order
    getOrders: (params) => request(`/order/list?${new URLSearchParams(params)}`),
    approveOrder: (id) => request(`/order/${id}/approve`, { method: 'POST' }),
    rejectOrder: (id) => request(`/order/${id}/reject`, { method: 'POST' }),

    // User
    getUsers: (params) => request(`/user/list?${new URLSearchParams(params)}`),
    updateUserStatus: (id, status) => request(`/user/${id}/status?status=${status}`, { method: 'PUT' }),

    // Notification
    publishNotification: (data) => request('/notification/publish', { method: 'POST', body: data }),
};
