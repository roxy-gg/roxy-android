package gg.roxy.shared.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import gg.roxy.shared.styles.RoxyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PinInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rejectedInputDoesNotRequireAnExtraBackspace() {
        var currentPin = ""
        composeRule.setContent {
            RoxyTheme(darkTheme = true) {
                var pin by remember { mutableStateOf("") }
                currentPin = pin
                PinInput(
                    value = pin,
                    onValueChange = { pin = it },
                )
            }
        }

        val input = composeRule.onNode(hasSetTextAction())
        input.performTextInput("abc1234567")
        composeRule.runOnIdle {
            assertEquals("123456", currentPin)
        }

        input.performKeyInput { pressKey(Key.Backspace) }
        composeRule.runOnIdle {
            assertEquals("12345", currentPin)
        }
    }
}
