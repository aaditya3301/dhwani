# DHWANI — Phase 2: Intelligence

> Phase 1 gave us a transparent pipe between the deaf user and the caller. Phase 2 makes Gemma 4 *useful*. Personal context, native function calling, streaming generation, smart replies during incoming calls, and a pre-call briefing for outgoing calls.

---

## 1. Goal of this phase

At the end of Phase 2:

1. **Incoming calls** — as the caller speaks, Gemma 4 generates 3 contextually-aware smart-reply chips on screen. The user can tap one to instantly speak it, or edit it first, or ignore and type freely.
2. **Outgoing calls** — before dialling, the user types or speaks the *goal* of the call ("ask my doctor's clinic for an appointment next Tuesday"). Gemma 4 prepares a briefing: predicted questions, required information, suggested opening line. During the call, this briefing is one tap away.
3. **Personal context** — addresses, family contacts, doctor, bank account hints, current medications, calendar, recent orders. All stored on-device. Gemma 4 fetches it via native function calls when the conversation needs it.
4. **Streaming** — tokens stream onto screen as they're generated. No more 3-second waits.

The conversation now flows. The user is no longer typing every word — they're choosing from suggestions Gemma 4 builds from context.

---

## 2. Architecture for Phase 2

```
                   ┌────────────────────────────────────────┐
                   │     Personal Context Store (Room DB)   │
                   │  - addresses, contacts, doctors        │
                   │  - calendar, recent orders             │
                   │  - call history + summaries            │
                   └────────────────┬───────────────────────┘
                                    │
                                    │ Function calls
                                    ▼
   ┌─────────────────┐    ┌──────────────────────┐    ┌──────────────────┐
   │  Caller's voice │───▶│   Live transcript    │───▶│  Gemma 4 E4B     │
   │  (Phase 1)      │    │   (rolling buffer)   │    │  (streaming +    │
   └─────────────────┘    └──────────────────────┘    │  function call)  │
                                    │                  └────────┬─────────┘
                                    │                           │
                                    ▼                           ▼
                          ┌──────────────────────┐    ┌──────────────────┐
                          │   Smart reply chips  │    │  Tool dispatcher │
                          │   (3 suggestions)    │    │  (Kotlin)        │
                          └──────────┬───────────┘    └──────────────────┘
                                     │
                                     ▼
                              [user taps a chip]
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  TTS into call (P1)  │
                          └──────────────────────┘
```

---

## 3. Personal context store — schema and seeding

Use Room. Single database. All entities encrypted at rest via SQLCipher (`net.zetetic:android-database-sqlcipher`).

### Entities

```kotlin
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,         // UUID
    val name: String,                   // "Dr. Mehta"
    val role: String,                   // "Doctor" / "Family" / "Bank" / "Driver" / "Friend"
    val phoneNumbers: List<String>,
    val notes: String,                  // free text — "my cardiologist, Apollo Indiranagar"
    val lastCallSummary: String? = null
)

@Entity(tableName = "addresses")
data class Address(
    @PrimaryKey val id: String,
    val label: String,                  // "Home" / "Office" / "Mom's place"
    val fullAddress: String,            // "123 MG Road, Bangalore 560001"
    val landmark: String? = null,       // "near the temple"
    val voiceFriendly: String           // pre-rendered: "near the Ganesh temple on MG Road"
)

@Entity(tableName = "medical")
data class MedicalContext(
    @PrimaryKey val id: String,
    val medications: List<String>,      // ["Telmisartan 40mg", "Metformin 500mg"]
    val allergies: List<String>,
    val conditions: List<String>,
    val emergencyContact: String        // contactId
)

@Entity(tableName = "payment_hints")
data class PaymentHint(
    @PrimaryKey val id: String,
    val label: String,                  // "Default UPI" / "Cash preferred"
    val safeToShare: String             // "ravi@upi" — NEVER full account numbers
)

@Entity(tableName = "call_log")
data class CallLogEntry(
    @PrimaryKey val id: String,
    val contactId: String?,
    val phoneNumber: String,
    val timestamp: Long,
    val direction: String,              // "incoming" / "outgoing"
    val durationSec: Int,
    val transcriptPath: String,         // file path under filesDir
    val summary: String,                // Gemma-generated, 1-2 lines
    val outcomes: List<String>          // ["confirmed appointment Tuesday 10am"]
)
```

### Seeding flow

On first launch after Phase 1, the user goes through a 5-screen onboarding:

1. **You** — name, preferred language
2. **Home** — full address + voice-friendly version (Gemma generates the voice-friendly one)
3. **People** — phone contacts the user wants Dhwani to know. They tap from the phone's contact list, label as Family/Doctor/Friend.
4. **Health** — optional. Meds, allergies, emergency contact.
5. **Money** — optional. UPI ID for shareable payment info. No bank accounts, no card numbers — ever.

Onboarding writes to the Room DB. Done.

---

## 4. The Gemma 4 prompt structure for live calls

We use the **MediaPipe LLM Inference session** (`LlmInferenceSession`) which holds KV-cache across turns — critical for E4B's 128K context window to stay responsive.

System prompt template (loaded once per call session):

```
You are Dhwani, a phone-call assistant for a deaf person. You help them participate in
voice calls by suggesting things they could say and pulling personal information when
the conversation calls for it.

The user is Ravi. They speak Hindi (Devanagari) and English. They are calling/being
called by a person whose role is: {{COUNTERPARTY_ROLE}}.

Your job during this call:
1. Listen to the live transcript of what the caller is saying.
2. Suggest 3 short, natural replies the user could say. Each suggestion ≤ 12 words.
3. When the conversation needs personal information (address, appointment, medication,
   etc.), call the appropriate function to fetch it.
4. Match the user's language preference. If the caller speaks Hindi, your suggestions
   should also be in Hindi (Devanagari script).

Tools available:
- get_address(label: "home"|"office"|"other")
- get_contact_info(name_or_role: string)
- get_medical_info(field: "medications"|"allergies"|"conditions")
- get_payment_hint(label: string)
- check_calendar(date_range: string)
- get_recent_call_summary(contact_name: string)

Output format for suggestions:
<suggestions>
1. [suggestion text]
2. [suggestion text]
3. [suggestion text]
</suggestions>

If you call a tool, wait for its result before generating suggestions.
```

Per-turn user prompt:

```
Conversation so far:
{{ROLLING_TRANSCRIPT}}

Latest caller utterance:
"{{LATEST_UTTERANCE}}"

Generate 3 suggested replies for the user.
```

The rolling transcript is a sliding window of the last ~3000 tokens of the call. Anything older gets summarized by Gemma in the background and prepended as `[Earlier in call: ...]`.

---

## 5. Native function calling — implementation

Gemma 4 supports native function calling via the MediaPipe LLM API. The model emits a tool call block; we parse, dispatch, return the result back into the session.

### Tool definitions

**`Tools.kt`**:

```kotlin
package com.dhwani.app.llm

import com.dhwani.app.data.AppDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ToolCall(val name: String, val args: Map<String, String>)

@Serializable
data class ToolResult(val name: String, val result: String)

class ToolDispatcher(private val db: AppDatabase) {

    fun spec(): String = """
        [
          {"name":"get_address","args":{"label":"string"}},
          {"name":"get_contact_info","args":{"name_or_role":"string"}},
          {"name":"get_medical_info","args":{"field":"string"}},
          {"name":"get_payment_hint","args":{"label":"string"}},
          {"name":"check_calendar","args":{"date_range":"string"}},
          {"name":"get_recent_call_summary","args":{"contact_name":"string"}}
        ]
    """.trimIndent()

    suspend fun dispatch(call: ToolCall): ToolResult {
        val result = when (call.name) {
            "get_address" -> {
                val label = call.args["label"] ?: "home"
                db.addressDao().findByLabel(label)?.voiceFriendly ?: "unknown"
            }
            "get_contact_info" -> {
                val q = call.args["name_or_role"] ?: ""
                db.contactDao().search(q).firstOrNull()?.let {
                    "${it.name} (${it.role}) ${it.phoneNumbers.firstOrNull() ?: ""} — ${it.notes}"
                } ?: "no match"
            }
            "get_medical_info" -> {
                val field = call.args["field"] ?: ""
                val m = db.medicalDao().get() ?: return ToolResult(call.name, "none")
                when (field) {
                    "medications" -> m.medications.joinToString(", ")
                    "allergies" -> m.allergies.joinToString(", ")
                    "conditions" -> m.conditions.joinToString(", ")
                    else -> "unknown field"
                }
            }
            "get_payment_hint" -> {
                val label = call.args["label"] ?: "default"
                db.paymentDao().findByLabel(label)?.safeToShare ?: "none"
            }
            "check_calendar" -> "calendar integration TBD"
            "get_recent_call_summary" -> {
                val name = call.args["contact_name"] ?: ""
                db.callLogDao().mostRecentByContactName(name)?.summary ?: "none"
            }
            else -> "unknown tool"
        }
        return ToolResult(call.name, result)
    }
}
```

### Parsing tool calls from Gemma 4 output

Gemma 4's instruction-tuned variant emits tool calls in the canonical Gemma function-calling format. As of the official Gemma 4 docs:

```
<tool_call>
{"name":"get_address","args":{"label":"home"}}
</tool_call>
```

Parser:

```kotlin
private val TOOL_CALL_RE = Regex("<tool_call>\\s*([\\s\\S]+?)\\s*</tool_call>")

fun extractToolCalls(text: String): List<ToolCall> {
    return TOOL_CALL_RE.findAll(text).map { match ->
        Json.decodeFromString<ToolCall>(match.groupValues[1])
    }.toList()
}
```

When detected, pause user-facing streaming, dispatch tools, inject results back as a `<tool_result>` chunk into the session, resume generation.

---

## 6. Streaming generation — `GemmaEngine` upgrade

Replace Phase 1's blocking `generate()` with streaming:

```kotlin
fun generateStream(prompt: String): Flow<String> = callbackFlow {
    val s = session ?: error("Gemma not initialized")
    s.addQueryChunk(prompt)
    s.generateResponseAsync { partial: String, done: Boolean ->
        trySend(partial)
        if (done) close()
    }
    awaitClose { /* nothing — MediaPipe handles cancellation */ }
}
```

Wire this to a `Flow<String>` in `CallViewModel` that emits each token. The smart-reply chips appear as they're generated — visible progress feels fast even if the full 3 suggestions take 1.5s.

---

## 7. Incoming-call flow — step by step

```
1. Phone state listener detects incoming/answered call
2. CallService starts (Phase 1 plumbing already wired)
3. Phone number → lookup in contacts → counterparty role inferred
4. New LlmInferenceSession created with system prompt + role-substituted
5. Vosk transcribes caller audio in real time → text chunks
6. Every 1.5s OR on detected speaker pause:
   a. Take rolling transcript + latest utterance → prompt Gemma 4
   b. Stream the response; parse tool calls as they appear
   c. Dispatch tools, inject results
   d. Extract <suggestions> block, render as 3 chips
7. User taps a chip → TTS speaks it → text logged as user turn
8. User edits + sends → same flow
9. User ignores → next caller utterance triggers a fresh suggestion round
10. Call ends → Gemma generates a 1-2 line summary, stored in CallLogEntry
```

---

## 8. Outgoing-call briefing flow

Before dialling:

1. User: "I want to call Apollo to reschedule Tuesday's appointment to Thursday."
   (typed or signed — sign input arrives in Phase 3, for now: typed)
2. App resolves "Apollo" via `get_contact_info("Apollo")` → finds the clinic contact.
3. App pulls last call to that contact via `get_recent_call_summary`.
4. Gemma 4 prompt:
   ```
   The user is about to call {{CONTACT_NAME}} ({{ROLE}}). Their goal: "{{GOAL}}".
   Prior context with this contact: {{LAST_CALL_SUMMARY}}.
   Generate:
   - A natural opening line for the user to start the call with.
   - 3 likely questions/responses the counterparty will say.
   - For each, a recommended user response.
   - Any information the user should have ready (appointment dates, account hints).
   ```
5. The briefing renders as a card the user reads, then taps "Place Call."
6. Dial intent (`ACTION_CALL`) → Phase 1 pipeline takes over from the call-connected moment.

---

## 9. UI changes — Compose

**`CallScreen.kt`** (updated):

```kotlin
@Composable
fun CallScreen(vm: CallViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {

        // 1. Counterparty + briefing chip
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(state.counterpartyDisplay, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            if (state.briefing != null) {
                AssistChip(onClick = vm::showBriefing, label = { Text("Briefing") })
            }
        }

        Spacer(Modifier.height(12.dp))

        // 2. Live caption — scrolling
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp),
            reverseLayout = true
        ) {
            items(state.transcript.reversed(), key = { it.id }) { line ->
                CaptionLine(line)
            }
        }

        Spacer(Modifier.height(8.dp))

        // 3. Smart reply chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.suggestions.forEach { s ->
                SuggestionChip(
                    onClick = { vm.speakSuggestion(s) },
                    label = { Text(s.text) },
                    leadingIcon = { Icon(Icons.Default.Send, null) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 4. Free-text input
        OutlinedTextField(
            value = state.draftReply,
            onValueChange = vm::onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Or type your own…") },
            trailingIcon = {
                IconButton(onClick = vm::sendReply) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                }
            }
        )
    }
}
```

---

## 10. Testing checklist — Phase 2 is "done" when

- [ ] Onboarding seeds contacts, addresses, medical, payment into Room DB (with SQLCipher encryption verified by inspecting the DB file)
- [ ] Pre-call briefing renders within 4s of user stating goal
- [ ] During an incoming call, 3 smart-reply chips appear within 2s of caller pause
- [ ] Tapping a chip speaks it within 600ms
- [ ] At least one tool call (e.g., `get_address`) fires successfully during a test call — verify in logcat
- [ ] When user asks "what's my home address?" via typed reply, Gemma calls `get_address` and answers correctly
- [ ] Call summary generated and saved to `CallLogEntry` after call ends
- [ ] Streaming visibly fills smart-reply chips token-by-token (not pop-in)
- [ ] Hindi-speaking caller → Hindi suggestions; English caller → English suggestions
- [ ] Memory usage stays under 6GB during a 5-minute call

### End-to-end demo scenario

This scenario is what you'll record for Phase 4. Make it work flawlessly here.

> Ravi (the deaf user) places an outgoing call to his cardiologist's clinic to reschedule. He types: "Reschedule Tuesday's appointment to next Thursday afternoon."
>
> Briefing appears: opening line, "they'll ask for your patient ID — it's 78231," "they may offer Thu 3pm or Fri 11am — confirm Thursday."
>
> Ravi places the call. Receptionist (Hindi) answers: "Apollo Indiranagar, namaste."
>
> Captions appear. Chips appear: "Namaste, main Ravi bol raha hoon, appointment reschedule karwana hai" / "Hello, I need to change my appointment" / "Patient ID 78231 ke liye reschedule chahiye."
>
> Ravi taps the third chip. Clinic hears it. Receptionist says "Haan ji, Thursday 3 baje milega." Captions show. Chips offer: "Thursday 3pm fine hai" / "Confirm please" / "Thanks." Ravi taps the first.
>
> Call ends. Notification: "Appointment moved to Thursday 3pm." (Gemma's summary.)

---

## 11. Common failure modes — Phase 2

| Symptom | Cause | Fix |
|---|---|---|
| Suggestions arrive 4–6s late | Re-running session from scratch each turn | Reuse `LlmInferenceSession` across turns; only add new chunks |
| Tool call parse fails | Gemma emitted malformed JSON | Add retry with stricter prompt: "Output EXACTLY this JSON shape…" |
| 256K context goes OOM on E4B | E4B caps at 128K; 256K is 26B/31B only | Cap rolling window at 100K tokens; summarize past >5min ago |
| Suggestions in wrong language | Language detection slow | Lock language from first 30s of transcript; expose manual override |
| Hindi suggestions in Latin script | Tokenizer fallback | Explicitly instruct "Devanagari script only" in system prompt |
| Streaming UI flickers | Compose recomposition on every token | Batch tokens into 50ms windows in the Flow |

---

## 12. Timeline — Phase 2 in 2 days

| Day | Morning | Afternoon |
|---|---|---|
| **Day 4** | Room DB + SQLCipher + onboarding screens | Function-calling tool dispatcher + tool spec injection |
| **Day 5** | Streaming generation + smart-reply chip UI | Pre-call briefing flow + end-to-end demo scenario rehearsal |

---

## 13. What this phase enables for Phase 3

Phase 3 adds the ISL (Indian Sign Language) input pathway. By Phase 2's end, the *output* path (text → TTS → caller) is solid. Phase 3 replaces the typed input with sign-language input from the phone's front camera. Everything downstream (the smart replies, the tools, the context store) stays the same — only the input modality changes.
