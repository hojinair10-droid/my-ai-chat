package com.example.zetachat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecommendationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecommendationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecommendationAdapter(generateSampleRecommendations())
        recyclerView.adapter = adapter
    }

    private fun generateSampleRecommendations(): List<Recommendation> {
        return listOf(
            Recommendation("친구처럼 대화하는 AI", "일상 대화에 딱 맞는 친근한 성격", 4.8),
            Recommendation("지식인이 되는 길", "깊이 있는 이야기를 나누고 싶을 때", 4.6),
            Recommendation("유머러스한 조언자", "웃음과 지혜를 동시에", 4.9),
            Recommendation("창의적인 스토리 메이커", "함께 이야기를 만들어가는 재미", 4.7),
            Recommendation("차분한 상담가", "마음의 위로가 필요할 때", 4.5)
        )
    }

    // Adapter for recommendations
    inner class RecommendationAdapter(private val recommendations: List<Recommendation>) :
        RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder>() {

        inner class RecommendationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: android.widget.TextView = itemView.findViewById(R.id.tvTitle)
            val description: android.widget.TextView = itemView.findViewById(R.id.tvDescription)
            val rating: android.widget.TextView = itemView.findViewById(R.id.tvRating)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecommendationViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recommendation, parent, false)
            return RecommendationViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
            val recommendation = recommendations[position]
            holder.title.text = recommendation.title
            holder.description.text = recommendation.description
            val ratingBar = "★".repeat((recommendation.rating).toInt()) + "☆".repeat(5 - (recommendation.rating).toInt())
            holder.rating.text = ratingBar
        }

        override fun getItemCount(): Int = recommendations.size
    }

    // Data class for a recommendation
    data class Recommendation(
        val title: String,
        val description: String,
        val rating: Double
    )
}