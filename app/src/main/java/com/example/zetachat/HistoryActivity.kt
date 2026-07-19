package com.example.zetachat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(generateSampleHistory())
        recyclerView.adapter = adapter
    }

    private fun generateSampleHistory(): List<HistoryItem> {
        return listOf(
            HistoryItem("친구처럼 대화하는 AI", "오늘 날씨가 정말 좋네요! 🌞\n네, 정말 기분 좋은 날씨예요. 뭐하고 계신가요?", "2시간 전"),
            HistoryItem("지식인이 되는 길", "양자역학에 대해 설명해줄 수 있나요?\n물론이죠! 양자역학은 미세한 입자들의 행동을 다루는 물리학의 분야입니다...", "5시간 전"),
            HistoryItem("유머러스한 조언자", "오늘 점심 뭐 먹지?\n매콤한 떡볶이는 어때요? 스트레스 확 풀릴 거예요!", "1일 전"),
            HistoryItem("창의적인 스토리 메이커", "우주 여행을 주제로 한 단편 소설의 첫 문장을 써줘.\n\"은하계 끝 hes의 작은 행성에서, 나는 신호를 기다렸다...\"", "2일 전"),
            HistoryItem("차분한 상담가", "요즘 마음이 조금 무거워요...\n어떤 것이قلق을 유발하고 있나요? 편안하게 이야기해봐요.", "3일 전")
        )
    }

    // Adapter for history items
    inner class HistoryAdapter(private val historyItems: List<HistoryItem>) :
        RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: android.widget.TextView = itemView.findViewById(R.id.tvTitle)
            val preview: android.widget.TextView = itemView.findViewById(R.id.tvPreview)
            val time: android.widget.TextView = itemView.findViewById(R.id.tvTime)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HistoryViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val item = historyItems[position]
            holder.title.text = item.title
            holder.preview.text = item.preview
            holder.time.text = item.time
        }

        override fun getItemCount(): Int = historyItems.size
    }

    // Data class for a history item
    data class HistoryItem(
        val title: String,
        val preview: String,
        val time: String
    )
}