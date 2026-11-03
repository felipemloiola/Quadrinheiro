import { CatalogStore, json } from './store.js';
import catalog from './catalog.cjs';
const { keyFor } = catalog;
export { CatalogStore };
const ips = new Map();
export default {
  async fetch(req, env) {
    if (new URL(req.url).pathname === '/characters.json') return env.ASSETS.fetch(req);
    if (req.method !== "GET") return json({ error: "method-not-allowed" }, 405);
    if (new URL(req.url).pathname === "/health") return json({ service: "Quadrinheiro", configured: !!env.COMIC_VINE_API_KEY });
    const ip = keyFor(req.headers.get("CF-Connecting-IP") || "unknown").slice(0, 16), minute = Math.floor(Date.now() / 60000);
    const old = ips.get(ip), count = old?.minute === minute ? old.count + 1 : 1;
    ips.set(ip, { minute, count }); if (ips.size > 2048) ips.delete(ips.keys().next().value);
    if (count > 60) return json({ error: "too-many-requests" }, 429);
    return env.CATALOG.get(env.CATALOG.idFromName("catalog-v4")).fetch(req);
  }
};

