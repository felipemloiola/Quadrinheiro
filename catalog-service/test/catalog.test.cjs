const { test } = require('node:test');
const assert = require('node:assert/strict');
const { validate, upstream, pictures, detail } = require('../catalog.cjs');
test('reject arbitrary resources, paths and unbounded offsets', () => {
  for (const q of [{op:'proxy',url:'https://example.com'}, {op:'issue',id:'../../secret'}, {op:'catalog',page:-1}, {op:'search',q:'x'}]) assert.throws(()=>validate(q));
});
test('cover alternatives are distinct images of this issue and limited to six', () => {
  const urls = pictures({image:{medium_url:'https://example.com/main'},associated_images:Array.from({length:8},(_,i)=>({original_url:'https://example.com/'+i}))});
  assert.equal(urls.length,6); assert.equal(urls[0],'https://example.com/main');
});
test('credits preserve character identities and production roles', () => {
  const d=detail({id:1,character_credits:[{id:2,name:'Batman'}],person_credits:[{id:3,name:'Writer',role:'writer'}]},'issue');
  assert.equal(d.characters[0].id,'2');assert.equal(d.creators[0].role,'writer');
});
test('provider credentials never appear in normalized responses', async () => {
  const mock=async()=>({ok:true,json:async()=>({status_code:1,number_of_total_results:1,results:[{id:1,name:'HQ',api_detail_url:'https://example.com?api_key=secret'}]})});
  const r=await upstream(validate({op:'search',q:'Batman'}),{comicVine:'secret'},mock);
  assert.equal(JSON.stringify(r).includes('secret'),false);assert.equal(r.items.length,2);
});

test('volume credits use characters and people fields without pretending to be issue credits',()=>{
 const d=detail({id:3622,characters:[{id:1,name:'Rorschach'}],people:[{id:2,name:'Alan Moore'}]},'volume');
 assert.equal(d.characters.length,1);assert.equal(d.creators[0].name,'Alan Moore');
});
test('same art in multiple resolutions is not a variant and cover art is not an interior banner',()=>{
 const d=detail({image:{super_url:'https://comicvine.gamespot.com/a/uploads/scale_large/0/1/1-cover.jpg'},associated_images:[{original_url:'https://comicvine.gamespot.com/a/uploads/original/0/1/1-cover.jpg'}]},'issue');
 assert.equal(d.covers.length,1);assert.equal(d.banner,'');
});
test('collection art stays scoped to the selected volume',async()=>{
 let called='';const mock=async url=>{called=String(url);return {ok:true,json:async()=>({status_code:1,results:[{image:{super_url:'https://example.com/cover'}}]})}};
 const r=await upstream(validate({op:'covers',kind:'volume',id:'3622'}),{comicVine:'secret'},mock);
 assert.ok(called.includes('volume%3A3622'));assert.equal(r.scope,'collection');assert.equal(r.items.length,1);
});
