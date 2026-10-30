from pathlib import Path
import hashlib

pending = {}
report = []
def load(path):
    p = Path(path)
    raw = p.read_bytes()
    return p, raw, raw.decode('utf-8').replace('\r\n', '\n')

def stage(p, raw, text):
    pending[p] = (text.replace('\n', '\r\n') if b'\r\n' in raw else text).encode('utf-8')

def once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('Typography anchor not unique; no source files written: ' + old[:100])
    return text.replace(old, new, 1)

TYPE = '''package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// APP_HIG_TEXT_STYLES_V12: Android adaptation, not Apple's proprietary SF font.
val AppSansFamily = FontFamily.SansSerif

// Compatibility aliases for existing screens; both now resolve to the same native family.
val PlusJakartaSansFamily = AppSansFamily
val SpaceGroteskTabularFamily = AppSansFamily

fun buildQuadrinheiroTypography(scaleFactor: Float, forceBold: Boolean): Typography {
    val scale = if (scaleFactor.isFinite()) scaleFactor.coerceIn(0.5f, 3f) else 1f
    fun weight(base: FontWeight): FontWeight = if (forceBold) {
        if (base.weight < FontWeight.SemiBold.weight) FontWeight.SemiBold else FontWeight.Bold
    } else base
    fun style(size: Float, leading: Float, base: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = AppSansFamily,
        fontWeight = weight(base),
        fontSynthesis = FontSynthesis.None,
        fontSize = (size * scale).sp,
        lineHeight = (leading * scale).sp,
        letterSpacing = 0.sp
    )
    // Apple HIG hierarchy adapted to Android sp; not a pixel-identical iOS rendering.
    return Typography(
        displayLarge = style(34f, 41f, FontWeight.Bold),
        displayMedium = style(28f, 34f),
        displaySmall = style(22f, 28f),
        headlineLarge = style(28f, 34f),
        headlineMedium = style(22f, 28f),
        headlineSmall = style(20f, 25f),
        titleLarge = style(20f, 25f, FontWeight.SemiBold),
        titleMedium = style(17f, 22f, FontWeight.SemiBold),
        titleSmall = style(15f, 20f, FontWeight.SemiBold),
        bodyLarge = style(17f, 22f),
        bodyMedium = style(15f, 20f),
        bodySmall = style(13f, 18f),
        labelLarge = style(16f, 21f, FontWeight.Medium),
        labelMedium = style(12f, 16f, FontWeight.Medium),
        labelSmall = style(11f, 13f, FontWeight.Medium)
    )
}
'''

p, raw, text = load('app/src/main/java/com/example/ui/theme/Type.kt')
if 'APP_HIG_TEXT_STYLES_V12' not in text:
    blob = hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()
    if blob != 'b663583279ed69ee05bd995e9999670b5198f598':
        raise RuntimeError('Type.kt changed since review; refusing replacement: ' + blob)
    stage(p, raw, TYPE)
    report.append('OK native sans-serif family and 15 text styles')
else:
    report.append('SKIP text styles already applied')

p, raw, text = load('app/src/main/java/com/example/ui/theme/Theme.kt')
if 'APP_HIG_THEME_SCALE_V12' not in text:
    text = once(text, '    val typography = remember(dynamicScale, settings.boldText) {', '    // APP_HIG_THEME_SCALE_V12: LocalDensity applies the user scale exactly once.\n    val typography = remember(dynamicScale, settings.boldText) {')
    text = once(text, '            scaleFactor = dynamicScale,', '            scaleFactor = 1f,')
    text = once(text, 'cfg.fontWeightAdjustment = if (settings.boldText) 300 else 0', 'cfg.fontWeightAdjustment = if (settings.boldText) 300 else baseConfig.fontWeightAdjustment')
    text = once(text, 'fontScale = baseDensity.fontScale * dynamicScale * (if (settings.boldText) 1.03f else 1f)', 'fontScale = baseDensity.fontScale * dynamicScale')
    text = once(text, '''                value = LocalTextStyle.current.copy(
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                    fontSynthesis = FontSynthesis.All,
                    letterSpacing = if (settings.boldText) 0.2.sp else LocalTextStyle.current.letterSpacing,
                    shadow = if (settings.boldText) {
                        androidx.compose.ui.graphics.Shadow(
                            color = scheme.textPrimary.copy(alpha = 0.70f),
                            offset = androidx.compose.ui.geometry.Offset(0.95f, 0.15f),
                            blurRadius = 0.1f
                        )
                    } else null
                ),''', '''                value = typography.bodyLarge.copy(
                    fontFamily = AppSansFamily,
                    fontWeight = if (settings.boldText) FontWeight.SemiBold else FontWeight.Normal,
                    fontSynthesis = FontSynthesis.None,
                    letterSpacing = 0.sp,
                    shadow = null
                ),''')
    stage(p, raw, text)
    report.append('OK single user text scale; native weights; no synthetic text shadow')
else:
    report.append('SKIP theme adaptation already applied')

TEST = '''package com.example

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppSansFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.example.ui.theme.buildQuadrinheiroTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyPolicyTest {
    @Test
    fun allTextRolesUseOneNativeFamily() {
        val t = buildQuadrinheiroTypography(1f, false)
        val roles = listOf(t.displayLarge, t.displayMedium, t.displaySmall,
            t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge, t.titleMedium, t.titleSmall,
            t.bodyLarge, t.bodyMedium, t.bodySmall,
            t.labelLarge, t.labelMedium, t.labelSmall)
        assertEquals(FontFamily.SansSerif, AppSansFamily)
        assertEquals(AppSansFamily, PlusJakartaSansFamily)
        assertEquals(AppSansFamily, SpaceGroteskTabularFamily)
        roles.forEach { assertEquals(AppSansFamily, it.fontFamily) }
    }

    @Test
    fun bodyHasReadableReferenceMetrics() {
        val body = buildQuadrinheiroTypography(1f, false).bodyLarge
        assertEquals(17f, body.fontSize.value, 0.001f)
        assertEquals(22f, body.lineHeight.value, 0.001f)
        assertEquals(FontWeight.Normal, body.fontWeight)
    }

    @Test
    fun boldPreferenceIncreasesWeightWithoutAddingAShadow() {
        val body = buildQuadrinheiroTypography(1f, true).bodyLarge
        assertTrue(body.fontWeight!!.weight >= FontWeight.SemiBold.weight)
        assertEquals(null, body.shadow)
    }

    @Test
    fun explicitStyleScaleIsAppliedOnce() {
        val body = buildQuadrinheiroTypography(1.15f, false).bodyLarge
        assertEquals(17f * 1.15f, body.fontSize.value, 0.001f)
        assertEquals(22f * 1.15f, body.lineHeight.value, 0.001f)
    }
}
'''
test_path = Path('app/src/test/java/com/example/TypographyPolicyTest.kt')
if test_path.exists() and test_path.read_text(encoding='utf-8') != TEST:
    raise RuntimeError('Typography test file already exists with different content; no sources written')
pending[test_path] = TEST.encode('utf-8')
for p, content in pending.items():
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/hig_typography_v12.txt').write_text('\n'.join(report) + '\nAndroid native sans-serif, not SF Pro. Explicit per-screen sizes are preserved. Compilation, tests, font-scale layouts and device visual checks remain to be verified.\n', encoding='utf-8')
