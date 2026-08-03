package com.mohnishraj.lunara.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohnishraj.lunara.ui.components.BrandWordmark
import com.mohnishraj.lunara.ui.components.LunaraBackdrop
import com.mohnishraj.lunara.ui.components.PrimaryAction
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Peach
import com.mohnishraj.lunara.ui.theme.Rose
import com.mohnishraj.lunara.ui.theme.Violet

private data class OnboardingPage(
    val title: String,
    val body: String,
    val accent: Color,
    val mode: Int,
)

private val pages = listOf(
    OnboardingPage(
        title = "Conversations that can do more",
        body = "Turn a thought into a task, poll, event or shared plan without leaving the conversation.",
        accent = Violet,
        mode = 0,
    ),
    OnboardingPage(
        title = "Everything important stays clear",
        body = "Keep people, ideas and decisions together in a space designed to stay calm as it grows.",
        accent = Mint,
        mode = 1,
    ),
    OnboardingPage(
        title = "Made for real connection",
        body = "A thoughtful place to talk, build and move forward together.",
        accent = Peach,
        mode = 2,
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]

    LunaraBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 28.dp, bottom = 26.dp),
        ) {
            BrandWordmark(compact = true)
            Spacer(Modifier.height(30.dp))

            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(180)) },
                label = "onboardingPage",
            ) { activePage ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OnboardingArtwork(activePage.mode, activePage.accent)
                    Spacer(Modifier.height(34.dp))
                    Text(
                        text = activePage.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = activePage.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (index == pageIndex) 28.dp else 8.dp, height = 8.dp)
                            .background(
                                color = if (index == pageIndex) page.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                shape = CircleShape,
                            )
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            PrimaryAction(
                text = if (pageIndex == pages.lastIndex) "Enter Lunara" else "Continue",
                onClick = {
                    if (pageIndex == pages.lastIndex) onFinished() else pageIndex++
                },
            )
        }
    }
}

@Composable
private fun OnboardingArtwork(mode: Int, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    )
                ),
                shape = RoundedCornerShape(34.dp),
            )
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = accent.copy(alpha = 0.10f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.5f, size.height * 0.48f),
            )
        }
        when (mode) {
            0 -> ModularConversationArt(accent)
            1 -> SpaceArt(accent)
            else -> ConnectionArt(accent)
        }
    }
}

@Composable
private fun ModularConversationArt(accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MiniBubble("What should we decide first?", Icons.Rounded.Forum, accent, end = false)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MiniCard("Plan", Icons.Rounded.TaskAlt, Mint, Modifier.weight(1f))
            MiniCard("Vote", Icons.Rounded.Poll, Peach, Modifier.weight(1f))
        }
        MiniBubble("Friday works for everyone", Icons.Rounded.CalendarMonth, Rose, end = true)
    }
}

@Composable
private fun SpaceArt(accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Weekend launch", style = MaterialTheme.typography.titleLarge)
        Text("12 people · 4 active plans", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniCard("Ideas", Icons.Rounded.Forum, accent, Modifier.weight(1f))
            MiniCard("Tasks", Icons.Rounded.CheckCircle, Mint, Modifier.weight(1f))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(accent.copy(alpha = 0.14f), CircleShape)
        ) {
            Box(Modifier.fillMaxWidth(0.68f).height(10.dp).background(accent, CircleShape))
        }
    }
}

@Composable
private fun ConnectionArt(accent: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        listOf(
            Triple(-82.dp, -40.dp, Violet),
            Triple(84.dp, -22.dp, Mint),
            Triple(-54.dp, 80.dp, Peach),
            Triple(68.dp, 82.dp, Rose),
        ).forEachIndexed { index, (x, y, color) ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = if (x.value > 0) x else 0.dp, end = if (x.value < 0) -x else 0.dp)
                    .padding(top = if (y.value > 0) y else 0.dp, bottom = if (y.value < 0) -y else 0.dp)
                    .size(58.dp)
                    .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f))), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(('A'.code + index).toChar().toString(), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
        Box(
            Modifier
                .size(100.dp)
                .background(Brush.radialGradient(listOf(accent, Color(0xFF5A48CB))), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Forum, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
private fun MiniBubble(text: String, icon: ImageVector, color: Color, end: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (end) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(34.dp).background(color.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MiniCard(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
