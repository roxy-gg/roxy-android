package gg.roxy.shared.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import gg.roxy.shared.styles.RoxyMonoFontFamily
import gg.roxy.shared.styles.RoxyTheme
import gg.roxy.shared.styles.roxyColors

/**
 * A segmented PIN entry: one digit per box, backed by a single text field.
 *
 * The boxes are decoration only -- there is exactly one focusable field holding
 * the whole PIN. That is what makes backspace behave: a per-box implementation
 * has to forward deletes between boxes by hand, and an already-empty box never
 * receives the key event in the first place, so the deletion stops there. With
 * one field the caret is always after the last digit, so backspace just erases
 * the PIN right to left, one digit per press.
 */
@Composable
fun PinInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Rebuilt on every recomposition with the caret pinned past the last digit,
    // so a tap on any box cannot drop the caret into the middle of the PIN and
    // desynchronise typing from the box the user is looking at.
    val fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))

    BasicTextField(
        value = fieldValue,
        onValueChange = { new ->
            // Filtering here rather than on the keyboard type also covers paste
            // and autofill, which happily deliver letters and spaces.
            val digits = new.text.filter(Char::isDigit).take(length)
            if (digits != value) onValueChange(digits)
        },
        modifier = modifier.semantics {
            contentDescription = "$length digit PIN, ${value.length} entered"
        },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        singleLine = true,
        interactionSource = interactionSource,
        // The real text is drawn by the boxes below; hide the field's own.
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = { innerTextField ->
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(length) { index ->
                        PinCell(
                            digit = value.getOrNull(index),
                            // Once full there is no next box, so keep the
                            // highlight on the last one instead of dropping it.
                            isActive = isFocused &&
                                (index == value.length || (index == length - 1 && value.length == length)),
                            isError = isError,
                            enabled = enabled,
                            // Splitting the available width keeps every cell the
                            // same size. Fixed widths cannot: once they overflow
                            // the dialog, Row shrinks whichever cell runs out of
                            // space last, leaving the final one visibly narrower.
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // Kept in the tree so the IME has a real field to attach to,
                // but invisible and non-interfering.
                Box(modifier = Modifier.matchParentSize().alpha(0f)) {
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun PinCell(
    digit: Char?,
    isActive: Boolean,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colors.danger
            isActive -> colors.accent
            digit != null -> colors.edgeStrong
            else -> colors.edge
        },
        animationSpec = tween(durationMillis = 150),
        label = "pinCellBorder",
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) colors.surface2 else colors.surface)
            .border(
                width = if (isActive || isError) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = RoxyMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                ),
                color = if (enabled) colors.text else colors.textSubtle,
            )
        } else if (isActive) {
            BlinkingCaret(color = colors.accent)
        } else {
            // Placeholder dot, so an empty box does not read as broken.
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.textSubtle),
            )
        }
    }
}

@Composable
private fun BlinkingCaret(color: Color) {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "caretAlpha",
    )
    Box(
        modifier = Modifier
            .alpha(alpha)
            .width(2.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(color),
    )
}

@Preview(name = "PinInput - Dark", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun PinInputDarkPreview() {
    RoxyTheme(darkTheme = true) {
        Box(modifier = Modifier.width(320.dp)) {
            PinInput(value = "123", onValueChange = {})
        }
    }
}

@Preview(name = "PinInput - Error", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun PinInputErrorPreview() {
    RoxyTheme(darkTheme = true) {
        Box(modifier = Modifier.width(320.dp)) {
            PinInput(value = "1234", onValueChange = {}, isError = true)
        }
    }
}
