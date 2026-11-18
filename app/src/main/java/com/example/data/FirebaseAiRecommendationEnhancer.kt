package com.example.data

import android.content.Context
import com.example.model.ComicEntry
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contrato seguro e opcional para enriquecimento semântico via Firebase AI Logic (Gemini).
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
    private const val MODEL_NAME = "gemini-2.5-flash"

    data class AiEnrichmentResult(
        val orderedCandidateIds: List<String>,
        val reasonOverrides: Map<String, String>,
        val relatedSearchTerms: List<String>
    )

    /**
     * Valida em runtime se o FirebaseApp padrão está inicializado com credenciais reais
     * (evitando chamadas em testes unitários sem backend ou projetos demo/offline).
     */
    fun isAiAvailable(context: Context? = null): Boolean {
        return try {
            val app = if (context != null) {
                FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            } else {
                runCatching { FirebaseApp.getInstance() }.getOrNull()
            } ?: return false
            val opts = app.options
            opts.projectId?.isNotBlank() == true &&
                !opts.projectId!!.startsWith("demo-") &&
                opts.apiKey.isNotBlank() &&
                !opts.apiKey.startsWith("fake-")
        } catch (_: Throwable) {
            false
        }
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

    internal fun applyValidatedEnrichment(
        candidates: List<RecommendedComic>,
        enrichment: AiEnrichmentResult
    ): List<RecommendedComic> {
        if (candidates.isEmpty()) return candidates
        val byId = candidates.associateBy { it.comic.id }
        val ordered = mutableListOf<RecommendedComic>()
        val seen = HashSet<String>()

        for (id in enrichment.orderedCandidateIds) {
            val existing = byId[id] ?: continue
            if (!seen.add(id)) continue
            val refinedReason = enrichment.reasonOverrides[id]?.take(42)?.takeIf { it.length >= 8 } ?: existing.reasonLabel
            ordered.add(existing.copy(reasonLabel = refinedReason))
        }

        for (item in candidates) {
            if (seen.add(item.comic.id)) {
                val refinedReason = enrichment.reasonOverrides[item.comic.id]?.take(42)?.takeIf { it.length >= 8 } ?: item.reasonLabel
                ordered.add(item.copy(reasonLabel = refinedReason))
            }
        }
        return ordered
    }

    /**
     * Tenta enriquecer a lista de recomendações via Firebase AI Logic (Gemini Flash).
     * Se desativado, sem credenciais ou em caso de timeout/erro, retorna a lista determinística original intacta.
     */
    suspend fun enhanceOrFallback(
        taste: UserTasteProfile,
        candidates: List<RecommendedComic>,
        context: Context? = null,
        enabled: Boolean = true
    ): List<RecommendedComic> = withContext(Dispatchers.IO) {
        if (!enabled || candidates.isEmpty() || !isAiAvailable(context)) {
            return@withContext candidates
        }

        val slice = candidates.take(16).map { it.comic }
        val payload = buildSafeAnonymizedPayload(taste, slice)
        val prompt = buildString {
            append("Reordene os candidateId do JSON abaixo priorizando afinidade literária real com os sinais do leitor ")
            append("e gere motivos curtos e verídicos em português do Brasil (8 a 42 caracteres) em reasonOverrides. ")
            append("NUNCA invente IDs fora de candidatePool. Retorne APENAS JSON no formato ")
            append("{\"orderedCandidateIds\":[...],\"reasonOverrides\":{\"id\":\"motivo\"},\"relatedSearchQueries\":[]}.\n")
            append(payload)
        }

        val enriched = try {
            withTimeoutOrNull(TIMEOUT_MS) {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = MODEL_NAME,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        temperature = 0.2f
                        maxOutputTokens = 512
                    }
                )
                val rawText = model.generateContent(prompt).text.orEmpty()
                val validated = validateAiResponse(rawText, slice) ?: return@withTimeoutOrNull null
                applyValidatedEnrichment(candidates, validated)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Throwable) {
            null
        }

        enriched ?: candidates
    }
}
