import axios from 'axios';
import { setupAuthInterceptor } from './auth';
import { GATEWAY_BASE_URL } from './gatewayBase';

const searchApi = axios.create({
    baseURL: GATEWAY_BASE_URL
});

setupAuthInterceptor(searchApi);

export default searchApi;
