import axios from 'axios';
import { setupAuthInterceptor } from './auth';

const api = axios.create({
    baseURL: 'http://localhost:8081/api'
});

setupAuthInterceptor(api);

export default api;