import axios from 'axios';

const searchApi = axios.create({
    baseURL: 'http://localhost:8085'
});

searchApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default searchApi;
