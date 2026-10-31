package com.vivek.reclaim.data.articles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RssParserTest {

    private val rss = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/" xmlns:content="http://purl.org/rss/1.0/modules/content/">
          <channel>
            <title>Example</title>
            <item>
              <title>Why Boredom Is Essential &amp; Good</title>
              <link>https://example.com/boredom</link>
              <pubDate>Mon, 02 Mar 2026 10:30:00 +0000</pubDate>
              <description><![CDATA[<p>How giving your mind <b>permission</b> to wander sparks problem solving.</p>]]></description>
              <media:content url="http://img.example.com/boredom.jpg" medium="image"/>
            </item>
            <item>
              <title>No link here</title>
              <description>skipped because it has no link</description>
            </item>
            <item>
              <title>Second story</title>
              <link>https://example.com/second</link>
              <description>Plain text only.</description>
              <enclosure url="https://img.example.com/second.png" type="image/png"/>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private val atom = """
        <?xml version="1.0" encoding="utf-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>Atom example</title>
          <entry>
            <title>Atom story</title>
            <link rel="alternate" href="https://example.org/atom-story"/>
            <link rel="replies" href="https://example.org/comments"/>
            <published>2026-03-02T10:30:00Z</published>
            <summary>Short atom summary.</summary>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun rss_readsTitleLinkAndDecodesEntities() {
        val first = RssParser.parse(rss).first()
        assertEquals("Why Boredom Is Essential & Good", first.title)
        assertEquals("https://example.com/boredom", first.url)
    }

    @Test
    fun rss_summaryIsPlainTextWithoutTags() {
        val first = RssParser.parse(rss).first()
        assertEquals("How giving your mind permission to wander sparks problem solving.", first.summary)
    }

    @Test
    fun rss_skipsItemsWithoutALink() {
        val titles = RssParser.parse(rss).map { it.title }
        assertEquals(listOf("Why Boredom Is Essential & Good", "Second story"), titles)
    }

    @Test
    fun rss_parsesTheDate() {
        val first = RssParser.parse(rss).first()
        assertEquals(Instant.parse("2026-03-02T10:30:00Z").toEpochMilli(), first.publishedAt)
    }

    @Test
    fun images_comeFromMediaContentOrEnclosure_andAreUpgradedToHttps() {
        val (a, b) = RssParser.parse(rss)
        assertEquals("https://img.example.com/boredom.jpg", a.imageUrl)
        assertEquals("https://img.example.com/second.png", b.imageUrl)
    }

    @Test
    fun imageFallsBackToTheFirstImgTagInTheContent() {
        val feed = """
            <rss version="2.0"><channel><item>
              <title>With inline image</title>
              <link>https://example.com/inline</link>
              <description><![CDATA[<p><img src="https://cdn.example.com/pic.jpg" width="600"/>Text</p>]]></description>
            </item></channel></rss>
        """.trimIndent()
        assertEquals("https://cdn.example.com/pic.jpg", RssParser.parse(feed).single().imageUrl)
    }

    @Test
    fun trackingPixelsAreNotUsedAsThePicture() {
        // NerdWallet's real feed has only this hidden 1x1 pixel in each article.
        val feed = """
            <rss version="2.0"><channel><item>
              <title>Only a pixel</title>
              <link>https://example.com/pixel</link>
              <description><![CDATA[<p>Text</p><img height="1" width="1" src="https://www.example.com/tracking/nw-pixel-v1.gif?post_id=1" style="display: none;" />]]></description>
            </item></channel></rss>
        """.trimIndent()
        assertNull(RssParser.parse(feed).single().imageUrl)
    }

    @Test
    fun aRealPictureAfterAPixelIsStillFound() {
        val feed = """
            <rss version="2.0"><channel><item>
              <title>Pixel then picture</title>
              <link>https://example.com/both</link>
              <description><![CDATA[<img width="1" height="1" src="https://t.example.com/p.gif"/><img src="https://cdn.example.com/hero.jpg" width="800"/>]]></description>
            </item></channel></rss>
        """.trimIndent()
        assertEquals("https://cdn.example.com/hero.jpg", RssParser.parse(feed).single().imageUrl)
    }

    @Test
    fun aVideoMediaContentIsSkippedInFavourOfTheThumbnail() {
        // Entrepreneur's real feed: media:content is a video player page, media:thumbnail is the picture.
        val feed = """
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/"><channel><item>
              <title>Video story</title>
              <link>https://example.com/video</link>
              <media:content url="https://players.example.net/index.html?videoId=1" type="text/html" medium="video"/>
              <media:thumbnail url="https://www.example.com/uploads/thumb.jpg"/>
            </item></channel></rss>
        """.trimIndent()
        assertEquals("https://www.example.com/uploads/thumb.jpg", RssParser.parse(feed).single().imageUrl)
    }

    @Test
    fun atom_usesTheAlternateLinkAndPublishedDate() {
        val story = RssParser.parse(atom).single()
        assertEquals("https://example.org/atom-story", story.url)
        assertEquals("Short atom summary.", story.summary)
        assertEquals(Instant.parse("2026-03-02T10:30:00Z").toEpochMilli(), story.publishedAt)
    }

    @Test
    fun limit_capsTheNumberOfArticles() {
        val items = (1..5).joinToString("") { "<item><title>T$it</title><link>https://e.com/$it</link></item>" }
        assertEquals(3, RssParser.parse("<rss><channel>$items</channel></rss>", limit = 3).size)
    }

    @Test
    fun junkAfterTheClosingTag_isIgnored() {
        // ScienceDaily's real feed ends with an extra "-->" after </rss>.
        val feed = "<rss><channel><item><title>A</title><link>https://e.com/a</link></item></channel></rss>\n<!-- cached --> -->"
        assertEquals(1, RssParser.parse(feed).size)
    }

    @Test
    fun byteOrderMarkAndControlCharacters_areIgnored() {
        val feed = "﻿<rss><channel><item><title>A\u0003B</title><link>https://e.com/a</link></item></channel></rss>"
        assertEquals("A B", RssParser.parse(feed).single().title)
    }

    @Test
    fun aBareAmpersandInATitle_doesNotRejectTheFeed() {
        // Literary Hub's real feed has "The Literary Film & TV You Need to Stream" as a plain title.
        val feed = "<rss><channel><item><title>Film & TV You Need</title><link>https://e.com/a?x=1&y=2</link></item></channel></rss>"
        val article = RssParser.parse(feed).single()
        assertEquals("Film & TV You Need", article.title)
        assertEquals("https://e.com/a?x=1&y=2", article.url)
    }

    @Test
    fun htmlOnlyEntitiesOutsideCdata_areReadAsText() {
        val feed = "<rss><channel><item><title>Fish&nbsp;chips &mdash; yum</title><link>https://e.com/a</link></item></channel></rss>"
        assertEquals("Fish chips — yum", RssParser.parse(feed).single().title)
    }

    @Test
    fun validEntitiesAndCdataAreLeftAlone() {
        val feed = "<rss><channel><item><title>Tom &amp; Jerry</title><link>https://e.com/a</link>" +
            "<description><![CDATA[Cats & dogs <b>together</b>]]></description></item></channel></rss>"
        val article = RssParser.parse(feed).single()
        assertEquals("Tom & Jerry", article.title)
        assertEquals("Cats & dogs together", article.summary)
    }

    @Test
    fun aFeedCutOffPartWay_keepsTheCompleteArticles() {
        val items = "<item><title>One</title><link>https://e.com/1</link></item>" +
            "<item><title>Two</title><link>https://e.com/2</link></item>" +
            "<item><title>Cut o"
        val titles = RssParser.parse("<rss><channel>$items").map { it.title }
        assertEquals(listOf("One", "Two"), titles)
    }

    @Test
    fun anAtomFeedCutOffPartWay_isClosedAsAtom() {
        val feed = "<feed xmlns=\"http://www.w3.org/2005/Atom\"><entry><title>One</title><link href=\"https://e.com/1\"/></entry><entry><title>Tw"
        assertEquals(listOf("One"), RssParser.parse(feed).map { it.title })
    }

    @Test
    fun garbageInput_givesAnEmptyListInsteadOfCrashing() {
        assertTrue(RssParser.parse("this is not xml at all").isEmpty())
        assertTrue(RssParser.parse("").isEmpty())
    }

    @Test
    fun missingDateAndImage_areNull() {
        val feed = "<rss><channel><item><title>Bare</title><link>https://e.com/bare</link></item></channel></rss>"
        val article = RssParser.parse(feed).single()
        assertNull(article.publishedAt)
        assertNull(article.imageUrl)
        assertNotNull(article.summary)
    }

    @Test
    fun readMinutes_usesWordCountOrATypicalThreeForShortDescriptions() {
        assertEquals(3, RssParser.readMinutes(40))
        assertEquals(3, RssParser.readMinutes(149))
        assertEquals(5, RssParser.readMinutes(1000))
        assertEquals(30, RssParser.readMinutes(100_000))
    }

    @Test
    fun plainText_stripsHtmlAndNumericEntities() {
        assertEquals("Tom & Jerry’s", RssParser.plainText("<p>Tom &amp; Jerry&#8217;s</p>"))
    }
}
