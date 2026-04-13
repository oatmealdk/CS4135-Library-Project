import axios from 'axios';
import { setupAuthInterceptor } from './auth';

const searchApi = axios.create({
    baseURL: 'http://localhost:8085'
});

setupAuthInterceptor(searchApi);

export default searchApi;
