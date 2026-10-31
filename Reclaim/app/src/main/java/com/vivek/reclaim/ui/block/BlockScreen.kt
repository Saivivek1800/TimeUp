package com.vivek.reclaim.ui.block

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.vivek.reclaim.data.db.ArticleEntity
import com.vivek.reclaim.domain.TimePerspective
import com.vivek.reclaim.domain.model.DailyLimit
import com.vivek.reclaim.ui.articles.ArticleCard
import com.vivek.reclaim.ui.articles.ArticlesEmptyState
import com.vivek.reclaim.ui.articles.ArticlesViewModel
import com.vivek.reclaim.ui.articles.InterestFilterRow
import com.vivek.reclaim.ui.articles.OfflineNote
import com.vivek.reclaim.ui.components.AppIcon
import kotlin.math.PI
import kotlin.math.cos

/**
 * Shown instead of a blocked app once the daily limit is reached (PRD 5.3, screen 7; design "Limit Interception
 * & Article Redirect"). It names the app that was stopped, offers a breathing pause, a gentle sense of what the
 * time adds up to, and one recommended read. "Browse all" opens the full list. There is no emergency unlock in v1.
 */
@Composable
fun BlockScreen(
    blockedPackage: String?,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
    articlesViewModel: ArticlesViewModel = hiltViewModel(),
    blockViewModel: BlockViewModel = hiltViewModel(),
) {
    val articles by articlesViewModel.state.collectAsStateWithLifecycle()
    val block by blockViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var browsing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(blockedPackage) { blockViewModel.setBlockedPackage(blockedPackage) }

    // Back from the full list returns to the pause screen; Back from the pause screen is handled by the activity.
    BackHandler(enabled = browsing) {
        browsing = false
        articlesViewModel.select(null)
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (browsing) {
            BrowseContent(
                articles = articles,
                onSelect = articlesViewModel::select,
                onOpen = { articlesViewModel.open(context, it) },
                onRetry = articlesViewModel::retry,
                onBack = {
                    browsing = false
                    articlesViewModel.select(null)
                },
            )
        } else {
            val featured = articles.articles.firstOrNull { !it.isRead } ?: articles.articles.firstOrNull()
            InterceptContent(
                block = block,
                featured = featured,
                articlesOffline = articles.offline,
                articlesLoading = articles.loading || articles.refreshing,
                loadIcon = blockViewModel::loadIcon,
                onContinue = { featured?.let { articlesViewModel.open(context, it) } },
                onBrowse = { browsing = true },
                onRetry = articlesViewModel::retry,
                onGoHome = onGoHome,
            )
        }
    }
}

@Composable
private fun InterceptContent(
    block: BlockUiState,
    featured: ArticleEntity?,
    articlesOffline: Boolean,
    articlesLoading: Boolean,
    loadIcon: (String) -> ImageBitmap?,
    onContinue: () -> Unit,
    onBrowse: () -> Unit,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { InterceptedPills(block, loadIcon) }
        item { BreathingCard() }
        item { PerspectiveCard(TimePerspective.of(block.limitMinutes)) }
        item { RecommendedSection(featured, articlesOffline, articlesLoading, onRetry) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (featured != null) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = CircleShape,
                    ) {
                        Text("Continue to article", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    }
                }
                FilledTonalButton(
                    onClick = onBrowse,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse all curated reads", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onGoHome) {
                    Text(
                        "Put phone down & step away",
                        style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InterceptedPills(block: BlockUiState, loadIcon: (String) -> ImageBitmap?) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Reclaim shield") }
                    append("  •  ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Intercepted") }
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (block.appPackage != null) {
                AppIcon(packageName = block.appPackage, loadIcon = loadIcon, size = 24.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "${block.appLabel ?: "Your app"} paused • ${DailyLimit.format(block.limitMinutes)} daily budget reached",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A slow 8-second breath: the circle grows while you inhale and shrinks while you exhale. */
@Composable
private fun BreathingCard() {
    val transition = rememberInfiniteTransition(label = "breath")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 8_000, easing = LinearEasing), RepeatMode.Restart),
        label = "breath-progress",
    )
    // 0 -> 1 -> 0 along a cosine, so the motion eases in and out at both ends of each breath.
    val breath = (0.5 - 0.5 * cos(2 * PI * t)).toFloat()
    val inhaling = t < 0.5f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(0.72f + 0.28f * breath)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(0.75f + 0.25f * breath)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                }
                Text(
                    if (inhaling) "Inhale" else "Exhale",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Pause. Take a breath.",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You reached for a habit. Let’s put your curiosity to work on something intentional instead.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PerspectiveCard(perspective: TimePerspective) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Time perspective", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Text("Gentle reality check", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${perspective.limitLabel} daily budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "= ${perspective.weeklyHoursLabel} / week",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildAnnotatedString {
                            append("That’s ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(perspective.dayPhrase) }
                            append(" every week, or around ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                append("${perspective.booksPerYear} books")
                            }
                            append(" a year at ${TimePerspective.HOURS_PER_BOOK} hours a book.")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "What could your future self build with these ${perspective.limitLabel} today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecommendedSection(featured: ArticleEntity?, offline: Boolean, loading: Boolean, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recommended alternate", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            if (featured != null) {
                Text("Ready to read", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        if (featured == null) {
            ArticlesEmptyState(offline = offline, refreshing = loading, onRetry = onRetry)
        } else {
            FeaturedArticle(featured)
        }
    }
}

@Composable
private fun FeaturedArticle(article: ArticleEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.secondaryContainer)) {
                Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center).size(40.dp))
                if (article.imageUrl != null) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    article.interest,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("~${article.readMinutes} min read", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White)
                }
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(article.source, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.height(4.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        article.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Saved on your phone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

/** The full reading list, opened from "Browse all curated reads". */
@Composable
private fun BrowseContent(
    articles: com.vivek.reclaim.ui.articles.ArticlesUiState,
    onSelect: (String?) -> Unit,
    onOpen: (ArticleEntity) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                OutlinedButton(onClick = onBack, shape = CircleShape) {
                    Text("Back", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Curated reads",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Picked from your interests.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (articles.interests.isNotEmpty()) {
            item { InterestFilterRow(articles.interests, articles.filter, onSelect, Modifier.fillMaxWidth()) }
        }
        if (articles.offline && articles.articles.isNotEmpty()) item { OfflineNote() }
        if (articles.articles.isEmpty()) {
            item {
                ArticlesEmptyState(
                    offline = articles.offline,
                    refreshing = articles.refreshing || articles.loading,
                    onRetry = onRetry,
                )
            }
        } else {
            items(articles.articles, key = { it.id }) { article ->
                ArticleCard(article = article, onClick = { onOpen(article) })
            }
        }
    }
}
