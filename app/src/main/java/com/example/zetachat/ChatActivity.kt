package com.example.zetachat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.zetachat.data.AppDatabase
import com.example.zetachat.data.CharacterEntity
import com.example.zetachat.data.CharacterDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var characterDao: CharacterDao
    private lateinit var characterList: MutableList<CharacterEntity>
    private var selectedCharacterIndex: Int = -1
    private val sessionId = "main_chat_session" // Fixed session ID to maintain conversation history

    // View bindings
    private lateinit var btnSelectCharacter: ImageButton
    private lateinit var tvSelectedCharacterName: TextView
    private lateinit var etMessage: android.widget.EditText
    private lateinit var btnSend: android.widget.Button
    private lateinit var tvChatHistory: TextView

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, ChatActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize database
        database = AppDatabase.getInstance(this)
        characterDao = database.characterDao()

        // Initialize views
        btnSelectCharacter = findViewById(R.id.btnSelectCharacter)
        tvSelectedCharacterName = findViewById(R.id.tvSelectedCharacterName)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvChatHistory = findViewById(R.id.tvChatHistory)

        // Initialize character list and selected index
        characterList = mutableListOf()

        // Load characters from database
        loadCharacters()

        // Set up UI listeners
        btnSelectCharacter.setOnClickListener {
            showCharacterSelectionDialog()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadCharacters() {
        lifecycleScope.launch {
            val characters = characterDao.getAll()
            withContext(Dispatchers.Main) {
                characterList.clear()
                characterList.addAll(characters)
                if (characterList.isNotEmpty() && selectedCharacterIndex == -1) {
                    // Select first character by default if none selected
                    selectedCharacterIndex = 0
                    updateSelectedCharacterUI()
                }
                // Initialize native session with selected character
                NativeChat.initSession(sessionId, selectedCharacterIndex)
            }
        }
    }

    private fun updateSelectedCharacterUI() {
        if (selectedCharacterIndex >= 0 && selectedCharacterIndex < characterList.size) {
            val character = characterList[selectedCharacterIndex]
            tvSelectedCharacterName.text = character.name
            tvSelectedCharacterName.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            tvSelectedCharacterName.text = "캐릭터 선택 안 함"
            tvSelectedCharacterName.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun showCharacterSelectionDialog() {
        if (characterList.isEmpty()) {
            // No characters available, go to creation screen
            startActivity(CharacterCreationActivity.createIntent(this))
            return
        }

        // Create a simple selection dialog
        val alertDialog = android.app.AlertDialog.Builder(this)
            .setTitle("캐릭터 선택")
            .setItems(characterList.map { it.name }.toTypedArray()) { dialog, which ->
                selectedCharacterIndex = which
                updateSelectedCharacterUI()
                // Update native session with new character selection
                NativeChat.initSession(sessionId, selectedCharacterIndex)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun sendMessage() {
        val message = etMessage.text.toString().trim()
        if (message.isEmpty()) {
            etMessage.error = "메시를 입력해주세요"
            etMessage.requestFocus()
            return
        }

        // Disable UI while sending
        etMessage.isEnabled = false
        btnSend.isEnabled = false

        // Add user message to chat history
        val userMessage = "사용자: $message\n"
        tvChatHistory.append(userMessage)

        // Clear input field
        etMessage.text.clear()

        // Get response from native layer
        lifecycleScope.launch {
            val response = NativeChat.sendMessage(sessionId, message)
            withContext(Dispatchers.Main) {
                val aiMessage = "어시스턴트: $response\n\n"
                tvChatHistory.append(aiMessage)
                // Scroll to bottom using postDelayed for better reliability
                tvChatHistory.post {
                    val scrollY = tvChatHistory.lineCount * tvChatHistory.lineHeight
                    tvChatHistory.scrollTo(0, scrollY)
                }
                // Re-enable UI
                etMessage.isEnabled = true
                btnSend.isEnabled = true
            }
        }
    }
}