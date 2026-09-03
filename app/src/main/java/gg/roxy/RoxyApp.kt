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
    onAddNewComputer: () -> Unit = {},
    onDismissConnectDialog: () -> Unit = {},
    onConnectComputer: (String, String) -> Unit = { _, _ -> },
    initialToken: String = "",
    initialPin: String = "",
) {
    when (uiState.destination) {
        RoxyDestination.Main -> MainFullScreen(
            uiState = uiState.main,
            onComputerMenuExpandedChange = onComputerMenuExpandedChange,
            onComputerSelected = onComputerSelected,
            onSessionSelected = onSessionSelected,
            onAddNewComputer = onAddNewComputer,
            onDismissConnectDialog = onDismissConnectDialog,
            onConnectComputer = onConnectComputer,
            initialToken = initialToken,
            initialPin = initialPin,
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
