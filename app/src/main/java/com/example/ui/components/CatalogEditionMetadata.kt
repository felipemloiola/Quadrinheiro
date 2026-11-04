package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CatalogPagePolicy
import com.example.ui.theme.LocalAppleMaterials

@Composable
internal fun CatalogEditionMetadata(
    entryId: String,
    synopsis: String,
    pages: String,
    metadataLoading: Boolean,
    pageEditorOpen: Boolean,
    pagesError: Boolean,
    onOpenPageEditor: () -> Unit,
    onChangePages: (String) -> Unit,
    translationLoading: Boolean = false,
    translationFailed: Boolean = false,
    onRetryTranslation: (() -> Unit)? = null
) {
    val s = LocalAppleMaterials.current.scheme
    val count = CatalogPagePolicy.count(pages)
    var expanded by remember(entryId) { mutableStateOf(false) }
    var overflowing by remember(synopsis) { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (count != null) "$count ${if (count == 1) "página" else "páginas"}"
                else if (metadataLoading) "Buscando dados da edição…"
                else "Páginas não informadas",
                color = s.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.testTag("edition_page_count")
            )
            if (count == null && !pageEditorOpen) {
                TextButton(
                    onClick = onOpenPageEditor,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.heightIn(min = 44.dp).testTag("inform_edition_pages")
                ) {
                    Text("Informar páginas", color = s.accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else if (count != null && !pageEditorOpen) {
                TextButton(
                    onClick = onOpenPageEditor,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.heightIn(min = 44.dp).testTag("edit_edition_pages")
                ) {
                    Text("Editar páginas", color = s.accentColor, fontSize = 12.5.sp)
                }
            }
        }
        if (pageEditorOpen) {
            AppleTextField(
                value = pages,
                onChange = onChangePages,
                placeholder = "Páginas (opcional)",
                modifier = Modifier.testTag("edition_page_count_input")
            )
        }
        if (pagesError) {
            Text(
                text = "Use um total entre 1 e 20.000 páginas.",
                color = s.accentColor,
                fontSize = 12.sp
            )
        }
        if (synopsis.isNotBlank()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sinopse",
                        color = s.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (translationLoading) {
                        Text(
                            text = "Traduzindo…",
                            color = s.textTertiary,
                            fontSize = 11.5.sp
                        )
                    }
                }
                Text(
                    text = synopsis,
                    color = s.textSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow },
                    modifier = Modifier.testTag("edition_synopsis")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (overflowing || expanded) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = if (expanded) "Menos" else "Mais",
                                color = s.accentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (translationFailed && onRetryTranslation != null) {
                        TextButton(
                            onClick = onRetryTranslation,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.heightIn(min = 44.dp).testTag("retry_synopsis_translation")
                        ) {
                            Text(
                                text = "Traduzir sinopse",
                                color = s.accentColor,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

