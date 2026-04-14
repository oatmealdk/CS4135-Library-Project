import axios from 'axios';
import { setupAuthInterceptor } from './auth';
import { GATEWAY_BASE_URL } from './gatewayBase';

const borrowingApi = axios.create({
    baseURL: `${GATEWAY_BASE_URL}/api`
});

setupAuthInterceptor(borrowingApi);

export default borrowingApi;
