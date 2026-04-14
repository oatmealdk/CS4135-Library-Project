/** Single entry URL (Spring Cloud Gateway). Override with REACT_APP_GATEWAY_URL when building. */
const raw = process.env.REACT_APP_GATEWAY_URL || 'http://localhost:8080';
export const GATEWAY_BASE_URL = raw.replace(/\/$/, '');
