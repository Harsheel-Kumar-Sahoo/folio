package com.folio.viewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.viewer.SlideData

@Composable
fun SlidesView(slides: List<SlideData>) {
    if (slides.isEmpty()) {
        EmptyState("This presentation has no slides")
        return
    }
    val pager = rememberPagerState { slides.size }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { i -> SlideCard(slides[i], i) }

        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                .clip(CircleShape).background(Color(0xCC17171A))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "${pager.currentPage + 1} / ${slides.size}",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SlideCard(s: SlideData, index: Int) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(Modifier.fillMaxSize().padding(top = 8.dp, bottom = 64.dp)) {
        val minHeight = maxWidth * 9f / 16f
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape)
                    .background(cs.surface).border(1.dp, cs.outline, shape).padding(22.dp)
            ) {
                if (s.title.isNotEmpty()) {
                    Text(
                        s.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                } else {
                    Text(
                        "Slide ${index + 1}",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
                s.body.forEach { (lvl, t) ->
                    Row(Modifier.padding(start = (lvl * 16).dp, bottom = 8.dp)) {
                        Text(
                            if (lvl == 0) "•" else "–",
                            color = cs.primary,
                            modifier = Modifier.width(18.dp)
                        )
                        Text(t, fontSize = 15.sp, lineHeight = 22.sp, color = cs.onSurface)
                    }
                }
            }
        }
    }
}
