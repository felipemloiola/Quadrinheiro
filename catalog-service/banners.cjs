"use strict";
const HOSTS = new Set(['comicvine.gamespot.com', 'www.comicvine.com', 'comicvine1.cbsistatic.com', 'static.comicvine.com']);
const TYPES = {volume: 4050, issue: 4000, character: 4005};
function safeImage(raw) {
  try {
    const u = new URL(raw);
    if (u.protocol !== 'https:' || !HOSTS.has(u.hostname) || u.username || u.password || (u.port && u.port !== '443') || !/^\/(?:a\/)?uploads\//.test(u.pathname)) return '';
    u.search = ''; u.hash = '';
    return u.href.length <= 2000 ? u.href : '';
  } catch { return ''; }
}
const imageKey = raw => { const u = new URL(raw); return u.hostname + u.pathname.replace(/\/(?:scale_[^/]+|screen_[^/]+|original)\//g, '/'); };
function requestInput(raw) {
  const u = new URL(raw), input = Object.fromEntries(u.searchParams);
  if (u.pathname === '/banner') { input.op = 'banners'; input.q = input.query ?? input.q ?? ''; }
  return input;
}
function validateBanners(query) {
  const q = String(query.q || '').trim(), kind = String(query.kind || 'volume');
  if (q.length < 2 || q.length > 100 || !Object.hasOwn(TYPES, kind) || String(query.id || '') || Number(query.page || 0) !== 0) throw Error('invalid-input');
  return {op: 'banners', q, id: '', page: 0, kind};
}
function dimensions(b) {
  const word = (at, n) => String.fromCharCode(...b.slice(at, at + n));
  if (b.length >= 24 && b[0] === 137 && word(1, 3) === 'PNG' && b[4] === 13 && b[5] === 10 && b[6] === 26 && b[7] === 10 && word(12, 4) === 'IHDR') {
    const v = new DataView(b.buffer, b.byteOffset, b.byteLength);
    return {width: v.getUint32(16), height: v.getUint32(20)};
  }
  if (b.length >= 30 && word(0, 4) === 'RIFF' && word(8, 4) === 'WEBP') {
    const u24 = at => b[at] | b[at + 1] << 8 | b[at + 2] << 16;
    if (word(12, 4) === 'VP8X') return {width: u24(24) + 1, height: u24(27) + 1};
    if (word(12, 4) === 'VP8 ' && b[23] === 157 && b[24] === 1 && b[25] === 42) return {width: (b[26] | b[27] << 8) & 16383, height: (b[28] | b[29] << 8) & 16383};
  }
  if (b.length >= 25 && word(0, 4) === 'RIFF' && word(8, 4) === 'WEBP' && word(12, 4) === 'VP8L' && b[20] === 47) return {width: 1 + (b[21] | (b[22] & 63) << 8), height: 1 + ((b[22] >> 6) | b[23] << 2 | (b[24] & 15) << 10)};
  if (b[0] !== 255 || b[1] !== 216) return null;
  let p = 2;
  const sof = new Set([192, 193, 194, 195, 197, 198, 199, 201, 202, 203, 205, 206, 207]);
  while (p + 3 < b.length) {
    if (b[p++] !== 255) return null;
    while (p < b.length && b[p] === 255) p++;
    const marker = b[p++];
    if (marker === 218 || marker === 217) return null;
    if (marker === 1 || (marker >= 208 && marker <= 215)) continue;
    if (p + 2 > b.length) return null;
    const size = b[p] << 8 | b[p + 1];
    if (size < 2 || p + size > b.length) return null;
    if (sof.has(marker) && size >= 7) return {width: b[p + 5] << 8 | b[p + 6], height: b[p + 3] << 8 | b[p + 4]};
    p += size;
  }
  return null;
}
async function pool(items, limit, fn) {
  let cursor = 0; const result = new Array(items.length);
  await Promise.all(Array.from({length: Math.min(limit, items.length)}, async () => {
    while (cursor < items.length) { const i = cursor++; result[i] = await fn(items[i]); }
  }));
  return result;
}
async function probe(url, fetcher) {
  let reader;
  try {
    const r = await fetcher(url, {headers: {Range: 'bytes=0-65535'}, redirect: 'error', signal: AbortSignal.timeout(2500)});
    if (!r.ok || !r.body || !/^image\/(?:jpeg|png|webp)(?:;|$)/i.test(r.headers.get('content-type') || '')) { await r.body?.cancel(); return null; }
    reader = r.body.getReader(); const chunks = []; let count = 0;
    while (count < 65536) {
      const part = await reader.read(); if (part.done) break;
      const chunk = part.value.subarray(0, 65536 - count); chunks.push(chunk); count += chunk.length;
    }
    const data = new Uint8Array(count); let at = 0;
    for (const chunk of chunks) { data.set(chunk, at); at += chunk.length; }
    const d = dimensions(data);
    return d && d.width >= 480 && d.height >= 200 && d.width <= 32768 && d.height <= 32768 && d.width / d.height >= 1.6 && d.width / d.height <= 4 ? d : null;
  } catch { return null; }
  finally { if (reader) await reader.cancel().catch(() => {}); }
}
function candidates(doc, kind) {
  const title = String(doc.name || '').slice(0, 300), id = String(doc.id || '');
  const list = (doc.associated_images || []).slice(0, 16).map(i => ({url: i.super_url || i.original_url || i.medium_url, priority: /spread|panoram|concept|landscape|wraparound/i.test(i.caption || '') ? 0 : 2}));
  for (const m of String(doc.description || '').slice(0, 100000).matchAll(/<img\b[^>]*(?:data-src|src)=["']([^"']+)["'][^>]*>/gi)) list.push({url: m[1].replaceAll('&amp;', '&'), priority: 1});
  list.push({url: doc.image?.super_url || doc.image?.original_url || doc.image?.medium_url, priority: 3});
  return list.sort((a, b) => a.priority - b.priority).map(i => ({url: safeImage(i.url), title, id, kind})).filter(i => i.url);
}
async function upstream(input, secrets, fetcher = fetch) {
  input = validateBanners(input);
  if (!secrets.comicVine) throw Error('provider-unavailable');
  async function cv(path, params = {}) {
    const u = new URL('https://comicvine.gamespot.com/api/' + path);
    u.searchParams.set('api_key', secrets.comicVine); u.searchParams.set('format', 'json');
    for (const [k, v] of Object.entries(params)) u.searchParams.set(k, String(v));
    const r = await fetcher(u, {headers: {'User-Agent': 'Quadrinheiro/1.0 personal reading journal'}, redirect: 'error', signal: AbortSignal.timeout(5000)});
    if (!r.ok) throw Error(r.status === 429 ? 'provider-limit' : 'provider-unavailable');
    const j = await r.json(); if (j.status_code !== 1) throw Error('provider-unavailable'); return j;
  }
  const normalized = input.q.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
  const aliases = {'batman ano um': 'Batman Year One', 'batman a piada mortal': 'Batman The Killing Joke', 'monstro do pantano': 'Swamp Thing', 'mulher maravilha': 'Wonder Woman', 'homem aranha': 'Spider-Man', 'lanterna verde': 'Green Lantern', 'demolidor': 'Daredevil', 'v de vinganca': 'V for Vendetta'};
  const search = await cv('search/', {query: aliases[normalized] || input.q, resources: input.kind, limit: 2, field_list: 'id,name'});
  const records = (search.results || []).filter(r => /^\d{1,12}$/.test(String(r.id))).slice(0, 2);
  const docs = await pool(records, 2, async r => {
    try { return (await cv(`${input.kind}/${TYPES[input.kind]}-${r.id}/`, {field_list: 'id,name,image,associated_images,description'})).results || {}; }
    catch (e) { if (e.message === 'provider-limit') throw e; return {}; }
  });
  const seen = new Set();
  const images = docs.flatMap(doc => candidates(doc, input.kind)).filter(i => {
    const key = imageKey(i.url); if (seen.has(key)) return false; seen.add(key); return true;
  }).slice(0, 8);
  const checked = await pool(images, 2, async image => {
    const size = await probe(image.url, fetcher);
    return size ? {url: image.url, ...size, title: image.title, resourceId: image.id, kind: image.kind} : null;
  });
  const artwork = checked.filter(Boolean).slice(0, 6);
  return {items: artwork.map(i => i.url), artwork, hasMore: false, attribution: 'Comic Vine'};
}
module.exports = {upstream, validateBanners, requestInput, dimensions, safeImage};
