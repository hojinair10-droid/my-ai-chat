package com.example.zetachat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.zetachat.data.AppDatabase
import com.example.zetachat.data.CharacterEntity
import com.example.zetachat.data.CharacterDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CharacterCreationActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var characterDao: CharacterDao

    // View bindings (we'll use findViewById for simplicity)
    private lateinit var etName: android.widget.EditText
    private lateinit var etAppearance: android.widget.EditText
    private lateinit var etPersonality: android.widget.EditText
    private lateinit var etSpeakingStyle: android.widget.EditText
    private lateinit var etWorldview: android.widget.EditText
    private lateinit var etScenarioPrompt: android.widget.EditText
    private lateinit var btnSaveCharacter: com.google.android.material.button.MaterialButton

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, CharacterCreationActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_creation)

        // Initialize database
        database = AppDatabase.getInstance(this)
        characterDao = database.characterDao()

        // Initialize views
        etName = findViewById(R.id.etName)
        etAppearance = findViewById(R.id.etAppearance)
        etPersonality = findViewById(R.id.etPersonality)
        etSpeakingStyle = findViewById(R.id.etSpeakingStyle)
        etWorldview = findViewById(R.id.etWorldview)
        etScenarioPrompt = findViewById(R.id.etScenarioPrompt)
        btnSaveCharacter = findViewById(R.id.btnSaveCharacter)

        // Set up back button (if using ActionBar, but we don't have one in this layout)
        // We'll rely on the system back button

        btnSaveCharacter.setOnClickListener {
            saveCharacter()
        }
    }

    private fun saveCharacter() {
        val name = etName.text.toString().trim()
        val appearance = etAppearance.text.toString().trim()
        val personality = etPersonality.text.toString().trim()
        val speakingStyle = etSpeakingStyle.text.toString().trim()
        val worldview = etWorldview.text.toString().trim()
        val scenarioPrompt = etScenarioPrompt.text.toString().trim()

        if (TextUtils.isEmpty(name)) {
            etName.error = "이름을 입력해주세요"
            etName.requestFocus()
            return
        }

        // Show loading indicator (optional)
        // We'll disable the button and show a toast when done
        btnSaveCharacter.isEnabled = false

        // Save to database and native layer on a background thread
        lifecycleScope.launch {
            val characterEntity = CharacterEntity(
                name = name,
                appearance = appearance,
                personality = personality,
                speakingStyle = speakingStyle,
                worldview = worldview,
                scenarioPrompt = scenarioPrompt,
                isPublic = false, // Default to private; we don't have a toggle in UI yet
                nativeIndex = -1
            )

            // Insert into database to get the ID
            val id = characterDao.insert(characterEntity)

            // Add to native vector and get the index
            val nativeIndex = NativeChat.addCharacter(
                name,
                appearance,
                personality,
                speakingStyle,
                worldview,
                scenarioPrompt,
                false // isPublic
            )

            // Update the entity with the native index
            val updatedEntity = characterEntity.copy(id = id, nativeIndex = nativeIndex)
            characterDao.updateNativeIndex(id, nativeIndex)

            // Return to main thread to show toast and finish
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CharacterCreationActivity,
                    "캐릭터가 저장되었습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                btnSaveCharacter.isEnabled = true
                finish()
            }
        }
    }
}