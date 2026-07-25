package com.dhwani.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhwani.app.ui.navigation.DhwaniTab
import com.dhwani.app.ui.onboarding.HandBubbleIcon
import com.dhwani.app.ui.theme.AIBrainIcon
import com.dhwani.app.ui.theme.BellNotificationIcon
import com.dhwani.app.ui.theme.CallNavIcon
import com.dhwani.app.ui.theme.LiveNavIcon
import com.dhwani.app.ui.theme.RecordVoiceOverIcon
import com.dhwani.app.ui.theme.YouNavIcon

@Composable
fun HomeScreen(
    onNavigateTab: (DhwaniTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF9FBF9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4F3E8)),
                    contentAlignment = Alignment.Center
                ) {
                    YouNavIcon(size = 20.dp, tint = Color(0xFF2E8540))
                }

                Text(
                    text = "Dhwani",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E8540)
                    )
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F4F1)),
                    contentAlignment = Alignment.Center
                ) {
                    BellNotificationIcon(size = 20.dp, tint = Color(0xFF1E2F23))
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F8F4)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E2F23)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Dhwani is here to help you communicate with confidence.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF55665A)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2F3E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👋", fontSize = 28.sp)
                        }
                    }
                }

                // Feature Navigation Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeFeatureCard(
                        iconBg = Color(0xFFE1F5FE),
                        icon = { HandBubbleIcon(color = Color(0xFF0288D1)) },
                        title = "Sign Language (ISL)",
                        subtitle = "Show video camera to interpret sign gestures",
                        onClick = { onNavigateTab(DhwaniTab.SIGN) }
                    )

                    HomeFeatureCard(
                        iconBg = Color(0xFFE4F3E8),
                        icon = { LiveNavIcon(size = 22.dp, tint = Color(0xFF2E8540)) },
                        title = "Live call",
                        subtitle = "Real-time captions during phone calls",
                        onClick = { onNavigateTab(DhwaniTab.LIVE) }
                    )

                    HomeFeatureCard(
                        iconBg = Color(0xFFE3F2FD),
                        icon = { CallNavIcon(size = 22.dp, tint = Color(0xFF0288D1)) },
                        title = "Make a call",
                        subtitle = "Prepare before calling someone",
                        onClick = { onNavigateTab(DhwaniTab.CALL) }
                    )

                    HomeFeatureCard(
                        iconBg = Color(0xFFF3E5F5),
                        icon = { YouNavIcon(size = 22.dp, tint = Color(0xFF7B1FA2)) },
                        title = "You",
                        subtitle = "Your details, language and preferences",
                        onClick = { onNavigateTab(DhwaniTab.YOU) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // On-device features Grid
                Text(
                    text = "On-device features",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2F23)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OnDeviceFeatureBadge(
                        modifier = Modifier.weight(1f),
                        bg = Color(0xFFE8F5E9),
                        icon = { LiveNavIcon(size = 20.dp, tint = Color(0xFF2E8540)) },
                        label = "Offline STT\n(Vosk)"
                    )

                    OnDeviceFeatureBadge(
                        modifier = Modifier.weight(1f),
                        bg = Color(0xFFFFFDE7),
                        icon = { RecordVoiceOverIcon(size = 20.dp, tint = Color(0xFFF57F17)) },
                        label = "TTS\nEngine"
                    )

                    OnDeviceFeatureBadge(
                        modifier = Modifier.weight(1f),
                        bg = Color(0xFFE1F5FE),
                        icon = { HandBubbleIcon(color = Color(0xFF0288D1)) },
                        label = "ISL\nRecognition",
                        onClick = { onNavigateTab(DhwaniTab.SIGN) }
                    )

                    OnDeviceFeatureBadge(
                        modifier = Modifier.weight(1f),
                        bg = Color(0xFFF3E5F5),
                        icon = { AIBrainIcon(size = 20.dp, tint = Color(0xFF7B1FA2)) },
                        label = "AI\nAssistant"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(
    iconBg: Color,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFEEF3EE), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2F23)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF708A77)
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = Color(0xFFB0C4B5),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun OnDeviceFeatureBadge(
    modifier: Modifier = Modifier,
    bg: Color,
    icon: @Composable () -> Unit,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334437),
                lineHeight = 14.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
