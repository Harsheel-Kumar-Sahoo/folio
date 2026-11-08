package com.folio.viewer.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.viewer.WBlock

@Composable
fun WordView(blocks: List<WBlock>) {
    if (blocks.isEmpty()) {
        EmptyState("This document has no readable text")
        return
    }
    SelectionContainer {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(blocks) { block ->
                Box(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    when (block) {
                        is WBlock.Para -> ParaView(block)
                        is WBlock.Table -> TableView(block)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParaView(p: WBlock.Para) {
    val (size, weight) = when (p.heading) {
        1 -> 28.sp to FontWeight.Bold
        2 -> 22.sp to FontWeight.SemiBold
        3 -> 18.sp to FontWeight.SemiBold
        4 -> 16.sp to FontWeight.SemiBold
        else -> 16.sp to FontWeight.Normal
    }
    val text = buildAnnotatedString {
        if (p.bullet) append("•  ")
        p.runs.forEach { r ->
            withStyle(
                SpanStyle(
                    fontWeight = if (r.bold) FontWeight.Bold else null,
                    fontStyle = if (r.italic) FontStyle.Italic else null
                )
            ) { append(r.text) }
        }
    }
    Text(
        text,
        fontSize = size,
        fontWeight = weight,
        lineHeight = size * 1.55f,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(
            top = if (p.heading > 0) 18.dp else 4.dp,
            bottom = 4.dp,
            start = if (p.bullet) 8.dp else 0.dp
        )
    )
}

@Composable
private fun TableView(t: WBlock.Table) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.padding(vertical = 8.dp).clip(shape).border(1.dp, outline, shape)) {
        t.rows.forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEach { c ->
                    Text(
                        c,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .border(0.5.dp, outline).padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
