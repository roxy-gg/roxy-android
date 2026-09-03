package gg.roxy.mainFullScreen.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.roxy.R
import gg.roxy.mainFullScreen.businessLogic.ComputerUiModel
import gg.roxy.mainFullScreen.businessLogic.MainFullScreenUiState
import gg.roxy.mainFullScreen.businessLogic.ProjectUiModel
import gg.roxy.mainFullScreen.businessLogic.SessionUiModel
import gg.roxy.shared.styles.RoxyMonoFontFamily
import gg.roxy.shared.styles.RoxyTheme
import gg.roxy.shared.styles.roxyColors

enum class ProjectSortOrder(val label: String) {
    Recent("Recent activity"),
    Alphabetical("Name (A-Z)"),
    Sessions("Most sessions"),
}

@Composable
fun MainFullScreen(
    uiState: MainFullScreenUiState,
    onComputerMenuExpandedChange: (Boolean) -> Unit,
    onComputerSelected: (String) -> Unit,
    onSessionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddNewComputer: () -> Unit = {},
    onScanQrCode: () -> Unit = {},
    onDismissConnectDialog: () -> Unit = {},
    onConnectComputer: (String, String) -> Unit = { _, _ -> },
    onDisconnectComputer: () -> Unit = {},
    initialToken: String = "",
    initialPin: String = "",
) {
    val colors = MaterialTheme.roxyColors
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf(ProjectSortOrder.Recent) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    val sortedProjects = remember(uiState.projects, sortOrder) {
        when (sortOrder) {
            ProjectSortOrder.Recent -> uiState.projects
            ProjectSortOrder.Alphabetical -> uiState.projects.sortedBy { it.name.lowercase() }
            ProjectSortOrder.Sessions -> uiState.projects.sortedByDescending { it.sessions.size }
        }
    }

    if (uiState.isConnectingDialogVisible) {
        ConnectComputerDialog(
            isConnecting = uiState.isConnecting,
            errorMessage = uiState.connectionError,
            onDismiss = onDismissConnectDialog,
            onConnect = onConnectComputer,
            onScanQrCode = onScanQrCode,
            qrFeedbackMessage = uiState.qrFeedbackMessage,
            initialTokenOrUrl = if (uiState.prefilledToken.isNotBlank()) uiState.prefilledToken else initialToken,
            initialPin = if (uiState.prefilledPin.isNotBlank()) uiState.prefilledPin else initialPin,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = isSettingsOpen,
            label = "settingsTransition",
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { showSettings ->
            if (showSettings) {
                SettingsView(
                    selectedComputer = uiState.selectedComputer,
                    onConnectClick = {
                        isSettingsOpen = false
                        onAddNewComputer()
                    },
                    onScanQrClick = {
                        isSettingsOpen = false
                        onScanQrCode()
                    },
                    onDisconnectClick = {
                        onDisconnectComputer()
                    },
                    onBackClick = { isSettingsOpen = false },
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Header (Brand + Settings)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(13.dp),
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.roxy_avatar),
                                    contentDescription = "Roxy Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .border(1.dp, colors.edgeStrong, RoundedCornerShape(13.dp)),
                                )
                                Column {
                                    Text(
                                        text = "Roxy",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = colors.text,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Remote Workspace",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                onClick = { isSettingsOpen = true },
                                shape = CircleShape,
                                color = colors.surface2,
                                border = BorderStroke(1.dp, colors.edgeStrong),
                                modifier = Modifier.size(42.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        modifier = Modifier.size(20.dp),
                                        tint = colors.textMuted,
                                    )
                                }
                            }
                        }
                    }

                    // 2. Connected Computer / Workspace Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CONNECTED COMPUTER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RoxyMonoFontFamily,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = colors.textSubtle,
                                modifier = Modifier.padding(start = 2.dp),
                            )
                            ComputerSelector(
                                selectedComputer = uiState.selectedComputer,
                                computers = uiState.computers,
                                expanded = uiState.isComputerMenuExpanded,
                                onExpandedChange = onComputerMenuExpandedChange,
                                onComputerSelected = onComputerSelected,
                                onAddNewComputer = onAddNewComputer,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // 3. Projects Header with Sort button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "PROJECTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RoxyMonoFontFamily,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = colors.textSubtle,
                            )

                            Box {
                                Surface(
                                    onClick = { isSortMenuExpanded = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSortMenuExpanded) colors.surface2 else Color.Transparent,
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.FilterList,
                                            contentDescription = "Sort projects",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (sortOrder != ProjectSortOrder.Recent) colors.text else colors.textSubtle,
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = isSortMenuExpanded,
                                    onDismissRequest = { isSortMenuExpanded = false },
                                    modifier = Modifier
                                        .background(colors.elevated)
                                        .border(1.dp, colors.edgeStrong, RoundedCornerShape(12.dp)),
                                ) {
                                    ProjectSortOrder.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (sortOrder == option) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (sortOrder == option) colors.text else colors.textMuted,
                                                )
                                            },
                                            trailingIcon = {
                                                if (sortOrder == option) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = colors.text,
                                                    )
                                                }
                                            },
                                            onClick = {
                                                sortOrder = option
                                                isSortMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (sortedProjects.isEmpty()) {
                        item {
                            EmptyProjectsState(
                                isConnected = uiState.selectedComputer.isConnected,
                                onConnectClick = onAddNewComputer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                            )
                        }
                    } else {
                        sortedProjects.forEach { project ->
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
        }
    }
}

@Composable
private fun EmptyProjectsState(
    isConnected: Boolean,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.edge),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = colors.elevated,
                border = BorderStroke(1.dp, colors.edgeStrong),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isConnected) Icons.Rounded.Folder else Icons.Rounded.Computer,
                        contentDescription = null,
                        tint = if (isConnected) colors.textMuted else colors.accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isConnected) "No active sessions" else "No computer connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    text = if (isConnected) {
                        "Open a project or run a session in Roxy Desktop to see it here."
                    } else {
                        "Connect your PC to sync projects and control your sessions remotely."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            if (!isConnected) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.bg,
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Connect PC",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private data class SettingsItemUi(
    val title: String,
    val subtitle: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit = {},
)

@Composable
private fun SettingsView(
    selectedComputer: ComputerUiModel,
    onConnectClick: () -> Unit,
    onScanQrClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors
    BackHandler(onBack = onBackClick)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to main",
                        tint = colors.textMuted,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.text,
                )
            }
        }

        item {
            val connectionItems = buildList {
                add(
                    SettingsItemUi(
                        title = "Scan Desktop QR",
                        subtitle = "Pair quickly by scanning PC screen",
                        onClick = onScanQrClick,
                    )
                )
                add(
                    SettingsItemUi(
                        title = "Remote Workspace",
                        subtitle = if (selectedComputer.isConnected) "Connected: ${selectedComputer.name}" else "Disconnected (Tap to enter PIN/link)",
                        onClick = onConnectClick,
                    )
                )
                add(
                    SettingsItemUi(
                        title = "Relay Service",
                        subtitle = "roxy.gg/api/remote",
                    )
                )
                if (selectedComputer.isConnected) {
                    add(
                        SettingsItemUi(
                            title = "Disconnect PC",
                            subtitle = "Disconnect from ${selectedComputer.name}",
                            isDestructive = true,
                            onClick = onDisconnectClick,
                        )
                    )
                }
            }

            SettingsCategorySection(
                title = "Connection",
                items = connectionItems,
            )
        }

        item {
            SettingsCategorySection(
                title = "About",
                items = listOf(
                    SettingsItemUi("Roxy Android", "v0.1.0"),
                    SettingsItemUi(
                        title = "Desktop Sync",
                        subtitle = if (selectedComputer.isConnected) "${selectedComputer.name} • Connected" else "Not paired",
                    ),
                ),
            )
        }
    }
}

@Composable
private fun SettingsCategorySection(
    title: String,
    items: List<SettingsItemUi>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.edge),
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (item.isDestructive) colors.accent else colors.text,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }
                    }
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = colors.border,
                        )
                    }
                }
            }
        }
    }
}

private val MainPreviewState = MainFullScreenUiState(
    selectedComputer = ComputerUiModel("pc-remote", "Desktop PC", "Connected", true),
    computers = listOf(ComputerUiModel("pc-remote", "Desktop PC", "Connected", true)),
    projects = emptyList(),
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
