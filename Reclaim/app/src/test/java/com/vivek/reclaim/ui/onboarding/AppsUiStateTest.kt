package com.vivek.reclaim.ui.onboarding

import com.vivek.reclaim.domain.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsUiStateTest {
    private val instagram = AppInfo("com.instagram.android", "Instagram", "Social")
    private val youtube = AppInfo("com.google.android.youtube", "YouTube", "Video")
    private val whatsapp = AppInfo("com.whatsapp", "WhatsApp", "Messaging", optional = true)
    private val calculator = AppInfo("com.example.calc", "Calculator")
    private val chess = AppInfo("com.example.chess", "Chess")
    private val apps = listOf(calculator, whatsapp, instagram, chess, youtube)

    @Test
    fun freshState_hasNothingSelected() {
        val state = AppsUiState(loading = false, apps = apps)
        assertTrue(state.selected.isEmpty())
        assertFalse(state.canContinue)
        assertFalse(state.allListedSelected)
    }

    @Test
    fun listed_showsSocialAppsInPopularityOrder() {
        val listed = AppsUiState(loading = false, apps = apps).listed.map { it.label }
        assertEquals(listOf("Instagram", "YouTube", "WhatsApp"), listed)
    }

    @Test
    fun listed_includesAnAddedAppOnlyWhileItIsSelected() {
        val added = AppsUiState(loading = false, apps = apps, selected = setOf(chess.packageName))
        assertTrue(chess in added.listed)
        assertFalse(chess in added.pickable)
        val removed = added.copy(selected = emptySet())
        assertFalse(chess in removed.listed)
        assertTrue(chess in removed.pickable)
    }

    @Test
    fun canContinue_needsAtLeastOneApp() {
        assertFalse(AppsUiState(loading = false, apps = apps).canContinue)
        assertTrue(AppsUiState(loading = false, apps = apps, selected = setOf(instagram.packageName)).canContinue)
    }

    @Test
    fun allListedSelected_isTrueOnlyWhenEveryShownAppIsOn() {
        val some = AppsUiState(loading = false, apps = apps, selected = setOf(instagram.packageName))
        assertFalse(some.allListedSelected)
        val all = some.copy(selected = some.listed.map { it.packageName }.toSet())
        assertTrue(all.allListedSelected)
    }
}
