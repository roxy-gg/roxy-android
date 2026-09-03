package gg.roxy.mainFullScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import gg.roxy.shared.styles.RoxyMonoFontFamily
import gg.roxy.shared.styles.roxyColors

@Composable
fun ConnectComputerDialog(
    isConnecting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConnect: (tokenOrUrl: String, pin: String) -> Unit,
    initialTokenOrUrl: String = "",
    initialPin: String = "",
) {
    val colors = MaterialTheme.roxyColors
    var tokenInput by remember { mutableStateOf(initialTokenOrUrl) }
    var pinInput by remember { mutableStateOf(initialPin) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val canConnect = tokenInput.isNotBlank() && pinInput.trim().length == 6 && !isConnecting

    Dialog(onDismissRequest = { if (!isConnecting) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colors.elevated,
            border = BorderStroke(1.dp, colors.edgeStrong),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.edge),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = colors.text,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Connect PC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                        )
                        Text(
                            text = "Roxy Remote Workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }

                Text(
                    text = "In Roxy on your computer, click 'Remote Workspace' in the sidebar to get your link and PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )

                // Input: Link / Token
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LINK OR TOKEN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RoxyMonoFontFamily,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.textSubtle,
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Paste https://roxy.gg/r/... or token",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSubtle,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.edgeStrong,
                            unfocusedBorderColor = colors.edge,
                            focusedContainerColor = colors.surface2,
                            unfocusedContainerColor = colors.surface2,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                }

                // Input: PIN
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "6-DIGIT PIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RoxyMonoFontFamily,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.textSubtle,
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "e.g. 123456",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSubtle,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.edgeStrong,
                            unfocusedBorderColor = colors.edge,
                            focusedContainerColor = colors.surface2,
                            unfocusedContainerColor = colors.surface2,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (canConnect) onConnect(tokenInput, pinInput)
                            }
                        ),
                    )
                }

                // Error alert
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.accent),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.accent,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textMuted,
                        ),
                        border = BorderStroke(1.dp, colors.edge),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onConnect(tokenInput, pinInput)
                        },
                        enabled = canConnect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.bg,
                            disabledContainerColor = colors.surface2,
                            disabledContentColor = colors.textSubtle,
                        ),
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.bg,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Connecting...")
                        } else {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}
