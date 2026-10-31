from pathlib import Path

pending = {}
report = []

def once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('Anchor not unique: ' + old[:100])
    return text.replace(old, new, 1)

def plan(path, marker, transform):
    p = Path(path)
    raw = p.read_bytes()
    text = raw.decode('utf-8').replace('\r\n', '\n')
    if marker in text:
        report.append('SKIP ' + path)
        return
    result = transform(text)
    pending[p] = (result.replace('\n', '\r\n') if b'\r\n' in raw else result).encode('utf-8')
    report.append('OK ' + path)

def patch_storage(text):
    text = once(
        text,
        '        const val AUTH_ONBOARDING_KEY = "quadrinheiro_auth_onboarding_done_v1"',
        '        const val AUTH_ONBOARDING_KEY = "quadrinheiro_auth_onboarding_done_v1"\n        const val WELCOME_INTRO_KEY = "quadrinheiro_welcome_intro_seen_v1"'
    )
    text = once(
        text,
        '    fun hasCompletedAuthOnboarding(): Boolean {',
        '''    @Synchronized
    fun setCompletedWelcomeIntro(completed: Boolean) {
        setItem(WELCOME_INTRO_KEY, if (completed) "true" else "false")
    }

    fun hasCompletedWelcomeIntro(): Boolean {
        return getItem(WELCOME_INTRO_KEY) == "true"
    }

    fun hasCompletedAuthOnboarding(): Boolean {'''
    )
    return text

def patch_main(text):
    text = once(
        text,
        'import com.example.ui.screens.AuthScreen',
        'import com.example.ui.screens.AuthScreen\nimport com.example.ui.screens.WelcomeIntroScreen'
    )
    text = once(
        text,
        '        // ROTEAMENTO CONDICIONAL SÍNCRONO NO BOOT (SEM FLASH DE TELA):',
        '''        // WELCOME_ONBOARDING_FLOW_V18: Welcome Intro Screen exclusively on fresh install
        val contextApp = androidx.compose.ui.platform.LocalContext.current
        val safeStorage = remember { com.example.data.SafeStorage(contextApp) }
        var hasSeenWelcomeIntro by remember { mutableStateOf(safeStorage.hasCompletedWelcomeIntro()) }

        if (!hasSeenWelcomeIntro && authUiState == AuthUiState.Unauthenticated) {
            androidx.compose.animation.AnimatedVisibility(
                visibleState = remember {
                    androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
                },
                enter = fadeIn(tween(450))
            ) {
                WelcomeIntroScreen(
                    onContinue = {
                        safeStorage.setCompletedWelcomeIntro(true)
                        hasSeenWelcomeIntro = true
                    }
                )
            }
            return@QuadrinheiroTheme
        }

        // ROTEAMENTO CONDICIONAL SÍNCRONO NO BOOT (SEM FLASH DE TELA):'''
    )
    return text

WELCOME_SCREEN_CODE = '''package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QuadrinheiroLogo
import com.example.ui.components.QuadrinheiroLogoVariant
import com.example.ui.components.liquidGlass
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class WelcomeSlide(
    val badge: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val highlightA: String,
    val highlightB: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeIntroScreen(
    onContinue: () -> Unit
) {
    val theme = LocalIOS27Theme.current
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val scope = rememberCoroutineScope()

    val slides = remember {
        listOf(
            WelcomeSlide(
                badge = "SEU ACERVO DEFINITIVO",
                title = "O universo dos seus quadrinhos, sob medida.",
                subtitle = "Organize encadernados, graphic novels e mangás com fichas ricas, edições, sinopses e capas em alta resolução.",
                icon = Icons.Default.AutoStories,
                highlightA = "Catálogo Curado & Metadados",
                highlightB = "Organização por Estantes e Formatos"
            ),
            WelcomeSlide(
                badge = "BUSCA EM TEMPO REAL",
                title = "Pesquise em múltiplos acervos online.",
                subtitle = "Encontre edições nacionais e internacionais com tradução automática em português e cadastro instantâneo.",
                icon = Icons.Default.Search,
                highlightA = "Multi-API Search Integrado",
                highlightB = "Sinopses e Títulos em PT-BR"
            ),
            WelcomeSlide(
                badge = "EXPERIÊNCIA IMERSIVA",
                title = "Metas, diário de leitura e Liquid Glass.",
                subtitle = "Acompanhe seu ritmo de leitura com anéis de atividade inspirados no iOS, iluminação ambiente e sincronização na nuvem.",
                icon = Icons.Default.BarChart,
                highlightA = "Design Óptico Liquid Glass",
                highlightB = "Histórico & Metas Anuais"
            )
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { slides.size })

    // Animação de entrada e respiração da luz ambiente
    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        isReady = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (isReady) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
        label = "contentScale"
    )

    val buttonTextColor = if (theme.accent.luminance() > 0.179f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgPrimary)
            .testTag("welcome_intro_root")
    ) {
        // 1. Fundo com Luzes Atmosféricas Liquid Glass
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(theme.bgPrimary)
            // Gradiente escultural de base
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.accent.copy(alpha = 0.28f * glowPulse),
                        theme.accentSecondary.copy(alpha = 0.12f * glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.minDimension * 0.85f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        s.gradTopRight.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.75f),
                    radius = size.minDimension * 0.65f
                )
            )
        }

        // 2. Conteúdo Responsivo e Pager de Apresentação
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .scale(contentScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Topo: Botão Pular
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onContinue,
                    modifier = Modifier.testTag("welcome_skip_button")
                ) {
                    Text(
                        text = "Pular",
                        color = theme.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PlusJakartaSansFamily
                    )
                }
            }

            // Centro: Logo, Pager e Destaques Visuais
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo central com badge de luz translúcida
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .shadow(20.dp, CircleShape, spotColor = theme.accent.copy(alpha = 0.35f))
                        .liquidGlass(shape = CircleShape, elevation = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QuadrinheiroLogo(
                        variant = QuadrinheiroLogoVariant.Primary,
                        titleVisible = false,
                        modifier = Modifier.size(92.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Quadrinheiro",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFamily,
                    letterSpacing = (-0.5).sp,
                    color = theme.textPrimary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Pager com os 3 slides de apresentação
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) { page ->
                    val slide = slides[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(theme.accent.copy(alpha = 0.12f))
                                .border(1.dp, theme.accent.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = slide.badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.accent,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = slide.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFamily,
                            textAlign = TextAlign.Center,
                            color = theme.textPrimary,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = slide.subtitle,
                            fontSize = 13.5.sp,
                            fontFamily = PlusJakartaSansFamily,
                            textAlign = TextAlign.Center,
                            color = theme.textSecondary,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(theme.textPrimary.copy(alpha = 0.05f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "✦ ${slide.highlightA}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.textSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(theme.textPrimary.copy(alpha = 0.05f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "✦ ${slide.highlightB}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }
                }

                // Indicador de Páginas estilo Apple Pill
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(slides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val pillWidth by animateFloatAsState(
                            targetValue = if (isSelected) 24f else 8f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "pillWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(pillWidth.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isSelected) theme.accent else theme.textSecondary.copy(alpha = 0.28f)
                                )
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                        )
                    }
                }
            }

            // Base: Botão de Avançar / Começar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isLastPage = pagerState.currentPage == slides.size - 1

                Button(
                    onClick = {
                        if (isLastPage) {
                            onContinue()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = theme.accent.copy(alpha = 0.35f))
                        .testTag("welcome_primary_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accent,
                        contentColor = buttonTextColor
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLastPage) "Começar" else "Continuar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSansFamily
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
'''

welcome_file = Path('app/src/main/java/com/example/ui/screens/WelcomeIntroScreen.kt')
pending[welcome_file] = WELCOME_SCREEN_CODE.encode('utf-8')

plan('app/src/main/java/com/example/data/SafeStorage.kt', 'WELCOME_INTRO_KEY', patch_storage)
plan('app/src/main/java/com/example/MainActivity.kt', 'WELCOME_ONBOARDING_FLOW_V18', patch_main)

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/welcome_onboarding_v18.txt').write_text('\n'.join(report) + '\nWelcome Intro screen created and plugged into first-launch flow before login.\n', encoding='utf-8')
