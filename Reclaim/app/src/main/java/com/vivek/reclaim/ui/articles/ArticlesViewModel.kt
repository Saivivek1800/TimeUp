package com.vivek.reclaim.ui.articles

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.articles.ArticleRepository
import com.vivek.reclaim.data.db.ArticleEntity
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.domain.ArticleMix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticlesUiState(
    val loading: Boolean = true,
    val interests: List<String> = emptyList(),
    /** null means "All". */
    val filter: String? = null,
    val articles: List<ArticleEntity> = emptyList(),
    val refreshing: Boolean = false,
    /** True when the last refresh could not reach any feed, so what you see is the saved copy. */
    val offline: Boolean = false,
)

/** Feeds both the Block screen and the Articles tab (PRD FR-17, FR-23). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticlesViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<String?>(null)
    private val status = MutableStateFlow(Status())
    private var refreshJob: Job? = null

    private data class Status(val refreshing: Boolean = false, val offline: Boolean = false)

    private val interests = settings.settings.map { it.interests.sorted() }.distinctUntilChanged()

    private val _state = MutableStateFlow(ArticlesUiState())
    val state: StateFlow<ArticlesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val articles = interests.flatMapLatest { list -> repository.observe(list.toSet()) }
            combine(interests, filter, articles, status) { list, selected, rows, st ->
                // A filter for an interest the user removed falls back to "All".
                val active = selected?.takeIf { it in list }
                val shown = if (active != null) rows.filter { it.interest == active } else ArticleMix.interleave(rows, list) { it.interest }
                ArticlesUiState(
                    loading = false,
                    interests = list,
                    filter = active,
                    articles = shown,
                    refreshing = st.refreshing,
                    offline = st.offline,
                )
            }.collect { _state.value = it }
        }
        viewModelScope.launch {
            // Shows whatever is saved right away, then tops it up if it is due (about every 2 days, or when an
            // interest has nothing saved yet). The interests come straight from the collector: reading them back
            // from the screen state would find an empty list here and silently skip the download.
            interests.collect { list -> if (list.isNotEmpty()) fetch(list, force = false) }
        }
    }

    fun select(interest: String?) {
        filter.value = interest
    }

    /** "Try again": fetches now, whatever the 2-day schedule says. */
    fun retry() {
        val list = _state.value.interests
        if (list.isNotEmpty()) fetch(list, force = true)
    }

    private fun fetch(interests: List<String>, force: Boolean) {
        refreshJob?.cancel()
        status.value = Status(refreshing = true, offline = status.value.offline)
        refreshJob = viewModelScope.launch {
            val set = interests.toSet()
            val reached = runCatching {
                if (force) repository.refresh(set) else repository.refreshIfDue(set)
            }.getOrDefault(false)
            status.value = Status(refreshing = false, offline = !reached)
        }
    }

    /** FR-19: opens the article in a Chrome Custom Tab and remembers it as read (FR-22). */
    fun open(context: Context, article: ArticleEntity) {
        viewModelScope.launch { repository.markRead(article.id) }
        val uri = article.url.toUri()
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
