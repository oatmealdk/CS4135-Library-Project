import axios from 'axios';
import { setupAuthInterceptor } from './auth';

const bookApi = axios.create({
    baseURL: 'http://localhost:8083/api'
});

setupAuthInterceptor(bookApi);

export default bookApi;