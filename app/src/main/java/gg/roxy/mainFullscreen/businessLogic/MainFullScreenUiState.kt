package gg.roxy.mainFullscreen.businessLogic

import androidx.compose.runtime.Immutable

@Immutable
data class ComputerUiModel(
    val id: String,
    val name: String,
    val status: String,
    val isConnected: Boolean,
)

@Immutable
data class SessionUiModel(
    val id: String,
    val title: String,
    val summary: String,
    val updatedAt: String,
    val isActive: Boolean = false,
)

@Immutable
data class ProjectUiModel(
    val id: String,
    val name: String,
    val sessions: List<SessionUiModel>,
)

@Immutable
data class MainFullScreenUiState(
    val selectedComputer: ComputerUiModel,
    val computers: List<ComputerUiModel>,
    val projects: List<ProjectUiModel>,
    val isComputerMenuExpanded: Boolean = false,
    val isConnectingDialogVisible: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val prefilledToken: String = "",
    val prefilledPin: String = "",
    val qrFeedbackMessage: String? = null,
)
