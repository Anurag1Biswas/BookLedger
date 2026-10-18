// Relative on purpose: in Docker nginx proxies /api to the backend; with `ng serve` proxy.conf.json does.
export const environment = {
  apiUrl: '/api'
};
