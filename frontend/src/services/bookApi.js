import axios from 'axios';

const bookApi = axios.create({
    baseURL: 'http://localhost:8083/api'
});

bookApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default bookApi;