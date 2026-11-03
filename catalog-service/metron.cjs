'use strict';

const clean = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9 ]/g, ' ').replace(/\s+/g, ' ').trim();
const aliases = {
 'batman ano um':'Batman Year One', 'ano um':'Batman Year One', 'a piada mortal':'The Killing Joke',
 'piada mortal':'The Killing Joke', 'o cavaleiro das trevas':'The Dark Knight Returns',
 'homem aranha':'Spider-Man', 'mulher maravilha':'Wonder Woman', 'monstro do pantano':'Swamp Thing',
 'lanterna verde':'Green Lantern', 'demolidor':'Daredevil', 'homem de ferro':'Iron Man',
 'capitao america':'Captain America', 'vingadores':'Avengers', 'v de vinganca':'V for Vendetta'
};
function searchPlan(query) {
 let words=clean(query.replace(/#\s*\d+\b/g,'')).replace(/^(quero (?:ler|encontrar)|buscar|pesquisar|procuro|mostre|quadrinhos?|hqs?)\s+/, '');
 const number=words.match(/(?:numero|edicao|issue|n)\s+(\d+)\b/)?.[1] || query.match(/#\s*(\d+)\b/)?.[1];
 words=words.replace(/(?:numero|edicao|issue|n)\s+\d+\b/g,'').replace(/\b(?:capa dura|edicao de luxo|encadernado|hq|quadrinho)\b/g,'').trim();
 for(const [pt,en] of Object.entries(aliases))if(words.includes(pt)){words=words.replace(pt,en);break;}
 return {query:words.replace(/\s+/g,' ').trim(),number};
}
const collection = item => /\b(?:HC|TPB?|Hardcover|Omnibus|Deluxe|Absolute Edition|Compact Comics|Graphic Novel)\b/i.test(`${item.issue||''} ${item.series?.series_type?.name||''} ${item.series?.name||''}`);
function rank(items, query) {
 const tokens=clean(searchPlan(query).query).split(' ').filter(Boolean);
 const titleTokens=item=>clean(item.series?.name||item.name).split(' ').filter(t=>t && (tokens.includes(t)|| !['the','a','and','edition','deluxe','hardcover','hc','tpb','tp','dc','comics','compact','absolute','omnibus','essentials'].includes(t)));
 const score=item=>{
  const name=titleTokens(item), hit=tokens.filter(t=>name.includes(t)).length;
  return hit*100 - name.filter(t=>!tokens.includes(t)).length*35 + (collection(item)?45:0) - (hit<tokens.length?200:0);
 };
 const exact=items.filter(item=>{const words=titleTokens(item);return tokens.every(t=>words.includes(t))&&words.every(t=>tokens.includes(t))});
 return (exact.length?exact:items).slice().sort((a,b)=>score(b)-score(a) || (a.series?.id===b.series?.id ? Number(a.number||0)-Number(b.number||0):String(b.store_date||b.cover_date||'').localeCompare(String(a.store_date||a.cover_date||''))));
}
function brief(item) {
 const collected=collection(item);
 return {id:String(item.id),type:'metron-issue',name:item.series?.name||item.title||item.issue||'',volume:item.series?.name||'',
  issue:collected?'':String(item.number||''),image:item.image||'',date:item.store_date||item.cover_date||'',
  publisher:item.publisher?.name||'',description:item.desc||'',pageCount:Number(item.page_count||item.page||0),isbn:item.isbn||'',
  format:collected ? (/hardcover|\bHC\b/i.test(item.series?.series_type?.name||item.issue||'')?'Capa dura':'Encadernado (TPB)'):'Edição',
  language:item.series?.language||'en',url:item.resource_url||'',cvId:item.cv_id?String(item.cv_id):'',covers:[item.image,...(item.variants||[]).map(v=>v.image)].filter(Boolean).slice(0,6)};
}
function detail(item,type='metron-issue') {
 if(type!=='metron-issue')return {id:String(item.id),type,name:item.name||'',image:item.image||'',description:item.desc||'',url:item.resource_url||'',
  aliases:Array.isArray(item.alias)?item.alias.join(', '):item.alias||'',realName:'',characters:[],creators:[],covers:[],banner:'',creditsScope:'issue',attribution:item.attribution||[]};
 return {...brief(item),banner:'',characters:(item.characters||[]).map(c=>({id:'mt-'+c.id,name:c.name})),
  creators:(item.credits||[]).map(c=>({id:'mt-'+c.id,name:c.creator,role:(c.role||[]).map(r=>r.name).join(', ')})),
  covers:[item.image,...(item.variants||[]).map(v=>v.image)].filter(Boolean).filter((v,i,a)=>a.indexOf(v)===i).slice(0,6),
  issues:[],creditsScope:'issue',attribution:item.attribution||[]};
}
async function upstream(input,token,fetcher=fetch) {
 async function get(path,params={}) {
  const url=new URL('https://metron.cloud/api/'+path);
  for(const [key,value] of Object.entries(params))if(value!=null)url.searchParams.set(key,String(value));
  const response=await fetcher(url,{headers:{Authorization:'Bearer '+token,'User-Agent':'Quadrinheiro reading journal'},signal:AbortSignal.timeout(6500)});
  if(!response.ok)throw Error(response.status===429?'provider-limit':'provider-unavailable');
  return response.json();
 }
 if(input.op==='search'||input.op==='catalog') {
  const plan=searchPlan(input.q),start=input.page*24,apiPage=Math.floor(start/100)+1,offset=start%100;
  const params=input.op==='search'?{series_q:plan.query,number:plan.number}:{store_date_range_after:new Date(Date.now()-730*86400000).toISOString().slice(0,10),store_date_range_before:new Date().toISOString().slice(0,10)};
  const page=await get('issue/',{...params,page:apiPage});
  let source=input.op==='search'?rank(page.results||[],input.q):[...(page.results||[])].sort((a,b)=>String(b.store_date||b.cover_date||'').localeCompare(String(a.store_date||a.cover_date||'')));
  let selected=source.slice(offset,offset+24);
  if(selected.length<24&&page.next){const next=await get('issue/',{...params,page:apiPage+1});selected=selected.concat((next.results||[]).slice(0,24-selected.length));}
  return {items:selected.map(brief),hasMore:start+selected.length<Number(page.count||0)};
 }
 if(input.op==='metron-issue')return detail(await get(`issue/${input.id}/`));
 if(input.op==='metron-character'||input.op==='metron-person'){
  const resource=input.op==='metron-character'?'character':'creator',body=detail(await get(`${resource}/${input.id}/`),input.op);
  if(resource==='character')body.issues=(await get(`character/${input.id}/issue_list/`)).results?.slice(0,24).map(brief)||[];
  return body;
 }
 if(input.op==='covers'&&input.kind==='metron-issue')return {items:detail(await get(`issue/${input.id}/`)).covers,scope:'issue'};
 if(input.op==='issue'){
  const match=(await get('issue/',{cv_id:input.id})).results?.[0];
  if(!match)return null;
  return {...detail(await get(`issue/${match.id}/`)),id:input.id,type:'issue'};
 }
 return null;
}
module.exports={upstream,brief,detail,rank,searchPlan};
