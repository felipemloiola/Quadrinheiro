"use strict";
const crypto = require('node:crypto');
const text = (v, max=18000) => typeof v === 'string' ? v.trim().slice(0,max) : '';
const image = v => typeof v === 'string' && /^https:\/\//.test(v) ? v.slice(0,2000) : '';
const imageKey = url => { try { return new URL(url).pathname.replace(/\/(scale_[^/]+|screen_[^/]+|original)\//g,'/'); } catch { return url; } };
const uniqueImages = list => { const seen=new Set(); return list.map(image).filter(Boolean).filter(u=>{const k=imageKey(u);if(seen.has(k))return false;seen.add(k);return true;}).slice(0,6); };
function pictures(item) { return uniqueImages([item.image?.super_url || item.image?.medium_url, ...(item.associated_images||[]).map(i=>i.original_url||i.super_url||i.medium_url)]); }
function embeddedImages(html) {
  return [...String(html||'').matchAll(/<img\b[^>]*(?:data-src|src)=["']([^"']+)["'][^>]*>/gi)]
    .map(m=>m[1].replaceAll('&amp;','&')).filter(u=>/^https:\/\/(comicvine\.gamespot\.com|www\.comicvine\.com|comicvine1\.cbsistatic\.com|static\.comicvine\.com)\/a\/uploads\//.test(u));
}
function brief(item,type='issue') {
 return {id:String(item.id||''),type,name:text(item.name,300),issue:String(item.issue_number||''),volume:text(item.volume?.name,300),volumeId:String(item.volume?.id||''),
 image:image(item.image?.medium_url||item.image?.super_url),banner:'',publisher:text(item.publisher?.name,160),date:text(item.cover_date||item.start_year,30),
 description:text(item.description||item.deck),realName:text(item.real_name,200),url:image(item.site_detail_url),covers:pictures(item),appearances:Number(item.count_of_issue_appearances||0)};
}
function detail(item,type) {
 const artwork=uniqueImages(embeddedImages(item.description).filter(u=>!pictures(item).some(p=>imageKey(p)===imageKey(u))));
 const coverArt=[...String(item.description||'').matchAll(/<(?:figure|a)\b[^>]*>[\s\S]*?<\/\s*(?:figure|a)>/gi)].filter(m=>/variant\s+cover|alternate\s+cover/i.test(m[0])).flatMap(m=>embeddedImages(m[0]));
 return {...brief(item,type),banner:artwork[0]||'',artwork,covers:uniqueImages([...pictures(item),...coverArt]),
 portraits:uniqueImages([item.image?.super_url,...embeddedImages(item.description)]),realName:text(item.real_name,200),aliases:text(item.aliases,1500),
 origin:text(item.origin?.name,200),birth:text(item.birth,100),appearances:Number(item.count_of_issue_appearances||0),
 characters:(item.character_credits||item.characters||[]).slice(0,80).map(c=>({id:String(c.id),name:text(c.name,200)})),
 creators:(item.person_credits||item.creators||item.people||[]).slice(0,60).map(c=>({id:String(c.id),name:text(c.name,200),role:text(c.role,160)})),
 powers:(item.powers||[]).slice(0,30).map(c=>text(c.name,150)),issues:(item.issue_credits||item.issues||[]).slice(0,24).map(c=>brief(c)),creditsScope:type==='volume'?'series':'issue'};
}
const allowed=['search','catalog','issue','character','person','volume','characters','superhero','portraits','translated','covers','metron-issue','metron-character','metron-person'];
function validate(query) {
 const op=String(query.op||'search'),q=String(query.q||'').trim().slice(0,100),id=String(query.id||''),page=Number(query.page||0),kind=String(query.kind||'issue');
 if(!allowed.includes(op)||!Number.isInteger(page)||page<0||page>1000)throw Error('invalid-input');
 if(['issue','character','person','volume','translated','covers','metron-issue','metron-character','metron-person'].includes(op)&&!/^\d{1,12}$/.test(id))throw Error('invalid-input');
 if(['search','characters','superhero','portraits'].includes(op)&&q.length<2)throw Error('invalid-input');
 if(!['issue','volume','character','person','metron-issue','metron-character','metron-person'].includes(kind))throw Error('invalid-input');
 return {op,q,id,page,kind};
}
const keyFor=v=>crypto.createHash('sha256').update(JSON.stringify(v)).digest('hex');
async function upstream(input,secrets,fetcher=fetch) {
 const {op,id,page}=input;
 const queryKey=input.q.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9 ]/g,' ').replace(/\s+/g,' ').trim();
 const localizedQueries={'batman ano um':'Batman Year One','ano um':'Batman Year One','piada mortal':'Batman The Killing Joke','batman a piada mortal':'Batman The Killing Joke','batman o cavaleiro das trevas':'Batman The Dark Knight Returns','homem aranha':'Spider-Man','mulher maravilha':'Wonder Woman','monstro do pantano':'Swamp Thing','lanterna verde':'Green Lantern','demolidor':'Daredevil','homem de ferro':'Iron Man','capitao america':'Captain America','vingadores':'Avengers','v de vinganca':'V for Vendetta'};
 const q=op==='search' ? localizedQueries[queryKey] || input.q : input.q;
 async function cv(path,params={}) {
  const u=new URL('https://comicvine.gamespot.com/api/'+path);u.searchParams.set('api_key',secrets.comicVine);u.searchParams.set('format','json');
  for(const [k,v] of Object.entries(params))u.searchParams.set(k,String(v));
  const r=await fetcher(u,{headers:{'User-Agent':'Quadrinheiro/1.0 personal reading journal'},signal:AbortSignal.timeout(8500)});
  if(!r.ok)throw Error(r.status===429?'provider-limit':'provider-unavailable');const j=await r.json();if(j.status_code!==1)throw Error('provider-unavailable');return j;
 }
 if(op==='covers') {
  if(input.kind==='volume') {
   const j=await cv('issues/',{filter:`volume:${id}`,sort:'issue_number:asc',limit:6,field_list:'id,image'});
   return {items:uniqueImages((j.results||[]).map(i=>i.image?.super_url||i.image?.medium_url)),scope:'collection'};
  }
  if(input.kind!=='issue')throw Error('invalid-input');
  return {items:detail((await cv(`issue/4000-${id}/`)).results||{},'issue').covers,scope:'issue'};
 }
 if(op==='superhero') {
  const r=await fetcher(`https://superheroapi.com/api/${secrets.superhero}/search/${encodeURIComponent(q)}`,{signal:AbortSignal.timeout(7000)});if(!r.ok)throw Error('provider-unavailable');const j=await r.json();
  return {items:(j.results||[]).slice(0,30).map(c=>({id:'sh-'+c.id,name:text(c.name,200),realName:text(c.biography?.['full-name'],200),image:image(c.image?.url),publisher:text(c.biography?.publisher,160),type:'superhero'})),hasMore:false};
 }
 if(['issue','character','person','volume'].includes(op)) {
  const prefix={issue:4000,character:4005,person:4040,volume:4050}[op];const j=await cv(`${op}/${prefix}-${id}/`);const body=detail(j.results||{},op);
  if(op==='issue'&&body.volumeId&&(!body.description||!body.characters.length||!body.publisher)) {
   try { const volume=detail((await cv(`volume/4050-${body.volumeId}/`)).results||{},'volume');
    body.publisher ||=volume.publisher;body.description ||=volume.description;body.banner ||=volume.banner;body.artwork=uniqueImages([...body.artwork,...volume.artwork]);
    if(!body.characters.length){body.characters=volume.characters;body.creditsScope='series';}if(!body.creators.length)body.creators=volume.creators;
   }catch(e){if(e.message==='provider-limit')throw e;}
  }
  return body;
 }
 if(op==='portraits') {
  const canonical={batman:'1699','hal jordan':'11202','green lantern':'11202'}[q.toLowerCase()];
  const found=canonical||String((await cv('search/',{query:q,resources:'character',limit:8,field_list:'id,name,count_of_issue_appearances'})).results.sort((a,b)=>(b.count_of_issue_appearances||0)-(a.count_of_issue_appearances||0))[0]?.id||'');
  if(!found)return {items:[]};const d=detail((await cv(`character/4005-${found}/`)).results||{},'character');return {items:d.portraits};
 }
 const params={limit:24,offset:page*24,field_list:'id,name,issue_number,volume,image,associated_images,cover_date,deck,publisher,real_name,aliases,count_of_issue_appearances,site_detail_url'};
 if(op==='catalog'){params.sort='cover_date:desc';params.filter=`cover_date:1900-01-01|${new Date().toISOString().slice(0,10)}`;}
 else {params.query=q;params.resources=op==='characters'?'character':'issue';}
 if(op==='catalog'){params.sort='start_year:desc';params.filter='count_of_issues:1|10000';params.field_list+=',start_year,count_of_issues';}
 if(op==='search'){
  const volumes=await cv('search/',{...params,resources:'volume',limit:12,offset:page*12,field_list:params.field_list+',start_year,count_of_issues'});
  const editions=await cv('search/',{...params,limit:12,offset:page*12});
  const ranked=(volumes.results||[]).sort((a,b)=>Number(!!(b.deck||b.description))-Number(!!(a.deck||a.description)) || Number(b.start_year||0)-Number(a.start_year||0));
  return {items:[...ranked.map(i=>brief(i,'volume')),...(editions.results||[]).map(i=>brief(i))],hasMore:page*12+12<Math.max(Number(volumes.number_of_total_results||0),Number(editions.number_of_total_results||0))};
 }
 const j=await cv(op==='catalog'?'volumes/':'search/',params);
 const items=(j.results||[]).map(i=>brief(i,op==='characters'?'character':op==='catalog'?'volume':'issue'));
 if(op==='characters')items.sort((a,b)=>b.appearances-a.appearances);
 return {items,hasMore:page*24+items.length<Number(j.number_of_total_results||0)};
}
module.exports={validate,upstream,brief,detail,pictures,keyFor,embeddedImages,uniqueImages};
