import { DurableObject } from 'cloudflare:workers';
import catalog from './catalog.cjs';
const {validate,upstream,keyFor}=catalog;
export const json=(body,status=200)=>Response.json(body,{status,headers:{'X-Content-Type-Options':'nosniff','Cache-Control':status===200?'public, max-age=600':'no-store'}});
export class CatalogStore extends DurableObject {
 constructor(ctx,env){super(ctx,env);this.pending=new Map();}
 async cached(key){
  const slot='v2:'+key.slice(0,2),meta=await this.ctx.storage.get(slot);
  if(meta?.key!==key||meta.until<Date.now())return null;
  const chunks=await Promise.all(Array.from({length:meta.chunks},(_,i)=>this.ctx.storage.get(slot+':'+i)));
  try{return JSON.parse(chunks.join(''));}catch{return null;}
 }
 async save(key,body){
  const slot='v2:'+key.slice(0,2),raw=JSON.stringify(body);if(raw.length>700000)return;
  const chunks=Array.from({length:Math.ceil(raw.length/50000)},(_,i)=>raw.slice(i*50000,(i+1)*50000));
  await this.ctx.storage.transaction(async tx=>{
   const old=await tx.get(slot);
   for(let i=0;i<chunks.length;i++)await tx.put(slot+':'+i,chunks[i]);
   for(let i=chunks.length;i<(old?.chunks||0);i++)await tx.delete(slot+':'+i);
   await tx.put(slot,{key,chunks:chunks.length,until:Date.now()+86400000*3});
  });
 }
 async permit(resource,amount=1,limit=180,period=3600000){
  await this.ctx.storage.transaction(async tx=>{const key='budget:'+resource,old=await tx.get(key),bucket=Math.floor(Date.now()/period),count=old?.bucket===bucket?old.count:0;
   if(count+amount>limit)throw Error('provider-limit');await tx.put(key,{bucket,count:count+amount});});
 }
 async load(input){
  const key=keyFor({...input,schema:"52-final"}),saved=await this.cached(key);if(saved)return saved;
  if(this.pending.has(key))return this.pending.get(key);
  const work=(async()=>{
   let body;
   if(input.op==='translated'){
    const original=await this.load({...input,op:input.kind,kind:'issue',q:''});
    const source=String(original.description||'').replace(/<[^>]+>/g,' ').replace(/&nbsp;|&#160;/g,' ').replace(/&amp;/g,'&').replace(/\s+/g,' ').trim().slice(0,10000);
    if(!source)return {descriptionPt:''};
    if(original.language==='pt')return {descriptionPt:source};
    await this.permit('translation',source.length,100000,86400000);
    const chunks=source.match(/.{1,850}(?:\s|$)|.{1,850}/g)||[source];
    const translated=[];
    for(let i=0;i<chunks.length;i+=3){
     const part=await Promise.all(chunks.slice(i,i+3).map(async text=>{
      const r=await this.env.AI.run('@cf/meta/m2m100-1.2b',{text,source_lang:'en',target_lang:'pt'});
      return r.translated_text||r.result?.translated_text||'';
     }));translated.push(...part);
    }
    if(translated.some(s=>!s))throw Error('translation-unavailable');
    body={descriptionPt:translated.join(' ')};
   }else{
    body=await upstream(input,{comicVine:this.env.COMIC_VINE_API_KEY,superhero:this.env.SUPERHERO_API_KEY,metron:this.env.METRON_API_KEY?.trim()},async(url,opts)=>{
     const u=new URL(url);if(u.hostname==='metron.cloud')await this.permit('metron');if(u.hostname==='comicvine.gamespot.com')await this.permit(u.pathname.split('/').filter(Boolean)[1]||'search');
     return fetch(url,opts);
    });
   }
   await this.save(key,body);return body;
  })().finally(()=>this.pending.delete(key));
  this.pending.set(key,work);return work;
 }
 async fetch(req){
  try{return json(await this.load(validate(Object.fromEntries(new URL(req.url).searchParams))));}
  catch(e){const status=e.message==='invalid-input'?400:e.message==='provider-limit'?429:503;return json({error:status===400?'invalid-input':status===429?'provider-limit':'catalog-unavailable'},status);}
 }
}
