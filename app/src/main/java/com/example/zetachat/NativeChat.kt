package com.example.zetachat

object NativeChat {
    // Native method declarations
    external fun initSession(sessionId: String, characterIndex: Int)
    external fun sendMessage(sessionId: String, message: String): String
    external fun addCharacter(
        name: String,
        appearance: String,
        personality: String,
        speakingStyle: String,
        worldview: String,
        scenarioPrompt: String,
        isPublic: Boolean
    ): Int

    // Load native library
    init {
        System.loadLibrary("native_chat")
    }
}
