"use strict"; // Shared provider normalization, used only on the server.
const crypto = require("node:crypto");
const image = value => typeof value === "string" && /^https:\/\//.test(value) ? value.slice(0, 2000) : "";
const text = (value, max = 5000) => typeof value === "string" ? value.slice(0, max) : "";
function pictures(item) {
  const seen = new Set();
  return [item.image?.medium_url || item.image?.super_url, ...(item.associated_images || []).map(i => i.original_url || i.medium_url)].map(image).filter(Boolean).filter(url => {
    const key = new URL(url).pathname.replace(/\/(scale_[^/]+|original)\//g, '/');
    if (seen.has(key)) return false; seen.add(key); return true;
  }).slice(0, 6);
}
function brief(item, type = "issue") {
  return { id: String(item.id || ""), type, name: text(item.name, 300), issue: text(String(item.issue_number || ""), 30),
    volume: text(item.volume?.name, 300), volumeId: String(item.volume?.id || ""), image: image(item.image?.medium_url || item.image?.super_url),
    banner: image(item.image?.screen_large_url || item.image?.super_url), publisher: text(item.publisher?.name, 160),
    date: text(item.cover_date || item.start_year, 30), description: text(item.description || item.deck, 18000),
    realName: text(item.real_name, 200), url: image(item.site_detail_url), covers: pictures(item) };
}
function detail(item, type) {
  return { ...brief(item, type), realName: text(item.real_name, 200), aliases: text(item.aliases, 1500),
    origin: text(item.origin?.name, 200), birth: text(item.birth, 100), firstIssue: item.first_appeared_in_issue ? brief(item.first_appeared_in_issue) : null,
    appearances: Number(item.count_of_issue_appearances || 0),
    characters: (item.character_credits || []).slice(0, 100).map(c => ({ id: String(c.id), name: text(c.name, 200) })),
    creators: (item.person_credits || item.creators || []).slice(0, 80).map(c => ({ id: String(c.id), name: text(c.name, 200), role: text(c.role, 160) })),
    powers: (item.powers || []).slice(0, 30).map(p => text(p.name, 150)),
    issues: (item.issue_credits || item.issues || []).slice(0, 30).map(c => brief(c)),
    teams: (item.teams || item.team_credits || []).slice(0, 30).map(c => text(c.name, 160)) };
}
function validate(query) {
  const op = String(query.op || "search");
  if (!["search", "catalog", "issue", "character", "person", "volume", "characters", "superhero"].includes(op)) throw new Error("invalid-input");
  const q = String(query.q || "").trim().slice(0, 100);
  const id = String(query.id || "");
  const page = Number(query.page || 0);
  if (!Number.isInteger(page) || page < 0 || page > 1000) throw new Error("invalid-input");
  if (["issue", "character", "person", "volume"].includes(op) && !/^\d{1,12}$/.test(id)) throw new Error("invalid-input");
  if (["search", "characters", "superhero"].includes(op) && q.length < 2) throw new Error("invalid-input");
  return { op, q, id, page };
}
const keyFor = value => crypto.createHash("sha256").update(JSON.stringify(value)).digest("hex");
async function upstream(input, secrets, fetcher = fetch) {
  const { op, q, id, page } = input;
  if (op === "superhero") {
    const r = await fetcher(`https://superheroapi.com/api/${secrets.superhero}/search/${encodeURIComponent(q)}`, { signal: AbortSignal.timeout(10000) });
    if (!r.ok) throw new Error("provider-unavailable");
    const j = await r.json();
    return { items: (j.results || []).slice(0, 30).map(c => ({ id: String(c.id), name: text(c.name, 200), realName: text(c.biography?.["full-name"], 200), image: image(c.image?.url), publisher: text(c.biography?.publisher, 160), description: text(c.work?.occupation), type: "superhero" })), hasMore: false };
  }
  const path = { issue: `issue/4000-${id}/`, character: `character/4005-${id}/`, person: `person/4040-${id}/`, volume: `volume/4050-${id}/` }[op] || (op === "catalog" ? "issues/" : "search/");
  const url = new URL("https://comicvine.gamespot.com/api/" + path);
  url.searchParams.set("api_key", secrets.comicVine); url.searchParams.set("format", "json");
  if (["search", "characters", "catalog"].includes(op)) {
    url.searchParams.set("limit", "24"); url.searchParams.set("offset", String(page * 24));
    if (op === "catalog") url.searchParams.set("sort", "cover_date:desc");
    else { url.searchParams.set("query", q); url.searchParams.set("resources", op === "characters" ? "character" : "issue"); }
    url.searchParams.set("field_list", "id,name,issue_number,volume,image,associated_images,cover_date,deck,publisher,real_name,aliases,site_detail_url");
  }
  const response = await fetcher(url, { headers: { "User-Agent": "Quadrinheiro/1.0 (personal reading journal)" }, signal: AbortSignal.timeout(12000) });
  if (!response.ok) throw new Error(response.status === 429 ? "provider-limit" : "provider-unavailable");
  const json = await response.json();
  if (json.status_code !== 1) throw new Error("provider-unavailable");
  if (!Array.isArray(json.results)) return detail(json.results || {}, op);
  return { items: json.results.map(i => brief(i, op === "characters" ? "character" : "issue")), hasMore: page * 24 + json.results.length < Number(json.number_of_total_results || 0) };
}
module.exports = { validate, upstream, brief, detail, pictures, keyFor };
