# DHWANI — Phase 4: Demo, Video, Writeup, Submit

> The hackathon judges 40% on impact/vision, 30% on video storytelling, 30% on technical depth. Phases 1–3 built the technology. Phase 4 is where you actually win the prize.

---

## 1. Goal of this phase

A submitted Kaggle Writeup with:

1. A 3-minute YouTube video that makes a judge stop scrolling
2. A 1500-word writeup that survives the technical-verification check
3. A public GitHub repo that runs end-to-end with one command
4. A live demo (APK download + Kaggle notebook for the fine-tune) that judges can poke
5. Submission attached to the Kaggle competition before the 11:59pm UTC deadline

---

## 2. Demo scenario — the script you'll shoot

The video must show one specific deaf person using Dhwani to do one specific thing they couldn't do before. Not three scenarios. One. Memorable.

**The chosen scenario: Booking a doctor's appointment.**

Why this scenario:
- Every viewer instantly understands the stakes (your health, your time, your dignity)
- It exercises every part of the system: incoming/outgoing call, smart replies, function calling for personal info, sign-language input, Hindi+English code-switching
- It ends with a concrete, satisfying outcome (an appointment confirmed)
- The counterparty (clinic receptionist) is universally relatable

### Story beats (60s setup → 90s demo → 30s impact)

**0:00 — 0:08** Cold open. Tight shot of a hand holding a phone. Phone screen black. Cut to a young man's face — Ravi, 26, deaf. He's looking at a missed-call notification. We don't hear anything. Caption: "Ravi is one of 18 million deaf Indians."

**0:08 — 0:25** Establishing problem. Ravi types in WhatsApp to his mother: "Please call Dr. Mehta's clinic for me." Mother (offscreen voice) says "Beta, main meeting mein hoon, baad mein." Ravi's face — frustration we all know. Caption: "Every appointment, every auto ride, every bank call needs someone else."

**0:25 — 0:40** The pivot. Ravi opens Dhwani. Title card: "DHWANI — A phone-call agent that lives on his phone, not in the cloud." Quick architecture flash: on-device, Gemma 4, no internet icon.

**0:40 — 0:55** Pre-call briefing. Ravi types "Reschedule Tuesday's appointment with Dr. Mehta to Thursday afternoon." Briefing card appears: opening line in Hindi, "they will ask for your patient ID — 78231," "they may offer Thu 3pm or Fri 11am — prefer Thursday."

**0:55 — 1:15** Sign language. Ravi flips to sign-input. He signs three short ISL signs — "HELLO," "RESCHEDULE," "MY APPOINTMENT." The captured frames flash. Gemma 4 vision recognises. Output: "Namaste, mujhe apna appointment reschedule karna hai."

**1:15 — 1:50** The call. Ravi taps "Place call." Real call connects. Receptionist (Hindi): "Apollo Indiranagar, namaste, kaise help kar sakte hain?" Live captions appear. Three smart-reply chips render — visible token-by-token streaming. Ravi taps one. TTS speaks Hindi clearly into the call. Receptionist responds in Hindi. Captions. Smart replies. Ravi taps. Receptionist: "Thursday 3 baje confirm." Captions. Ravi taps "Thank you" chip.

**1:50 — 2:10** Call ends. Notification: "Appointment moved to Thursday 3pm with Dr. Mehta." Ravi looks at the phone. Slight smile. Caption: "For the first time, he made the call himself."

**2:10 — 2:35** The technical proof. Quick cuts: airplane mode toggle ON, app keeps working. Wireshark trace showing zero network packets. The Kaggle/HF page with the ISL model + benchmarks. Code repo scrolling. Latency overlay: "p50 1.1s on Pixel 8."

**2:35 — 2:55** The vision. Ravi (text overlay, not voiceover — keep it deaf-centred): "There are 70 million deaf people in the world. Most don't have hearing family to translate for them every day. With Dhwani, they don't need to."

**2:55 — 3:00** End card: "DHWANI — built on Gemma 4 — open source — github.com/your-handle/dhwani"

### Critical video production notes

- **No voiceover narration explaining the app.** The deaf user is the protagonist. Voiceover centring a hearing narrator would undermine the entire ethos.
- **Use captions on every line of dialogue.** Yours, the receptionist's. Always.
- **Show the screen with screen-recording, not phone-pointing-at-itself.** ADB `scrcpy` or built-in Android screen recorder. Composite over a real-phone-in-hand shot.
- **Hindi audio with English subtitles for the international judge audience.**
- **One real deaf collaborator on screen.** Not an actor. Pay them. Credit them.
- **Shoot in a quiet room** for the call audio — judges will scrutinize whether the TTS actually sounds clear on the line.

---

## 3. Pre-shoot checklist

- [ ] Deaf collaborator confirmed, briefed, signed consent form (template in repo)
- [ ] Second phone for the "clinic receptionist" call. Use a friend who speaks fluent Hindi.
- [ ] Both phones charged, on speakerphone tested at the same room
- [ ] Dhwani APK installed on Ravi's phone, all data seeded (his "address," "doctor," "patient ID")
- [ ] Test the full scenario 5 times before recording. Fix any latency stutters.
- [ ] OBS or DaVinci Resolve installed for editing
- [ ] Screen recording set up: ADB scrcpy with `--record` flag
- [ ] Lighting: soft daylight or a single softbox. No on-camera flash.
- [ ] Audio: a lapel mic or shotgun on a boom. *Phone speakers won't pick up cleanly.*
- [ ] Storyboard printed for each 10-second beat

---

## 4. Filming day

Shoot in this order (covers reshoots cheaply):

1. **B-roll first** — Ravi on his phone in different settings, the airplane mode toggle, the architecture animation. 30 min.
2. **The screen recordings** — run the full scenario from inside the app 3 times. Capture screen at 60fps. 45 min.
3. **The call scene** — set up both phones, do the real call. 3 takes minimum. 60 min.
4. **The closing portrait** — Ravi looking at the phone, slight reaction. 15 min.
5. **Pickup shots** — any expression beats or transitions missed. 30 min.

Total: ~3 hours filming.

---

## 5. The Kaggle writeup — 1500-word structure

The judges read this *after* the video. It exists to prove the demo isn't faked. Structure it around the 40/30/30 rubric.

### Title + subtitle (use these or close variants)

**DHWANI: The phone-call agent that lets 70 million deaf people make their own calls**
*An on-device Gemma 4 system combining ISL vision, function calling, and live audio captioning — built for Android, runs without internet.*

### Section 1 — The problem (≈250 words)

Open with one person, not statistics. "Ravi is 26. He has been deaf since infancy. To call his cardiologist, he has to ask his mother…" Build out from there. Cite WHO (~63M deaf globally; 18M in India). Name the failure cases: bank verification (legally cannot be relayed), auto-driver coordination, appointment booking, school principal calls. End with: existing solutions (video relay services, hearing-family dependence) fail because of cost, availability, latency, and privacy. State the gap precisely.

### Section 2 — The solution (≈300 words)

What Dhwani does, concretely, in three sentences. Then a per-feature walk-through: live captioning, smart-reply chips, function-call-fetched personal context, ISL sign input, on-device pre-call briefing. **Crucially:** explain why each feature *needs to exist locally on the phone* — latency for real-time captioning, cost for sustainable daily use, privacy for medical/financial calls, regulatory compliance for bank verification audio. State explicitly that no audio or transcript ever leaves the device.

### Section 3 — Why Gemma 4 specifically (≈300 words)

This is the technical-depth core. Four Gemma 4 capabilities, each tied to one feature:

1. **Audio-native E4B** → enables ASR + TTS in the same model in Hindi/English on-device (no need for separate Whisper + TTS engines, though we use Vosk in Phase 1 for stability — *be honest about the hybrid*).
2. **Native function calling** → personal context fetched as structured tool calls (`get_address`, `get_medical_info`), keeping PII out of the prompt window until the conversation needs it.
3. **Vision with interleaved multimodal input** → ISL recognition takes 8 frames + text prompt in one call, fine-tuned with Unsloth.
4. **128K context on E4B** → holds the full call transcript + Phase 2 system prompt + function-call results across a 10-minute conversation without dropping context.

Add latency numbers, model size, quantization (int4), peak memory. Concrete proof.

### Section 4 — Architecture (≈200 words + 1 diagram)

The architecture diagram from Phase 3. One paragraph explaining the data flow. One paragraph on the LiteRT deployment choice (why over llama.cpp on Android, why over Ollama). One paragraph on the Unsloth fine-tune.

### Section 5 — Fine-tune: weights and benchmarks (≈200 words)

Required by competition rules ("If training a model, publish your weights and benchmarks"). Tabular results — accuracy on INCLUDE test split, latency on Pixel 8, comparison to hand-landmark + LSTM baseline. Link to HuggingFace + Kaggle Model. Acknowledge limitations (regional ISL dialects, dataset size).

### Section 6 — Real-world utility (≈150 words)

One paragraph on the live demo scenario (with link to video timestamp). One paragraph on what we tested with the real deaf collaborator: which signs worked, which didn't, what they said about the experience. **This paragraph is what separates real projects from theater.**

### Section 7 — Limitations and what's next (≈100 words)

Be specific. Echo-cancellation on cheap phones. ISL dataset is one dialect. Sign segmentation breaks on rapid signing. Bank IVRs use DTMF tones (no current support). Plans: Telugu/Tamil expansion, integration with WhatsApp calling, partnership with NAD India.

---

## 6. GitHub repo structure

```
dhwani/
├── README.md                       # the front door — match the writeup tone
├── LICENSE                         # Apache 2.0
├── docs/
│   ├── PHASE_1_IMPLEMENTATION.md
│   ├── PHASE_2_IMPLEMENTATION.md
│   ├── PHASE_3_IMPLEMENTATION.md
│   ├── PHASE_4_IMPLEMENTATION.md
│   ├── architecture.png
│   └── ethics.md
├── android/                        # The Android app — Kotlin
│   ├── app/
│   │   └── src/main/java/com/dhwani/app/...
│   ├── build.gradle.kts
│   └── README.md                   # build + run instructions
├── training/                       # Unsloth fine-tune
│   ├── train_isl_adapter.py
│   ├── prepare_include_dataset.py
│   ├── evaluate.py
│   ├── convert_to_litert.sh
│   ├── kaggle_notebook.ipynb       # the public Kaggle notebook
│   └── README.md
├── models/                         # NOT checked in — pointers only
│   └── README.md                   # links to HF model card
├── demo/
│   ├── scenarios.md
│   ├── video_storyboard.md
│   └── consent_template.pdf
└── benchmarks/
    ├── results.csv
    ├── plot_accuracy.py
    └── README.md
```

### README.md — the GitHub front door

Match the writeup style. Hero section with: title, one-line description, hero gif (12-second loop of the smart-replies populating), the video embed, three buttons:

- **Try the APK** (release asset)
- **Read the writeup** (Kaggle link)
- **Watch the demo** (YouTube)

Then: architecture diagram, quick-start (1 paragraph: install Android Studio, clone, push the model, run). Then: links to each phase doc. Then: benchmarks table. Then: license + citation + acknowledgements (INCLUDE dataset, deaf collaborators by first name).

---

## 7. Live demo — what the judges can actually touch

### APK release on GitHub

Build a signed release APK. Upload as a GitHub release. The release page should say:

> ⚠️ Read before installing. Dhwani is a prototype. To use it on your phone:
> 1. Phone must be Android 8.0+, 6GB+ RAM (Pixel 7/8/9 recommended)
> 2. Download the APK and the Gemma 4 ISL model bundle
> 3. Push the model via `adb push`...
> 4. Grant phone, mic, camera permissions
> 5. Place a call to anyone, put on speakerphone, see captions

Plus a 2-minute video of the install flow.

### Kaggle Notebook — the fine-tune

Publish `kaggle_notebook.ipynb` as a public Kaggle notebook. Title: "DHWANI: Fine-tuning Gemma 4 for Indian Sign Language with Unsloth." Make sure it runs end-to-end on T4×2 from "Run All." Inside, load the INCLUDE dataset, train one mini epoch (for runnable demo), evaluate, show results. Link prominently from the writeup.

### HuggingFace model

Publish `dhwani/gemma-4-e4b-isl-lora` with the model card from Phase 3 section 5. This is what makes you eligible for the "publish your weights" rule.

---

## 8. Submission to Kaggle

The Kaggle Writeup is the actual submission artifact. From the competition rules:

### Required attachments

1. **Public video link (YouTube)** — attach to media gallery
2. **Public code repository link (GitHub)** — under Project Links
3. **Live demo link** — APK release page OR the Kaggle notebook URL (you can submit both)
4. **Cover image** — required for media gallery. A clean, branded shot. Either the Dhwani logo over a phone-in-hand, or a still from the video.

### Track selection

- **Impact Track:** Digital Equity & Inclusivity ($10K)
- **Special Technology Track:** LiteRT ($10K)
- **Eligible for Main Track** ($50K/$25K/$15K/$10K) automatically

You select *one* primary track in the Kaggle Writeup form. Pick **Digital Equity & Inclusivity** — that's the most defensible. The Special Tech track is judged separately and you don't need to "pick" LiteRT explicitly; eligibility is automatic from your repo's tech choices.

### Submission sanity check (30 min before deadline)

- [ ] Writeup word count ≤ 1500 (the rule)
- [ ] Writeup includes title and subtitle
- [ ] Track selected
- [ ] YouTube video unlisted-or-public, plays without login, captions burned in
- [ ] GitHub repo public, README renders, APK release downloadable
- [ ] Kaggle notebook public and "Run All" verified within last 24h
- [ ] HuggingFace model card public, weights downloadable
- [ ] Cover image attached to media gallery
- [ ] Click "Submit" — verify status is "Submitted," not "Draft"

---

## 9. Common Phase-4 failure modes

| Symptom | Cause | Fix |
|---|---|---|
| Video runs over 3:00 | Too many features shown | Cut to ONE scenario. Ruthless. |
| Demo crashes on shoot day | Untested under recording-load | Test with screen-record running for 30min straight beforehand |
| Receptionist can't hear TTS clearly | Audio routing wrong | Verify `MODE_IN_COMMUNICATION` was set; reshoot with phone re-set |
| ISL sign misrecognized on camera | Lighting changed | Light Ravi from front-above; matte background; no backlight |
| Writeup over 1500 words | Padding | Cut Section 7 first; then Section 4 |
| Repo doesn't build | `local.properties` or NDK path missing | Add a `setup.sh` that prints expected env vars |
| Judge can't install APK | unsigned build | Sign with a debug keystore that's documented |
| HF model link 404s | typo or wrong repo visibility | Test in incognito 24h before deadline |

---

## 10. Timeline — Phase 4 in 1 day (or 1.5)

Phase 4 is the day after Phase 3 lands. With training overnight on Day 6, you'll touch Phase 4 around the morning of Day 7.

| Time | Task |
|---|---|
| **Day 7, 06:00** | Pull trained model from Kaggle, finalize ISL accuracy numbers |
| **Day 7, 08:00 – 11:00** | Filming day with Ravi (per section 4) |
| **Day 7, 11:00 – 14:00** | Edit video. Burn captions. Export 1080p. Upload to YouTube. |
| **Day 7, 14:00 – 17:00** | Write the 1500-word writeup against the section 5 outline |
| **Day 7, 17:00 – 18:00** | Polish GitHub repo, README, release APK, publish Kaggle notebook + HF model |
| **Day 7, 18:00 – 19:00** | End-to-end smoke test of every link in writeup, in incognito |
| **Day 7, 19:00** | Submit on Kaggle. Verify "Submitted" status. |
| **Day 7, 19:30 – 23:59** | Buffer for fixes. Keep submission editable. |

You have until 11:59pm UTC. From India (IST) that's 5:29am the next day. Use the buffer wisely.

---

## 11. The judging-rubric crosscheck

Print this and tape it above your monitor while editing.

**Impact & Vision (40 pts)** — Does the video make the judge feel the problem in the first 25 seconds? Does it show a real human, not a feature tour? Does the closing statement land?

**Video Pitch & Storytelling (30 pts)** — Is the pacing tight? Are captions everywhere? Is the deaf collaborator centred? Is there ONE scenario, not a feature catalogue?

**Technical Depth & Execution (30 pts)** — Does the writeup explain *why Gemma 4 specifically*? Is the fine-tune real and published? Does the code build? Are the latency numbers honest? Is the on-device privacy claim verifiable?

If any of these answers is "no" or "not clearly," fix it before submitting. The submission is editable until the deadline — use that.

---

## 12. After submission

The Kaggle competition will publicize finalists. Whether or not you win:

- The HF model has standalone value — it's the first published Gemma-4-on-ISL model
- The repo has standalone value for deaf-led organisations to fork and adapt
- The deaf collaborator who appeared in the video should be credited as a co-author on any follow-on academic paper
- Plan one follow-up post on LinkedIn/Twitter showing the project and tagging deaf-community organisations (NAD India, ISH News, Noida Deaf Society) — this is how impact actually compounds

That's Phase 4. Submit. Sleep.
