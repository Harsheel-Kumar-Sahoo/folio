package com.folio.viewer

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.util.zip.ZipFile
import kotlin.math.abs
import kotlin.math.max

// ---------------------------------------------------------------------------
// Entry point: sniff the file's real type from its bytes (names/mime can lie).
// ---------------------------------------------------------------------------

object Loader {
    class Loaded(val kind: Kind, val doc: Doc)

    fun load(file: File): Loaded {
        val head = ByteArray(8)
        val n = file.inputStream().use { it.read(head) }
        val b = head.map { it.toInt() and 0xFF }

        if (n >= 4 && b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46) {
            return Loaded(Kind.PDF, Doc.Pdf(PdfSource(file)))
        }
        if (n >= 4 && b[0] == 0x50 && b[1] == 0x4B) {
            ZipFile(file).use { zip ->
                return when {
                    zip.getEntry("word/document.xml") != null ->
                        Loaded(Kind.WORD, Doc.Word(DocxParser.parse(zip)))
                    zip.getEntry("xl/workbook.xml") != null ->
                        Loaded(Kind.EXCEL, Doc.Sheets(XlsxParser.parse(zip)))
                    zip.getEntry("ppt/presentation.xml") != null ->
                        Loaded(Kind.SLIDES, Doc.Slides(PptxParser.parse(zip)))
                    else -> throw UnsupportedException(
                        "This isn't a PDF, Word, Excel or PowerPoint document."
                    )
                }
            }
        }
        if (n >= 4 && b[0] == 0xD0 && b[1] == 0xCF && b[2] == 0x11 && b[3] == 0xE0) {
            throw UnsupportedException(
                "Older Office files (.doc, .xls, .ppt) aren't supported yet. " +
                    "Re-save as .docx, .xlsx or .pptx and try again."
            )
        }
        throw UnsupportedException("Unsupported file type.")
    }
}

// ---------------------------------------------------------------------------
// XML helpers (parser is namespace-unaware, so names look like "w:p" / "a:t")
// ---------------------------------------------------------------------------

internal fun XmlPullParser.local(): String = (name ?: "").substringAfter(':')

internal fun XmlPullParser.attr(local: String): String? {
    for (i in 0 until attributeCount) {
        if (getAttributeName(i).substringAfter(':') == local) return getAttributeValue(i)
    }
    return null
}

/** The relationship id attribute (r:id), as opposed to a plain id attribute. */
internal fun XmlPullParser.relId(): String? {
    for (i in 0 until attributeCount) {
        val n = getAttributeName(i)
        if (n.contains(':') && n.substringAfter(':') == "id") return getAttributeValue(i)
    }
    return null
}

internal fun <T> ZipFile.parseXml(name: String, block: (XmlPullParser) -> T): T? {
    val entry = getEntry(name) ?: return null
    return getInputStream(entry).use { stream ->
        val p = Xml.newPullParser()
        p.setInput(stream, null)
        block(p)
    }
}

private fun readRels(zip: ZipFile, relsPath: String, baseDir: String): Map<String, String> {
    val rels = HashMap<String, String>()
    zip.parseXml(relsPath) { p ->
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.local() == "Relationship") {
                val id = p.attr("Id")
                val target = p.attr("Target")
                if (id != null && target != null) {
                    rels[id] = if (target.startsWith("/")) target.removePrefix("/") else "$baseDir$target"
                }
            }
            ev = p.next()
        }
    }
    return rels
}

// ---------------------------------------------------------------------------
// DOCX
// ---------------------------------------------------------------------------

object DocxParser {
    private fun String?.isOn(): Boolean =
        this == null || (this != "0" && !this.equals("false", true) && !this.equals("off", true))

    fun parse(zip: ZipFile): List<WBlock> {
        val out = ArrayList<WBlock>()
        zip.parseXml("word/document.xml") { p ->
            var tbl = 0
            var rows = ArrayList<MutableList<String>>()
            var cell = StringBuilder()
            var runs = ArrayList<Run>()
            var heading = 0
            var bullet = false
            var inPPr = false
            var inRPr = false
            var bold = false
            var italic = false

            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    when (p.local()) {
                        "tbl" -> {
                            tbl++
                            if (tbl == 1) rows = ArrayList()
                        }
                        "tr" -> if (tbl == 1) rows.add(ArrayList())
                        "tc" -> if (tbl == 1) cell = StringBuilder()
                        "p" -> {
                            runs = ArrayList()
                            heading = 0
                            bullet = false
                        }
                        "pPr" -> inPPr = true
                        "pStyle" -> if (inPPr) {
                            val v = p.attr("val") ?: ""
                            heading = when {
                                v.equals("Title", true) -> 1
                                v.startsWith("Heading", true) ->
                                    v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 4) ?: 2
                                else -> 0
                            }
                        }
                        "numPr" -> if (inPPr) bullet = true
                        "r" -> {
                            bold = false
                            italic = false
                        }
                        "rPr" -> inRPr = true
                        "b" -> if (inRPr && !inPPr) bold = p.attr("val").isOn()
                        "i" -> if (inRPr && !inPPr) italic = p.attr("val").isOn()
                        "t" -> {
                            val t = p.nextText()
                            if (t.isNotEmpty()) runs.add(Run(t, bold, italic))
                        }
                        "tab" -> if (!inPPr) runs.add(Run("    ", false, false))
                        "br" -> runs.add(Run("\n", false, false))
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    when (p.local()) {
                        "pPr" -> inPPr = false
                        "rPr" -> inRPr = false
                        "p" -> {
                            val text = runs.joinToString("") { it.text }
                            if (tbl > 0) {
                                if (text.isNotBlank()) {
                                    if (cell.isNotEmpty()) cell.append('\n')
                                    cell.append(text.trim())
                                }
                            } else if (text.isNotBlank()) {
                                out.add(WBlock.Para(runs.toList(), heading, bullet))
                            }
                        }
                        "tc" -> if (tbl == 1) rows.lastOrNull()?.add(cell.toString())
                        "tbl" -> {
                            tbl--
                            if (tbl == 0 && rows.isNotEmpty()) {
                                out.add(WBlock.Table(rows.map { it.toList() }))
                            }
                        }
                    }
                }
                ev = p.next()
            }
        }
        return out
    }
}

// ---------------------------------------------------------------------------
// XLSX
// ---------------------------------------------------------------------------

object XlsxParser {
    const val MAX_ROWS = 2000
    const val MAX_COLS = 60

    fun parse(zip: ZipFile): List<SheetData> {
        val shared = readShared(zip)
        return sheetTargets(zip).map { (name, path) -> readSheet(zip, name, path, shared) }
    }

    private fun readShared(zip: ZipFile): List<String> {
        val list = ArrayList<String>()
        zip.parseXml("xl/sharedStrings.xml") { p ->
            var sb: StringBuilder? = null
            var inPhonetic = false
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    when (p.local()) {
                        "si" -> sb = StringBuilder()
                        "rPh" -> inPhonetic = true
                        "t" -> {
                            val t = p.nextText()
                            if (!inPhonetic) sb?.append(t)
                        }
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    when (p.local()) {
                        "rPh" -> inPhonetic = false
                        "si" -> {
                            list.add(sb?.toString() ?: "")
                            sb = null
                        }
                    }
                }
                ev = p.next()
            }
        }
        return list
    }

    private fun sheetTargets(zip: ZipFile): List<Pair<String, String>> {
        val rels = readRels(zip, "xl/_rels/workbook.xml.rels", "xl/")
        val out = ArrayList<Pair<String, String>>()
        zip.parseXml("xl/workbook.xml") { p ->
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.local() == "sheet") {
                    val name = p.attr("name") ?: "Sheet"
                    val path = p.relId()?.let { rels[it] }
                    if (path != null) out.add(name to path)
                }
                ev = p.next()
            }
        }
        return out
    }

    private fun colFromRef(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (ch.isLetter()) n = n * 26 + (ch.uppercaseChar() - 'A' + 1) else break
        }
        return n
    }

    private fun fmtNum(v: String?): String {
        if (v == null) return ""
        val d = v.toDoubleOrNull() ?: return v
        if (d.isNaN() || d.isInfinite()) return v
        if (d == Math.floor(d) && abs(d) < 1e15) return d.toLong().toString()
        return BigDecimal(d).round(MathContext(10)).stripTrailingZeros().toPlainString()
    }

    private fun readSheet(
        zip: ZipFile,
        name: String,
        path: String,
        shared: List<String>
    ): SheetData {
        val cells = HashMap<Int, HashMap<Int, String>>()
        var maxRow = 0
        var maxCol = 0
        var truncated = false

        zip.parseXml(path) { p ->
            var rowIdx = 0
            var colIdx = 0
            var type = ""
            var v: String? = null
            var inline = StringBuilder()
            var inCell = false

            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    when (p.local()) {
                        "row" -> {
                            rowIdx = p.attr("r")?.toIntOrNull() ?: (rowIdx + 1)
                            colIdx = 0
                            if (rowIdx > MAX_ROWS) {
                                truncated = true
                                break
                            }
                        }
                        "c" -> {
                            inCell = true
                            type = p.attr("t") ?: ""
                            v = null
                            inline = StringBuilder()
                            val r = p.attr("r")
                            colIdx = if (r != null) colFromRef(r) else colIdx + 1
                        }
                        "v" -> v = p.nextText()
                        "t" -> if (inCell) inline.append(p.nextText())
                    }
                } else if (ev == XmlPullParser.END_TAG && p.local() == "c") {
                    inCell = false
                    val text = when (type) {
                        "s" -> v?.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                        "inlineStr" -> inline.toString()
                        "b" -> if (v == "1") "TRUE" else "FALSE"
                        "str", "e" -> v ?: ""
                        else -> fmtNum(v)
                    }
                    if (text.isNotEmpty()) {
                        if (colIdx > MAX_COLS) {
                            truncated = true
                        } else {
                            cells.getOrPut(rowIdx) { HashMap() }[colIdx] = text
                            maxRow = max(maxRow, rowIdx)
                            maxCol = max(maxCol, colIdx)
                        }
                    }
                }
                ev = p.next()
            }
        }

        val rows = (1..maxRow).map { r ->
            val m = cells[r]
            List(maxCol) { c -> m?.get(c + 1) ?: "" }
        }
        return SheetData(name, rows, maxCol, truncated)
    }
}

// ---------------------------------------------------------------------------
// PPTX (text content, in presentation order)
// ---------------------------------------------------------------------------

object PptxParser {
    fun parse(zip: ZipFile): List<SlideData> = orderedSlides(zip).map { readSlide(zip, it) }

    private fun orderedSlides(zip: ZipFile): List<String> {
        val rels = readRels(zip, "ppt/_rels/presentation.xml.rels", "ppt/")
        val ordered = ArrayList<String>()
        zip.parseXml("ppt/presentation.xml") { p ->
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.local() == "sldId") {
                    p.relId()?.let { rels[it] }?.let { ordered.add(it) }
                }
                ev = p.next()
            }
        }
        if (ordered.isNotEmpty()) return ordered

        // Fallback: numeric order of slideN.xml
        val re = Regex("^ppt/slides/slide(\\d+)\\.xml$")
        return zip.entries().toList()
            .mapNotNull { e -> re.find(e.name)?.let { e.name to it.groupValues[1].toInt() } }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun readSlide(zip: ZipFile, path: String): SlideData {
        var title = ""
        val body = ArrayList<Pair<Int, String>>()
        zip.parseXml(path) { p ->
            var isTitle = false
            var lvl = 0
            val sb = StringBuilder()
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    when (p.local()) {
                        "sp" -> isTitle = false
                        "ph" -> {
                            val t = p.attr("type")
                            if (t == "title" || t == "ctrTitle") isTitle = true
                        }
                        "p" -> {
                            sb.setLength(0)
                            lvl = 0
                        }
                        "pPr" -> lvl = p.attr("lvl")?.toIntOrNull() ?: 0
                        "br" -> sb.append('\n')
                        "t" -> sb.append(p.nextText())
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    when (p.local()) {
                        "p" -> {
                            val s = sb.toString().trim()
                            if (s.isNotEmpty()) {
                                if (isTitle) {
                                    title = if (title.isEmpty()) s else "$title $s"
                                } else {
                                    body.add(lvl to s)
                                }
                            }
                            sb.setLength(0)
                        }
                        "sp" -> isTitle = false
                    }
                }
                ev = p.next()
            }
        }
        return SlideData(title, body)
    }
}
