package com.example.data

import com.example.model.ComicEntry
import com.example.model.ReadingStatus

/** Display only. Opening a page or its actions never changes a reading record. */
data class ComicReadingPresentation(val title: String, val detail: String, val action: String, val progress: Float? = null) {
    companion object {
        fun from(entry: ComicEntry?): ComicReadingPresentation = when (entry?.status) {
            null -> ComicReadingPresentation("Sem registro de leitura", "", "Registrar leitura")
            ReadingStatus.WANT_TO_READ -> ComicReadingPresentation("Quero ler", "Ainda não iniciada", "Iniciar leitura")
            ReadingStatus.READ -> ComicReadingPresentation("Lido",
                listOfNotNull(entry.rating.takeIf { it > 0 }?.let { "Sua avaliação: ${it.toString().removeSuffix(".0")}/5" },
                    entry.readDate.takeIf { it.isNotBlank() }?.let { "Concluído em $it" }).joinToString(" · "), "Editar registro")
            ReadingStatus.READING -> ComicReadingPresentation("Lendo",
                if (entry.pageCount > 0) "pág. ${entry.currentPage}/${entry.pageCount}" else "pág. ${entry.currentPage} · Total indisponível",
                "Atualizar leitura", if (entry.pageCount > 0) (entry.currentPage.toFloat() / entry.pageCount).coerceIn(0f, 1f) else null)
        }
    }
}
