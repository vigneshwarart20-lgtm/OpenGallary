package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JapaneseFanSplash(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant color spectrum for the petals
    val petalColors = remember {
        listOf(
            Color(0xFFFF3B30).copy(alpha = 0.88f), // Red
            Color(0xFFFF9500).copy(alpha = 0.88f), // Orange
            Color(0xFFFFCC00).copy(alpha = 0.88f), // Yellow
            Color(0xFF34C759).copy(alpha = 0.88f), // Lime
            Color(0xFF30D158).copy(alpha = 0.88f), // Green
            Color(0xFF00C7BE).copy(alpha = 0.88f), // Teal
            Color(0xFF007AFF).copy(alpha = 0.88f), // Sky Blue
            Color(0xFF5856D6).copy(alpha = 0.88f), // Indigo
            Color(0xFFAF52DE).copy(alpha = 0.88f), // Purple
            Color(0xFFFF2D55).copy(alpha = 0.88f)  // Pink
        )
    }

    // Animation progress drivers
    val fanUnfoldProgress = remember { Animatable(0f) }
    val fanScale = remember { Animatable(0.45f) }
    val titleAlpha = remember { Animatable(0f) }
    val overallExitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Step 1: Fan unfolds like a traditional Japanese fan (sensu) with spring elasticity
        launch {
            fanScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 850,
                    easing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1.0f) // Elastic spring overshoot
                )
            )
        }

        launch {
            fanUnfoldProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 850,
                    easing = CubicBezierEasing(0.25f, 1.25f, 0.5f, 1.0f)
                )
            )
        }

        // Step 2: Title fades in
        delay(400)
        titleAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )

        // Hold briefly for visual impact (~350ms)
        delay(350)

        // Step 3: Seamless smooth exit reveal into gallery
        overallExitAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        )

        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(overallExitAlpha.value)
            .background(MaterialTheme.colorScheme.background)
            .testTag("japanese_fan_splash"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Unfolding Japanese Fan / Color Pinwheel Canvas
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(fanScale.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                    val petalCount = petalColors.size
                    val targetAngleStep = 360f / petalCount

                    val petalWidth = 52.dp.toPx()
                    val petalHeight = 84.dp.toPx()
                    val cornerRadius = CornerRadius(petalWidth / 2f, petalWidth / 2f)

                    // Draw each petal unfolding from 0° out to its final fan angle
                    for (i in 0 until petalCount) {
                        val finalAngle = i * targetAngleStep

                        // Staggered fan sweep: earlier blades fan out first, trailing blades follow
                        val staggerDelay = i.toFloat() / petalCount
                        val bladeProgress = ((fanUnfoldProgress.value - staggerDelay * 0.4f) / (1f - staggerDelay * 0.4f))
                            .coerceIn(0f, 1f)

                        // Angle sweeping from initial folded fan stack (0°) to final radial position
                        val currentAngle = finalAngle * bladeProgress

                        rotate(degrees = currentAngle, pivot = center) {
                            val path = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = center.x - petalWidth / 2f,
                                        top = center.y - petalHeight,
                                        right = center.x + petalWidth / 2f,
                                        bottom = center.y + 12.dp.toPx(),
                                        cornerRadius = cornerRadius
                                    )
                                )
                            }
                            drawPath(path = path, color = petalColors[i])
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Elegant typography matching Apple Photos
            Text(
                text = "Photos",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(titleAlpha.value)
            )
        }
    }
}
