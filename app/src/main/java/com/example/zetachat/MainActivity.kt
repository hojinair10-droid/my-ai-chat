package com.example.zetachat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up button clicks
        findViewById<Button>(R.id.btnChat).setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnImage).setOnClickListener {
            val intent = Intent(this, ImageGenerationActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnFeed).setOnClickListener {
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnRecommendations).setOnClickListener {
            val intent = Intent(this, RecommendationActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnCharacterCreate).setOnClickListener {
            val intent = Intent(this, CharacterCreationActivity::class.java)
            startActivity(intent)
        }
    }
}