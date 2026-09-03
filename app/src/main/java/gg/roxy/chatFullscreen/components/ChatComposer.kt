package gg.roxy.chatFullscreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.roxy.shared.styles.RoxyMonoFontFamily
import gg.roxy.shared.styles.roxyColors

enum class ModelProvider {
    Anthropic,
    Google,
}

data class RoxyModelItem(
    val id: String,
    val provider: ModelProvider,
    val section: String,
    val hasReasoning: Boolean = true,
    val hasTools: Boolean = true,
    val isPinned: Boolean = true,
)

val DesktopRoxyModels = listOf(
    // PINNED
    RoxyModelItem("claude-sonnet-4-6", ModelProvider.Anthropic, "PINNED"),
    RoxyModelItem("gemini-pro-agent", ModelProvider.Google, "PINNED"),
    RoxyModelItem("claude-opus-4-6-thinking", ModelProvider.Anthropic, "PINNED"),
    RoxyModelItem("claude-opus-5", ModelProvider.Anthropic, "PINNED"),
    RoxyModelItem("claude-sonnet-5", ModelProvider.Anthropic, "PINNED"),
    RoxyModelItem("gemini-3.1-pro-low", ModelProvider.Google, "PINNED"),
    RoxyModelItem("gemini-3.8-flash-high", ModelProvider.Google, "PINNED"),

    // LATEST - CLAUDE (SUBSCRIPTION)
    RoxyModelItem("claude-sonnet-4-6", ModelProvider.Anthropic, "LATEST - CLAUDE (SUBSCRIPTION)", isPinned = false),

    // CLAUDE (SUBSCRIPTION)
    RoxyModelItem("claude-opus-5", ModelProvider.Anthropic, "CLAUDE (SUBSCRIPTION)", isPinned = true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    initialModel: String = "gemini-3.8-flash-high",
) {
    val colors = MaterialTheme.roxyColors
    var currentModel by rememberSaveable { mutableStateOf(initialModel) }
    var showModelSheet by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface2,
        contentColor = colors.text,
        border = BorderStroke(1.dp, colors.edgeStrong),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, top = 11.dp, end = 12.dp, bottom = 10.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp, max = 120.dp)
                    .semantics { contentDescription = "Message Roxy" }
                    .padding(vertical = 4.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSubmit() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "Ask Roxy anything... (paste or drop images)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // Toolbar matching PC desktop layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Attach button (+)
                Surface(
                    onClick = { /* Skeleton: attach */ },
                    shape = CircleShape,
                    color = colors.elevated,
                    border = BorderStroke(1.dp, colors.edge),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Attach image or file",
                            modifier = Modifier.size(16.dp),
                            tint = colors.textMuted,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Model Selector Pill (Interactive)
                Surface(
                    onClick = { showModelSheet = true },
                    shape = CircleShape,
                    color = colors.elevated,
                    border = BorderStroke(1.dp, colors.edgeStrong),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = colors.accent,
                        )
                        Text(
                            text = currentModel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.text,
                        )
                        Icon(
                            imageVector = Icons.Rounded.UnfoldMore,
                            contentDescription = "Change model",
                            modifier = Modifier.size(14.dp),
                            tint = colors.textSubtle,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Send Button with clean default theme (White when active)
                val isSendActive = text.isNotBlank()
                Surface(
                    onClick = onSubmit,
                    enabled = isSendActive,
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSendActive) Color.White else colors.white.copy(alpha = 0.25f),
                    contentColor = Color.Black,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Send",
                            modifier = Modifier.size(18.dp),
                            tint = if (isSendActive) Color.Black else colors.textSubtle,
                        )
                    }
                }
            }
        }
    }

    if (showModelSheet) {
        ModelSelectorBottomSheet(
            selectedModel = currentModel,
            onModelSelected = { modelId ->
                currentModel = modelId
                showModelSheet = false
            },
            onDismiss = { showModelSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorBottomSheet(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.roxyColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredModels = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            DesktopRoxyModels
        } else {
            DesktopRoxyModels.filter { it.id.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    val groupedModels = remember(filteredModels) {
        filteredModels.groupBy { it.section }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131418),
        contentColor = Color(0xFFEEEEEE),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFF2E2F38), CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Search Bar matching desktop popup
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xFF1C1D23),
                border = BorderStroke(1.dp, Color(0xFF2B2C36)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF7A7C88),
                        modifier = Modifier.size(16.dp),
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE4E4E7), fontSize = 13.5.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search models...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = Color(0xFF6B6D7A),
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(18.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF8E90A0),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            // Models List grouped by section matching desktop
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                groupedModels.forEach { (section, models) ->
                    item(key = "header_$section") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 2.dp, start = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            when (section) {
                                "PINNED" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF888B98),
                                    )
                                }
                                "LATEST - CLAUDE (SUBSCRIPTION)" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF888B98),
                                    )
                                }
                                else -> {
                                    ClaudeAsteriskIcon(modifier = Modifier.size(12.dp), color = Color(0xFFD97706))
                                }
                            }
                            Text(
                                text = section,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RoxyMonoFontFamily,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                ),
                                color = Color(0xFF888B98),
                            )
                        }
                    }

                    items(models, key = { "${it.section}_${it.id}" }) { model ->
                        val isSelected = model.id == selectedModel
                        Surface(
                            onClick = { onModelSelected(model.id) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF1B2433) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Left checkmark
                                Box(
                                    modifier = Modifier.size(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF38BDF8),
                                        )
                                    }
                                }

                                Spacer(Modifier.width(6.dp))

                                // Vendor logo
                                if (model.provider == ModelProvider.Google) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Gemini",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF38BDF8),
                                    )
                                } else {
                                    ClaudeAsteriskIcon(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFFE06C43),
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                // Model name
                                Text(
                                    text = model.id,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = RoxyMonoFontFamily,
                                        fontSize = 12.5.sp,
                                    ),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                                    modifier = Modifier.weight(1f),
                                )

                                // Right capability badges (Reasoning, Tools, Pin)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    if (model.hasReasoning) {
                                        Icon(
                                            imageVector = Icons.Rounded.Psychology,
                                            contentDescription = "Reasoning",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF38BDF8),
                                        )
                                    }
                                    if (model.hasTools) {
                                        Icon(
                                            imageVector = Icons.Rounded.Build,
                                            contentDescription = "Tools",
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF4ADE80),
                                        )
                                    }
                                    if (model.isPinned) {
                                        Icon(
                                            imageVector = Icons.Rounded.PushPin,
                                            contentDescription = "Pinned",
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF38BDF8),
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
}

@Composable
fun ClaudeAsteriskIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE06C43),
) {
    Canvas(modifier = modifier.size(15.dp)) {
        val strokeWidth = 1.9.dp.toPx()
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        for (i in 0 until 8) {
            val angle = (i * 45f) * (Math.PI / 180f).toFloat()
            val start = Offset(
                center.x + (radius * 0.28f) * kotlin.math.cos(angle),
                center.y + (radius * 0.28f) * kotlin.math.sin(angle),
            )
            val end = Offset(
                center.x + radius * kotlin.math.cos(angle),
                center.y + radius * kotlin.math.sin(angle),
            )
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
