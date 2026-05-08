package com.vectorminds.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vectorminds.app.ui.theme.Vm

// ─────────────────────────────────────────────────────────────────────────────
// VmMarkdown — lightweight, dependency-free Compose markdown renderer.
//
// Goals:
//   • Render the actual content shapes the backend (Gemini) generates:
//       headings (## / ###), **bold**, *italics*, `inline code`, ```fenced
//       code blocks```, bullet lists, numbered lists, > quotes, --- dividers.
//   • Never emit raw markdown sigils ("**Bold**", "## Title") to the user.
//   • Cheap to compose — block list is parsed once via remember(text) and the
//     render is plain Compose primitives (Column/Row/Text), no canvas or
//     custom layout passes, so big briefs scroll smoothly inside LazyColumn.
//   • Long lines wrap; horizontal overflow only inside code blocks (which
//     get their own h-scroll so they never break the surrounding card).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VmMarkdown(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val vm = Vm.colors

    Column(modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) {
                val gap = when (block) {
                    is MdHeading -> 14.dp
                    is MdBulletList, is MdNumberedList -> 8.dp
                    is MdCodeBlock -> 10.dp
                    is MdDivider -> 12.dp
                    else -> 8.dp
                }
                Spacer(Modifier.height(gap))
            }
            when (block) {
                is MdHeading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }.copy(fontWeight = FontWeight.SemiBold)
                    Text(
                        parseInline(block.text),
                        style = style,
                        color = vm.text,
                    )
                }
                is MdParagraph -> Text(
                    parseInline(block.text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = vm.text,
                )
                is MdBulletList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .padding(top = 8.dp)
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(vm.brand),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                parseInline(item),
                                style = MaterialTheme.typography.bodyMedium,
                                color = vm.text,
                            )
                        }
                    }
                }
                is MdNumberedList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { (n, item) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "$n.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = vm.brand,
                                modifier = Modifier.widthIn(min = 22.dp),
                            )
                            Text(
                                parseInline(item),
                                style = MaterialTheme.typography.bodyMedium,
                                color = vm.text,
                            )
                        }
                    }
                }
                is MdCodeBlock -> Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(vm.surfaceSunken)
                        .border(1.dp, vm.border, RoundedCornerShape(10.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        block.code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = vm.text,
                    )
                }
                is MdQuote -> Row(
                    Modifier
                        .heightIn(min = 24.dp)
                        .fillMaxWidth(),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .heightIn(min = 24.dp)
                            .background(vm.brand),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        parseInline(block.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = vm.textMuted,
                    )
                }
                MdDivider -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(vm.border),
                )
            }
        }
    }
}

// ─── Block model ─────────────────────────────────────────────────────────────

private sealed interface MdBlock
private data class MdHeading(val level: Int, val text: String) : MdBlock
private data class MdParagraph(val text: String) : MdBlock
private data class MdBulletList(val items: List<String>) : MdBlock
private data class MdNumberedList(val items: List<Pair<Int, String>>) : MdBlock
private data class MdCodeBlock(val code: String) : MdBlock
private data class MdQuote(val text: String) : MdBlock
private data object MdDivider : MdBlock

// ─── Block parser ────────────────────────────────────────────────────────────

private val HeadingRegex   = Regex("^(#{1,6})\\s+(.*)$")
private val DividerRegex   = Regex("^\\s*([-*_])\\1{2,}\\s*$")
private val BulletRegex    = Regex("^\\s*[-*+]\\s+(.+)$")
private val NumberedRegex  = Regex("^\\s*(\\d+)\\.\\s+(.+)$")

private fun parseMarkdown(input: String): List<MdBlock> {
    val cleaned = input.replace("\r\n", "\n").replace("\r", "\n")
    val lines = cleaned.split("\n")
    val out = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()
    val bullets = mutableListOf<String>()
    val numbers = mutableListOf<Pair<Int, String>>()
    val codeBuf = StringBuilder()
    var inCode = false

    fun flushPara() {
        if (paragraph.isNotEmpty()) {
            out.add(MdParagraph(paragraph.toString().trim()))
            paragraph.clear()
        }
    }
    fun flushBullets() {
        if (bullets.isNotEmpty()) {
            out.add(MdBulletList(bullets.toList()))
            bullets.clear()
        }
    }
    fun flushNumbers() {
        if (numbers.isNotEmpty()) {
            out.add(MdNumberedList(numbers.toList()))
            numbers.clear()
        }
    }
    fun flushAll() { flushPara(); flushBullets(); flushNumbers() }

    for (raw in lines) {
        val line = raw
        if (inCode) {
            if (line.trimStart().startsWith("```")) {
                out.add(MdCodeBlock(codeBuf.toString().trimEnd('\n')))
                codeBuf.clear()
                inCode = false
            } else {
                if (codeBuf.isNotEmpty()) codeBuf.append('\n')
                codeBuf.append(line)
            }
            continue
        }

        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") -> {
                flushAll(); inCode = true
            }
            DividerRegex.matches(trimmed) -> {
                flushAll(); out.add(MdDivider)
            }
            HeadingRegex.matches(trimmed) -> {
                flushAll()
                val m = HeadingRegex.matchEntire(trimmed)!!
                out.add(MdHeading(m.groupValues[1].length, m.groupValues[2].trim()))
            }
            trimmed.startsWith("> ") -> {
                flushAll(); out.add(MdQuote(trimmed.removePrefix("> ")))
            }
            BulletRegex.matches(line) -> {
                flushPara(); flushNumbers()
                bullets.add(BulletRegex.matchEntire(line)!!.groupValues[1])
            }
            NumberedRegex.matches(line) -> {
                flushPara(); flushBullets()
                val m = NumberedRegex.matchEntire(line)!!
                numbers.add(m.groupValues[1].toInt() to m.groupValues[2])
            }
            line.isBlank() -> flushAll()
            else -> {
                flushBullets(); flushNumbers()
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(line.trim())
            }
        }
    }
    flushAll()
    if (codeBuf.isNotEmpty()) out.add(MdCodeBlock(codeBuf.toString().trimEnd('\n')))
    return out
}

// ─── Inline parser ───────────────────────────────────────────────────────────

private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = text.length
    while (i < n) {
        val c = text[i]
        when {
            c == '*' && i + 1 < n && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end == -1) {
                    append(text.substring(i)); i = n
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                }
            }
            c == '*' || c == '_' -> {
                val end = text.indexOf(c, i + 1)
                if (end == -1 || end == i + 1) {
                    append(c); i++
                } else {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                }
            }
            c == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end == -1) {
                    append(c); i++
                } else {
                    withStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                }
            }
            c == '[' -> {
                val close = text.indexOf(']', i + 1)
                val open = if (close != -1 && close + 1 < n && text[close + 1] == '(') close + 1 else -1
                val urlEnd = if (open != -1) text.indexOf(')', open + 1) else -1
                if (close != -1 && open != -1 && urlEnd != -1) {
                    val label = text.substring(i + 1, close)
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Medium,
                            color = androidx.compose.ui.graphics.Color(0xFF7EA6FF),
                        ),
                    ) { append(label) }
                    i = urlEnd + 1
                } else {
                    append(c); i++
                }
            }
            else -> {
                append(c); i++
            }
        }
    }
}
