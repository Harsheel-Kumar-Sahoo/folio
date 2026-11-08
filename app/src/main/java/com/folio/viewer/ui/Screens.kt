package com.folio.viewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.viewer.Doc
import com.folio.viewer.Kind
import com.folio.viewer.UiState

@Composable
fun App(state: UiState, onPick: () -> Unit, onClose: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            when (state) {
                UiState.Idle -> HomeScreen(onPick)

                is UiState.Loading -> {
                    TopBar(state.name, null, onClose)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.5.dp)
                    }
                }

                is UiState.Failed -> {
                    TopBar(state.name, null, onClose)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Can't open this file",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.message,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is UiState.Ready -> {
                    TopBar(state.name, state.kind, onClose)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (val d = state.doc) {
                            is Doc.Pdf -> PdfView(d.source)
                            is Doc.Word -> WordView(d.blocks)
                            is Doc.Sheets -> SheetView(d.sheets)
                            is Doc.Slides -> SlidesView(d.slides)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(name: String, kind: Kind?, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (kind != null) {
            Spacer(Modifier.width(8.dp))
            KindChip(kind)
        }
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun KindChip(kind: Kind) {
    val c = kindColor(kind)
    Row(
        Modifier.clip(CircleShape).background(c.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(6.dp))
        Text(kind.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c)
    }
}

@Composable
private fun HomeScreen(onPick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kind.entries.forEach { FileTile(it) }
        }
        Spacer(Modifier.height(36.dp))
        Text(
            "Folio",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "PDF, Word, Excel and PowerPoint.\nClean, fast and offline.",
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onPick,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text("Browse files", fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Tip: tap a file in WhatsApp and pick Folio in the Open with menu.",
            color = cs.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FileTile(kind: Kind) {
    val c = kindColor(kind)
    Box(
        Modifier.size(54.dp).clip(RoundedCornerShape(15.dp)).background(c.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(kind.short, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c)
    }
}
