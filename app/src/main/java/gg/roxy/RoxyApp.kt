package gg.roxy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gg.roxy.chatFullscreen.components.ChatFullScreen
import gg.roxy.mainFullscreen.components.MainFullScreen
import gg.roxy.shared.businessLogic.RoxyAppUiState
import gg.roxy.shared.businessLogic.RoxyDestination

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
    onScanQrCode: () -> Unit = {},
    onDismissConnectDialog: () -> Unit = {},
    onConnectComputer: (String, String) -> Unit = { _, _ -> },
    onDisconnectComputer: () -> Unit = {},
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
            onScanQrCode = onScanQrCode,
            onDismissConnectDialog = onDismissConnectDialog,
            onConnectComputer = onConnectComputer,
            onDisconnectComputer = onDisconnectComputer,
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
