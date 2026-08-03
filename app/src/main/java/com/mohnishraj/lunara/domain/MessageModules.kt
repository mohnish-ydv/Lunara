package com.mohnishraj.lunara.domain

enum class MessageModuleType(val wireName: String, val displayName: String) {
    Task("task", "Task"),
    Checklist("checklist", "Checklist"),
    Poll("poll", "Poll"),
    Event("event", "Event"),
    Reminder("reminder", "Reminder"),
    Note("note", "Note"),
    Countdown("countdown", "Countdown"),
    Code("code", "Code"),
    Location("location", "Location"),
    Contact("contact", "Contact");

    companion object {
        fun fromWire(value: String): MessageModuleType? = entries.firstOrNull { it.wireName == value }
    }
}

data class ModuleChecklistItem(
    val id: String,
    val text: String,
    val completed: Boolean = false,
)

data class ModulePollOption(
    val id: String,
    val text: String,
    val voterIds: List<String> = emptyList(),
) {
    fun selectedBy(userId: String): Boolean = userId in voterIds
}

data class MessageModule(
    val type: MessageModuleType,
    val title: String,
    val description: String = "",
    val completed: Boolean = false,
    val dueAt: String = "",
    val items: List<ModuleChecklistItem> = emptyList(),
    val options: List<ModulePollOption> = emptyList(),
    val eventAt: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rsvps: Map<String, String> = emptyMap(),
    val code: String = "",
    val language: String = "",
    val contactName: String = "",
    val contactValue: String = "",
) {
    val cleanTitle: String get() = title.trim().take(120)

    fun previewText(): String = when (type) {
        MessageModuleType.Task -> "Task · $cleanTitle"
        MessageModuleType.Checklist -> "Checklist · $cleanTitle"
        MessageModuleType.Poll -> "Poll · $cleanTitle"
        MessageModuleType.Event -> "Event · $cleanTitle"
        MessageModuleType.Reminder -> "Reminder · $cleanTitle"
        MessageModuleType.Note -> "Note · $cleanTitle"
        MessageModuleType.Countdown -> "Countdown · $cleanTitle"
        MessageModuleType.Code -> "Code · $cleanTitle"
        MessageModuleType.Location -> "Location · $cleanTitle"
        MessageModuleType.Contact -> "Contact · $cleanTitle"
    }

    fun validate(): Result<MessageModule> = runCatching {
        require(cleanTitle.isNotBlank()) { "Add a title first" }
        require(description.length <= 1200) { "Description can be up to 1200 characters" }
        require(items.size <= 20) { "A checklist can contain up to 20 items" }
        require(options.size in 0..10) { "A poll can contain up to 10 options" }
        require(code.length <= 8000) { "Code can be up to 8000 characters" }
        require(contactValue.length <= 180) { "Contact detail is too long" }
        if (type == MessageModuleType.Checklist) {
            require(items.size >= 2) { "Add at least two checklist items" }
            require(items.all { it.text.trim().isNotBlank() }) { "Checklist items cannot be empty" }
        }
        if (type == MessageModuleType.Poll) {
            require(options.size >= 2) { "Add at least two poll options" }
            require(options.all { it.text.trim().isNotBlank() }) { "Poll options cannot be empty" }
        }
        require(items.map(ModuleChecklistItem::id).filter(String::isNotBlank).distinct().size == items.size) { "Checklist item IDs must be unique" }
        require(options.map(ModulePollOption::id).filter(String::isNotBlank).distinct().size == options.size) { "Poll option IDs must be unique" }
        if (type == MessageModuleType.Event) require(eventAt.trim().isNotBlank()) { "Add the event time" }
        if (type == MessageModuleType.Reminder || type == MessageModuleType.Countdown) {
            require(dueAt.trim().isNotBlank()) { "Add a target time" }
        }
        if (type == MessageModuleType.Code) require(code.trim().isNotBlank()) { "Add a code snippet" }
        if (type == MessageModuleType.Location) require(locationName.trim().isNotBlank()) { "Add a place name" }
        require((latitude == null) == (longitude == null)) { "Add both latitude and longitude" }
        latitude?.let { require(it in -90.0..90.0) { "Latitude must be between -90 and 90" } }
        longitude?.let { require(it in -180.0..180.0) { "Longitude must be between -180 and 180" } }
        if (type == MessageModuleType.Contact) {
            require(contactName.trim().isNotBlank() && contactValue.trim().isNotBlank()) { "Add a name and contact detail" }
        }
        copy(
            title = cleanTitle,
            description = description.trim(),
            items = items.map { it.copy(text = it.text.trim()) },
            options = options.map { it.copy(text = it.text.trim(), voterIds = it.voterIds.distinct()) },
            code = code.trimEnd(),
            language = language.trim().take(32),
            contactName = contactName.trim().take(80),
            contactValue = contactValue.trim().take(180),
            locationName = locationName.trim().take(160),
            rsvps = rsvps.filterValues { it in setOf("going", "maybe", "not_going") },
        )
    }

    fun toggleTask(): MessageModule = copy(completed = !completed)

    fun toggleChecklistItem(itemId: String): MessageModule = copy(
        items = items.map { item -> if (item.id == itemId) item.copy(completed = !item.completed) else item }
    )

    fun vote(optionId: String, userId: String): MessageModule = copy(
        options = options.map { option ->
            val withoutUser = option.voterIds.filterNot { it == userId }
            if (option.id == optionId) option.copy(voterIds = withoutUser + userId)
            else option.copy(voterIds = withoutUser)
        }
    )

    fun rsvp(userId: String, status: String): MessageModule = copy(
        rsvps = if (status.isBlank()) rsvps - userId else rsvps + (userId to status)
    )
}
