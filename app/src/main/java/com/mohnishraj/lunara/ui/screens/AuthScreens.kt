package com.mohnishraj.lunara.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohnishraj.lunara.domain.Validators
import com.mohnishraj.lunara.ui.AppUiState
import com.mohnishraj.lunara.ui.components.BrandMark
import com.mohnishraj.lunara.ui.components.BrandWordmark
import com.mohnishraj.lunara.ui.components.LunaraBackdrop
import com.mohnishraj.lunara.ui.components.LunaraTextField
import com.mohnishraj.lunara.ui.components.MessageBanner
import com.mohnishraj.lunara.ui.components.PrimaryAction
import com.mohnishraj.lunara.ui.components.SecondaryAction
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Peach
import com.mohnishraj.lunara.ui.theme.Violet

@Composable
fun AuthWelcomeScreen(
    cloudReady: Boolean,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    LunaraBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            BrandWordmark(compact = true)
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Violet.copy(alpha = 0.24f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                Mint.copy(alpha = 0.10f),
                            )
                        ),
                        RoundedCornerShape(34.dp),
                    )
                    .padding(26.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (cloudReady) Icons.Rounded.CloudDone else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (cloudReady) Mint else Peach,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (cloudReady) "Ready when you are" else "Explore every screen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Column {
                        BrandMark()
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = "Talk less around the work. Do more inside the conversation.",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "A calmer place for conversations, plans and shared momentum.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WelcomeTag("Chat", Violet)
                        WelcomeTag("Plan", Mint)
                        WelcomeTag("Build", Peach)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            PrimaryAction(text = "Create an account", onClick = onCreateAccount)
            Spacer(Modifier.height(10.dp))
            SecondaryAction(text = "I already have an account", onClick = onSignIn)
        }
    }
}

@Composable
private fun WelcomeTag(label: String, color: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun SignInScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onCreateAccount: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    AuthFormScreen(
        title = "Welcome back",
        subtitle = "Pick up where your conversations left off.",
        actionLabel = "Sign in",
        footerLead = "New to Lunara?",
        footerAction = "Create an account",
        state = state,
        onBack = onBack,
        onSubmit = onSubmit,
        onFooterAction = onCreateAccount,
        onDismissMessage = onDismissMessage,
    )
}

@Composable
fun SignUpScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onSignIn: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    AuthFormScreen(
        title = "Create your place",
        subtitle = "Start with the essentials. You can shape the rest as you go.",
        actionLabel = "Continue",
        footerLead = "Already joined?",
        footerAction = "Sign in",
        state = state,
        onBack = onBack,
        onSubmit = onSubmit,
        onFooterAction = onSignIn,
        onDismissMessage = onDismissMessage,
    )
}

@Composable
private fun AuthFormScreen(
    title: String,
    subtitle: String,
    actionLabel: String,
    footerLead: String,
    footerAction: String,
    state: AppUiState,
    onBack: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onFooterAction: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val emailError = if (attempted) Validators.emailError(email) else null
    val passwordError = if (attempted) Validators.passwordError(password) else null

    LunaraBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                BrandMark(compact = true)
            }
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(26.dp))
            MessageBanner(state.message, onDismissMessage)
            if (state.message != null) Spacer(Modifier.height(14.dp))
            LunaraTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                error = emailError,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(14.dp))
            LunaraTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                error = passwordError,
                keyboardType = KeyboardType.Password,
                password = true,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(
                    text = "Your sign-in details stay protected on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
            PrimaryAction(
                text = actionLabel,
                loading = state.isLoading,
                onClick = {
                    attempted = true
                    if (Validators.emailError(email) == null && Validators.passwordError(password) == null) {
                        onSubmit(email, password)
                    }
                },
            )
            Spacer(Modifier.height(22.dp))
            Row(modifier = Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(footerLead, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    footerAction,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable(onClick = onFooterAction),
                )
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
fun ConfirmEmailScreen(email: String, onContinue: () -> Unit) {
    LunaraBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Mint.copy(alpha = 0.14f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Email, contentDescription = null, tint = Mint, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Check your inbox", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "We sent a confirmation link to $email. Open it, then return here to sign in.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Violet)
                Text("The link may take a moment to arrive.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            PrimaryAction(text = "Return to sign in", onClick = onContinue, showArrow = false)
        }
    }
}
