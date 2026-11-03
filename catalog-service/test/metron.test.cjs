const {test}=require('node:test');
const assert=require('node:assert/strict');
const {searchPlan,rank,detail,upstream}=require('../metron.cjs');
const {validate}=require('../catalog.cjs');

test('Portuguese titles, format words and explicit issue numbers preserve intent',()=>{
 assert.equal(searchPlan('batman ano um').query,'Batman Year One');
 assert.equal(searchPlan('quero ler batman ano um capa dura').query,'Batman Year One');
 assert.deepEqual(searchPlan('Batman #404'),{query:'batman',number:'404'});
});
test('Year One is not replaced by Batman and Robin and Watchmen starts with its collected edition',()=>{
 const original={id:1,series:{id:1,name:'Batman: Year One - The Deluxe Edition'},issue:'Batman: Year One HC #1'};
 const other={id:2,series:{id:2,name:'Batman and Robin: Year One'},issue:'Batman and Robin: Year One #1'};
 assert.deepEqual(rank([other,original],'batman ano um').map(x=>x.id),[1]);
 const monthly=n=>({id:n,series:{id:3,name:'Watchmen'},number:String(n),issue:`Watchmen #${n}`,store_date:`1987-0${n}-01`});
 const compact={id:99,series:{id:4,name:'Watchmen: DC Compact Comics Edition'},issue:'Watchmen TPB #1',store_date:'2024-01-01'};
 assert.deepEqual(rank([monthly(2),monthly(1),compact],'watchmen').map(x=>x.id),[99,1,2]);
});
test('real page field, creator roles and variant covers survive normalization',()=>{
 const book=detail({id:184296,page:144,series:{name:'Batman Year One',series_type:{name:'Hardcover'}},image:'https://example.com/main',
  credits:[{id:1,creator:'Frank Miller',role:[{name:'Writer'}]}],characters:[{id:2,name:'Batman'}],variants:[{image:'https://example.com/variant'}]});
 assert.equal(book.pageCount,144);assert.equal(book.creators[0].role,'Writer');assert.equal(book.characters[0].id,'mt-2');assert.equal(book.covers.length,2);
 assert.equal(detail({id:1,series:{name:'Unknown'}}).pageCount,0);
});
test('Absolute Batman remains a monthly issue rather than being classified as an absolute collection',()=>{
 assert.equal(detail({id:1,series:{name:'Absolute Batman'},issue:'Absolute Batman #1',number:'1'}).issue,'1');
});
test('catalog filters recent publications and pagination does not skip a provider page boundary',async()=>{
 const calls=[];
 const fetcher=async(url,options)=>{
  calls.push(new URL(url));assert.equal(options.headers.Authorization,'Bearer test-token');
  const page=Number(new URL(url).searchParams.get('page'));
  return {ok:true,json:async()=>({count:200,next:page===1?'next':null,results:Array.from({length:100},(_,i)=>({id:(page-1)*100+i,series:{name:'Title'},number:String(i),store_date:'2026-01-01'}))})};
 };
 const result=await upstream(validate({op:'catalog',page:4}),'test-token',fetcher);
 assert.deepEqual(result.items.map(x=>Number(x.id)),Array.from({length:24},(_,i)=>96+i));
 assert.ok(calls[0].searchParams.has('store_date_range_after'));
 assert.equal(JSON.stringify(result).includes('test-token'),false);
});
test('all metron resources require numeric identifiers and unknown paths are refused',()=>{
 assert.throws(()=>validate({op:'metron-issue',id:'../collection'}));
 assert.throws(()=>validate({op:'metron-collection',id:'1'}));
 assert.equal(validate({op:'metron-issue',id:'184296'}).id,'184296');
});
