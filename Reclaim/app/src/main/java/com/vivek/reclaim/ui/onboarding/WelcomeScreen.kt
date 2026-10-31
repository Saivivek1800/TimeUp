package com.vivek.reclaim.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivek.reclaim.R
import com.vivek.reclaim.domain.LimitLock
import com.vivek.reclaim.ui.components.SandTimer
import com.vivek.reclaim.ui.theme.ReclaimTheme

/** Onboarding step 1 (FR-1): short value statement and a "Get started" button. */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopRow()
            Spacer(Modifier.height(8.dp))
            Hero()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Take your time back.",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Set a daily limit for social media. When time’s up, we’ll show you something worth reading instead.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
            FeatureCard(
                icon = Icons.Outlined.Eco,
                title = "Gentle habit reset",
                subtitle = "No shame or guilt, just calm boundaries.",
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primaryContainer,
                trailing = Icons.Outlined.TaskAlt,
                trailingTint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            FeatureCard(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                title = "Smart reading tailored to you",
                subtitle = "Bite-sized science, art & longform thought.",
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                trailing = Icons.Outlined.TaskAlt,
                trailingTint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            FeatureCard(
                icon = Icons.Outlined.Lock,
                title = "Monthly commitment shield",
                subtitle = "Your limit locks for ${LimitLock.LOCK_DAYS} days, so a weak moment can’t undo it.",
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                trailing = Icons.Outlined.Verified,
                trailingTint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Get started", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Takes only 60 seconds to personalize",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TopRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Reclaim",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Eco,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Mindful Tech",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun Hero() {
    val primary = MaterialTheme.colorScheme.primary
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val outerRing = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Box(modifier = Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(224.dp)) {
            drawCircle(
                color = outerRing,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f))),
            )
        }
        Canvas(modifier = Modifier.size(182.dp)) {
            drawCircle(
                color = ringColor,
                style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 18f))),
            )
        }
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.85f), primary))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SandTimer()
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "MINDFUL",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                    )
                }
            }
        }
        FloatingChip(
            text = "Save 1.5h",
            icon = Icons.Outlined.Bolt,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
        )
        FloatingChip(
            text = "12 Daily Reads",
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 12.dp, start = 4.dp),
        )
    }
}

@Composable
private fun FloatingChip(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    iconBackground: Color,
    trailing: ImageVector,
    trailingTint: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Icon(trailing, contentDescription = null, tint = trailingTint, modifier = Modifier.size(24.dp))
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WelcomeScreenPreview() {
    ReclaimTheme(darkTheme = false) { WelcomeScreen(onGetStarted = {}) }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WelcomeScreenDarkPreview() {
    ReclaimTheme(darkTheme = true) { WelcomeScreen(onGetStarted = {}) }
}
