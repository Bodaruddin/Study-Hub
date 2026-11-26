package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatus
import com.example.ui.theme.BlueEnd
import com.example.ui.theme.BlueStart
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenEnd
import com.example.ui.theme.GreenStart
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrangeEnd
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PinkEnd
import com.example.ui.theme.PinkStart
import com.example.ui.theme.PurpleEnd
import com.example.ui.theme.PurpleStart
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun StudyHubBrandLogo(
    subtitle: String,
    isDarkSurface: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .drawBehind {
                    val r = 13.dp.toPx()
                    val pad = 4.dp.toPx()
                    val cardW = size.width - pad
                    val cardH = size.height - pad
                    listOf(0.025f to 2.dp.toPx(), 0.045f to 1.dp.toPx(), 0.065f to 0f).forEach { (alpha, spread) ->
                        drawRoundRect(
                            color = Color(0xFF0B0E36).copy(alpha = alpha),
                            topLeft = Offset(pad * 0.72f - spread * 0.25f, pad * 0.78f - spread * 0.25f),
                            size = Size(cardW + spread * 0.6f, cardH + spread * 0.6f),
                            cornerRadius = CornerRadius(r, r)
                        )
                    }
                    drawRoundRect(
                        color = Color(0xFF3A7BFD).copy(alpha = 0.10f),
                        topLeft = Offset(pad * 0.65f, pad * 0.70f),
                        size = Size(cardW + 1.dp.toPx(), cardH + 1.dp.toPx()),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
                .padding(end = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3528A8), Color(0xFF1A1263)))
                )
                .padding(end = 2.5.dp, bottom = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF9E6BFF), Color(0xFF5D3DFB), Color(0xFF3A7BFD)))
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.14f))),
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.65f, size.height * 0.65f)
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = "StudyHub logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = if (isDarkSurface) Color.White else MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append("Study") }
                    withStyle(
                        SpanStyle(
                            color = if (isDarkSurface) Color(0xFFA98CFF) else ElectricPurple,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append("Hub") }
                },
                fontFamily = OutfitFontFamily,
                fontSize = 20.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkSurface) Color(0xFFBFC9EE) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SyncStatusBadge(
    syncStatus: SyncStatus,
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (syncStatus) {
        SyncStatus.ONLINE -> Triple(Color(0xFF143532), Color(0xFF3CE39B), Icons.Rounded.Wifi)
        SyncStatus.OFFLINE -> Triple(Color(0xFF3E2914), WarningAmber, Icons.Rounded.CloudOff)
        SyncStatus.SYNCING -> Triple(Color(0xFF1E2B58), CyanAccent, Icons.Rounded.CloudSync)
        SyncStatus.SYNCED -> Triple(Color(0xFF123629), SuccessGreen, Icons.Rounded.CloudDone)
        SyncStatus.SYNC_FAILED -> Triple(Color(0xFF3F1922), DangerRed, Icons.Rounded.ErrorOutline)
    }
    val label = if (pendingCount > 0 && syncStatus == SyncStatus.OFFLINE) {
        "${syncStatus.label} ($pendingCount)"
    } else {
        syncStatus.label
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Sync status: $label",
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun Tactile3DCard(
    faceBrush: Brush,
    depthColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    depthHeight: Dp = 4.5.dp,
    shadowElevation: Dp = 14.dp,
    shadowColor: Color = depthColor,
    highlightBorderColor: Color = Color.White.copy(alpha = 0.82f),
    borderEndColor: Color = Color.White.copy(alpha = 0.26f),
    glassGlossAlpha: Float = 1f,
    floatingLift: Dp = 5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.976f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tactile_card_scale"
    )
    val pressDrop by animateDpAsState(
        targetValue = if (onClick != null && isPressed) (floatingLift * 0.55f) else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tactile_card_drop"
    )
    val activeDepth = if (onClick != null && isPressed) (depthHeight * 0.55f) else depthHeight
    val activeRightDepth = activeDepth * 0.72f
    val rightLift = floatingLift * 0.95f
    val slabEdgeSide = Color(
        red = (depthColor.red * 0.70f).coerceIn(0f, 1f),
        green = (depthColor.green * 0.70f).coerceIn(0f, 1f),
        blue = (depthColor.blue * 0.70f).coerceIn(0f, 1f),
        alpha = depthColor.alpha
    )
    val slabEdgeCenter = Color(
        red = (depthColor.red * 1.12f).coerceIn(0f, 1f),
        green = (depthColor.green * 1.12f).coerceIn(0f, 1f),
        blue = (depthColor.blue * 1.12f).coerceIn(0f, 1f),
        alpha = depthColor.alpha
    )

    Box(
        modifier = modifier
            .drawBehind {
                val w = size.width
                val h = size.height
                val liftBottomPx = floatingLift.toPx()
                val liftRightPx = rightLift.toPx()
                val cardW = (w - liftRightPx).coerceAtLeast(1f)
                val cardH = (h - liftBottomPx).coerceAtLeast(1f)
                val pressedFactor = if (onClick != null && isPressed) 0.52f else 1f
                val r = cornerRadius.toPx()

                // Soft feathered colored ambient shadow extending to BOTH right & bottom
                listOf(
                    0.025f to 2.4.dp.toPx(),
                    0.045f to 1.2.dp.toPx(),
                    0.070f to 0f
                ).forEach { (alpha, spread) ->
                    val sLeft = (liftRightPx * 0.74f - spread * 0.25f).coerceAtLeast(1f)
                    val sTop = (liftBottomPx * 0.78f - spread * 0.25f).coerceAtLeast(1f)
                    val sW = (cardW + liftRightPx * 0.22f + spread * 0.45f).coerceAtMost(w - sLeft)
                    val sH = (cardH + liftBottomPx * 0.20f + spread * 0.45f).coerceAtMost(h - sTop)
                    drawRoundRect(
                        color = shadowColor.copy(alpha = alpha * pressedFactor),
                        topLeft = Offset(sLeft, sTop),
                        size = Size(sW.coerceAtLeast(1f), sH.coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                // Subtle multi-step dark-indigo floating contact shadow cast bottom-right
                listOf(
                    0.018f to 2.dp.toPx(),
                    0.032f to 0.9.dp.toPx(),
                    0.048f to 0f
                ).forEach { (alpha, spread) ->
                    val cLeft = (liftRightPx * 0.90f - spread * 0.2f).coerceAtLeast(1f)
                    val cTop = (liftBottomPx * 0.92f - spread * 0.2f).coerceAtLeast(1f)
                    val cW = (cardW + liftRightPx * 0.08f + spread * 0.3f).coerceAtMost(w - cLeft)
                    val cH = (cardH + liftBottomPx * 0.08f + spread * 0.3f).coerceAtMost(h - cTop)
                    drawRoundRect(
                        color = Color(0xFF090D2E).copy(alpha = alpha * pressedFactor),
                        topLeft = Offset(cLeft, cTop),
                        size = Size(cW.coerceAtLeast(1f), cH.coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
            .padding(end = rightLift, bottom = floatingLift),
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pressDrop.toPx() * 0.8f
                    translationY = pressDrop.toPx()
                }
                .shadow(
                    elevation = if (onClick != null && isPressed) 2.dp else (shadowElevation * 0.35f).coerceAtMost(5.dp),
                    shape = shape,
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(slabEdgeCenter, depthColor, slabEdgeSide)
                    )
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(),
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(end = activeRightDepth, bottom = activeDepth),
            propagateMinConstraints = true
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(faceBrush)
                    .border(
                        width = 1.25.dp,
                        brush = Brush.linearGradient(listOf(highlightBorderColor, borderEndColor)),
                        shape = shape
                    ),
                propagateMinConstraints = true
            ) {
                if (glassGlossAlpha > 0f) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val radiusPx = cornerRadius.toPx()

                        // 1. Diagonal optical glass sheen across upper-left plane
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.26f * glassGlossAlpha),
                                    Color.White.copy(alpha = 0.08f * glassGlossAlpha),
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(w * 0.68f, h * 0.62f)
                            )
                        )

                        // 2. Crisp specular top-edge light catch inside the rim
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.68f * glassGlossAlpha),
                                    Color.White.copy(alpha = 0.48f * glassGlossAlpha),
                                    Color.Transparent
                                )
                            ),
                            start = Offset(radiusPx * 0.55f, 1.5.dp.toPx()),
                            end = Offset((w - radiusPx * 0.55f).coerceAtLeast(radiusPx), 1.5.dp.toPx()),
                            strokeWidth = 1.3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // 3. Bottom & right front-edge specular glint separating face from 3D extruded bevel
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.34f * glassGlossAlpha),
                                    Color.Transparent
                                )
                            ),
                            start = Offset(radiusPx * 0.7f, (h - 1.5.dp.toPx()).coerceAtLeast(1f)),
                            end = Offset((w - radiusPx * 0.7f).coerceAtLeast(radiusPx), (h - 1.5.dp.toPx()).coerceAtLeast(1f)),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.30f * glassGlossAlpha),
                                    Color.Transparent
                                )
                            ),
                            start = Offset((w - 1.5.dp.toPx()).coerceAtLeast(1f), radiusPx * 0.7f),
                            end = Offset((w - 1.5.dp.toPx()).coerceAtLeast(1f), (h - radiusPx * 0.7f).coerceAtLeast(radiusPx)),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // 4. Soft inner frosted glass rim for dimensional glass thickness
                        val inset = 1.6.dp.toPx()
                        val innerR = (radiusPx - inset).coerceAtLeast(2f)
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.28f * glassGlossAlpha),
                                    Color.White.copy(alpha = 0.04f * glassGlossAlpha),
                                    Color.White.copy(alpha = 0.16f * glassGlossAlpha)
                                )
                            ),
                            topLeft = Offset(inset, inset),
                            size = Size((w - inset * 2f).coerceAtLeast(1f), (h - inset * 2f).coerceAtLeast(1f)),
                            cornerRadius = CornerRadius(innerR, innerR),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun SubjectIconBadge(
    iconKey: String,
    colorKey: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val (gradient, depthColor) = when (colorKey.lowercase()) {
        "green" -> listOf(Color(0xFF28E88E), Color(0xFF0AA860)) to Color(0xFF05703E)
        "orange" -> listOf(Color(0xFFFFBE3B), Color(0xFFF26B05)) to Color(0xFFB34400)
        "purple" -> listOf(Color(0xFFA056FF), Color(0xFF6319E6)) to Color(0xFF3E099E)
        "pink" -> listOf(Color(0xFFFF5992), Color(0xFFD91C5E)) to Color(0xFF960B3C)
        "teal" -> listOf(Color(0xFF28D6EA), Color(0xFF0C8CA8)) to Color(0xFF075D73)
        else -> listOf(Color(0xFF448CFF), Color(0xFF1755E6)) to Color(0xFF0D359E)
    }
    val icon = when (iconKey.lowercase()) {
        "science" -> Icons.Rounded.Science
        "calculate" -> Icons.Rounded.Calculate
        "globe" -> Icons.Rounded.Public
        "hindi" -> Icons.Rounded.Translate
        else -> Icons.Rounded.Book
    }

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size + 3.dp)
            .drawBehind {
                val r = 14.dp.toPx()
                val pad = 4.dp.toPx()
                val cardW = this.size.width - pad
                val cardH = this.size.height - pad
                listOf(0.035f to 1.5.dp.toPx(), 0.065f to 0f).forEach { (alpha, spread) ->
                    drawRoundRect(
                        color = depthColor.copy(alpha = alpha),
                        topLeft = Offset(pad * 0.75f - spread * 0.2f, pad * 0.80f - spread * 0.2f),
                        size = Size(cardW + spread * 0.5f, cardH + spread * 0.5f),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
            .padding(end = 3.5.dp, bottom = 3.5.dp)
            .clip(shape)
            .background(depthColor)
            .padding(end = 2.5.dp, bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(gradient))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.16f))
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(this.size.width * 0.68f, this.size.height * 0.60f)
                    )
                )
            }
            if (iconKey.lowercase() == "hindi") {
                Text(
                    text = "अ",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.54f)
                )
            }
        }
    }
}

@Composable
fun CircularProgressRing(
    progress: Float,
    label: String,
    size: Dp = 68.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 7.5.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF39D2FF).copy(alpha = 0.22f), Color.Transparent)
                ),
                radius = this.size.minDimension * 0.56f
            )
            drawArc(
                color = Color.White.copy(alpha = 0.20f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF2BF296), Color(0xFF39D2FF), Color(0xFF42F5AE), Color(0xFF2BF296))
                ),
                startAngle = -90f,
                sweepAngle = (progress.coerceIn(0f, 1f)) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = label,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}

@Composable
fun PlatformAnalyticsLineChart(
    studentsSeries: List<Float>,
    quizzesSeries: List<Float>,
    days: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    peakLabel: String = "12.5K",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9353FF))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Students", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD1D8F5))
            Spacer(modifier = Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CyanAccent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Quizzes", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD1D8F5))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .height(120.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("15K", "10K", "5K", "0").forEach { lbl ->
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8C97C4)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).height(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val gridColor = Color.White.copy(alpha = 0.08f)
                        for (i in 0..3) {
                            val y = h * (i / 3f)
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                        }
                        val count = studentsSeries.size.coerceAtLeast(2)
                        val stepX = w / (count - 1).coerceAtLeast(1)

                        fun buildPoints(series: List<Float>): List<Offset> =
                            series.mapIndexed { idx, v ->
                                Offset(idx * stepX, h - (v.coerceIn(0f, 1f) * (h - 14.dp.toPx())) - 7.dp.toPx())
                            }

                        fun drawSmoothLine(pts: List<Offset>, lineColor: Color) {
                            if (pts.size < 2) return
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 0 until pts.lastIndex) {
                                    val p0 = pts[i]
                                    val p1 = pts[i + 1]
                                    val cx = (p0.x + p1.x) / 2f
                                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = lineColor.copy(alpha = 0.35f),
                                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            pts.forEach { pt ->
                                drawCircle(color = lineColor, radius = 5.5.dp.toPx(), center = pt)
                                drawCircle(color = Color.White, radius = 2.8.dp.toPx(), center = pt)
                            }
                        }

                        drawSmoothLine(buildPoints(quizzesSeries), CyanAccent)
                        drawSmoothLine(buildPoints(studentsSeries), Color(0xFF9E5BFF))
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricPurple
                    ) {
                        Text(
                            text = peakLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(start = 34.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9AA5D1)
                    )
                }
            }
        }
    }
}

@Composable
fun EthicalAdFreeUpgradeBanner(
    adsEnabled: Boolean,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!adsEnabled || isPremium) return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onUpgradeClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricPurple.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "StudyHub Plus • Family-Safe Learning",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Unlock unlimited offline packs, advanced analytics & ad-free mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upgrade",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricPurple,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
