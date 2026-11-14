package com.boolder.boolder.view.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.boolder.boolder.domain.glossary.ClimbingGlossary
import com.boolder.boolder.domain.glossary.GlossaryMatch
import com.boolder.boolder.domain.glossary.GlossaryTerm
import com.boolder.boolder.utils.getLanguage
import kotlin.math.roundToInt

/**
 * Text, typically a problem name, whose French climbing terms ("assis",
 * "rallongé", "du bas"...) get a subtle dotted underline and show their
 * English meaning when tapped. Plain text when the app is in French.
 */
@Composable
fun GlossaryText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val matches = remember(text) {
        if (getLanguage() == "fr" || isPreview) {
            emptyList()
        } else {
            ClimbingGlossary.get(context).findTerms(text)
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var shownMatch by remember(text) { mutableStateOf<GlossaryMatch?>(null) }

    val annotatedText = remember(text, matches) {
        buildAnnotatedString {
            append(text)

            matches.forEach { match ->
                addLink(
                    LinkAnnotation.Clickable(tag = match.term.term) {
                        shownMatch = match.takeUnless { shownMatch == match }
                    },
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }

    val underlineColor = MaterialTheme.colorScheme.onSurfaceVariant

    // the popup is positioned relative to this box, i.e. to the text
    Box(modifier = modifier) {
        Text(
            modifier = Modifier.drawBehind {
                val layout = layoutResult ?: return@drawBehind
                val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))

                matches.forEach { match ->
                    val start = match.range.first
                    val end = match.range.last + 1
                    val line = layout.getLineForOffset(start)

                    // skip terms wrapped onto two lines or cut off by the ellipsis
                    if (line != layout.getLineForOffset(end - 1)) return@forEach
                    if (end > layout.getLineEnd(line, visibleEnd = true)) return@forEach

                    val y = layout.getLineBaseline(line) + 3.dp.toPx()

                    drawLine(
                        color = underlineColor,
                        start = Offset(layout.getHorizontalPosition(start, usePrimaryDirection = true), y),
                        end = Offset(layout.getHorizontalPosition(end, usePrimaryDirection = true), y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }
            },
            text = annotatedText,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult = it }
        )

        val match = shownMatch ?: return@Box
        val layout = layoutResult ?: return@Box

        Popup(
            offset = IntOffset(
                x = layout.getHorizontalPosition(match.range.first, usePrimaryDirection = true).roundToInt(),
                y = layout.getLineBottom(layout.getLineForOffset(match.range.first)).roundToInt()
            ),
            onDismissRequest = { shownMatch = null },
            properties = PopupProperties(focusable = true)
        ) {
            GlossaryCard(term = match.term)
        }
    }
}

@Composable
private fun GlossaryCard(term: GlossaryTerm) {
    Surface(
        modifier = Modifier.widthIn(max = 280.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = "${term.term} · ${term.en.translation}",
                style = MaterialTheme.typography.titleSmall
            )

            term.en.explanation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
