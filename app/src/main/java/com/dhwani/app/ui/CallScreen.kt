package com.dhwani.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhwani.app.data.UserContext

@Composable
fun CallScreen(vm: CallViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dhwani",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.modelStatus,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (state.isRunning) {
                    OutlinedButton(onClick = vm::stopCallPipe) {
                        Text("Stop")
                    }
                } else {
                    Button(onClick = vm::startCallPipe) {
                        Text("Start")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.userContext.isConfigured) {
                        "Context: ${state.userContext.name}"
                    } else {
                        "Context not set"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = vm::toggleContextEditor) {
                    Text(if (state.isContextEditorOpen || !state.userContext.isConfigured) "Hide" else "Edit")
                }
            }

            if (state.isContextEditorOpen || !state.userContext.isConfigured) {
                ContextSetupCard(
                    context = state.userContext,
                    message = state.contextMessage,
                    onChange = vm::onContextChange,
                    onSave = vm::saveContext,
                )
                Spacer(Modifier.height(12.dp))
            }

            BriefingCard(
                goal = state.callGoal,
                briefing = state.briefing,
                isLoading = state.isBriefingLoading,
                onGoalChange = vm::onCallGoalChange,
                onGenerate = vm::generateBriefing,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = state.liveCaption,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                reverseLayout = true,
            ) {
                items(state.transcript.asReversed(), key = { it.id }) { line ->
                    Text(
                        text = if (line.isFinal) line.text else "${line.text}...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.suggestions.isNotEmpty() || state.isSuggesting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.isSuggesting) {
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                    state.suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { vm.speakSuggestion(suggestion) },
                            label = { Text(suggestion) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.draftReply,
                    onValueChange = vm::onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type to speak") },
                    singleLine = false,
                    maxLines = 3,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = vm::sendReply) {
                    Text("Speak")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = vm::testGemma,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Test Gemma")
            }
        }
    }
}

@Composable
private fun ContextSetupCard(
    context: UserContext,
    message: String,
    onChange: (UserContext) -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Personal context", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = context.name,
                onValueChange = { onChange(context.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
            )
            OutlinedTextField(
                value = context.preferredLanguage,
                onValueChange = { onChange(context.copy(preferredLanguage = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Preferred language") },
                singleLine = true,
            )
            OutlinedTextField(
                value = context.voiceFriendlyAddress,
                onValueChange = { onChange(context.copy(voiceFriendlyAddress = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Voice-friendly address") },
                maxLines = 2,
            )
            OutlinedTextField(
                value = context.importantPeople,
                onValueChange = { onChange(context.copy(importantPeople = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Important people") },
                maxLines = 2,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onSave) {
                    Text("Save")
                }
                Spacer(Modifier.width(12.dp))
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BriefingCard(
    goal: String,
    briefing: String,
    isLoading: Boolean,
    onGoalChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Pre-call briefing", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = onGoalChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Call goal") },
                    maxLines = 2,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = onGenerate, enabled = !isLoading) {
                    Text("Brief")
                }
            }
            if (briefing.isNotBlank()) {
                Text(
                    text = briefing,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.heightIn(max = 180.dp),
                )
            }
        }
    }
}
