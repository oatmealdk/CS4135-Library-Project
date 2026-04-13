import axios from 'axios';

const notificationApi = axios.create({
    baseURL: 'http://localhost:8084/api'
});

notificationApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default notificationApi;
