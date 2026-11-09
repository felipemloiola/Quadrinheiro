/* Public engineering progress only. Account notes and dashboard state remain in Firestore. */
(()=>{
 'use strict';
 const root=document.getElementById('progressEntries');
 const summary=document.getElementById('progressSummary');
 const updated=document.getElementById('progressUpdated');
 const button=document.getElementById('refreshProgress');
 if(!root||!summary||!updated||!button)return;
 const labels={done:'Verificado',work:'Em andamento',pending:'Pendente',review:'Em validação'};
 let loaded=false,busy=false;
 function text(tag,value,className){const el=document.createElement(tag);el.textContent=value;if(className)el.className=className;return el}
 async function load(){
  if(busy)return;busy=true;button.disabled=true;root.setAttribute('aria-busy','true');
  try{
   const response=await fetch('progress.json',{cache:'no-store'});
   if(!response.ok)throw Error('HTTP '+response.status);
   const data=await response.json();
   if(!Array.isArray(data.items)||data.items.length>30)throw Error('Invalid progress');
   summary.textContent=data.summary;
   const date=new Date(data.updatedAt);
   updated.textContent='Atualizado em '+new Intl.DateTimeFormat('pt-BR',{dateStyle:'medium',timeStyle:'short',timeZone:'America/Sao_Paulo'}).format(date);
   const cards=data.items.map(item=>{
    const card=text('article','', 'panel progress-entry');
    const head=text('div','', 'progress-entry-head');
    head.append(text('h2',item.title,'panel-title'),text('span',labels[item.status]||labels.pending,'pill '+(labels[item.status]?item.status:'pending')));
    card.append(head,text('p',item.detail,'progress-detail'));
    if(item.evidence)card.append(text('p',item.evidence,'progress-evidence'));
    return card;
   });
   root.replaceChildren(...cards);loaded=true;
  }catch{
   updated.textContent='Não foi possível consultar o progresso. Tente novamente.';
  }finally{busy=false;button.disabled=false;root.setAttribute('aria-busy','false')}
 }
 button.addEventListener('click',load);
 window.addEventListener('hashchange',()=>{if(location.hash==='#progress'&&!loaded)load()});
 // One bounded read; no polling and no changes to private dashboard records.
 load();
})();
