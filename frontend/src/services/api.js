import axios from 'axios';
import { setupAuthInterceptor } from './auth';
import { GATEWAY_BASE_URL } from './gatewayBase';

const api = axios.create({
    baseURL: `${GATEWAY_BASE_URL}/api`
});

setupAuthInterceptor(api);

export default api;