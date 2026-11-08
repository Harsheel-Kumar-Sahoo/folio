package com.folio.viewer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.viewer.PdfSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PdfView(source: PdfSource) {
    var zoom by remember { mutableFloatStateOf(1f) }
    val listState = rememberLazyListState()
    val scrollX = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageBoxWidth = maxWidth * zoom
        val renderWidthPx = with(density) { (pageBoxWidth - 24.dp).roundToPx() }

        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { zoom = if (zoom > 1f) 1f else 2.2f })
            }
        ) {
            Box(Modifier.fillMaxSize().horizontalScroll(scrollX)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.width(pageBoxWidth).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(source.pageCount) { index ->
                        PdfPage(source, index, renderWidthPx)
                    }
                }
            }

            val current by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                    .clip(CircleShape).background(Color(0xCC17171A))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    "${current.coerceAtMost(source.pageCount.coerceAtLeast(1))} / ${source.pageCount}",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PdfPage(source: PdfSource, index: Int, widthPx: Int) {
    val bitmap by produceState<Bitmap?>(null, index, widthPx) {
        value = try {
            withContext(Dispatchers.IO) { source.render(index, widthPx.coerceIn(200, 2000)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    val shape = RoundedCornerShape(6.dp)
    val ratio = bitmap?.let { it.width.toFloat() / it.height } ?: source.placeholderRatio
    Box(
        Modifier.fillMaxWidth().aspectRatio(ratio).clip(shape).background(Color.White)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}
