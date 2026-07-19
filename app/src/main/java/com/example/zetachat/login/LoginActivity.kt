package com.example.zetachat.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.zetachat.R
import com.example.zetachat.network.AuthApi
import com.example.zetachat.network.NetworkClient
import com.example.zetachat.network.SendCodeRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnSendCode: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var authApi: AuthApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        btnSendCode = findViewById(R.id.btnSendCode)
        pbLoading = findViewById(R.id.pbLoading)
        authApi = NetworkClient.getAuthApi()

        // 이메일 형식 실시간 검사
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnSendCode.isEnabled = android.util.Patterns.EMAIL_ADDRESS.matcher(s ?: "").matches()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                etEmail.error = "이메일을 입력하세요"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "유효한 이메일 형식이 아닙니다"
                return@setOnClickListener
            }

            sendVerificationCode(email)
        }
    }

    private fun sendVerificationCode(email: String) {
        pbLoading.visibility = View.VISIBLE
        btnSendCode.isEnabled = false

        val call = authApi.sendEmailCode(SendCodeRequest(email))
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                pbLoading.visibility = View.GONE
                btnSendCode.isEnabled = true
                if (response.isSuccessful()) {
                    Toast.makeText(this@LoginActivity, "인증코드가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, VerifyCodeActivity::class.java).apply {
                        putExtra("email", email)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@LoginActivity, "오류: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                pbLoading.visibility = View.GONE
                btnSendCode.isEnabled = true
                Toast.makeText(this@LoginActivity, "네트워크 오류: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }
}