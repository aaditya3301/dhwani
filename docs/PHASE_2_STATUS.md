# Phase 2 Status

## Status

Phase 2 is partially implemented as a usable prototype. The current version adds personal context, pre-call briefing, Gemma-generated smart replies, labeled conversation history, and local call summaries on top of the Phase 1 communication pipe.

## Built

- Local personal context storage.
- Context setup UI with editable fields.
- Collapsible personal context panel.
- Medical notes and safe payment hint context fields.
- Pre-call briefing goal input.
- Collapsible briefing panel.
- Gemma-generated call briefing.
- Recent call notes shown in the briefing panel.
- Smart reply prompt generation from final Vosk transcripts.
- Smart reply chips shown in the call UI.
- Manual smart reply refresh.
- Tap-to-speak smart replies through TTS.
- Labeled conversation transcript with `Caller` and `You`.
- User typed replies and tapped smart replies are added to the transcript.
- Local call-summary storage.
- Call summary generation when the user taps Stop.
- Recent call summaries are fed back into briefing and smart-reply prompts.
- Shorter and stricter prompts for faster, more practical responses.

## Working

- Personal context can be entered, saved, hidden, and edited.
- Briefing generation works with the local Gemma model.
- Briefing panel can be hidden after generation.
- Final Vosk transcripts trigger Gemma smart-reply generation.
- Smart reply chips can be tapped and spoken aloud.
- Refresh regenerates smart replies from the latest caller line.
- Stop clears active captions, transcript, draft reply, smart replies, and thinking state.
- A transcript copy is summarized before the visible call UI is cleared.
- Recent summaries persist locally and appear in later briefing context.

## Tested

- Android debug build succeeds.
- Personal context UI no longer disappears while typing.
- Screen is scrollable.
- Long briefing responses no longer cover the full app.
- Briefing generation returns a usable response.
- Smart replies appear after Gemma finishes thinking.
- Context and briefing panels can be hidden and reopened.
- Conversation labels show caller/user turns.
- Stop reset behavior works at build level and is ready for phone testing.
- Android debug build succeeds after the latest Phase 2 updates.

## Mocked Or Approximate

- Personal context and summaries are stored in SharedPreferences, not encrypted Room.
- Tool/function calling is represented through prompt context, not native tool calls.
- Gemma output is one-shot, not streaming.
- Suggestions are generated after final transcript chunks, not from robust caller-pause detection.
- Call summaries are saved locally, but there is no full history management UI yet.
- Contact lookup and outgoing dial flow are not implemented yet.

## Known Gaps

- Replace SharedPreferences with encrypted Room or another secure local store.
- Add structured contacts, addresses, medical notes, payment hints, and call logs.
- Add tool-call parser and dispatcher.
- Add streaming responses if supported reliably by the selected MediaPipe/model version.
- Improve smart reply quality with more examples and better transcript context.
- Add call summary list/detail UI.
- Add outgoing call briefing-to-dial flow.
- Add debounce/cooldown logic so smart replies do not trigger too often.
- Add manual language selector or language lock for Hindi/English.

## Next Updates

Start next from structured context and tool dispatch:

1. Replace the flat SharedPreferences context with structured local records for contacts, addresses, medical notes, payment hints, and call summaries.
2. Add a Kotlin tool dispatcher for `get_address`, `get_contact_info`, `get_medical_info`, `get_payment_hint`, and `get_recent_call_summary`.
3. Update Gemma prompts so it emits simple tool-call blocks or selects from available structured context.
4. Add a small call-summary history panel so saved summaries can be inspected and cleared.
5. Add smarter suggestion timing: generate after caller pauses, and add cooldown to avoid repeated generations.

Testing to do next:

- Real phone call test with `Caller` and `You` transcript labels.
- Stop/reset test after a real call.
- English smart-reply quality test.
- Hindi smart-reply quality test.
- Measure average briefing generation time.
- Measure average smart-reply generation time.
