package gg.roxy.mainFullScreen.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import gg.roxy.mainFullScreen.businessLogic.ComputerUiModel
import gg.roxy.shared.styles.roxyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComputerSelector(
    selectedComputer: ComputerUiModel,
    computers: List<ComputerUiModel>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onComputerSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = colors.surface2,
            contentColor = colors.text,
            border = BorderStroke(1.dp, if (expanded) colors.edgeStrong else colors.edge),
            tonalElevation = 0.dp,
            shadowElevation = if (expanded) 2.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.elevated,
                    border = BorderStroke(1.dp, colors.edge),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colors.textMuted,
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedComputer.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ConnectionDot(isConnected = selectedComputer.isConnected)
                        Text(
                            text = selectedComputer.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse computers" else "Choose computer",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                    tint = colors.textMuted,
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
            shape = MaterialTheme.shapes.large,
            containerColor = colors.elevated,
            border = BorderStroke(1.dp, colors.edgeStrong),
            shadowElevation = 8.dp,
        ) {
            computers.forEachIndexed { index, computer ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = computer.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.text,
                            )
                            Text(
                                text = computer.status,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }
                    },
                    onClick = { onComputerSelected(computer.id) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Computer,
                            contentDescription = null,
                            tint = colors.textMuted,
                        )
                    },
                    trailingIcon = if (computer.id == selectedComputer.id) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = colors.accent,
                            )
                        }
                    } else {
                        null
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                if (index != computers.lastIndex) {
                    HorizontalDivider(color = colors.border)
                }
            }
        }
    }
}

@Composable
private fun ConnectionDot(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors
    Surface(
        modifier = modifier.size(7.dp),
        shape = CircleShape,
        color = if (isConnected) colors.success else colors.textSubtle,
        content = {},
    )
}
