package gg.roxy.chatFullscreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    canSubmit: Boolean = true,
) {
    val colors = MaterialTheme.roxyColors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface2,
        contentColor = colors.text,
        border = BorderStroke(1.dp, colors.edgeStrong),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, top = 11.dp, end = 12.dp, bottom = 10.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp, max = 120.dp)
                    .semantics { contentDescription = "Message Roxy" }
                    .padding(vertical = 4.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSubmit && text.isNotBlank()) onSubmit() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "Ask Roxy anything... (paste or drop images)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // Toolbar matching PC desktop layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Attach button (+)
                Surface(
                    onClick = { /* Attach image or file */ },
                    shape = CircleShape,
                    color = colors.elevated,
                    border = BorderStroke(1.dp, colors.edge),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Attach image or file",
                            modifier = Modifier.size(16.dp),
                            tint = colors.textMuted,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Uses this session's desktop model",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )

                // Send Button with clean default theme (White when active)
                val isSendActive = canSubmit && text.isNotBlank()
                Surface(
                    onClick = onSubmit,
                    enabled = isSendActive,
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSendActive) Color.White else colors.white.copy(alpha = 0.25f),
                    contentColor = Color.Black,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Send",
                            modifier = Modifier.size(18.dp),
                            tint = if (isSendActive) Color.Black else colors.textSubtle,
                        )
                    }
                }
            }
        }
    }
}
