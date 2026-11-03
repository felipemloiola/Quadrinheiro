import { DurableObject } from "cloudflare:workers";
import catalog from "./catalog.cjs";
const { validate, upstream, keyFor } = catalog;
const json = (body, status = 200) => Response.json(body, { status, headers: {
  "X-Content-Type-Options": "nosniff", "Cache-Control": status === 200 ? "public, max-age=3600" : "no-store"
} });
export class CatalogStore extends DurableObject {
  constructor(ctx, env) { super(ctx, env); this.pending = new Map(); }
  async fetch(req) {
    let input;
    try { input = validate(Object.fromEntries(new URL(req.url).searchParams)); }
    catch { return json({ error: "invalid-input" }, 400); }
    const key = keyFor(input), slot = "cache:" + key.slice(0, 2);
    const saved = await this.ctx.storage.get(slot);
    if (saved?.key === key && saved.until > Date.now()) return json(saved.body);
    if (this.pending.has(key)) return json(await this.pending.get(key));
    const work = (async () => {
      const resource = ["search", "characters"].includes(input.op) ? "search" : input.op;
      await this.ctx.storage.transaction(async tx => {
        const old = await tx.get("budget:" + resource), hour = Math.floor(Date.now() / 3600000);
        const count = old?.hour === hour ? old.count : 0;
        if (count >= 180) throw new Error("provider-limit");
        await tx.put("budget:" + resource, { hour, count: count + 1 });
      });
      const body = await upstream(input, { comicVine: this.env.COMIC_VINE_API_KEY, superhero: this.env.SUPERHERO_API_KEY });
      if (JSON.stringify(body).length < 700000) await this.ctx.storage.put(slot, { key, body, until: Date.now() + 86400000 });
      return body;
    })().finally(() => this.pending.delete(key));
    this.pending.set(key, work);
    try { return json(await work); }
    catch (error) { return json({ error: error.message === "provider-limit" ? "provider-limit" : "catalog-unavailable" }, error.message === "provider-limit" ? 429 : 503); }
  }
}
const ips = new Map();
export default {
  async fetch(req, env) {
    if (req.method !== "GET") return json({ error: "method-not-allowed" }, 405);
    if (new URL(req.url).pathname === "/health") return json({ service: "Quadrinheiro", configured: !!env.COMIC_VINE_API_KEY });
    const ip = keyFor(req.headers.get("CF-Connecting-IP") || "unknown").slice(0, 16), minute = Math.floor(Date.now() / 60000);
    const old = ips.get(ip), count = old?.minute === minute ? old.count + 1 : 1;
    ips.set(ip, { minute, count }); if (ips.size > 2048) ips.delete(ips.keys().next().value);
    if (count > 60) return json({ error: "too-many-requests" }, 429);
    return env.CATALOG.get(env.CATALOG.idFromName("catalog-v1")).fetch(req);
  }
};
