package gg.roxy

import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoxyAppViewModelTest {
    @Test
    fun sessionSelectionAndBackNavigationUpdateTheRootState() {
        val viewModel = RoxyAppViewModel()

        viewModel.openSession("project-2-session-1")

        assertEquals(RoxyDestination.Chat, viewModel.uiState.value.destination)
        assertEquals("Project #2", viewModel.uiState.value.chat.projectName)
        assertEquals("Session #1", viewModel.uiState.value.chat.sessionTitle)
        assertTrue(
            viewModel.uiState.value.main.projects
                .flatMap { it.sessions }
                .first { it.id == "project-2-session-1" }
                .isActive,
        )
        assertFalse(
            viewModel.uiState.value.main.projects
                .flatMap { it.sessions }
                .first { it.id == "project-1-session-1" }
                .isActive,
        )

        viewModel.showMainScreen()

        assertEquals(RoxyDestination.Main, viewModel.uiState.value.destination)
    }

    @Test
    fun composerAndToolCallsAreControlledByTheViewModel() {
        val viewModel = RoxyAppViewModel()
        val toolCall = viewModel.uiState.value.chat.toolCalls.first()

        viewModel.updateComposer("Port the theme")
        assertEquals("Port the theme", viewModel.uiState.value.chat.composerText)

        viewModel.submitComposer()
        assertEquals("", viewModel.uiState.value.chat.composerText)

        assertFalse(toolCall.isExpanded)
        viewModel.toggleToolCall(toolCall.id)
        assertTrue(viewModel.toolCall(toolCall.id).isExpanded)
    }

    @Test
    fun computerSelectionClosesTheControlledMenu() {
        val viewModel = RoxyAppViewModel()
        val computerId = viewModel.uiState.value.main.selectedComputer.id

        viewModel.setComputerMenuExpanded(true)
        assertTrue(viewModel.uiState.value.main.isComputerMenuExpanded)

        viewModel.selectComputer(computerId)
        assertFalse(viewModel.uiState.value.main.isComputerMenuExpanded)
    }

    private fun RoxyAppViewModel.toolCall(id: String): ToolCallUiModel =
        uiState.value.chat.toolCalls.first { it.id == id }
}
