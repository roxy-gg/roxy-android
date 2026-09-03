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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gg.roxy.mainFullScreen.businessLogic.ProjectUiModel
import gg.roxy.mainFullScreen.businessLogic.SessionUiModel
import gg.roxy.shared.styles.roxyColors

@Composable
fun ProjectSection(
    project: ProjectUiModel,
    onSessionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = colors.textSubtle,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = project.name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = project.sessions.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSubtle,
            )
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.edge),
            tonalElevation = 0.dp,
        ) {
            Column {
                project.sessions.forEachIndexed { index, session ->
                    SessionRow(
                        session = session,
                        onClick = { onSessionSelected(session.id) },
                    )
                    if (index != project.sessions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 19.dp),
                            color = colors.border,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionRow(
    session: SessionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = if (session.isActive) colors.surface2 else colors.surface,
        contentColor = colors.text,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(if (session.isActive) 7.dp else 5.dp),
                    shape = CircleShape,
                    color = if (session.isActive) colors.accent else colors.borderStrong,
                    content = {},
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = session.updatedAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSubtle,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = session.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(7.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.textSubtle,
            )
        }
    }
}
