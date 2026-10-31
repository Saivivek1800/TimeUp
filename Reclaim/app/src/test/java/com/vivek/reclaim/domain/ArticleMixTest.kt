package com.vivek.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleMixTest {
    private data class A(val id: String, val interest: String)

    @Test
    fun interleave_alternatesBetweenInterestsAndKeepsEachOrder() {
        val items = listOf(A("s1", "Science"), A("s2", "Science"), A("s3", "Science"), A("t1", "Tech"), A("t2", "Tech"))
        val mixed = ArticleMix.interleave(items, listOf("Science", "Tech")) { it.interest }.map { it.id }
        assertEquals(listOf("s1", "t1", "s2", "t2", "s3"), mixed)
    }

    @Test
    fun interleave_dropsItemsOutsideTheChosenInterests() {
        val items = listOf(A("s1", "Science"), A("g1", "Gaming"))
        assertEquals(listOf("s1"), ArticleMix.interleave(items, listOf("Science")) { it.interest }.map { it.id })
    }

    @Test
    fun interleave_ofNothingIsEmpty() {
        assertEquals(emptyList<A>(), ArticleMix.interleave(emptyList<A>(), listOf("Science")) { it.interest })
    }
}
