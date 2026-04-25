package com.cohort.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(1_400L)
        onDone()
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splash_fade",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(fadeAlpha),
        ) {
            CohortLogo(size = 100.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Cohort",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Understand research faster",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Canvas-drawn Cohort logo:
 *  - Thick "C" arc with iridescent gradient (silver-indigo → indigo-400 → cyan-300 → indigo-700)
 *  - Atom symbol inside the C bowl: 2 elliptical orbits rotated ±30°, nucleus, 3 electrons
 */
@Composable
fun CohortLogo(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w   = this.size.width
        val sw  = w * 0.13f          // C stroke width (13% of canvas)
        val cR  = w * 0.43f          // C arc path radius
        val cCx = w * 0.50f
        val cCy = w * 0.50f

        // ── Iridescent C arc ─────────────────────────────────────────────
        // startAngle=45° (lower-right gap edge), sweepAngle=270° clockwise
        // → arc runs: lower-right → bottom → left → top → upper-right
        // → 90° gap remains on the RIGHT = classic bold "C"
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE0E7FF),   // silver-indigo sheen (top-left)
                    Color(0xFF818CF8),   // indigo-400
                    Color(0xFF67E8F9),   // cyan-300 teal
                    Color(0xFF4338CA),   // indigo-700 (bottom-right)
                ),
                start = Offset(0f, 0f),
                end   = Offset(w, w),
            ),
            startAngle = 45f,
            sweepAngle = 270f,
            useCenter  = false,
            topLeft    = Offset(cCx - cR, cCy - cR),
            size       = Size(cR * 2f, cR * 2f),
            style      = Stroke(width = sw, cap = StrokeCap.Round),
        )

        // ── Atom inside the C bowl ───────────────────────────────────────
        val ax   = w * 0.52f          // slightly right of canvas center (inside bowl)
        val ay   = w * 0.50f
        val orbA = w * 0.19f          // semi-major axis
        val orbB = w * 0.09f          // semi-minor axis
        val orbW = w * 0.024f         // orbit stroke width
        val cyan = Color(0xFF67E8F9)

        rotate(-30f, pivot = Offset(ax, ay)) {
            drawOval(
                color   = cyan.copy(alpha = 0.80f),
                topLeft = Offset(ax - orbA, ay - orbB),
                size    = Size(orbA * 2f, orbB * 2f),
                style   = Stroke(width = orbW),
            )
        }
        rotate(30f, pivot = Offset(ax, ay)) {
            drawOval(
                color   = cyan.copy(alpha = 0.80f),
                topLeft = Offset(ax - orbA, ay - orbB),
                size    = Size(orbA * 2f, orbB * 2f),
                style   = Stroke(width = orbW),
            )
        }

        // Nucleus
        drawCircle(cyan, radius = w * 0.043f, center = Offset(ax, ay))

        // Three electron dots
        val er = w * 0.024f
        drawCircle(cyan,                    er,          Offset(ax,                ay - orbB * 1.08f))
        drawCircle(cyan,                    er,          Offset(ax + orbA * 0.88f, ay + orbB * 0.52f))
        drawCircle(cyan.copy(alpha = 0.60f), er * 0.82f, Offset(ax - orbA * 0.75f, ay + orbB * 0.85f))
    }
}
