package com.dhwani.app.sign

object SignRecognitionPolicy {
    private const val MIN_GESTURE_VOTES = 5
    private const val MIN_GESTURE_SUPPORT = 0.50f
    private const val MIN_GESTURE_CONFIDENCE = 0.62f
    private const val MIN_ISL_CONFIDENCE = 0.50f
    private const val MIN_GREETING_CONFIDENCE = 0.42f
    private const val MIN_UNRELIABLE_ISL_CONFIDENCE = 0.68f
    private const val MIN_TWO_VIEW_CONFIDENCE = 0.62f
    private const val MIN_ISL_MARGIN = 0.14f
    private const val MIN_GREETING_MARGIN = 0.10f
    private const val MIN_TEMPORAL_AGREEMENT = 2f / 3f

    private val lowerConfidenceGreetings = setOf(
        "HELLO",
        "HOW ARE YOU",
        "THANK YOU",
        "GOOD MORNING",
        "GOOD AFTERNOON",
        "GOOD EVENING",
        "GOOD NIGHT",
    )

    val supportedIncludeGlosses: Set<String> = setOf(
        "HELLO",
        "HOW ARE YOU",
        "THANK YOU",
        "GOOD MORNING",
        "GOOD AFTERNOON",
        "GOOD EVENING",
        "GOOD NIGHT",
        "GOOD",
        "BAD",
        "HAPPY",
        "SAD",
        "SICK",
        "HEALTHY",
        "DOCTOR",
        "HOSPITAL",
        "MEDICINE",
        "PATIENT",
        "TELEPHONE",
        "CELLPHONE",
        "LOCATION",
        "BILL",
        "MONEY",
        "PRICE",
        "TODAY",
        "TOMORROW",
        "YESTERDAY",
        "TIME",
        "I",
        "YOU",
        "YOU (PLURAL)",
        "WE",
        "THEY",
        "HE",
        "SHE",
        "FAMILY",
        "FRIEND",
        "MOTHER",
        "FATHER",
        "DEAF",
        "SIGN",
    )

    fun stableGesture(
        predictions: List<SignCandidate>,
        analyzedFrames: Int,
    ): SignRecognition? {
        if (analyzedFrames <= 0 || predictions.isEmpty()) return null

        val ranked = predictions
            .groupBy { it.gloss }
            .map { (gloss, votes) ->
                StableGesture(
                    candidate = SignCandidate(
                        gloss = gloss,
                        confidence = votes.map { it.confidence }.average().toFloat(),
                    ),
                    votes = votes.size,
                )
            }
            .sortedWith(
                compareByDescending<StableGesture> { it.votes }
                    .thenByDescending { it.candidate.confidence },
            )

        val top = ranked.first()
        val support = top.votes.toFloat() / analyzedFrames
        if (top.votes < MIN_GESTURE_VOTES ||
            support < MIN_GESTURE_SUPPORT ||
            top.candidate.confidence < MIN_GESTURE_CONFIDENCE
        ) {
            return null
        }
        return SignRecognition(
            candidates = ranked.take(3).map { it.candidate },
            framingReliable = true,
        )
    }

    fun acceptInclude(recognition: SignRecognition): SignRecognition? {
        if (recognition.candidates.isEmpty()) return null
        val top = recognition.candidates[0]
        val secondConfidence = recognition.candidates.getOrNull(1)?.confidence ?: 0f
        val reliableConfidence = if (top.gloss in lowerConfidenceGreetings) {
            MIN_GREETING_CONFIDENCE
        } else {
            MIN_ISL_CONFIDENCE
        }
        val framingConfidence = if (recognition.framingReliable) {
            reliableConfidence
        } else {
            MIN_UNRELIABLE_ISL_CONFIDENCE
        }
        val requiredConfidence = if (
            recognition.temporalAgreement < 0.99f &&
            top.gloss !in lowerConfidenceGreetings
        ) {
            maxOf(framingConfidence, MIN_TWO_VIEW_CONFIDENCE)
        } else {
            framingConfidence
        }
        val requiredMargin = if (top.gloss in lowerConfidenceGreetings) {
            MIN_GREETING_MARGIN
        } else {
            MIN_ISL_MARGIN
        }
        return recognition.takeIf {
            top.gloss in supportedIncludeGlosses &&
                recognition.temporalAgreement >= MIN_TEMPORAL_AGREEMENT &&
                top.confidence >= requiredConfidence &&
                top.confidence - secondConfidence >= requiredMargin
        }
    }

    fun noSign(): SignRecognition = SignRecognition(
        candidates = listOf(SignCandidate("NO_SIGN_DETECTED", 1f)),
        framingReliable = false,
    )

    fun unknownSign(): SignRecognition = SignRecognition(
        candidates = listOf(SignCandidate("UNKNOWN_SIGN", 1f)),
        framingReliable = false,
    )

    private data class StableGesture(
        val candidate: SignCandidate,
        val votes: Int,
    )
}
