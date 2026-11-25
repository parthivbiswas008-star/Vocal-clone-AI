package com.example.data

import com.example.data.database.VoiceProfileEntity

data class LanguageOption(
    val code: String,
    val displayName: String,
    val shortLabel: String,
    val regionHint: String
)

data class ScriptOption(
    val title: String,
    val text: String
)

object VoiceCatalog {
    val supportedLanguages: List<LanguageOption> = listOf(
        LanguageOption("en-US", "English", "English", "US / UK / Global English"),
        LanguageOption("bn-IN", "Bengali", "Bengali", "Bengali (Bangla)"),
        LanguageOption("hi-IN", "Hindi", "Hindi", "Hindi"),
        LanguageOption("es-ES", "Spanish", "Spanish", "Spanish"),
        LanguageOption("fr-FR", "French", "French", "French"),
        LanguageOption("ar-XA", "Arabic", "Arabic", "Modern Standard Arabic"),
        LanguageOption("de-DE", "German", "German", "German"),
        LanguageOption("ja-JP", "Japanese", "Japanese", "Japanese"),
        LanguageOption("pt-BR", "Portuguese", "Portuguese", "Portuguese"),
        LanguageOption("ko-KR", "Korean", "Korean", "Korean"),
        LanguageOption("ur-PK", "Urdu", "Urdu", "Urdu"),
        LanguageOption("it-IT", "Italian", "Italian", "Italian")
    )

    val voiceFilterCategories: List<String> = listOf(
        "All",
        "My Clones",
        "English",
        "Bengali",
        "Hindi",
        "Global"
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

    val curatedStudioVoices: List<VoiceProfileEntity> = listOf(
        // --- ENGLISH CHARACTERS ---
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

        // --- BENGALI CHARACTERS ---
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

        // --- HINDI CHARACTERS ---
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

        // --- GLOBAL / MULTILINGUAL CHARACTERS ---
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
        )
    )
}
