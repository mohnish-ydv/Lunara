package com.mohnishraj.lunara.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohnishraj.lunara.domain.MessageModule
import com.mohnishraj.lunara.domain.MessageModuleType
import com.mohnishraj.lunara.domain.ModuleChecklistItem
import com.mohnishraj.lunara.domain.ModulePollOption
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Peach
import com.mohnishraj.lunara.ui.theme.Rose
import com.mohnishraj.lunara.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Composable
internal fun MessageModuleCard(
    module: MessageModule,
    currentUserId: String,
    enabled: Boolean,
    onUpdate: (MessageModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = moduleAccent(module.type)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    )
                )
            )
            .animateContentSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(moduleIcon(module.type), contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(module.type.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                Text(module.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (module.description.isNotBlank()) {
            Text(module.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (module.type) {
            MessageModuleType.Task -> TaskBody(module, enabled, onUpdate)
            MessageModuleType.Checklist -> ChecklistBody(module, enabled, onUpdate)
            MessageModuleType.Poll -> PollBody(module, currentUserId, enabled, onUpdate)
            MessageModuleType.Event -> EventBody(module, currentUserId, enabled, onUpdate)
            MessageModuleType.Reminder -> DatePill("Remind", module.dueAt.ifBlank { "No reminder time" }, Icons.Rounded.Alarm, Peach)
            MessageModuleType.Note -> Unit
            MessageModuleType.Countdown -> CountdownBody(module.dueAt)
            MessageModuleType.Code -> CodeBody(module)
            MessageModuleType.Location -> LocationBody(module)
            MessageModuleType.Contact -> ContactBody(module)
        }
    }
}

@Composable
private fun TaskBody(module: MessageModule, enabled: Boolean, onUpdate: (MessageModule) -> Unit) {
    if (module.dueAt.isNotBlank()) DatePill("Due", module.dueAt, Icons.Rounded.Schedule, Peach)
    Surface(
        onClick = { if (enabled) onUpdate(module.toggleTask()) },
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (module.completed) Mint.copy(alpha = 0.18f) else Violet.copy(alpha = 0.13f),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (module.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (module.completed) Mint else Violet,
            )
            Spacer(Modifier.width(9.dp))
            Text(if (module.completed) "Completed" else "Mark complete", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ChecklistBody(module: MessageModule, enabled: Boolean, onUpdate: (MessageModule) -> Unit) {
    val completed = module.items.count(ModuleChecklistItem::completed)
    Text("$completed of ${module.items.size} complete", style = MaterialTheme.typography.labelMedium, color = Mint)
    module.items.forEach { item ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable(enabled = enabled) { onUpdate(module.toggleChecklistItem(item.id)) }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)).padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (item.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (item.completed) Mint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(item.text, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PollBody(module: MessageModule, currentUserId: String, enabled: Boolean, onUpdate: (MessageModule) -> Unit) {
    val total = module.options.sumOf { it.voterIds.size }.coerceAtLeast(1)
    module.options.forEach { option ->
        val selected = option.selectedBy(currentUserId)
        val ratio = option.voterIds.size.toFloat() / total
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(enabled = enabled) { onUpdate(module.vote(option.id, currentUserId)) }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)),
        ) {
            Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).height(48.dp).background(Violet.copy(alpha = if (selected) 0.28f else 0.14f))) { }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = if (selected) Mint else Violet, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(option.text, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text(option.voterIds.size.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EventBody(module: MessageModule, currentUserId: String, enabled: Boolean, onUpdate: (MessageModule) -> Unit) {
    if (module.eventAt.isNotBlank()) DatePill("When", module.eventAt, Icons.Rounded.CalendarMonth, Violet)
    if (module.locationName.isNotBlank()) DatePill("Where", module.locationName, Icons.Rounded.LocationOn, Mint)
    val selected = module.rsvps[currentUserId].orEmpty()
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("going" to "Going", "maybe" to "Maybe", "not_going" to "Can't go").forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                enabled = enabled,
                onClick = { onUpdate(module.rsvp(currentUserId, if (selected == value) "" else value)) },
                label = { Text(label) },
            )
        }
    }
    if (module.rsvps.isNotEmpty()) Text("${module.rsvps.size} responses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CountdownBody(targetText: String) {
    val target = remember(targetText) { runCatching { Instant.parse(targetText) }.getOrNull() }
    var now by remember(target) { mutableStateOf(Instant.now()) }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        while (isActive) {
            now = Instant.now()
            if (!now.isBefore(target)) break
            delay(1_000)
        }
    }
    val label = target?.let {
        val duration = Duration.between(now, it)
        val totalSeconds = duration.seconds.coerceAtLeast(0L)
        val days = totalSeconds / 86_400L
        val hours = (totalSeconds % 86_400L) / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        when {
            duration.isNegative || duration.isZero -> "Countdown ended"
            days > 0 -> "${days}d ${hours}h remaining"
            hours > 0 -> "${hours}h ${minutes}m remaining"
            else -> "${minutes}m ${seconds}s remaining"
        }
    } ?: targetText.ifBlank { "Set a target time" }
    DatePill("Countdown", label, Icons.Rounded.Schedule, Rose)
}

@Composable
private fun CodeBody(module: MessageModule) {
    if (module.language.isNotBlank()) {
        Text(module.language.uppercase(), style = MaterialTheme.typography.labelSmall, color = Mint, fontWeight = FontWeight.Bold)
    }
    Text(
        module.code,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.Black.copy(alpha = 0.24f)).padding(12.dp),
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 14,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LocationBody(module: MessageModule) {
    DatePill("Place", module.locationName.ifBlank { module.title }, Icons.Rounded.LocationOn, Mint)
    if (module.latitude != null && module.longitude != null) {
        Text("${"%.5f".format(module.latitude)}, ${"%.5f".format(module.longitude)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ContactBody(module: MessageModule) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)).padding(12.dp)) {
        Text(module.contactName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(module.contactValue, style = MaterialTheme.typography.bodyMedium, color = Mint)
    }
}

@Composable
private fun DatePill(label: String, value: String, icon: ImageVector, color: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(color.copy(alpha = 0.11f)).padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModuleComposerSheet(
    onDismiss: () -> Unit,
    onSend: (MessageModule) -> Unit,
) {
    var type by remember { mutableStateOf(MessageModuleType.Task) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf("") }
    var eventAt by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("kotlin") }
    var code by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactValue by remember { mutableStateOf("") }
    val checklist = remember { mutableStateListOf("", "", "") }
    val poll = remember { mutableStateListOf("", "", "") }
    var validation by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create something live", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Send a card that can stay useful after the conversation moves on.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageModuleType.entries.forEach { item ->
                    FilterChip(
                        selected = type == item,
                        onClick = { type = item; validation = null },
                        label = { Text(item.displayName) },
                        leadingIcon = { Icon(moduleIcon(item), contentDescription = null, modifier = Modifier.size(17.dp)) },
                    )
                }
            }
            Text("Selected · ${type.displayName}", style = MaterialTheme.typography.labelLarge, color = moduleAccent(type))
            OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth(), label = { Text(moduleTitleHint(type)) }, singleLine = true)
            OutlinedTextField(description, { description = it.take(1200) }, Modifier.fillMaxWidth(), label = { Text("Description (optional)") }, minLines = 2, maxLines = 4)

            when (type) {
                MessageModuleType.Task, MessageModuleType.Reminder, MessageModuleType.Countdown ->
                    OutlinedTextField(dueAt, { dueAt = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Time · ISO timestamp or readable note") }, singleLine = true)
                MessageModuleType.Checklist -> checklist.forEachIndexed { index, value ->
                    OutlinedTextField(value, { checklist[index] = it.take(120) }, Modifier.fillMaxWidth(), label = { Text("Item ${index + 1}") }, singleLine = true)
                }
                MessageModuleType.Poll -> poll.forEachIndexed { index, value ->
                    OutlinedTextField(value, { poll[index] = it.take(120) }, Modifier.fillMaxWidth(), label = { Text("Option ${index + 1}") }, singleLine = true)
                }
                MessageModuleType.Event -> {
                    OutlinedTextField(eventAt, { eventAt = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("When") }, singleLine = true)
                    OutlinedTextField(location, { location = it.take(160) }, Modifier.fillMaxWidth(), label = { Text("Where") }, singleLine = true)
                }
                MessageModuleType.Code -> {
                    OutlinedTextField(language, { language = it.take(32) }, Modifier.fillMaxWidth(), label = { Text("Language") }, singleLine = true)
                    OutlinedTextField(code, { code = it.take(8000) }, Modifier.fillMaxWidth(), label = { Text("Code") }, minLines = 5, maxLines = 10, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                }
                MessageModuleType.Location -> {
                    OutlinedTextField(location, { location = it.take(160) }, Modifier.fillMaxWidth(), label = { Text("Place name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(latitude, { latitude = it.take(20) }, Modifier.weight(1f), label = { Text("Latitude") }, singleLine = true)
                        OutlinedTextField(longitude, { longitude = it.take(20) }, Modifier.weight(1f), label = { Text("Longitude") }, singleLine = true)
                    }
                }
                MessageModuleType.Contact -> {
                    OutlinedTextField(contactName, { contactName = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Contact name") }, singleLine = true)
                    OutlinedTextField(contactValue, { contactValue = it.take(180) }, Modifier.fillMaxWidth(), label = { Text("Phone, email or handle") }, singleLine = true)
                }
                MessageModuleType.Note -> Unit
            }

            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = {
                    val module = MessageModule(
                        type = type,
                        title = title,
                        description = description,
                        dueAt = dueAt,
                        items = checklist.mapIndexedNotNull { index, text -> text.trim().takeIf(String::isNotBlank)?.let { ModuleChecklistItem("item-${index + 1}-${UUID.randomUUID()}", it) } },
                        options = poll.mapIndexedNotNull { index, text -> text.trim().takeIf(String::isNotBlank)?.let { ModulePollOption("option-${index + 1}-${UUID.randomUUID()}", it) } },
                        eventAt = eventAt,
                        locationName = location,
                        latitude = latitude.toDoubleOrNull(),
                        longitude = longitude.toDoubleOrNull(),
                        code = code,
                        language = language,
                        contactName = contactName,
                        contactValue = contactValue,
                    )
                    module.validate().onSuccess(onSend).onFailure { validation = it.message ?: "Complete the required fields" }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Send ${type.displayName.lowercase()}")
            }
        }
    }
}

private fun moduleAccent(type: MessageModuleType): Color = when (type) {
    MessageModuleType.Task, MessageModuleType.Checklist -> Mint
    MessageModuleType.Poll -> Violet
    MessageModuleType.Event, MessageModuleType.Countdown -> Rose
    MessageModuleType.Reminder -> Peach
    MessageModuleType.Note -> Color(0xFFFFD166)
    MessageModuleType.Code -> Color(0xFF78C6FF)
    MessageModuleType.Location -> Color(0xFF6EE7B7)
    MessageModuleType.Contact -> Color(0xFFC4A7FF)
}

private fun moduleIcon(type: MessageModuleType): ImageVector = when (type) {
    MessageModuleType.Task -> Icons.Rounded.TaskAlt
    MessageModuleType.Checklist -> Icons.Rounded.Checklist
    MessageModuleType.Poll -> Icons.Rounded.HowToVote
    MessageModuleType.Event -> Icons.Rounded.EventAvailable
    MessageModuleType.Reminder -> Icons.Rounded.Alarm
    MessageModuleType.Note -> Icons.Rounded.NoteAlt
    MessageModuleType.Countdown -> Icons.Rounded.Schedule
    MessageModuleType.Code -> Icons.Rounded.Code
    MessageModuleType.Location -> Icons.Rounded.LocationOn
    MessageModuleType.Contact -> Icons.Rounded.ContactPage
}

private fun moduleTitleHint(type: MessageModuleType): String = when (type) {
    MessageModuleType.Task -> "What needs to be done?"
    MessageModuleType.Checklist -> "Checklist title"
    MessageModuleType.Poll -> "Ask a question"
    MessageModuleType.Event -> "Event name"
    MessageModuleType.Reminder -> "What should everyone remember?"
    MessageModuleType.Note -> "Note title"
    MessageModuleType.Countdown -> "What are you counting down to?"
    MessageModuleType.Code -> "Snippet title"
    MessageModuleType.Location -> "Location label"
    MessageModuleType.Contact -> "Contact card title"
}
