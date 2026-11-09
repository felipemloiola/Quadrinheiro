const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('docs/index.html','utf8');
const source=fs.readFileSync('docs/progress.js','utf8');

test('Progress is routed on desktop and mobile without altering the private cloud schema',()=>{
 assert.ok(html.includes('data-page="progress"'));
 assert.ok(html.includes('id="progress"'));
 assert.ok(html.includes("['progress','Progresso']"));
 assert.ok(html.includes("progress:'Progresso'"));
 assert.ok(html.includes('data-mobile-goto="progress"'));
 assert.ok(fs.readFileSync('docs/mobile.js','utf8').includes("'progress'"));
 for(const match of html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g))new vm.Script(match[1]);
});

test('Progress uses text nodes, bounded public records and a single read without polling',async()=>{
 const nodes=new Map();let reads=0;
 function element(){return {textContent:'',children:[],setAttribute(){},append(...children){this.children.push(...children)},replaceChildren(...children){this.children=children},addEventListener(){}}}
 for(const id of ['progressEntries','progressSummary','progressUpdated','refreshProgress'])nodes.set(id,element());
 const data=JSON.parse(fs.readFileSync('docs/progress.json','utf8'));
 data.items[0].title='<img src=x onerror=alert(1)>';
 const sandbox={document:{getElementById:id=>nodes.get(id),createElement:element},window:{addEventListener(){}},Intl,Date,location:{hash:'#progress'},fetch:async()=>{reads++;return {ok:true,json:async()=>data}}};
 vm.runInNewContext(source,sandbox);
 await new Promise(resolve=>setImmediate(resolve));
 assert.equal(reads,1);
 assert.equal(nodes.get('progressEntries').children.length,data.items.length);
 assert.equal(nodes.get('progressEntries').children[0].children[0].children[0].textContent,data.items[0].title);
 assert.equal(nodes.get('refreshProgress').disabled,false);
 assert.equal(sandbox.setInterval,undefined);
});
