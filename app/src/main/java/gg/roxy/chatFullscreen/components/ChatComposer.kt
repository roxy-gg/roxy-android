package gg.roxy.chatFullscreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import gg.roxy.shared.styles.roxyColors

@Composable
fun ChatComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface2,
        contentColor = colors.text,
        border = BorderStroke(1.dp, colors.edgeStrong),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 62.dp)
                .padding(start = 15.dp, top = 9.dp, end = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp, max = 140.dp)
                    .semantics { contentDescription = "Message Roxy" }
                    .padding(vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSubmit() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "Message Roxy...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onSubmit,
                enabled = text.isNotBlank(),
                modifier = Modifier.size(38.dp),
                shape = MaterialTheme.shapes.medium,
                color = colors.white.copy(alpha = if (text.isNotBlank()) 1f else 0.30f),
                contentColor = colors.black,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Send",
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}
