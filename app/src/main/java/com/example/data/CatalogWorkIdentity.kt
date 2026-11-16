package com.example.data

import com.example.model.ComicEntry

/** Presentation grouping only: never rewrites saved edition IDs, ISBNs or page counts. */
internal object CatalogWorkIdentity {
    fun series(raw:String):String = CatalogSearchPolicy.normalize(raw)
        .replace(Regex("\\b(batman|superman|wonder woman|mulher maravilha) (?:absolut[oa]|assolut[oa]|absolute)\\b"),"absolute $1")
        .replace(Regex("\\b(vol|volume|livro|book)\\s*\\d+\\b"),"")
        .replace(Regex("\\b(?:19|20)\\d{2}\\b"),"").replace(Regex("\\s*\\d+$"),"").trim()
    fun number(c:ComicEntry)=c.issueNumber.takeIf {it>0} ?: Regex("(?:#|\\s)(\\d+)\\s*$").find(c.title)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    fun collected(c:ComicEntry)=c.pageCount>=80 || Regex("(?i)coleção|collection|encadernado|tpb|capa dura|graphic novel|omnibus").containsMatchIn(c.format)
    fun coverage(text:String):Pair<Int,Int>? = Regex("(?i)(?:collects?|contiene|re[uú]ne|inclui|cont[eé]m)[\\s\\S]{0,120}?\\b(\\d{1,4})\\s*[-–]\\s*#?(\\d{1,4})\\b")
        .find(text)?.let {it.groupValues[1].toInt() to it.groupValues[2].toInt()}?.takeIf {it.first>0 && it.second>=it.first}
    fun preferred(items:List<ComicEntry>):List<ComicEntry> {
        val groups=linkedMapOf<String,ComicEntry>()
        fun score(c:ComicEntry)= (if(c.externalId.startsWith("mt-")||c.externalId.startsWith("cv-")) 16 else 0)+
            (if(c.pageCount>0) 8 else 0)+(if(c.authors.isNotBlank()) 6 else 0)+(if(c.synopsis.length>80 || c.synopsisPtBr.length>80) 8 else 0)+
            (if(c.publisher.isNotBlank()) 2 else 0)+(if(c.editionLanguage=="pt") 4 else 0)
        for(c in items) {
            val number=number(c)
            val base=series(c.seriesName.ifBlank {c.displayTitle})
            // Unnumbered collections with missing search metadata cannot be proven equivalent.
            val unknown=collected(c) && number==0 && coverage(c.synopsis)==null
            val year=Regex("\\b((?:19|20)\\d{2})\\b").find(c.title)?.value.orEmpty()
            val prefix="$base|$number|${collected(c)}|"
            val compatible=groups.keys.firstOrNull {it.startsWith(prefix) && (year.isBlank() || it.substringAfterLast('|').isBlank() || it.substringAfterLast('|')==year)}
            val key=if(unknown || base.length<3)c.externalId.ifBlank {c.id} else compatible ?: "$prefix$year"
            val old=groups[key]
            if(old==null || score(c)>score(old))groups[key]=c
        }
        return groups.values.toList()
    }
    fun companion(comic:ComicEntry,details:CatalogDetails):CatalogDetails? {
        val text=comic.synopsis.ifBlank {comic.synopsisPtBr}
        val range=coverage(text)
        val otherRange=coverage(details.description)
        fun names(raw:String)=Regex("\\b[A-Z][a-z]{2,}\\b").findAll(raw).map {it.value}.filterNot {it in setOf("Batman","Absolute","Absoluto","The","Collects","Contiene","Volume")}.toSet()
        val sameStory=if(range!=null && otherRange!=null)range==otherRange
            else number(comic)>0 && number(comic)==details.issueNumber && details.pageCount>=80 && names(text).intersect(names(details.description)).size>=3
        if(series(comic.seriesName.ifBlank {comic.displayTitle})!=series(details.title) || !sameStory || !collected(comic))return null
        return details.copy(title=comic.displayTitle,image=comic.coverUrl.ifBlank {details.image},description=comic.synopsis.ifBlank {details.description},
            pageCount=comic.pageCount,isbn=comic.isbn,publisher=comic.publisher,date=comic.releaseYear.takeIf {it>0}?.toString().orEmpty(),format=comic.format,creditsScope="work")
    }
}
