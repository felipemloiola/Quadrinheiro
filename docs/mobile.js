/* Quadrinheiro · navegação compacta para celulares.
   Reutiliza o roteador e os ícones do painel desktop; não duplica estado. */
(()=>{
 'use strict';
 const trigger=document.getElementById('mobileMoreToggle');
 const panel=document.getElementById('mobileNavPanel');
 const backdrop=document.getElementById('mobileNavBackdrop');
 const closeButton=document.getElementById('mobileNavClose');
 if(!trigger||!panel||!backdrop||!closeButton)return;
 const media=window.matchMedia('(max-width:620px)');
 const pages=['repository','integrations','performance','decisions'];
 const titles={repository:'Repositório',integrations:'Integrações',performance:'Performance',decisions:'Decisões'};
 let opened=false;
 for(const page of pages){
  const target=panel.querySelector('[data-mobile-goto="'+page+'"]');
  const source=document.querySelector('.sidebar .nav .nav-btn[data-page="'+page+'"]');
  if(!target||!source)continue;
  const icon=source.querySelector('svg');
  if(icon)target.appendChild(icon.cloneNode(true));
  const label=document.createElement('span');label.textContent=titles[page];target.appendChild(label);
  target.addEventListener('click',()=>{
   close(false);
   window.QuadrinheiroPanel?.go(page);
  });
 }
 function open(){
  if(!media.matches||opened)return;
  opened=true;
  panel.hidden=false;
  backdrop.hidden=false;
  trigger.setAttribute('aria-expanded','true');
  closeButton.focus({preventScroll:true});
 }
 function close(restoreFocus=true){
  if(!opened)return;
  opened=false;
  panel.hidden=true;
  backdrop.hidden=true;
  trigger.setAttribute('aria-expanded','false');
  if(restoreFocus&&media.matches)trigger.focus({preventScroll:true});
 }
 function sync(page){
  const current=page||location.hash.slice(1)||'overview';
  trigger.setAttribute('aria-pressed',String(pages.includes(current)));
  panel.querySelectorAll('[data-mobile-goto]').forEach(button=>{
   button.setAttribute('aria-current',button.dataset.mobileGoto===current?'page':'false');
  });
  close(false);
 }
 trigger.addEventListener('click',()=>opened?close():open());
 closeButton.addEventListener('click',()=>close());
 backdrop.addEventListener('click',()=>close());
 document.addEventListener('keydown',event=>{
  if(!opened)return;
  if(event.key==='Escape'){event.preventDefault();close();return}
  if(event.key!=='Tab')return;
  const items=[closeButton,...panel.querySelectorAll('[data-mobile-goto]')];
  const first=items[0],last=items[items.length-1];
  if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus()}
  else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus()}
 });
 if(media.addEventListener)media.addEventListener('change',event=>{if(!event.matches)close(false)});
 else if(media.addListener)media.addListener(event=>{if(!event.matches)close(false)});
 window.addEventListener('hashchange',()=>sync(location.hash.slice(1)));
 window.QuadrinheiroMobileNav={sync,close};
 sync(location.hash.slice(1)||'overview');
})();
