package gg.roxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import gg.roxy.shared.styles.RoxyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RoxyAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            RoxyTheme {
                RoxyApp(
                    uiState = uiState,
                    onComputerMenuExpandedChange = viewModel::setComputerMenuExpanded,
                    onComputerSelected = viewModel::selectComputer,
                    onSessionSelected = viewModel::openSession,
                    onBackFromChat = viewModel::showMainScreen,
                    onComposerChange = viewModel::updateComposer,
                    onComposerSubmit = viewModel::submitComposer,
                    onToolCallClick = viewModel::toggleToolCall,
                )
            }
        }
    }
}
