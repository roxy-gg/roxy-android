package gg.roxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import gg.roxy.shared.businessLogic.RoxyAppViewModel
import gg.roxy.shared.data.PlayServicesRemoteQrScanner
import gg.roxy.shared.data.RemoteQrScanner
import gg.roxy.shared.styles.RoxyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RoxyAppViewModel by viewModels()
    private val qrScanner: RemoteQrScanner by lazy { PlayServicesRemoteQrScanner(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDeepLink(intent)

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
                    onAddNewComputer = viewModel::showConnectDialog,
                    onScanQrCode = ::startQrScanner,
                    onDismissConnectDialog = viewModel::dismissConnectDialog,
                    onConnectComputer = viewModel::connectRemote,
                    onDisconnectComputer = viewModel::disconnectRemote,
                    initialToken = viewModel.getInitialToken(),
                    initialPin = viewModel.getInitialPin(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        viewModel.onQrCodeScanned(uri.toString())
    }

    private fun startQrScanner() {
        qrScanner.startScan(
            onSuccess = { scannedContent ->
                viewModel.onQrCodeScanned(scannedContent)
            },
            onFailure = { error ->
                viewModel.onScanError(error.localizedMessage ?: "Failed to scan QR code")
            },
        )
    }
}
