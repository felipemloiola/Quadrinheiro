package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.CloudCacheRecovery
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.launch

/** No ViewModel or cloud listener is created behind this screen. */
@Composable
internal fun CloudCacheRecoveryScreen(onContinue: () -> Unit) {
    val context=LocalContext.current
    val theme=LocalIOS27Theme.current
    val scope=rememberCoroutineScope()
    var confirm by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var repaired by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxSize().background(theme.bgPrimary).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.Start) {
        Text(if(repaired)"Sincronização preparada" else "Revisar sincronização",style=MaterialTheme.typography.headlineMedium,color=theme.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(if(repaired)"Seu acervo foi preservado. Uma cópia de segurança ficou guardada neste aparelho. Ao continuar, os registros locais serão reconciliados com sua conta."
            else "O armazenamento de sincronização cresceu muito e pode causar travamentos. Seu acervo continua salvo. Podemos guardar uma cópia de segurança e reconstruir essa fila.",
            color=theme.textSecondary,style=MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        if(busy) {
            CircularProgressIndicator(color=theme.accent)
            Spacer(Modifier.height(12.dp));Text("Preservando os dados e preparando a sincronização…",color=theme.textSecondary)
        } else if(repaired) Button(onClick=onContinue){Text("Continuar")}
        else {
            Button(onClick={confirm=true}){Text("Preparar recuperação")}
            TextButton(onClick={CloudCacheRecovery.continueWithoutRepair();onContinue()}){Text("Continuar sem reparar",color=theme.accent)}
        }
        if(failed)Text("Não foi possível concluir a recuperação. Os dados originais e qualquer cópia já criada foram preservados.",color=theme.textSecondary)
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Reconstruir a fila local?")},
        text={Text("Primeiro será guardada uma cópia privada do estado local e do banco de sincronização. Depois, a fila antiga do Firestore será substituída e os registros salvos no app serão reenviados. Operações antigas que existam apenas nessa fila, como exclusões ainda não confirmadas, podem precisar de revisão. Sua conta não será desconectada.")},
        dismissButton={TextButton(onClick={confirm=false}){Text("Cancelar")}},
        confirmButton={TextButton(onClick={confirm=false;busy=true;failed=false;scope.launch {
            try {CloudCacheRecovery.repair(context);repaired=true}
            catch(cancel:kotlinx.coroutines.CancellationException){throw cancel}
            catch(_:Exception){failed=true}
            finally{busy=false}
        }}){Text("Criar cópia e reconstruir")}})
}
