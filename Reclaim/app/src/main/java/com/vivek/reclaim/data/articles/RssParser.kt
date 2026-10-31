package com.vivek.reclaim.data.articles

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.ceil

data class ParsedArticle(
    val title: String,
    val url: String,
    val summary: String,
    val imageUrl: String?,
    /** Epoch millis from the feed, or null when the feed has no usable date. */
    val publishedAt: Long?,
    /** Words in the full text when the feed includes it, otherwise in the short description. */
    val wordCount: Int,
)

/** Reads RSS 2.0 and Atom feeds (PRD FR-18). Plain DOM, so it also runs in JVM unit tests. */
object RssParser {
    private const val SUMMARY_LENGTH = 220
    private val imageExtension = Regex("\\.(jpe?g|png|webp|gif)(\\?.*)?$", RegexOption.IGNORE_CASE)
    private val imgElement = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val srcAttribute = Regex("\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val hiddenStyle = Regex("display\\s*:\\s*none|visibility\\s*:\\s*hidden", RegexOption.IGNORE_CASE)
    private val trackerUrl = Regex("pixel|tracking|beacon|/stats?[/.?]|1x1|spacer|feedburner", RegexOption.IGNORE_CASE)
    private val blocks = Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val breaks = Regex("<(br|/p|/div|/li)[^>]*>", RegexOption.IGNORE_CASE)
    private val tags = Regex("<[^>]+>")
    private val whitespace = Regex("\\s+")
    private val numericEntity = Regex("&#(\\d+);")
    private val hexEntity = Regex("&#x([0-9a-fA-F]+);")

    fun parse(xml: String, limit: Int = 15): List<ParsedArticle> {
        val root = runCatching { newBuilder().parse(InputSource(StringReader(sanitize(xml)))).documentElement }.getOrNull()
            ?: return emptyList()
        val entries = descendants(root, "item").ifEmpty { descendants(root, "entry") }
        return entries.asSequence().mapNotNull(::toArticle).take(limit).toList()
    }

    private val invalidXmlChars = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]")
    private val rootEnds = listOf("</rss>", "</feed>", "</rdf:RDF>")

    /**
     * Real feeds are messy, and one stray character makes a strict XML parser reject the whole feed. This removes
     * what is outside the feed itself: junk before the first tag (such as a byte-order mark), junk after the closing
     * tag (ScienceDaily ends with an extra "-->"), and control characters XML does not allow. A feed that was cut
     * off part-way (our download is capped) keeps every complete article and is closed again.
     */
    internal fun sanitize(xml: String): String {
        val start = xml.indexOf('<')
        if (start < 0) return xml
        var text = escapeBareAmpersands(xml.substring(start).replace(invalidXmlChars, " "))
        val end = rootEnds.maxOf { tag -> text.lastIndexOf(tag).let { if (it >= 0) it + tag.length else -1 } }
        if (end > 0) return text.substring(0, end)

        val lastItem = maxOf(
            text.lastIndexOf("</item>").let { if (it >= 0) it + "</item>".length else -1 },
            text.lastIndexOf("</entry>").let { if (it >= 0) it + "</entry>".length else -1 },
        )
        if (lastItem > 0) text = text.substring(0, lastItem) + (if (text.contains("<feed")) "</feed>" else "</channel></rss>")
        return text
    }

    /** A CDATA block (left alone) or an "&" that does not start a valid XML entity (escaped). */
    private val cdataOrBareAmpersand = Regex(
        "<!\\[CDATA\\[.*?]]>|&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)",
        RegexOption.DOT_MATCHES_ALL,
    )

    /**
     * "Film & TV" in a title is invalid XML and makes a strict parser reject the whole feed. Escape such ampersands.
     * HTML-only entities like "&nbsp;" are handled the same way and decoded later as ordinary text.
     */
    private fun escapeBareAmpersands(text: String): String =
        cdataOrBareAmpersand.replace(text) { match -> if (match.value.startsWith("<![CDATA[")) match.value else "&amp;" }

    /** Minutes to read. Short descriptions say nothing about article length, so they get a typical 3. */
    fun readMinutes(wordCount: Int): Int =
        if (wordCount < 150) 3 else ceil(wordCount / 200.0).toInt().coerceIn(1, 30)

    private fun toArticle(item: Element): ParsedArticle? {
        val title = plainText(childText(item, "title") ?: return null)
        val url = linkOf(item) ?: return null
        if (title.isBlank()) return null

        val full = childText(item, "content:encoded") ?: childText(item, "content")
        val short = childText(item, "description") ?: childText(item, "summary")
        val html = full ?: short.orEmpty()
        val summary = truncate(plainText(short ?: full.orEmpty()), SUMMARY_LENGTH)
        val words = plainText(html).split(' ').count { it.isNotBlank() }

        return ParsedArticle(
            title = title,
            url = url,
            summary = summary,
            imageUrl = imageOf(item, html + " " + short.orEmpty())?.let(::upgradeToHttps),
            publishedAt = parseDate(childText(item, "pubDate") ?: childText(item, "published") ?: childText(item, "updated") ?: childText(item, "dc:date")),
            wordCount = words,
        )
    }

    private fun linkOf(item: Element): String? {
        childText(item, "link")?.takeIf { it.startsWith("http") }?.let { return it }
        // Atom: <link rel="alternate" href="..."/>
        val atom = children(item, "link")
            .filter { it.getAttribute("rel").let { rel -> rel.isEmpty() || rel == "alternate" } }
            .map { it.getAttribute("href") }
            .firstOrNull { it.startsWith("http") }
        return atom ?: childText(item, "guid")?.takeIf { it.startsWith("http") }
    }

    private fun imageOf(item: Element, html: String): String? {
        for (tag in listOf("media:content", "media:thumbnail")) {
            descendants(item, tag).forEach { el ->
                val url = el.getAttribute("url")
                val medium = el.getAttribute("medium")
                val type = el.getAttribute("type")
                val looksLikeImage = medium == "image" || type.startsWith("image") || imageExtension.containsMatchIn(url) ||
                    (tag == "media:thumbnail" && medium.isEmpty() && type.isEmpty())
                if (url.startsWith("http") && looksLikeImage) return url
            }
        }
        children(item, "enclosure").forEach { el ->
            if (el.getAttribute("type").startsWith("image") && el.getAttribute("url").startsWith("http")) return el.getAttribute("url")
        }
        return firstRealImage(html)
    }

    /**
     * The first picture in the article text that is actually a picture. Many feeds put a 1x1 hidden tracking
     * pixel there (NerdWallet does), which would otherwise show as a blank card.
     */
    private fun firstRealImage(html: String): String? = imgElement.findAll(html).map { it.value }.firstNotNullOfOrNull { tag ->
        val src = srcAttribute.find(tag)?.groupValues?.get(1)
        val tiny = (dimension(tag, "width") ?: Int.MAX_VALUE) <= 2 || (dimension(tag, "height") ?: Int.MAX_VALUE) <= 2
        when {
            src == null || !src.startsWith("http") -> null
            tiny || hiddenStyle.containsMatchIn(tag) || trackerUrl.containsMatchIn(src) -> null
            else -> src
        }
    }

    private fun dimension(tag: String, name: String): Int? =
        Regex("\\b$name\\s*=\\s*[\"']?(\\d+)", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1)?.toIntOrNull()

    private fun upgradeToHttps(url: String) = if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url

    private fun parseDate(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val attempts = listOf<() -> Long>(
            { OffsetDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() },
            { OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() },
            { Instant.parse(text).toEpochMilli() },
            { LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli() },
        )
        for (attempt in attempts) runCatching { attempt() }.getOrNull()?.let { return it }
        return null
    }

    internal fun plainText(html: String): String {
        var text = html.replace(blocks, " ").replace(breaks, " ").replace(tags, "")
        text = decodeEntities(text)
        return text.replace(whitespace, " ").trim()
    }

    private fun decodeEntities(input: String): String {
        var s = input
            .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'")
            .replace("&hellip;", "\u2026").replace("&rsquo;", "\u2019").replace("&lsquo;", "\u2018")
            .replace("&rdquo;", "\u201D").replace("&ldquo;", "\u201C").replace("&mdash;", "\u2014").replace("&ndash;", "\u2013")
        s = numericEntity.replace(s) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value }
        s = hexEntity.replace(s) { m -> m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value }
        return s
    }

    private fun truncate(text: String, max: Int): String {
        if (text.length <= max) return text
        val cut = text.substring(0, max)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > max / 2) cut.substring(0, lastSpace) else cut).trimEnd(',', ';', ':', '-', ' ') + "\u2026"
    }

    private fun children(parent: Element, name: String): List<Element> {
        val result = ArrayList<Element>()
        var node: Node? = parent.firstChild
        while (node != null) {
            if (node is Element && node.nodeName == name) result += node
            node = node.nextSibling
        }
        return result
    }

    private fun childText(parent: Element, name: String): String? =
        children(parent, name).firstOrNull()?.textContent?.trim()?.takeIf { it.isNotEmpty() }

    private fun descendants(parent: Element, name: String): List<Element> {
        val nodes = parent.getElementsByTagName(name)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun newBuilder(): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        // Feeds never need a DTD; refuse it to stay clear of entity-expansion tricks. Some parsers lack the feature.
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        return factory.newDocumentBuilder()
    }
}
