package gg.roxy.mainFullScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gg.roxy.mainFullScreen.businessLogic.ComputerUiModel
import gg.roxy.mainFullScreen.businessLogic.MainFullScreenUiState
import gg.roxy.mainFullScreen.businessLogic.ProjectUiModel
import gg.roxy.mainFullScreen.businessLogic.SessionUiModel
import gg.roxy.shared.styles.RoxyTheme
import gg.roxy.shared.styles.roxyColors

@Composable
fun MainFullScreen(
    uiState: MainFullScreenUiState,
    onComputerMenuExpandedChange: (Boolean) -> Unit,
    onComputerSelected: (String) -> Unit,
    onSessionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "Roxy",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Pick up a session from any connected computer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
            }

            item {
                ComputerSelector(
                    selectedComputer = uiState.selectedComputer,
                    computers = uiState.computers,
                    expanded = uiState.isComputerMenuExpanded,
                    onExpandedChange = onComputerMenuExpandedChange,
                    onComputerSelected = onComputerSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    text = "Sessions",
                    modifier = Modifier.padding(top = 5.dp, start = 2.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                )
            }

            uiState.projects.forEach { project ->
                item(key = project.id) {
                    ProjectSection(
                        project = project,
                        onSessionSelected = onSessionSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private val MainPreviewState = MainFullScreenUiState(
    selectedComputer = ComputerUiModel("computer-1", "Computer #1", "Connected", true),
    computers = listOf(ComputerUiModel("computer-1", "Computer #1", "Connected", true)),
    projects = listOf(
        ProjectUiModel(
            id = "project-1",
            name = "Project #1",
            sessions = listOf(
                SessionUiModel("session-1", "Session #1", "Building the Android client", "Now", true),
                SessionUiModel("session-2", "Session #2", "Desktop theme parity", "18m"),
                SessionUiModel("session-3", "Session #3", "Compose architecture", "Yesterday"),
            ),
        ),
        ProjectUiModel(
            id = "project-2",
            name = "Project #2",
            sessions = listOf(
                SessionUiModel("session-4", "Session #1", "Remote workspace", "Mon"),
            ),
        ),
    ),
)

@Preview(name = "Main - Dark", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun MainFullScreenDarkPreview() {
    RoxyTheme(darkTheme = true) {
        MainFullScreen(
            uiState = MainPreviewState,
            onComputerMenuExpandedChange = {},
            onComputerSelected = {},
            onSessionSelected = {},
        )
    }
}

@Preview(name = "Main - Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MainFullScreenLightPreview() {
    RoxyTheme(darkTheme = false) {
        MainFullScreen(
            uiState = MainPreviewState,
            onComputerMenuExpandedChange = {},
            onComputerSelected = {},
            onSessionSelected = {},
        )
    }
}
