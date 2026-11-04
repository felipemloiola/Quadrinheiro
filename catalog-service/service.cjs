'use strict';
const cv=require('./providers.cjs');
const metron=require('./metron.cjs');
const banners=require('./banners.cjs');
function validate(query){
 return query.op==='banners' ? banners.validateBanners(query) : cv.validate(query);
}
async function upstream(input,secrets,fetcher=fetch){
 if(input.op==='banners')return banners.upstream(input,secrets,fetcher);
 if(secrets.metron && (['search','catalog','issue','metron-issue','metron-character','metron-person'].includes(input.op)||(input.op==='covers'&&input.kind==='metron-issue'))){
  try {
   const result=await metron.upstream(input,secrets.metron,fetcher);
   if(result && (!result.items || result.items.length))return result;
  }catch(error){if(input.op.startsWith('metron-'))throw error;}
 }
 return cv.upstream(input,secrets,fetcher);
}
module.exports={...cv,validate,upstream,requestInput:banners.requestInput};
