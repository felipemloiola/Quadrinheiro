package com.example.data

import com.example.model.ComicEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contrato seguro e opcional para enriquecimento semântico via Firebase AI / Gemini.
 * 
 * Regras estritas de segurança:
 * 1. NUNCA envia biblioteca bruta, notas, avaliações, emails ou IDs sensíveis (Zero PII).
 * 2. A IA NUNCA pode inventar HQs, autores, sinopses ou links.
 * 3. Todo ID retornado pela IA é estritamente validado contra o candidatePool enviado.
 * 4. Em caso de offline, falha, timeout (3.5s) ou resposta inválida, o app continua
 *    operando 100% com o motor determinístico local.
 */
object FirebaseAiRecommendationEnhancer {

    private const val TIMEOUT_MS = 3500L

    data class AiEnrichmentResult(
        val orderedCandidateIds: List<String>,
        val reasonOverrides: Map<String, String>,
        val relatedSearchTerms: List<String>
    )

    /**
     * Valida em runtime se a IA está provisionada e pronta.
     * Como libs.firebase.ai e ai_logic ainda não estão ativos em build.gradle.kts/firebase_applet_config,
     * retorna false de forma segura e não bloqueante.
     */
    fun isAiAvailable(): Boolean {
        return false
    }

    /**
     * Cria payload anonimizado leve (<= 1.8 KB).
     */
    fun buildSafeAnonymizedPayload(
        taste: UserTasteProfile,
        candidates: List<ComicEntry>
    ): String {
        val root = JSONObject()
        val topSeriesArr = JSONArray()
        taste.readingSeries.take(2).forEach { topSeriesArr.put(it.first.take(30)) }
        taste.readSeries.take(2).forEach { topSeriesArr.put(it.first.take(30)) }
        root.put("topSeriesSignals", topSeriesArr)

        val topAuthorsArr = JSONArray()
        taste.topAuthors.take(3).forEach { topAuthorsArr.put(it.take(30)) }
        root.put("topAuthorsSignals", topAuthorsArr)

        val charsArr = JSONArray()
        taste.favoriteCharacters.take(3).forEach { charsArr.put(it.substringBefore('(').trim().take(30)) }
        root.put("favoriteCharacters", charsArr)

        root.put("favoriteUniverse", taste.favoriteUniverse.take(30))

        val poolArr = JSONArray()
        candidates.take(16).forEach { c ->
            val item = JSONObject()
            item.put("candidateId", c.id)
            item.put("title", c.displayTitle.take(50))
            item.put("authors", c.authors.take(50))
            poolArr.put(item)
        }
        root.put("candidatePool", poolArr)

        return root.toString()
    }

    /**
     * Valida rigidamente a resposta da IA contra o pool real de candidatos.
     */
    fun validateAiResponse(
        rawResponseJson: String,
        candidatePool: List<ComicEntry>
    ): AiEnrichmentResult? {
        return try {
            val root = JSONObject(rawResponseJson)
            val allowedIds = candidatePool.map { it.id }.toSet()

            val orderedIds = mutableListOf<String>()
            val idsArr = root.optJSONArray("orderedCandidateIds")
            if (idsArr != null) {
                for (i in 0 until idsArr.length()) {
                    val id = idsArr.optString(i)
                    // Segurança: descarta qualquer ID que não esteja no pool real!
                    if (id in allowedIds && id !in orderedIds) {
                        orderedIds.add(id)
                    }
                }
            }

            val reasonOverrides = mutableMapOf<String, String>()
            val reasonsObj = root.optJSONObject("reasonOverrides")
            if (reasonsObj != null) {
                for (key in reasonsObj.keys()) {
                    if (key in allowedIds) {
                        val reasonText = reasonsObj.optString(key, "").trim()
                        if (reasonText.length in 8..55) {
                            reasonOverrides[key] = reasonText
                        }
                    }
                }
            }

            val searchTerms = mutableListOf<String>()
            val termsArr = root.optJSONArray("relatedSearchQueries")
            if (termsArr != null) {
                for (i in 0 until termsArr.length()) {
                    val term = termsArr.optString(i, "").trim()
                    if (term.length in 3..40) {
                        searchTerms.add(term)
                    }
                }
            }

            if (orderedIds.isEmpty() && reasonOverrides.isEmpty() && searchTerms.isEmpty()) {
                null
            } else {
                AiEnrichmentResult(
                    orderedCandidateIds = orderedIds,
                    reasonOverrides = reasonOverrides,
                    relatedSearchTerms = searchTerms.take(3)
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Tenta enriquecer a lista de recomendações. Se offline ou falhar, retorna a lista original intacta.
     */
    suspend fun enhanceOrFallback(
        taste: UserTasteProfile,
        candidates: List<RecommendedComic>
    ): List<RecommendedComic> = withContext(Dispatchers.Default) {
        if (!isAiAvailable() || candidates.isEmpty()) {
            return@withContext candidates
        }

        val enriched = withTimeoutOrNull(TIMEOUT_MS) {
            // Placeholder seguro para chamada assíncrona Firebase.ai
            null
        } ?: return@withContext candidates

        enriched
    }
}
