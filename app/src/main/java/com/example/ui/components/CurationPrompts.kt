package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIOS27Theme

/** Compact menus use the same retained sheet, materials and exit path as the editor. */
@Composable
internal fun CurationPrompt(title:String,onDismiss:()->Unit,confirmLabel:String,onConfirm:()->Unit,
    cancelLabel:String="Cancelar",onCancel:()->Unit={},confirmEnabled:Boolean=true,
    alternativeLabel:String?=null,onAlternative:()->Unit={},content:@Composable ColumnScope.()->Unit) {
    val panel=rememberCurationPanelState()
    CurationSheet(title,onDismiss,panel=panel,compact=true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            content()
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                CurationGlassAction(confirmLabel,{panel.close(onConfirm)},Modifier.fillMaxWidth(),primary=true,enabled=confirmEnabled)
                alternativeLabel?.let {CurationGlassAction(it,{panel.close(onAlternative)},Modifier.fillMaxWidth())}
                CurationGlassAction(cancelLabel,{panel.close(onCancel)},Modifier.fillMaxWidth())
            }
        }
    }
}
