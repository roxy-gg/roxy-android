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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import gg.roxy.R
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                SettingsSkeletonView(
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
                    // 1. Header (Brand + Status badge + Settings)
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
                                        text = "Pick up a session from any connected computer.",
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

private data class SettingsItemUi(val title: String, val subtitle: String)

@Composable
private fun SettingsSkeletonView(
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
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "App preferences and configurations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
            }
        }

        item {
            ActivityHeatmapCard(modifier = Modifier.fillMaxWidth())
        }

        item {
            SettingsCategorySection(
                title = "General",
                items = listOf(
                    SettingsItemUi("Language", "Español / English"),
                ),
            )
        }

        item {
            SettingsCategorySection(
                title = "Roxy AI Engine",
                items = listOf(
                    SettingsItemUi("Default Model", "gemini-3.8-flash-high"),
                    SettingsItemUi("Reasoning Effort", "High"),
                    SettingsItemUi("Max Context", "128k tokens"),
                ),
            )
        }

        item {
            SettingsCategorySection(
                title = "Connection",
                items = listOf(
                    SettingsItemUi("Remote Workspaces", "1 connected"),
                    SettingsItemUi("Bridge Port", "5555"),
                ),
            )
        }

        item {
            SettingsCategorySection(
                title = "About",
                items = listOf(
                    SettingsItemUi("Roxy Android", "v0.1.0 • Technical Preview"),
                    SettingsItemUi("Desktop Sync", "Connected to Computer #1"),
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
                            .clickable {}
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.text,
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

@Composable
fun ActivityHeatmapCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.roxyColors
    val scrollState = rememberScrollState(initial = Int.MAX_VALUE)

    Column(modifier = modifier) {
        Text(
            text = "ACTIVITY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RoxyMonoFontFamily,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.textMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.edge),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .fillMaxWidth(),
            ) {
                // Day labels pinned on the left
                Column(
                    modifier = Modifier.padding(top = 22.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    val days = listOf("", "Mon", "", "Wed", "", "Fri", "")
                    days.forEach { day ->
                        Box(
                            modifier = Modifier.size(width = 24.dp, height = 10.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (day.isNotEmpty()) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = RoxyMonoFontFamily,
                                        fontSize = 9.sp,
                                    ),
                                    color = colors.textSubtle,
                                )
                            }
                        }
                    }
                }

                // Horizontally scrollable heatmap grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                ) {
                    // Months header
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(23.dp),
                    ) {
                        val months = listOf(
                            "sep", "oct", "nov", "dic", "ene", "feb",
                            "mar", "abr", "may", "jun", "jul", "ago"
                        )
                        months.forEach { month ->
                            Text(
                                text = month,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RoxyMonoFontFamily,
                                    fontSize = 9.5.sp,
                                ),
                                color = colors.textSubtle,
                            )
                        }
                    }

                    // 7 rows of activity cells
                    val numCols = 46
                    val activeCells = remember { generateActivityGrid(numCols) }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (row in 0 until 7) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                for (col in 0 until numCols) {
                                    val level = activeCells[col][row]
                                    val cellColor = when (level) {
                                        4 -> Color(0xFF67E8F9) // Glowing electric cyan
                                        3 -> Color(0xFF38BDF8) // Bright cyan
                                        2 -> Color(0xFF0284C7) // Mid cyan
                                        1 -> Color(0xFF0369A1) // Dark cyan
                                        else -> Color(0xFF18181D) // Empty charcoal
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(cellColor, RoundedCornerShape(2.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateActivityGrid(numCols: Int): List<List<Int>> {
    val grid = MutableList(numCols) { MutableList(7) { 0 } }

    if (numCols > 38) {
        // Under 'jul' (week 38):
        grid[38][2] = 2; grid[38][3] = 3; grid[38][4] = 2; grid[38][6] = 3
    }

    if (numCols > 45) {
        // Under 'ago' (weeks 41..45):
        grid[41][0] = 3; grid[41][1] = 2; grid[41][2] = 4; grid[41][3] = 2; grid[41][4] = 3; grid[41][5] = 3; grid[41][6] = 2
        grid[42][0] = 2; grid[42][1] = 2; grid[42][2] = 3; grid[42][3] = 4; grid[42][4] = 2; grid[42][5] = 3
        grid[43][0] = 4; grid[43][1] = 2; grid[43][2] = 3; grid[43][3] = 2; grid[43][4] = 3; grid[43][5] = 2; grid[43][6] = 2
        grid[44][1] = 2; grid[44][2] = 3; grid[44][3] = 2; grid[44][4] = 2; grid[44][6] = 3
        grid[45][0] = 3; grid[45][1] = 4; grid[45][2] = 0; grid[45][3] = 3; grid[45][4] = 2; grid[45][6] = 3
    }

    return grid
}

private val MainPreviewState = MainFullScreenUiState(
    selectedComputer = ComputerUiModel("computer-1", "Computer #1", "Connected", true),
    computers = listOf(ComputerUiModel("computer-1", "Computer #1", "Connected", true)),
    projects = listOf(
        ProjectUiModel(
            id = "project-1",
            name = "Project #1",
            sessions = listOf(
                SessionUiModel("session-1", "Session #1", "Building the Android client", "Now", true),
                SessionUiModel("session-2", "Session #2", "Desktop theme parity", "18m"),
                SessionUiModel("session-3", "Session #3", "Compose architecture", "Yesterday"),
            ),
        ),
        ProjectUiModel(
            id = "project-2",
            name = "Project #2",
            sessions = listOf(
                SessionUiModel("session-4", "Session #1", "Remote workspace", "Mon"),
            ),
        ),
    ),
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
