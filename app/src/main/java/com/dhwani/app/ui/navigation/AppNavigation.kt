package com.dhwani.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhwani.app.ui.theme.CallNavIcon
import com.dhwani.app.ui.theme.HomeNavIcon
import com.dhwani.app.ui.theme.LiveNavIcon
import com.dhwani.app.ui.theme.YouNavIcon

import com.dhwani.app.ui.onboarding.HandBubbleIcon

enum class DhwaniTab(val label: String) {
    HOME("Home"),
    LIVE("Live"),
    SIGN("Sign"),
    CALL("Call"),
    YOU("You")
}

@Composable
fun DhwaniBottomBar(
    currentTab: DhwaniTab,
    onTabSelected: (DhwaniTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DhwaniTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab
                    val activeColor = Color(0xFF2E8540)
                    val inactiveColor = Color(0xFF8E9B90)
                    val tint = if (isSelected) activeColor else inactiveColor

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            )
                            .padding(vertical = 6.dp)
                    ) {
                        when (tab) {
                            DhwaniTab.HOME -> HomeNavIcon(size = 24.dp, tint = tint)
                            DhwaniTab.LIVE -> LiveNavIcon(size = 24.dp, tint = tint)
                            DhwaniTab.SIGN -> HandBubbleIcon(color = tint)
                            DhwaniTab.CALL -> CallNavIcon(size = 24.dp, tint = tint)
                            DhwaniTab.YOU -> YouNavIcon(size = 24.dp, tint = tint)
                        }

                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tint
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
