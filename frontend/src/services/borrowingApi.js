import axios from 'axios';

const borrowingApi = axios.create({
    baseURL: 'http://localhost:8082/api'
});

borrowingApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default borrowingApi;
