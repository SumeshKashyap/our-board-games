@file:OptIn(ExperimentalLayoutApi::class)

package com.sumesh.ourchess.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.board.displayName
import com.sumesh.ourchess.ui.board.pieceDrawable

object StatusColors {
    val Neutral = Color(0xFFE4DCC8)
    val Good = Color(0xFFBFE3C0)
    val Alert = Color(0xFFF6C1BE)
}

/** Full-screen page background with safe-area padding. */
@Composable
fun Page(background: Color = MaterialTheme.colorScheme.background, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .padding(12.dp),
    ) { content() }
}

/** A scrolling page for menus and lists, kept to a comfortable reading width on tablets. */
@Composable
fun MenuPage(background: Color = MaterialTheme.colorScheme.background, content: @Composable ColumnScope.() -> Unit) {
    Page(background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

/** Title row with a large, labelled back button. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?, backLabel: String = "Back") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text(backLabel, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(12.dp))
        }
        // Room on the right for the magnifying glass.
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(end = 60.dp))
    }
}

@Composable
fun StatusBanner(text: String, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Color(0xFF1F1F1F))
    }
}

@Composable
fun BigButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 64.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/**
 * Action buttons: side by side in portrait, stacked in the narrow landscape side panel —
 * and also stacked whenever there isn't room (large system text, or zoomed in on a phone),
 * so labels never get squeezed.
 */
@Composable
fun ButtonGroup(stacked: Boolean, content: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (stacked || LocalDensity.current.fontScale >= 1.3f || maxWidth < 380.dp) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                content(Modifier.weight(1f))
            }
        }
    }
}

/** A large tappable card used for menu choices. */
@Composable
fun ChoiceCard(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) Color(0xFFD7E8D0) else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = BorderStroke(
            if (highlighted) 3.dp else 1.dp,
            if (highlighted) MaterialTheme.colorScheme.primary else Color(0xFFD6CDB8),
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4A4A4A))
            }
            trailing?.invoke(this)
        }
    }
}

@Composable
fun PieceIcon(side: Side, type: PieceType, size: Dp = 56.dp) {
    Image(painterResource(pieceDrawable(side, type)), contentDescription = null, Modifier.size(size))
}

/**
 * Standard layout for any screen with a board.
 * Portrait: header, info, board, below-board info, buttons. Landscape: board left, everything else in a side panel.
 */
@Composable
fun BoardScreenLayout(
    header: @Composable () -> Unit,
    info: @Composable ColumnScope.() -> Unit,
    board: @Composable (Modifier) -> Unit,
    belowBoard: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable (stacked: Boolean) -> Unit,
) {
    Page {
        BoxWithConstraints {
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.fillMaxHeight().weight(1f))
                    Column(
                        Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        header()
                        info()
                        belowBoard()
                        Spacer(Modifier.heightIn(min = 8.dp))
                        actions(true)
                    }
                }
            } else if (LocalDensity.current.fontScale >= 1.3f || maxWidth < 380.dp) {
                // Large text or zoomed in: keep the board full width and let the page scroll, rather than shrinking the board.
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    header()
                    info()
                    board(Modifier.fillMaxWidth().aspectRatio(1f))
                    belowBoard()
                    actions(false)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    header()
                    info()
                    board(Modifier.fillMaxWidth().weight(1f, fill = false))
                    belowBoard()
                    Spacer(Modifier.weight(0.001f))
                    actions(false)
                }
            }
        }
    }
}

@Composable
fun PromotionDialog(side: Side, onChoose: (PieceType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a piece", style = MaterialTheme.typography.headlineSmall) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (type in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    OutlinedButton(onClick = { onChoose(type) }, contentPadding = PaddingValues(8.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PieceIcon(side, type, 64.dp)
                            Text(type.displayName(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
