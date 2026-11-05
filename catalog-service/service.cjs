'use strict';
const cv=require('./providers.cjs');
const metron=require('./metron.cjs');
const banners=require('./banners.cjs');
function validate(query){
 return query.op==='banners' ? banners.validateBanners(query) : cv.validate(query);
}
async function upstream(input,secrets,fetcher=fetch){
  const providerUpper = String(input.provider || '').toUpperCase();
  const providerMetron = providerUpper === 'METRON' || providerUpper === 'METRO';
  const metronAllowed = ['search','catalog','issue','metron-issue','metron-character','metron-person'].includes(input.op) || ((input.op === 'covers') && (input.kind === 'metron-issue'));
  const wantsMetron = (providerMetron || providerUpper === 'MULTI') && metronAllowed;
  if(wantsMetron&&secrets.metron&&String(secrets.metron).trim()){
    try {
     const result=await metron.upstream(input,secrets.metron,fetcher);
     if(result && (!result.items || result.items.length))return result;
    }catch(error){if(input.op.startsWith('metron-'))throw error;}
  }
  if(input.op==='banners')return banners.upstream(input,secrets,fetcher);
  return cv.upstream(input,secrets,fetcher);
}
module.exports={...cv,validate,upstream,requestInput:banners.requestInput};
