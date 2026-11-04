const test = require('node:test');
const assert = require('node:assert/strict');
const {upstream, validateBanners, requestInput, dimensions, safeImage} = require('../banners.cjs');
function png(width, height) {
  const b = Buffer.alloc(24); Buffer.from([137,80,78,71,13,10,26,10]).copy(b);
  b.write('IHDR',12); b.writeUInt32BE(width,16); b.writeUInt32BE(height,20); return b;
}
test('banner path maps query and kind without changing legacy requests', () => {
  assert.deepEqual(validateBanners(requestInput('https://worker.test/banner?query=batman&kind=character')), {op:'banners',q:'batman',id:'',page:0,kind:'character'});
  assert.deepEqual(requestInput('https://worker.test/?op=search&q=Batman'), {op:'search',q:'Batman'});
});
test('rejects invalid resources, oversized queries, IDs and pagination', () => {
  for (const q of [{q:'x'}, {q:'a'.repeat(101)}, {q:'Batman',kind:'person'}, {q:'Batman',id:'1699'}, {q:'Batman',page:1}]) assert.throws(() => validateBanners(q), /invalid-input/);
});
test('only public Comic Vine upload URLs are accepted', () => {
  assert.equal(safeImage('https://comicvine.gamespot.com/a/uploads/original/1/art.jpg?api_key=no#x'), 'https://comicvine.gamespot.com/a/uploads/original/1/art.jpg');
  for (const u of ['http://comicvine.gamespot.com/a/uploads/a.jpg','https://127.0.0.1/a/uploads/a.jpg','https://evil.test/a/uploads/a.jpg','https://user:password@comicvine.gamespot.com/a/uploads/a.jpg','https://comicvine.gamespot.com/api/']) assert.equal(safeImage(u),'');
});
test('reads PNG, JPEG and WebP dimensions and rejects incomplete headers', () => {
  assert.deepEqual(dimensions(png(1200,400)), {width:1200,height:400});
  const jpeg = Buffer.alloc(21); Buffer.from([255,216,255,192,0,17,8,2,88,4,176]).copy(jpeg);
  assert.deepEqual(dimensions(jpeg), {width:1200,height:600});
  const webp = Buffer.alloc(30); webp.write('RIFF'); webp.write('WEBP',8); webp.write('VP8X',12); webp.writeUIntLE(1199,24,3); webp.writeUIntLE(399,27,3);
  assert.deepEqual(dimensions(webp), {width:1200,height:400});
  assert.equal(dimensions(Buffer.from([255,216,255])), null);
});
test('returns verified landscapes, not portraits or duplicate resolutions, and never leaks a key', async () => {
  const wide='https://comicvine.gamespot.com/a/uploads/original/1/wide.png';
  const portrait='https://comicvine.gamespot.com/a/uploads/original/1/portrait.png';
  let api=0, probes=0;
  const mock = async (raw, opts) => {
    const u = new URL(raw);
    if (u.pathname.startsWith('/api/')) {
      api++; assert.equal(u.searchParams.get('api_key'),'test-secret');
      return Response.json({status_code:1,results:u.pathname==='/api/search/' ? [{id:1}] : {id:1,name:'Batman',associated_images:[{original_url:wide,caption:'Spread'},{original_url:wide.replace('/original/','/scale_super/')},{original_url:portrait},{original_url:'https://evil.test/a/uploads/a.png'}]}});
    }
    probes++; assert.equal(opts.headers.Range,'bytes=0-65535'); assert.equal(opts.redirect,'error');
    return new Response(png(u.pathname.includes('portrait') ? 600 : 1200, u.pathname.includes('portrait') ? 900 : 400),{headers:{'content-type':'image/png'}});
  };
  const result = await upstream({q:'Batman'}, {comicVine:'test-secret'}, mock);
  assert.deepEqual(result.items,[wide]); assert.equal(result.artwork[0].width,1200);
  assert.equal(api,2); assert.equal(probes,2); assert.equal(JSON.stringify(result).includes('test-secret'),false);
});
test('returns an empty list rather than inventing a banner when image metadata cannot be verified', async () => {
  const mock=async raw => new URL(raw).pathname.startsWith('/api/') ? Response.json({status_code:1,results:new URL(raw).pathname==='/api/search/' ? [{id:1}] : {id:1,image:{super_url:'https://comicvine.gamespot.com/a/uploads/original/1/cover.jpg'}}}) : new Response(png(600,900),{headers:{'content-type':'image/png'}});
  assert.deepEqual((await upstream({q:'Batman'}, {comicVine:'test-secret'}, mock)).items,[]);
});
test('bounds metadata lookups, image probes and returned choices', async () => {
  let api=0, probes=0;
  const mock=async raw => {
    const u=new URL(raw);
    if(u.pathname.startsWith('/api/')) {api++;return Response.json({status_code:1,results:u.pathname==='/api/search/' ? Array.from({length:5},(_,i)=>({id:i+1})) : {id:1,name:'Batman',associated_images:Array.from({length:20},(_,i)=>({original_url:`https://comicvine.gamespot.com/a/uploads/original/1/art${i}.png`}))}});}
    probes++;return new Response(png(1200,400),{headers:{'content-type':'image/png'}});
  };
  const result=await upstream({q:'Batman'}, {comicVine:'test-secret'}, mock);
  assert.equal(api,3);assert.equal(probes,8);assert.equal(result.items.length,6);
});

test('uses known Portuguese aliases without exposing credentials to the caller', async () => {
  const mock=async raw=>{
    const u=new URL(raw);assert.equal(u.searchParams.get('query'),'Swamp Thing');
    return Response.json({status_code:1,results:[]});
  };
  assert.deepEqual((await upstream({q:'Monstro do Pântano',kind:'character'}, {comicVine:'test-secret'}, mock)).items,[]);
});
test('cancels the image stream when a server ignores the Range header', async () => {
  let cancelled=false;
  const mock=async raw=>{
    const u=new URL(raw);
    if(u.pathname.startsWith('/api/'))return Response.json({status_code:1,results:u.pathname==='/api/search/'?[{id:1}]:{id:1,image:{super_url:'https://comicvine.gamespot.com/a/uploads/original/1/wide.png'}}});
    const bytes=new Uint8Array(70000);bytes.set(png(1200,400));
    const body=new ReadableStream({start(controller){controller.enqueue(bytes);},cancel(){cancelled=true;}});
    return new Response(body,{headers:{'content-type':'image/png'}});
  };
  const result=await upstream({q:'Batman'}, {comicVine:'test-secret'}, mock);
  assert.equal(result.items.length,1);assert.equal(cancelled,true);
});
