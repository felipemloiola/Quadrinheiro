package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CatalogPagePolicy
import com.example.ui.theme.LocalAppleMaterials

@Composable
internal fun CatalogEditionMetadata(entryId: String, synopsis: String, pages: String,
    metadataLoading: Boolean, pageEditorOpen: Boolean, pagesError: Boolean,
    onOpenPageEditor: () -> Unit, onChangePages: (String) -> Unit) {
    val s = LocalAppleMaterials.current.scheme
    val count = CatalogPagePolicy.count(pages)
    var expanded by remember(entryId) { mutableStateOf(false) }
    var overflowing by remember(synopsis) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (count != null) "$count ${if (count == 1) "página" else "páginas"}"
            else if (metadataLoading) "Buscando dados da edição…" else "Páginas não informadas",
            color = s.textSecondary, fontSize = 13.sp, modifier = Modifier.testTag("edition_page_count"))
        if (count == null && !pageEditorOpen) {
            TextButton(onClick = onOpenPageEditor, contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier.heightIn(min = 44.dp).testTag("inform_edition_pages")) {
                Text("Informar páginas", color = s.accentColor)
            }
        }
        if (pageEditorOpen) {
            AppleTextField(pages, onChangePages, "Páginas (opcional)", modifier = Modifier.testTag("edition_page_count_input"))
        }
        if (pagesError) Text("Use um total entre 1 e 20.000 páginas.", color = s.accentColor, fontSize = 12.sp)
        if (synopsis.isNotBlank()) {
            Text("Sinopse", color = s.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(synopsis, color = s.textSecondary, fontSize = 14.sp, lineHeight = 21.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow }, modifier = Modifier.testTag("edition_synopsis"))
            if (overflowing || expanded) TextButton(onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                Text(if (expanded) "Menos" else "Mais", color = s.accentColor)
            }
        }
    }
}
