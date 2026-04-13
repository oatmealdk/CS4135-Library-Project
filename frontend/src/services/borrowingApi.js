import axios from 'axios';
import { setupAuthInterceptor } from './auth';

const borrowingApi = axios.create({
    baseURL: 'http://localhost:8082/api'
});

setupAuthInterceptor(borrowingApi);

export default borrowingApi;
