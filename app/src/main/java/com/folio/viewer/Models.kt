package com.folio.viewer

enum class Kind(val label: String, val short: String) {
    PDF("PDF", "PDF"),
    WORD("Word", "DOC"),
    EXCEL("Excel", "XLS"),
    SLIDES("PowerPoint", "PPT")
}

data class Run(val text: String, val bold: Boolean, val italic: Boolean)

sealed interface WBlock {
    data class Para(val runs: List<Run>, val heading: Int, val bullet: Boolean) : WBlock
    data class Table(val rows: List<List<String>>) : WBlock
}

class SheetData(
    val name: String,
    val rows: List<List<String>>,
    val colCount: Int,
    val truncated: Boolean
)

class SlideData(val title: String, val body: List<Pair<Int, String>>)

sealed interface Doc {
    class Pdf(val source: PdfSource) : Doc
    class Word(val blocks: List<WBlock>) : Doc
    class Sheets(val sheets: List<SheetData>) : Doc
    class Slides(val slides: List<SlideData>) : Doc
}

sealed interface UiState {
    data object Idle : UiState
    data class Loading(val name: String) : UiState
    data class Failed(val name: String, val message: String) : UiState
    data class Ready(val name: String, val kind: Kind, val doc: Doc) : UiState
}

class UnsupportedException(message: String) : Exception(message)
