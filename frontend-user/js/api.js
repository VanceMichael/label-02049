// API 基础配置
const API_BASE = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : '/api';

const WS_BASE = window.location.hostname === 'localhost'
    ? 'ws://localhost:8080/ws'
    : `ws://${window.location.host}/ws`;

// 通用请求方法
async function request(url, options = {}) {
    const token = localStorage.getItem('token');
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
            // 仅在有 token 时才清除并 reload，避免未登录用户死循环
            if (token) {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
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

// API 方法
const api = {
    // Auth
    register: (data) => request('/auth/register', { method: 'POST', body: data }),
    login: (data) => request('/auth/login', { method: 'POST', body: data }),

    // User
    getProfile: () => request('/user/profile'),
    updateProfile: (data) => request('/user/profile', { method: 'PUT', body: data }),

    // Concert
    getConcerts: (params) => {
        const query = new URLSearchParams(params).toString();
        return request(`/concert/list?${query}`);
    },
    getConcert: (id) => request(`/concert/${id}`),

    // Ticket Tier
    getTicketTiers: (concertId) => request(`/ticket-tier/concert/${concertId}`),

    // Grab
    grabTicket: (data) => request('/grab', { method: 'POST', body: data }),

    // Order
    getMyOrders: (params) => {
        const query = new URLSearchParams(params).toString();
        return request(`/order/my?${query}`);
    },
    payOrder: (id) => request(`/order/${id}/pay`, { method: 'POST' }),
    cancelOrder: (id) => request(`/order/${id}/cancel`, { method: 'POST' }),
    refundOrder: (id) => request(`/order/${id}/refund`, { method: 'POST' }),

    // Notification
    getNotifications: (params) => {
        const query = new URLSearchParams(params).toString();
        return request(`/notification/list?${query}`);
    },
    getUnreadCount: () => request('/notification/unread-count'),
    markRead: (id) => request(`/notification/${id}/read`, { method: 'PUT' }),
    markAllRead: () => request('/notification/read-all', { method: 'PUT' }),
};
