import axios from 'axios';
import { setupAuthInterceptor } from './auth';
import { GATEWAY_BASE_URL } from './gatewayBase';

const bookApi = axios.create({
    baseURL: `${GATEWAY_BASE_URL}/api`
});

setupAuthInterceptor(bookApi);

export default bookApi;