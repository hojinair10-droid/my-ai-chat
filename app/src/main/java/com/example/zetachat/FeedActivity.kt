package com.example.zetachat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class FeedActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FeedAdapter(generateSamplePosts())
        recyclerView.adapter = adapter
    }

    private fun generateSamplePosts(): List<FeedPost> {
        return listOf(
            FeedPost("Alice", "오늘 날씨가 정말 좋네요! 🌞", "2시간 전"),
            FeedPost("Bob", "새로운 취미를 시작했는데, 정말 재미있어요!", "5시간 전"),
            FeedPost("Charlie", "주말에 뭐할지 고민 중이에요...", "1일 전")
        )
    }

    // Inner class for the adapter
    inner class FeedAdapter(private val posts: List<FeedPost>) :
        RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

        inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val authorText: TextView = itemView.findViewById(R.id.tvAuthor)
            val contentText: TextView = itemView.findViewById(R.id.tvContent)
            val timeText: TextView = itemView.findViewById(R.id.tvTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_post, parent, false)
            return PostViewHolder(view)
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = posts[position]
            holder.authorText.text = post.author
            holder.contentText.text = post.content
            holder.timeText.text = post.timestamp
        }

        override fun getItemCount(): Int = posts.size
    }

    // Data class for a post
    data class FeedPost(val author: String, val content: String, val timestamp: String)
}