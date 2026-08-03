package com.mohnishraj.lunara.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohnishraj.lunara.domain.Validators
import com.mohnishraj.lunara.ui.AppUiState
import com.mohnishraj.lunara.ui.components.AvatarOrb
import com.mohnishraj.lunara.ui.components.BrandWordmark
import com.mohnishraj.lunara.ui.components.LunaraBackdrop
import com.mohnishraj.lunara.ui.components.LunaraTextField
import com.mohnishraj.lunara.ui.components.MessageBanner
import com.mohnishraj.lunara.ui.components.PrimaryAction

@Composable
fun ProfileSetupScreen(
    state: AppUiState,
    onSubmit: (String, String, String, Int) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarSeed by remember { mutableIntStateOf(0) }
    var attempted by remember { mutableStateOf(false) }

    val displayNameError = if (attempted) Validators.displayNameError(displayName) else null
    val usernameError = if (attempted) Validators.usernameError(username) else null

    LunaraBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            BrandWordmark(compact = true)
            Spacer(Modifier.height(30.dp))
            Text("Make it yours", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(
                "Choose how people will see you across Lunara.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            MessageBanner(state.message, onDismissMessage)
            if (state.message != null) Spacer(Modifier.height(18.dp))

            Text("Choose a look", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(6) { seed ->
                    AvatarOrb(
                        seed = seed,
                        label = displayName.ifBlank { "L" },
                        selected = avatarSeed == seed,
                        onClick = { avatarSeed = seed },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            LunaraTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display name",
                error = displayNameError,
            )
            Spacer(Modifier.height(14.dp))
            LunaraTextField(
                value = username,
                onValueChange = { username = Validators.normalizeUsername(it) },
                label = "Username",
                error = usernameError,
                leading = "@",
            )
            Spacer(Modifier.height(14.dp))
            LunaraTextField(
                value = bio,
                onValueChange = { if (it.length <= 120) bio = it },
                label = "A short intro",
                singleLine = false,
                maxLines = 3,
            )
            Text(
                text = "${bio.length}/120",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(top = 5.dp, end = 4.dp),
            )
            Spacer(Modifier.height(28.dp))
            PrimaryAction(
                text = "Finish setup",
                loading = state.isLoading,
                onClick = {
                    attempted = true
                    if (
                        Validators.displayNameError(displayName) == null &&
                        Validators.usernameError(username) == null
                    ) {
                        onSubmit(
                            displayName.trim(),
                            Validators.normalizeUsername(username),
                            bio.trim(),
                            avatarSeed,
                        )
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
