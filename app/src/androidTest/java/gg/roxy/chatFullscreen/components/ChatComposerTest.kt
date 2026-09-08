package gg.roxy.chatFullscreen.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import gg.roxy.shared.styles.RoxyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatComposerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun offlineComposerBlocksBothButtonAndKeyboardSubmission() {
        var sends = 0
        composeRule.setContent {
            RoxyTheme {
                ChatComposer("Saved draft", {}, { sends++ }, canSubmit = false)
            }
        }
        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Message Roxy").performImeAction()
        composeRule.onNodeWithContentDescription("Attach image or file").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, sends) }
    }

    @Test
    fun stopReplacesSendAndDisablesWhileWaitingForTheHost() {
        var stops = 0
        composeRule.setContent {
            var stopping by remember { mutableStateOf(false) }
            RoxyTheme {
                ChatComposer(
                    text = "Next draft",
                    onTextChange = {},
                    onSubmit = { error("A running turn must not submit another prompt") },
                    canSubmit = false,
                    showStop = true,
                    canStop = !stopping,
                    isStopping = stopping,
                    onStop = { stops++; stopping = true },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Send").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Stop").performClick()
        composeRule.onNodeWithContentDescription("Stopping").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(1, stops) }
    }
}
