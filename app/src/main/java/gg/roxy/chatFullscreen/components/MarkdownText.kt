package gg.roxy.chatFullscreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import gg.roxy.chatFullscreen.businessLogic.ChatPartUiModel
import gg.roxy.shared.styles.RoxyColors
import gg.roxy.shared.styles.RoxyMonoFontFamily
import gg.roxy.shared.styles.roxyColors

sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
}

private val NUMBERED_LIST_REGEX = Regex("""^(\d+)\.\s+(.*)""")
private val BULLET_LIST_REGEX = Regex("""^[-*]\s+(.*)""")

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()

    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Fenced Code Block
        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // Skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        val trimmed = line.trim()

        // 2. Empty line
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 3. Headings
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            if (level in 1..6 && trimmed.length > level && trimmed[level] == ' ') {
                val text = trimmed.substring(level + 1).trim()
                blocks.add(MarkdownBlock.Heading(level, text))
                i++
                continue
            }
        }

        // 4. Blockquote
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf(trimmed.removePrefix(">").trim())
            i++
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }

        // 5. Numbered list: e.g. "1. ", "12. "
        val numberedMatch = NUMBERED_LIST_REGEX.matchEntire(trimmed)
        if (numberedMatch != null) {
            val number = numberedMatch.groupValues[1]
            val itemText = numberedMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(number, itemText))
            i++
            continue
        }

        // 6. Bullet list: e.g. "- ", "* "
        val bulletMatch = BULLET_LIST_REGEX.matchEntire(trimmed)
        if (bulletMatch != null) {
            val itemText = bulletMatch.groupValues[1]
            blocks.add(MarkdownBlock.BulletItem(itemText))
            i++
            continue
        }

        // 7. Regular paragraph: accumulate lines until next block delimiter or empty line
        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val current = lines[i]
            val currentTrimmed = current.trim()
            if (currentTrimmed.isEmpty() ||
                currentTrimmed.startsWith("```") ||
                currentTrimmed.startsWith("#") ||
                currentTrimmed.startsWith(">") ||
                NUMBERED_LIST_REGEX.matches(currentTrimmed) ||
                BULLET_LIST_REGEX.matches(currentTrimmed)
            ) {
                break
            }
            paragraphLines.add(current)
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
        }
    }

    return blocks
}

// Regex matching inline formatting:
// Group 1-2: `code`
// Group 3-4: ***bold italic***
// Group 5-6: **bold**
// Group 7-8: __bold__
// Group 9-10: *italic*
// Group 11-12: _italic_
// Group 13-15: [link](url)
private val INLINE_MARKDOWN_REGEX = Regex(
    """(`([^`]+)`)""" +
    """|(\*\*\*([^*]+)\*\*\*)""" +
    """|(\*\*([^*]+)\*\*)""" +
    """|(__(?!_)([^_]+)__)""" +
    """|(\*([^*]+)\*)""" +
    """|(_([^_]+)_)""" +
    """|(\[([^\]]+)\]\(([^)]+)\))"""
)

fun buildMarkdownAnnotatedString(
    text: String,
    colors: RoxyColors,
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        for (match in INLINE_MARKDOWN_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                append(text.substring(cursor, start))
            }

            when {
                // Inline code `...`
                match.groups[2] != null -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = RoxyMonoFontFamily,
                            background = colors.surface2,
                            color = colors.accent,
                        )
                    ) {
                        append(" ${match.groups[2]!!.value} ")
                    }
                }
                // Bold-Italic ***...***
                match.groups[4] != null -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = colors.text,
                        )
                    ) {
                        append(match.groups[4]!!.value)
                    }
                }
                // Bold **...** or __...__
                match.groups[6] != null -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                        )
                    ) {
                        append(match.groups[6]!!.value)
                    }
                }
                match.groups[8] != null -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                        )
                    ) {
                        append(match.groups[8]!!.value)
                    }
                }
                // Italic *...* or _..._
                match.groups[10] != null -> {
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = colors.text,
                        )
                    ) {
                        append(match.groups[10]!!.value)
                    }
                }
                match.groups[12] != null -> {
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = colors.text,
                        )
                    ) {
                        append(match.groups[12]!!.value)
                    }
                }
                // Link [text](url)
                match.groups[14] != null -> {
                    withStyle(
                        SpanStyle(
                            color = colors.accent,
                            textDecoration = TextDecoration.Underline,
                        )
                    ) {
                        append(match.groups[14]!!.value)
                    }
                }
            }
            cursor = end
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text, colors) {
                        buildMarkdownAnnotatedString(block.text, colors)
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text,
                    )
                }
                is MarkdownBlock.Heading -> {
                    val annotated = remember(block.text, colors) {
                        buildMarkdownAnnotatedString(block.text, colors)
                    }
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = annotated,
                        style = style,
                        color = colors.text,
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        language = block.language,
                        code = block.code,
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.accent,
                            modifier = Modifier.padding(start = 2.dp, end = 8.dp),
                        )
                        val annotated = remember(block.text, colors) {
                            buildMarkdownAnnotatedString(block.text, colors)
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "${block.number}.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = RoxyMonoFontFamily,
                            ),
                            color = colors.textMuted,
                            modifier = Modifier.padding(start = 2.dp, end = 8.dp),
                        )
                        val annotated = remember(block.text, colors) {
                            buildMarkdownAnnotatedString(block.text, colors)
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(colors.accent, MaterialTheme.shapes.extraSmall)
                        )
                        Spacer(Modifier.width(10.dp))
                        val annotated = remember(block.text, colors) {
                            buildMarkdownAnnotatedString(block.text, colors)
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.edge),
    ) {
        Column {
            if (language.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.elevated)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = language,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = RoxyMonoFontFamily),
                        color = colors.textMuted,
                    )
                }
                HorizontalDivider(color = colors.edge)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = RoxyMonoFontFamily),
                    color = colors.text,
                )
            }
        }
    }
}

@Composable
fun ReasoningCard(
    reasoning: ChatPartUiModel.Reasoning,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(reasoning.isExpanded) }
    val colors = MaterialTheme.roxyColors

    Surface(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.edge),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = colors.accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Thinking Process",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textMuted,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (isExpanded) 90f else 0f),
                    tint = colors.textSubtle,
                )
            }
            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = colors.edge)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reasoning.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = colors.textMuted,
                )
            }
        }
    }
}
