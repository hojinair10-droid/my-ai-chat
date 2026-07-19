package com.example.zetachat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.zetachat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up button clicks
        binding.btnChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        binding.btnImage.setOnClickListener {
            val intent = Intent(this, ImageGenerationActivity::class.java)
            startActivity(intent)
        }

        binding.btnFeed.setOnClickListener {
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }

        binding.btnSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        binding.btnRecommendations.setOnClickListener {
            val intent = Intent(this, RecommendationActivity::class.java)
            startActivity(intent)
        }

        binding.btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        binding.btnCharacterCreate.setOnClickListener {
            val intent = Intent(this, CharacterCreationActivity::class.java)
            startActivity(intent)
        }
    }
}