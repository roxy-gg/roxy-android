package gg.roxy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gg.roxy.chatFullscreen.components.ChatFullScreen
import gg.roxy.mainFullScreen.components.MainFullScreen

@Composable
fun RoxyApp(
    uiState: RoxyAppUiState,
    onComputerMenuExpandedChange: (Boolean) -> Unit,
    onComputerSelected: (String) -> Unit,
    onSessionSelected: (String) -> Unit,
    onBackFromChat: () -> Unit,
    onComposerChange: (String) -> Unit,
    onComposerSubmit: () -> Unit,
    onToolCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.destination) {
        RoxyDestination.Main -> MainFullScreen(
            uiState = uiState.main,
            onComputerMenuExpandedChange = onComputerMenuExpandedChange,
            onComputerSelected = onComputerSelected,
            onSessionSelected = onSessionSelected,
            modifier = modifier,
        )

        RoxyDestination.Chat -> ChatFullScreen(
            uiState = uiState.chat,
            onBackClick = onBackFromChat,
            onComposerChange = onComposerChange,
            onComposerSubmit = onComposerSubmit,
            onToolCallClick = onToolCallClick,
            modifier = modifier,
        )
    }
}
