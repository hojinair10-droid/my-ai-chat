package com.example.zetachat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat

class SearchActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchResultAdapter

    // Dummy search results
    private val results = listOf(
        SearchResult("Java 프로그래밍入门", "초보자를 위한 자바 기초 강의", "개발·코딩"),
        SearchResult("안드로이드 스튜디오 완벽 가이드", "스마트폰 앱 개발부터 출판까지", "개발·도구"),
        SearchResult("머신러닝 수학 기초", "선형대수, 확률, 통계 기초", "데이터사이언스"),
        SearchResult("React로 만드는 SPA", "현대적 프론트엔드 개발 입문", "프론트엔드"),
        SearchResult("유튜브 영상 편집 기초", "프리미어 프로로 시작하는 편집", "미디어·편집")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SearchResultAdapter(results)
        recyclerView.adapter = adapter
    }

    // Adapter for search results
    inner class SearchResultAdapter(private val results: List<SearchResult>) :
        RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

        inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.tvTitle)
            val description: TextView = itemView.findViewById(R.id.tvDescription)
            val category: TextView = itemView.findViewById(R.id.tvCategory) // assuming we add this
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
            return SearchResultViewHolder(view)
        }

        override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
            val result = results[position]
            holder.title.text = result.title
            holder.description.text = result.description
            // Note: item_search_result.xml currently doesn't have a category TextView.
            // We'll need to update the layout or ignore for now.
            // For simplicity, let's assume we add it later or use description for both.
        }

        override fun getItemCount(): Int = results.size
    }

    // Data class for a search result
    data class SearchResult(
        val title: String,
        val description: String,
        val category: String
    )
}