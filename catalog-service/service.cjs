'use strict';
const cv=require('./providers.cjs');
const metron=require('./metron.cjs');
async function upstream(input,secrets,fetcher=fetch){
 if(secrets.metron && (['search','catalog','issue','metron-issue','metron-character','metron-person'].includes(input.op)||(input.op==='covers'&&input.kind==='metron-issue'))){
  try {
   const result=await metron.upstream(input,secrets.metron,fetcher);
   if(result && (!result.items || result.items.length))return result;
  }catch(error){if(input.op.startsWith('metron-'))throw error;}
 }
 return cv.upstream(input,secrets,fetcher);
}
module.exports={...cv,upstream};
