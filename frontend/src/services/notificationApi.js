import axios from 'axios';
import { setupAuthInterceptor } from './auth';
import { GATEWAY_BASE_URL } from './gatewayBase';

const notificationApi = axios.create({
    baseURL: `${GATEWAY_BASE_URL}/api`
});

setupAuthInterceptor(notificationApi);

export default notificationApi;
