package com.dhwani.app.ui.call

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhwani.app.ui.CallState
import com.dhwani.app.ui.CallViewModel
import com.dhwani.app.ui.theme.AccessTimeIcon
import com.dhwani.app.ui.theme.ContactPhoneIcon

@Composable
fun MakeCallScreen(
    state: CallState,
    vm: CallViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enableSmartReplies by remember { mutableStateOf(true) }

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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E2F23)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Make a call",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E8540)
                    )
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Call Brief Section
                Column {
                    Text(
                        text = "Call brief (optional)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E2F23)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tell Dhwani what this call is about to get better suggestions.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF708A77)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = state.callGoal,
                        onValueChange = {
                            if (it.length <= 100) {
                                vm.onCallGoalChange(it)
                            }
                        },
                        placeholder = {
                            Text(
                                "E.g. Doctor appointment, College admission, Job interview...",
                                color = Color(0xFFA0B5A6),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF2E8540),
                            unfocusedBorderColor = Color(0xFFE2ECE4)
                        )
                    )

                    Text(
                        text = "${state.callGoal.length}/100",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF90A495),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp, end = 4.dp)
                    )
                }

                // 2. Phone Number Section
                Column {
                    Text(
                        text = "Phone number",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E2F23)
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2ECE4), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "+91",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E2F23)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF708A77),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        OutlinedTextField(
                            value = state.callPhoneNumber,
                            onValueChange = vm::onCallPhoneNumberChange,
                            placeholder = { Text("Enter phone number", color = Color(0xFFA0B5A6)) },
                            trailingIcon = {
                                ContactPhoneIcon(size = 22.dp, tint = Color(0xFF2E8540))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2E8540),
                                unfocusedBorderColor = Color(0xFFE2ECE4)
                            ),
                            singleLine = true
                        )
                    }
                }

                // 3. Enable Smart Replies Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable smart replies",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2F23)
                            )
                        )
                        Text(
                            text = "Get AI-suggested replies during the call",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = Color(0xFF708A77)
                            )
                        )
                    }

                    Switch(
                        checked = enableSmartReplies,
                        onCheckedChange = { enableSmartReplies = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E8540)
                        )
                    )
                }

                // 4. Open Dialer Button
                Button(
                    onClick = {
                        val num = state.callPhoneNumber.trim()
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E8540),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Open dialer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Recent calls Section
                Text(
                    text = "Recent calls",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2F23)
                    )
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFEEF3EE), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccessTimeIcon(size = 20.dp, tint = Color(0xFFA0B5A6))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No recent calls",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = Color(0xFF90A495)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
