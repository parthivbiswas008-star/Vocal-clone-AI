package com.example.data

import com.example.data.database.VoiceProfileEntity

data class LanguageOption(
    val code: String,
    val displayName: String,
    val flag: String,
    val regionHint: String
) {
    val labelWithFlag: String
        get() = "$flag $displayName"
}

data class VoiceFilterOption(
    val key: String,
    val label: String,
    val flag: String
) {
    val labelWithFlag: String
        get() = "$flag $label"
}

data class ScriptOption(
    val title: String,
    val text: String
)

data class EmotionStyleOption(
    val id: String,
    val label: String,
    val icon: String,
    val directionCue: String
) {
    val labelWithIcon: String
        get() = "$icon $label"
}

object VoiceCatalog {
    val emotionStyles: List<EmotionStyleOption> = listOf(
        EmotionStyleOption(
            id = "Natural Chat",
            label = "Natural Chat",
            icon = "\uD83D\uDCAC",
            directionCue = "Speak like a real person talking casually face-to-face with a friend — relaxed, authentic, slightly imperfect natural rhythm, warm spontaneous intonation, zero robotic announcer stiffness"
        ),
        EmotionStyleOption(
            id = "Warm Story",
            label = "Warm Story",
            icon = "\uD83D\uDCD6",
            directionCue = "Deliver like an intimate human storyteller sitting nearby — warm rising and falling melody, expressive gentle pauses at commas and periods, rich emotional color"
        ),
        EmotionStyleOption(
            id = "Emotional",
            label = "Emotional",
            icon = "\u2764\uFE0F",
            directionCue = "Speak with genuine heartfelt emotion, tender sincerity, soft vulnerable vocal texture, subtle breathy pauses, and heartfelt emphasis on meaningful words"
        ),
        EmotionStyleOption(
            id = "Cheerful",
            label = "Cheerful",
            icon = "\uD83D\uDE0A",
            directionCue = "Speak with a genuine smile in your voice — lively, upbeat, friendly warmth, natural cheerful lift at key words, bright human energy"
        ),
        EmotionStyleOption(
            id = "Dramatic",
            label = "Dramatic",
            icon = "\uD83C\uDFAC",
            directionCue = "Deliver with cinematic human depth — resonant chest tone, deliberate suspenseful pauses, strong expressive contrast, compelling gravitas"
        ),
        EmotionStyleOption(
            id = "Calm & Soft",
            label = "Calm & Soft",
            icon = "\uD83C\uDF19",
            directionCue = "Speak softly and peacefully close to the microphone — gentle soothing breath, unhurried calm phrasing, warm tender endings"
        ),
        EmotionStyleOption(
            id = "Podcast Host",
            label = "Podcast Host",
            icon = "\uD83C\uDF99\uFE0F",
            directionCue = "Speak like a charismatic, relaxed podcast host talking naturally into a warm condenser mic — engaging, conversational, articulate yet completely unscripted-sounding"
        )
    )

    val eqToneOptions: List<Pair<String, String>> = listOf(
        "warm" to "Warm",
        "deep" to "Deep",
        "balanced" to "Balanced",
        "bright" to "Bright",
        "intimate" to "Intimate"
    )

    fun emotionDirectionFor(styleId: String): String {
        return emotionStyles.firstOrNull { it.id.equals(styleId, ignoreCase = true) }?.directionCue
            ?: emotionStyles.first().directionCue
    }

    fun cloneAuditionSentence(language: String): String {
        return when {
            language.equals("Bengali", true) ->
                "হ্যালো! এটা আমার ক্লোন করা কণ্ঠস্বর, শুনতে এখন একদম স্বাভাবিক আর বাস্তব মনে হচ্ছে।"
            language.equals("Hindi", true) ->
                "नमस्ते! यह मेरी क्लोन की हुई आवाज़ है, जो अब बिल्कुल स्वाभाविक और असली लग रही है।"
            language.equals("Spanish", true) ->
                "¡Hola! Esta es mi voz clonada, sonando completamente natural y humana."
            language.equals("French", true) ->
                "Bonjour ! Voici ma voix clonée, avec un timbre chaleureux et tout à fait naturel."
            language.equals("Arabic", true) ->
                "مرحباً! هذا صوتي المستنسخ الآن بنبرة طبيعية وواقعية للغاية."
            language.equals("German", true) ->
                "Hallo! Das ist meine geklonte Stimme, die jetzt ganz natürlich und menschlich klingt."
            language.equals("Japanese", true) ->
                "こんにちは。これが私のクローン音声です。とても自然で人間らしく聞こえます。"
            language.equals("Urdu", true) ->
                "السلام علیکم! یہ میری کلون کی ہوئی آواز ہے جو بالکل قدرتی اور اصلی لگ رہی ہے۔"
            else ->
                "Hey there! This is my cloned voice speaking naturally, capturing my real accent and warmth."
        }
    }
    val supportedLanguages: List<LanguageOption> = listOf(
        LanguageOption("en-US", "English", "\uD83C\uDDFA\uD83C\uDDF8", "US / UK / Global English"),
        LanguageOption("bn-BD", "Bengali", "\uD83C\uDDE7\uD83C\uDDE9", "Bengali (Bangla)"),
        LanguageOption("hi-IN", "Hindi", "\uD83C\uDDEE\uD83C\uDDF3", "Hindi"),
        LanguageOption("es-ES", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8", "Spanish"),
        LanguageOption("fr-FR", "French", "\uD83C\uDDEB\uD83C\uDDF7", "French"),
        LanguageOption("ar-SA", "Arabic", "\uD83C\uDDF8\uD83C\uDDE6", "Modern Standard Arabic"),
        LanguageOption("de-DE", "German", "\uD83C\uDDE9\uD83C\uDDEA", "German"),
        LanguageOption("ja-JP", "Japanese", "\uD83C\uDDEF\uD83C\uDDF5", "Japanese"),
        LanguageOption("pt-BR", "Portuguese", "\uD83C\uDDE7\uD83C\uDDF7", "Brazilian & European Portuguese"),
        LanguageOption("ko-KR", "Korean", "\uD83C\uDDF0\uD83C\uDDF7", "Korean"),
        LanguageOption("ur-PK", "Urdu", "\uD83C\uDDF5\uD83C\uDDF0", "Urdu"),
        LanguageOption("it-IT", "Italian", "\uD83C\uDDEE\uD83C\uDDF9", "Italian"),
        LanguageOption("ru-RU", "Russian", "\uD83C\uDDF7\uD83C\uDDFA", "Russian"),
        LanguageOption("tr-TR", "Turkish", "\uD83C\uDDF9\uD83C\uDDF7", "Turkish"),
        LanguageOption("zh-CN", "Chinese", "\uD83C\uDDE8\uD83C\uDDF3", "Mandarin Chinese"),
        LanguageOption("id-ID", "Indonesian", "\uD83C\uDDEE\uD83C\uDDE9", "Indonesian"),
        LanguageOption("vi-VN", "Vietnamese", "\uD83C\uDDFB\uD83C\uDDF3", "Vietnamese"),
        LanguageOption("th-TH", "Thai", "\uD83C\uDDF9\uD83C\uDDED", "Thai")
    )

    fun flagForLanguage(languageName: String): String {
        val exact = supportedLanguages.firstOrNull {
            it.displayName.equals(languageName.trim(), ignoreCase = true)
        }
        if (exact != null) return exact.flag
        return when {
            languageName.contains("UK", true) || languageName.contains("British", true) -> "\uD83C\uDDEC\uD83C\uDDE7"
            languageName.contains("English", true) -> "\uD83C\uDDFA\uD83C\uDDF8"
            languageName.contains("Bengali", true) || languageName.contains("Bangla", true) -> "\uD83C\uDDE7\uD83C\uDDE9"
            languageName.contains("Hindi", true) -> "\uD83C\uDDEE\uD83C\uDDF3"
            languageName.contains("Spanish", true) -> "\uD83C\uDDEA\uD83C\uDDF8"
            languageName.contains("French", true) -> "\uD83C\uDDEB\uD83C\uDDF7"
            languageName.contains("Arabic", true) -> "\uD83C\uDDF8\uD83C\uDDE6"
            languageName.contains("German", true) -> "\uD83C\uDDE9\uD83C\uDDEA"
            languageName.contains("Japanese", true) -> "\uD83C\uDDEF\uD83C\uDDF5"
            languageName.contains("Portuguese", true) -> "\uD83C\uDDE7\uD83C\uDDF7"
            languageName.contains("Korean", true) -> "\uD83C\uDDF0\uD83C\uDDF7"
            languageName.contains("Urdu", true) -> "\uD83C\uDDF5\uD83C\uDDF0"
            languageName.contains("Italian", true) -> "\uD83C\uDDEE\uD83C\uDDF9"
            languageName.contains("Russian", true) -> "\uD83C\uDDF7\uD83C\uDDFA"
            languageName.contains("Turkish", true) -> "\uD83C\uDDF9\uD83C\uDDF7"
            languageName.contains("Chinese", true) || languageName.contains("Mandarin", true) -> "\uD83C\uDDE8\uD83C\uDDF3"
            languageName.contains("Indonesian", true) -> "\uD83C\uDDEE\uD83C\uDDE9"
            languageName.contains("Vietnamese", true) -> "\uD83C\uDDFB\uD83C\uDDF3"
            languageName.contains("Thai", true) -> "\uD83C\uDDF9\uD83C\uDDED"
            else -> "\uD83C\uDF10"
        }
    }

    val voiceFilterOptions: List<VoiceFilterOption> = listOf(
        VoiceFilterOption("All", "All", "\uD83C\uDF10"),
        VoiceFilterOption("My Clones", "My Clones", "\uD83C\uDF99\uFE0F"),
        VoiceFilterOption("English", "English", "\uD83C\uDDFA\uD83C\uDDF8"),
        VoiceFilterOption("Bengali", "Bengali", "\uD83C\uDDE7\uD83C\uDDE9"),
        VoiceFilterOption("Hindi", "Hindi", "\uD83C\uDDEE\uD83C\uDDF3"),
        VoiceFilterOption("Spanish", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8"),
        VoiceFilterOption("French", "French", "\uD83C\uDDEB\uD83C\uDDF7"),
        VoiceFilterOption("Arabic", "Arabic", "\uD83C\uDDF8\uD83C\uDDE6"),
        VoiceFilterOption("German", "German", "\uD83C\uDDE9\uD83C\uDDEA"),
        VoiceFilterOption("Japanese", "Japanese", "\uD83C\uDDEF\uD83C\uDDF5"),
        VoiceFilterOption("Urdu", "Urdu", "\uD83C\uDDF5\uD83C\uDDF0"),
        VoiceFilterOption("More", "More", "\uD83C\uDF0D")
    )

    val officialGeminiVoices: Map<String, String> = mapOf(
        "Achernar" to "Female, soft, delicate, velvet tone",
        "Achird" to "Female, friendly, approachable, warm mid-range",
        "Algenib" to "Male, deep, gravelly, textured baritone",
        "Algieba" to "Male, smooth, velvety, lyrical storyteller",
        "Alnilam" to "Male, firm, resonant, authoritative",
        "Aoede" to "Female, breezy, expressive, melodic conversational",
        "Autonoe" to "Female, bright, articulate, poised",
        "Callirrhoe" to "Female, easy-going, relaxed, natural",
        "Charon" to "Male, clear, informative, confident studio baritone",
        "Despina" to "Female, smooth, polished, calm",
        "Enceladus" to "Male, breathy, intimate, soft-spoken",
        "Erinome" to "Female, crystal-clear, crisp, gentle",
        "Fenrir" to "Male, bold, energetic, deep excitable",
        "Gacrux" to "Male, mature, grounded, trustworthy",
        "Iapetus" to "Male, clear, neutral, articulate",
        "Kore" to "Female, firm, crisp, professional presenter",
        "Laomedeia" to "Female, upbeat, lively, cheerful",
        "Leda" to "Female, youthful, vibrant, sweet",
        "Orus" to "Male, firm, strong, commanding cinematic",
        "Puck" to "Male, upbeat, friendly, youthful energetic",
        "Pulcherrima" to "Female, forward, confident, dynamic",
        "Rasalgethi" to "Male, informative, steady, documentary tone",
        "Sadachbia" to "Male, lively, expressive, rhythmic",
        "Sadaltager" to "Male, knowledgeable, warm, balanced",
        "Schedar" to "Male, even, composed, steady",
        "Sulafat" to "Female, warm, rich, nurturing alto",
        "Umbriel" to "Male, easy-going, relaxed, conversational",
        "Vindemiatrix" to "Female, gentle, serene, soothing",
        "Zephyr" to "Female, bright, crisp, modern energetic",
        "Zubenelgenubi" to "Male, casual, laid-back, natural"
    )

    val femaleOfficialVoices: Set<String> = officialGeminiVoices
        .filterValues { it.startsWith("Female", ignoreCase = true) }
        .keys

    val maleOfficialVoices: Set<String> = officialGeminiVoices
        .filterValues { it.startsWith("Male", ignoreCase = true) }
        .keys

    val curatedStudioVoices: List<VoiceProfileEntity> = listOf(
        // --- ENGLISH CHARACTERS (6) ---
        VoiceProfileEntity(
            name = "Liam Brooks",
            gender = "Male",
            accent = "General American",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Charon",
            language = "English",
            recordingLanguage = "English",
            pitchTone = "Warm Baritone",
            description = "Confident studio narrator with crisp articulation — ideal for documentaries, podcasts, and explainers.",
            voiceTraits = "Clean chest resonance, steady cadence, broadcast clarity"
        ),
        VoiceProfileEntity(
            name = "Clara Vance",
            gender = "Female",
            accent = "American Conversational",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Aoede",
            language = "English",
            recordingLanguage = "English",
            pitchTone = "Expressive Alto",
            description = "Breezy, expressive storyteller with natural warmth — great for audiobooks, vlogs, and personal messages.",
            voiceTraits = "Warm melodic inflection, approachable tone, lively phrasing"
        ),
        VoiceProfileEntity(
            name = "Arthur Sterling",
            gender = "Male",
            accent = "British RP",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Algenib",
            language = "English",
            recordingLanguage = "English",
            pitchTone = "Deep Gravelly",
            description = "Deep, gravelly British baritone full of gravitas — crafted for movie trailers and dramatic narration.",
            voiceTraits = "Rich low bass, slight smoky rasp, authoritative British phrasing"
        ),
        VoiceProfileEntity(
            name = "Maya Lin",
            gender = "Female",
            accent = "Modern Global English",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Zephyr",
            language = "English",
            recordingLanguage = "English",
            pitchTone = "Bright Soprano",
            description = "Bright, energetic and crisp modern voice — tailored for tech demos, commercials, and social reels.",
            voiceTraits = "Bright upper register, energetic clarity, modern pacing"
        ),
        VoiceProfileEntity(
            name = "Ethan Cole",
            gender = "Male",
            accent = "American Youthful",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Puck",
            language = "English",
            recordingLanguage = "English",
            pitchTone = "Upbeat Mid-range",
            description = "Friendly, enthusiastic conversational host — built for YouTube intros, gaming, and tutorials.",
            voiceTraits = "Upbeat delivery, crisp consonants, friendly conversational spark"
        ),
        VoiceProfileEntity(
            name = "Evelyn Harper",
            gender = "Female",
            accent = "British Studio English",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Despina",
            language = "English",
            recordingLanguage = "English",
            pitchTone = "Smooth & Calm",
            description = "Polished British studio voice with refined calm — suited for luxury ads, meditation, and audio guides.",
            voiceTraits = "Silky smooth projection, composed British diction, serene tone"
        ),

        // --- BENGALI CHARACTERS (6) ---
        VoiceProfileEntity(
            name = "Aarav Sen",
            gender = "Male",
            accent = "Kolkata Bengali",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Algieba",
            language = "Bengali",
            recordingLanguage = "Bengali",
            pitchTone = "Smooth Lyrical",
            description = "Smooth, poetic Bengali storyteller with cultured intonation — ideal for literature, poetry, and narration.",
            voiceTraits = "Smooth melodic Bengali cadence, warm midrange resonance, expressive pauses"
        ),
        VoiceProfileEntity(
            name = "Ananya Roy",
            gender = "Female",
            accent = "Standard Bengali",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Sulafat",
            language = "Bengali",
            recordingLanguage = "Bengali",
            pitchTone = "Warm Melodic",
            description = "Warm, graceful Bengali voice with clear diction — great for audiobooks, news updates, and warm greetings.",
            voiceTraits = "Rich nurturing warmth, clear Bengali vowel rounding, natural rhythm"
        ),
        VoiceProfileEntity(
            name = "Rohan Das",
            gender = "Male",
            accent = "Dhaka Bengali",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Fenrir",
            language = "Bengali",
            recordingLanguage = "Bengali",
            pitchTone = "Deep Dramatic",
            description = "Bold, resonant Bengali narrator with cinematic depth — built for thrillers, history, and impactful intros.",
            voiceTraits = "Deep chest projection, bold dramatic emphasis, commanding presence"
        ),
        VoiceProfileEntity(
            name = "Meera Chatterjee",
            gender = "Female",
            accent = "Soft Bengali",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Vindemiatrix",
            language = "Bengali",
            recordingLanguage = "Bengali",
            pitchTone = "Gentle & Soothing",
            description = "Serene, gentle Bengali voice — perfect for calm storytelling, meditation guides, and heartfelt letters.",
            voiceTraits = "Soft breathy serenity, unhurried soothing cadence, gentle endings"
        ),
        VoiceProfileEntity(
            name = "Tanvir Rahman",
            gender = "Male",
            accent = "Bangladeshi Broadcast Bengali",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Sadaltager",
            language = "Bengali",
            recordingLanguage = "Bengali",
            pitchTone = "Balanced Presenter",
            description = "Knowledgeable, articulate Bengali host — crafted for tech reviews, podcasts, and educational explainers.",
            voiceTraits = "Clear Bangladeshi studio accent, balanced warmth, steady pacing"
        ),
        VoiceProfileEntity(
            name = "Sraboni Mukherjee",
            gender = "Female",
            accent = "Expressive Kolkata Bengali",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Laomedeia",
            language = "Bengali",
            recordingLanguage = "Bengali",
            pitchTone = "Lively & Cheerful",
            description = "Vibrant, upbeat Bengali voice — tailored for lifestyle reels, brand promos, and engaging vlogs.",
            voiceTraits = "Bright cheerful lift, expressive Kolkata inflection, energetic clarity"
        ),

        // --- HINDI CHARACTERS (6) ---
        VoiceProfileEntity(
            name = "Kabir Sharma",
            gender = "Male",
            accent = "North Indian Hindi",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Orus",
            language = "Hindi",
            recordingLanguage = "Hindi",
            pitchTone = "Commanding Baritone",
            description = "Firm, cinematic Hindi narrator — ideal for bold trailers, motivational talks, and news broadcasts.",
            voiceTraits = "Strong grounded bass, crisp Hindi articulation, authoritative poise"
        ),
        VoiceProfileEntity(
            name = "Diya Kapoor",
            gender = "Female",
            accent = "Mumbai Conversational Hindi",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Leda",
            language = "Hindi",
            recordingLanguage = "Hindi",
            pitchTone = "Youthful & Vibrant",
            description = "Cheerful, expressive young Hindi voice — tailored for lifestyle reels, podcasts, and lively storytelling.",
            voiceTraits = "Youthful brightness, expressive emotional lift, natural conversational flow"
        ),
        VoiceProfileEntity(
            name = "Vikram Mehta",
            gender = "Male",
            accent = "Classic Studio Hindi",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Gacrux",
            language = "Hindi",
            recordingLanguage = "Hindi",
            pitchTone = "Mature & Composed",
            description = "Mature, trustworthy Hindi baritone — suited for documentaries, business briefings, and educational series.",
            voiceTraits = "Grounded mature timbre, steady authoritative rhythm, warm clarity"
        ),
        VoiceProfileEntity(
            name = "Priya Nair",
            gender = "Female",
            accent = "Clear Formal Hindi",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Kore",
            language = "Hindi",
            recordingLanguage = "Hindi",
            pitchTone = "Crisp Professional",
            description = "Polished, articulate Hindi presenter — crafted for e-learning modules, corporate narration, and assistants.",
            voiceTraits = "Crisp broadcast diction, poised intonation, balanced clarity"
        ),
        VoiceProfileEntity(
            name = "Arjun Verma",
            gender = "Male",
            accent = "Warm Storytelling Hindi",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Schedar",
            language = "Hindi",
            recordingLanguage = "Hindi",
            pitchTone = "Steady & Warm",
            description = "Composed, friendly Hindi storyteller — great for audiobooks, devotional narrations, and daily podcasts.",
            voiceTraits = "Even mellow baritone, natural pauses, warm Indian cadence"
        ),
        VoiceProfileEntity(
            name = "Nisha Patel",
            gender = "Female",
            accent = "Friendly Modern Hindi",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Achird",
            language = "Hindi",
            recordingLanguage = "Hindi",
            pitchTone = "Warm Mid-range",
            description = "Approachable, friendly Hindi narrator — ideal for tutorials, customer guides, and warm conversations.",
            voiceTraits = "Friendly natural smile in tone, clear diction, smooth pacing"
        ),

        // --- SPANISH CHARACTERS (2) ---
        VoiceProfileEntity(
            name = "Mateo Cruz",
            gender = "Male",
            accent = "Latin American Spanish",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Sadachbia",
            language = "Spanish",
            recordingLanguage = "Spanish",
            pitchTone = "Lively & Expressive",
            description = "Energetic, warm Spanish storyteller — great for travel vlogs, podcasts, and engaging dialogue.",
            voiceTraits = "Expressive Spanish rhythm, upbeat friendly warmth, clear projection"
        ),
        VoiceProfileEntity(
            name = "Sofia Morales",
            gender = "Female",
            accent = "Castilian Spanish",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Autonoe",
            language = "Spanish",
            recordingLanguage = "Spanish",
            pitchTone = "Bright & Poised",
            description = "Articulate, radiant Spanish presenter — ideal for audio courses, news summaries, and storytelling.",
            voiceTraits = "Crisp Castilian clarity, graceful poise, bright natural timbre"
        ),

        // --- FRENCH CHARACTERS (2) ---
        VoiceProfileEntity(
            name = "Amélie Laurent",
            gender = "Female",
            accent = "Parisian French",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Achernar",
            language = "French",
            recordingLanguage = "French",
            pitchTone = "Soft Velvet",
            description = "Refined, velvety French narrator — designed for luxury branding, cinema, and cultural essays.",
            voiceTraits = "Soft velvet texture, refined Parisian elegance, smooth legato flow"
        ),
        VoiceProfileEntity(
            name = "Julien Moreau",
            gender = "Male",
            accent = "Warm Studio French",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Enceladus",
            language = "French",
            recordingLanguage = "French",
            pitchTone = "Intimate & Warm",
            description = "Soft-spoken, cinematic French storyteller — great for short films, podcasts, and late-night narration.",
            voiceTraits = "Intimate breathy warmth, subtle French cadence, calm resonance"
        ),

        // --- ARABIC CHARACTERS (2) ---
        VoiceProfileEntity(
            name = "Tariq Al-Mansoor",
            gender = "Male",
            accent = "Modern Standard Arabic",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Alnilam",
            language = "Arabic",
            recordingLanguage = "Arabic",
            pitchTone = "Rich & Eloquent",
            description = "Eloquent, authoritative Arabic speaker — ideal for heritage stories, formal presentations, and narration.",
            voiceTraits = "Resonant firm voice, classical Arabic poise, clear articulation"
        ),
        VoiceProfileEntity(
            name = "Layla Hassan",
            gender = "Female",
            accent = "Melodic Modern Arabic",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Callirrhoe",
            language = "Arabic",
            recordingLanguage = "Arabic",
            pitchTone = "Relaxed & Natural",
            description = "Warm, natural Arabic narrator — suited for podcasts, cultural audiobooks, and lifestyle videos.",
            voiceTraits = "Gentle natural rhythm, clear Arabic vowels, welcoming tone"
        ),

        // --- GERMAN CHARACTERS (2) ---
        VoiceProfileEntity(
            name = "Lukas Weber",
            gender = "Male",
            accent = "Standard Hochdeutsch",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Rasalgethi",
            language = "German",
            recordingLanguage = "German",
            pitchTone = "Steady Documentary",
            description = "Clear, analytical German narrator — built for science explainers, audiobooks, and tech videos.",
            voiceTraits = "Steady documentary clarity, precise German consonants, calm authority"
        ),
        VoiceProfileEntity(
            name = "Clara Hoffmann",
            gender = "Female",
            accent = "Modern Berlin German",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Pulcherrima",
            language = "German",
            recordingLanguage = "German",
            pitchTone = "Confident & Dynamic",
            description = "Forward, confident German presenter — great for business briefings, product launches, and ads.",
            voiceTraits = "Dynamic projection, crisp articulation, confident modern tone"
        ),

        // --- JAPANESE CHARACTERS (2) ---
        VoiceProfileEntity(
            name = "Hana Sato",
            gender = "Female",
            accent = "Tokyo Japanese",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Erinome",
            language = "Japanese",
            recordingLanguage = "Japanese",
            pitchTone = "Crystal Clear",
            description = "Polite, crystal-clear Japanese voice — crafted for serene narration, product guides, and storytelling.",
            voiceTraits = "Pure gentle timbre, precise Japanese pitch accent, calm clarity"
        ),
        VoiceProfileEntity(
            name = "Kenji Takahashi",
            gender = "Male",
            accent = "Calm Studio Japanese",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Iapetus",
            language = "Japanese",
            recordingLanguage = "Japanese",
            pitchTone = "Clean & Composed",
            description = "Composed, articulate Japanese narrator — ideal for documentaries, anime recaps, and calm explainers.",
            voiceTraits = "Neutral studio clarity, poised Japanese rhythm, clean diction"
        ),

        // --- URDU CHARACTERS (2) ---
        VoiceProfileEntity(
            name = "Hamza Qureshi",
            gender = "Male",
            accent = "Classical Poetic Urdu",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Algieba",
            language = "Urdu",
            recordingLanguage = "Urdu",
            pitchTone = "Velvet Baritone",
            description = "Eloquent, velvety Urdu narrator — crafted for poetry, ghazals, drama, and timeless storytelling.",
            voiceTraits = "Rich poetic resonance, refined Urdu pronunciation, smooth pacing"
        ),
        VoiceProfileEntity(
            name = "Zara Siddiqui",
            gender = "Female",
            accent = "Graceful Studio Urdu",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Sulafat",
            language = "Urdu",
            recordingLanguage = "Urdu",
            pitchTone = "Warm & Graceful",
            description = "Graceful, melodic Urdu voice — ideal for audiobooks, heartfelt letters, and radio-style broadcasts.",
            voiceTraits = "Warm alto timbre, soft elegant diction, expressive cadence"
        ),

        // --- MORE GLOBAL CHARACTERS (7) ---
        VoiceProfileEntity(
            name = "Gabriel Silva",
            gender = "Male",
            accent = "Brazilian Portuguese",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Umbriel",
            language = "Portuguese",
            recordingLanguage = "Portuguese",
            pitchTone = "Relaxed Conversational",
            description = "Warm, easy-going Brazilian Portuguese host — great for podcasts, lifestyle vlogs, and storytelling.",
            voiceTraits = "Relaxed Brazilian cadence, friendly warmth, natural conversational ease"
        ),
        VoiceProfileEntity(
            name = "Ji-eun Park",
            gender = "Female",
            accent = "Seoul Korean",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Leda",
            language = "Korean",
            recordingLanguage = "Korean",
            pitchTone = "Bright & Expressive",
            description = "Vibrant, clear Seoul Korean voice — ideal for K-culture content, shorts, and modern narration.",
            voiceTraits = "Clean bright tone, natural Seoul inflection, friendly clarity"
        ),
        VoiceProfileEntity(
            name = "Marco Rossi",
            gender = "Male",
            accent = "Expressive Italian",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Zubenelgenubi",
            language = "Italian",
            recordingLanguage = "Italian",
            pitchTone = "Warm Casual",
            description = "Charismatic, expressive Italian narrator — great for culinary stories, travel guides, and cinema.",
            voiceTraits = "Melodic Italian flow, charismatic warmth, natural rhythmic emphasis"
        ),
        VoiceProfileEntity(
            name = "Dmitri Volkov",
            gender = "Male",
            accent = "Moscow Russian",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Gacrux",
            language = "Russian",
            recordingLanguage = "Russian",
            pitchTone = "Deep Grounded",
            description = "Mature, resonant Russian voice — suited for classic literature, historical narration, and trailers.",
            voiceTraits = "Grounded chest depth, clear Slavic articulation, steady cadence"
        ),
        VoiceProfileEntity(
            name = "Elif Kaya",
            gender = "Female",
            accent = "Istanbul Turkish",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Aoede",
            language = "Turkish",
            recordingLanguage = "Turkish",
            pitchTone = "Expressive & Clear",
            description = "Warm, expressive Istanbul Turkish narrator — tailored for podcasts, drama narration, and social videos.",
            voiceTraits = "Expressive Turkish melody, warm natural phrasing, clear tone"
        ),
        VoiceProfileEntity(
            name = "Mei-Ling Zhou",
            gender = "Female",
            accent = "Standard Mandarin Chinese",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Despina",
            language = "Chinese",
            recordingLanguage = "Chinese",
            pitchTone = "Polished & Calm",
            description = "Smooth, articulate Mandarin speaker — ideal for explainers, audiobooks, and professional voiceovers.",
            voiceTraits = "Crisp Mandarin tonal clarity, smooth studio warmth, steady rhythm"
        ),
        VoiceProfileEntity(
            name = "Budi Santoso",
            gender = "Male",
            accent = "Jakarta Indonesian",
            audioPath = null,
            isPrebuilt = true,
            prebuiltCode = "Charon",
            language = "Indonesian",
            recordingLanguage = "Indonesian",
            pitchTone = "Clear & Friendly",
            description = "Confident, friendly Indonesian narrator — great for YouTube explainers, podcasts, and tutorials.",
            voiceTraits = "Clear Indonesian articulation, approachable studio baritone, natural pace"
        )
    )

    val cloneReadingScripts: Map<String, List<ScriptOption>> = mapOf(
        "English" to listOf(
            ScriptOption(
                "Natural Voice",
                "Hello! I am speaking in my natural everyday voice so the AI can accurately capture my personal accent, pitch, and rhythm for voice cloning."
            ),
            ScriptOption(
                "Expressive Story",
                "The crisp morning breeze swept across the quiet harbor as golden sunlight danced across the water. Every word carries its own unique tone and warmth."
            ),
            ScriptOption(
                "Studio Clarity",
                "Clear pronunciation and steady pacing help artificial intelligence learn every nuance of your voice, from subtle emphasis to natural breathing."
            )
        ),
        "Bengali" to listOf(
            ScriptOption(
                "Natural Bengali",
                "নমস্কার! আমি আমার নিজের স্বাভাবিক কণ্ঠে কথা বলছি, যাতে এআই আমার অ্যাকসেন্ট, উচ্চারণ এবং কণ্ঠস্বরের সূক্ষ্ম বৈশিষ্ট্য নিখুঁতভাবে শিখে নিতে পারে।"
            ),
            ScriptOption(
                "Poetic Reading",
                "ভোরের মিষ্টি রোদে বাংলার প্রকৃতি জেগে উঠেছে। নদীর কলতান আর পাখির ডাকে প্রতিটি সকাল নতুন গল্পের সূচনা করে।"
            )
        ),
        "Hindi" to listOf(
            ScriptOption(
                "Natural Hindi",
                "नमस्ते! मैं अपनी स्वाभाविक आवाज़ में यह अनुच्छेद पढ़ रहा हूँ ताकि एआई मेरे लहजे, पिच और बोलने के अंदाज़ को सटीकता से पहचान सके।"
            ),
            ScriptOption(
                "Expressive Hindi",
                "सुबह की पहली किरण के साथ हर नया दिन नई उम्मीद और नई कहानी लेकर आता है। स्पष्ट आवाज़ और आत्मविश्वास ही हमारी पहचान है।"
            )
        ),
        "Spanish" to listOf(
            ScriptOption(
                "Natural Spanish",
                "¡Hola! Estoy hablando con mi voz natural para que la inteligencia artificial pueda detectar mi acento, mi entonación y mi ritmo con total precisión."
            )
        ),
        "French" to listOf(
            ScriptOption(
                "Natural French",
                "Bonjour! Je parle avec ma voix naturelle afin que l'intelligence artificielle puisse analyser mon accent, mon timbre et mon rythme avec fidélité."
            )
        ),
        "Arabic" to listOf(
            ScriptOption(
                "Natural Arabic",
                "مرحباً! أتحدث بصوتي الطبيعي الآن لكي يتمكن الذكاء الاصطناعي من تحليل لهجتي ونبرة صوتي وإيقاع حديثي بدقة عالية."
            )
        ),
        "German" to listOf(
            ScriptOption(
                "Natural German",
                "Hallo! Ich spreche in meiner natürlichen Stimme, damit die künstliche Intelligenz meinen Akzent, meine Klangfarbe und mein Sprechtempo genau erfassen kann."
            )
        ),
        "Japanese" to listOf(
            ScriptOption(
                "Natural Japanese",
                "こんにちは。私の自然な声とアクセント、話し方のリズムを正確に学習してもらうために、この文章をゆっくりと読んでいます。"
            )
        ),
        "Portuguese" to listOf(
            ScriptOption(
                "Natural Portuguese",
                "Olá! Estou falando com a minha voz natural para que a inteligência artificial possa captar meu sotaque, tom e ritmo com clareza."
            )
        ),
        "Korean" to listOf(
            ScriptOption(
                "Natural Korean",
                "안녕하세요! 인공지능이 제 자연스러운 목소리와 억양, 말하는 리듬을 정확하게 분석할 수 있도록 또박또박 읽고 있습니다."
            )
        ),
        "Urdu" to listOf(
            ScriptOption(
                "Natural Urdu",
                "السلام علیکم! میں اپنی قدرتی آواز میں بات کر رہا ہوں تاکہ اے آئی میرے لہجے، پچ اور بولنے کے انداز کو بالکل درست طریقے سے سمجھ سکے۔"
            )
        ),
        "Italian" to listOf(
            ScriptOption(
                "Natural Italian",
                "Ciao! Sto parlando con la mia voce naturale così che l'intelligenza artificiale possa riconoscere il mio accento, timbro e ritmo."
            )
        ),
        "Russian" to listOf(
            ScriptOption(
                "Natural Russian",
                "Привет! Я говорю своим естественным голосом, чтобы искусственный интеллект точно определил мой акцент, тембр и интонацию."
            )
        ),
        "Turkish" to listOf(
            ScriptOption(
                "Natural Turkish",
                "Merhaba! Yapay zekânın aksanımı, ses tonumu ve konuşma ritmimi doğru bir şekilde öğrenmesi için doğal sesimle konuşuyorum."
            )
        ),
        "Chinese" to listOf(
            ScriptOption(
                "Natural Chinese",
                "你好！我现在正用我最自然的声音朗读这段话，让人工智能准确捕捉我的口音、音色和说话节奏。"
            )
        ),
        "Indonesian" to listOf(
            ScriptOption(
                "Natural Indonesian",
                "Halo! Saya berbicara dengan suara alami saya agar kecerdasan buatan dapat mengenali aksen, nada, dan ritme bicara saya dengan akurat."
            )
        ),
        "Vietnamese" to listOf(
            ScriptOption(
                "Natural Vietnamese",
                "Xin chào! Tôi đang nói bằng giọng tự nhiên của mình để trí tuệ nhân tạo có thể nhận diện chính xác giọng điệu và nhịp nói của tôi."
            )
        ),
        "Thai" to listOf(
            ScriptOption(
                "Natural Thai",
                "สวัสดีครับ ฉันกำลังพูดด้วยน้ำเสียงธรรมชาติเพื่อให้ปัญญาประดิษฐ์สามารถวิเคราะห์สำเนียงและจังหวะการพูดได้อย่างแม่นยำ"
            )
        )
    )

    val ttsQuickSamples: Map<String, List<String>> = mapOf(
        "English" to listOf(
            "Welcome back! Today we are exploring how AI voice cloning brings stories to life.",
            "Take a deep breath, stay focused, and remember that great things take time.",
            "Breaking news from the studio: next-generation multilingual speech synthesis is here."
        ),
        "Bengali" to listOf(
            "সবাইকে স্বাগতম! আজ আমরা এআই ভয়েস ক্লোনিংয়ের জাদু নিজের কণ্ঠে শুনে দেখব।",
            "জীবন সুন্দর, যদি আমরা প্রতিটি মুহূর্তকে ভালোবাসা ও কৃতজ্ঞতার সাথে উপভোগ করি।",
            "পডকাস্টের নতুন পর্বে আপনাদের সবাইকে আন্তরিক শুভেচ্ছা ও অভিনন্দন।"
        ),
        "Hindi" to listOf(
            "नमस्ते दोस्तों! आज के इस खास एपिसोड में आप सभी का दिल से स्वागत है।",
            "सफलता की शुरुआत हमेशा एक छोटे लेकिन मजबूत कदम से होती है।",
            "अब एआई की मदद से आपकी अपनी आवाज़ में किसी भी भाषा में ऑडियो बनाना बेहद आसान है।"
        ),
        "Spanish" to listOf(
            "¡Bienvenidos! Hoy descubriremos cómo la clonación de voz transforma nuestras historias."
        ),
        "French" to listOf(
            "Bienvenue à tous! Découvrons ensemble la magie de la synthèse vocale naturelle."
        ),
        "Arabic" to listOf(
            "أهلاً بكم جميعاً! اليوم نستمع إلى تجربة صوتية طبيعية بتقنية الذكاء الاصطناعي."
        ),
        "German" to listOf(
            "Willkommen zurück! Heute erleben wir, wie modernste KI-Stimmen Geschichten lebendig machen."
        ),
        "Japanese" to listOf(
            "みなさんこんにちは！今日はAI音声クローン技術の魅力をお届けします。"
        ),
        "Portuguese" to listOf(
            "Olá pessoal! Hoje vamos explorar como a clonagem de voz com IA dá vida às nossas histórias."
        ),
        "Korean" to listOf(
            "안녕하세요 여러분! 오늘 AI 보이스 클로닝으로 생생한 음성을 만들어 보겠습니다."
        ),
        "Urdu" to listOf(
            "خوش آمدید! آج ہم مصنوعی ذہانت کی مدد سے قدرتی آواز بنانے کا تجربہ کریں گے۔"
        ),
        "Italian" to listOf(
            "Benvenuti! Oggi scopriamo come la clonazione vocale rende ogni racconto più vivo."
        ),
        "Russian" to listOf(
            "Добро пожаловать! Сегодня мы покажем, как ИИ-клонирование голоса оживляет истории."
        ),
        "Turkish" to listOf(
            "Hoş geldiniz! Bugün yapay zekâ ses klonlamanın hikayelere nasıl hayat verdiğini keşfediyoruz."
        ),
        "Chinese" to listOf(
            "欢迎回来！今天我们一起探索人工智能语音克隆如何让故事更加生动。"
        ),
        "Indonesian" to listOf(
            "Selamat datang! Hari ini kita akan mencoba teknologi kloning suara AI yang sangat alami."
        ),
        "Vietnamese" to listOf(
            "Chào mừng bạn! Hôm nay chúng ta sẽ khám phá công nghệ nhân bản giọng nói AI tự nhiên."
        ),
        "Thai" to listOf(
            "ยินดีต้อนรับครับ วันนี้เรามาฟังพลังของการโคลนเสียงด้วยปัญญาประดิษฐ์กันครับ"
        )
    )
}
