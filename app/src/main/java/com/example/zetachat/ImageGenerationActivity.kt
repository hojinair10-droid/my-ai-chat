package com.example.zetachat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageGenerationActivity : AppCompatActivity() {

    private lateinit var etPrompt: EditText
    private lateinit var btnGenerate: Button
    private lateinit var ivResult: ImageView
    private lateinit var pbLoading: ProgressBar

    private val apiKey = System.getenv("OPENROUTER_API_KEY") ?: ""
    private val apiUrl = "https://openrouter.ai/api/v1/images/generations"
    private val model = "stable-diffusion-xl-base-1.0" // you can change to other image models available on OpenRouter
    private val gson = Gson()
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_generation)

        etPrompt = findViewById(R.id.etPrompt)
        btnGenerate = findViewById(R.id.btnGenerate)
        ivResult = findViewById(R.id.ivResult)
        pbLoading = findViewById(R.id.pbLoading)

        btnGenerate.setOnClickListener {
            val prompt = etPrompt.text.toString().trim()
            if (prompt.isEmpty()) {
                etPrompt.error = "프롬프트를 입력해주세요"
                etPrompt.requestFocus()
                return@setOnClickListener
            }

            generateImage(prompt)
        }
    }

    private fun generateImage(prompt: String) {
        showLoading(true)

        // Execute network request on IO dispatcher
        lifecycleScope.launch {
            try {
                val response = async(Dispatchers.IO) {
                    val json = gson.toJson(ImageRequest(model, prompt, 1, "1024x1024", "url"))
                    val mediaType = "application/json".toMediaType()
                    val body = okhttp3.RequestBody.create(mediaType, json)
                    val request = Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    httpClient.newCall(request).execute()
                }

                val images = async(Dispatchers.IO) {
                    val resp = response.await()
                    if (!resp.isSuccessful) {
                        throw Exception("Error ${resp.code}: ${resp.body?.string()}")
                    }
                    val responseBody = resp.body?.string() ?: throw Exception("Empty response")
                    val imageResponse = gson.fromJson(responseBody, ImageResponse::class.java)
                    imageResponse
                }

                // Wait for response
                val imageResponse = images.await()

                // Process response on main thread
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (imageResponse.data.isNotEmpty()) {
                        val imageData = imageResponse.data[0]
                        if (imageData.url != null && !imageData.url.isEmpty()) {
                            // Load image via Glide from URL
                            Glide.with(this@ImageGenerationActivity)
                                .load(imageData.url)
                                .placeholder(android.R.drawable.sym_def_app_icon) // placeholder
                                .error(android.R.drawable.ic_dialog_alert) // error placeholder
                                .into(ivResult)
                            Toast.makeText(this@ImageGenerationActivity, "이미지 생성 완료!", Toast.LENGTH_SHORT).show()
                        } else if (imageData.b64_json != null && !imageData.b64_json.isEmpty()) {
                            // Decode base64 string to Bitmap
                            val decodedBytes = Base64.decode(imageData.b64_json, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            ivResult.setImageBitmap(bitmap)
                            Toast.makeText(this@ImageGenerationActivity, "이미지 생성 완료! (Base64)", Toast.LENGTH_SHORT).show()
                        } else {
                            showError("이미지 데이터를 받지 못했습니다.")
                        }
                    } else {
                        showError("이미지 데이터를 받지 못했습니다.")
                    }
                }
            } catch (e: Exception) {
                showError("이미지 생성 실패: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        pbLoading.visibility = if (show) View.VISIBLE else View.GONE
        btnGenerate.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // Data classes for request/response
    private data class ImageRequest(
        val model: String,
        val prompt: String,
        val n: Int,
        val size: String,
        val response_format: String
    )

    private data class ImageResponse(
        val data: List<ImageData>
    )

    private data class ImageData(
        val url: String?,
        val b64_json: String?
    )
}