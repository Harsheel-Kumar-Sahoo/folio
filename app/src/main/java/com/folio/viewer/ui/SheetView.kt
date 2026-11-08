package com.folio.viewer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.viewer.SheetData
import com.folio.viewer.XlsxParser

@Composable
fun SheetView(sheets: List<SheetData>) {
    var sel by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        if (sheets.size > 1) {
            Row(
                Modifier.horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sheets.forEachIndexed { i, s -> SheetTab(s.name, i == sel) { sel = i } }
            }
        }
        val sheet = sheets.getOrNull(sel)
        if (sheet == null || sheet.rows.isEmpty()) {
            EmptyState("This sheet is empty")
        } else {
            Grid(sheet)
        }
    }
}

@Composable
private fun SheetTab(name: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) cs.primary else cs.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) cs.onPrimary else cs.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun colName(index: Int): String {
    var n = index
    val sb = StringBuilder()
    do {
        sb.append('A' + n % 26)
        n = n / 26 - 1
    } while (n >= 0)
    return sb.reverse().toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Grid(sheet: SheetData) {
    val colWidths = remember(sheet) {
        List(sheet.colCount) { c ->
            var m = 3
            for (r in sheet.rows) {
                val l = r[c].length
                if (l > m) m = l
            }
            (minOf(m, 30) * 8 + 24).dp
        }
    }
    val rowNumWidth = 48.dp
    val totalWidth = colWidths.fold(rowNumWidth) { acc, w -> acc + w }

    Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        LazyColumn(Modifier.width(totalWidth).fillMaxHeight()) {
            stickyHeader {
                Row {
                    Cell("", rowNumWidth, header = true)
                    colWidths.forEachIndexed { i, w -> Cell(colName(i), w, header = true) }
                }
            }
            itemsIndexed(sheet.rows) { ri, row ->
                Row {
                    Cell((ri + 1).toString(), rowNumWidth, header = true)
                    row.forEachIndexed { ci, t -> Cell(t, colWidths[ci], header = false) }
                }
            }
            if (sheet.truncated) {
                item {
                    Text(
                        "Showing the first ${XlsxParser.MAX_ROWS} rows and ${XlsxParser.MAX_COLS} columns",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Cell(text: String, width: Dp, header: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.width(width).height(36.dp)
            .background(if (header) cs.surfaceVariant else cs.background)
            .border(0.5.dp, cs.outline.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp),
        contentAlignment = if (header) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            fontWeight = if (header) FontWeight.Medium else FontWeight.Normal,
            color = if (header) cs.onSurfaceVariant else cs.onBackground
        )
    }
}
