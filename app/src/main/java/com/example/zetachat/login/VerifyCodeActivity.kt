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
import com.example.zetachat.MainActivity
import com.example.zetachat.R
import com.example.zetachat.network.AuthApi
import com.example.zetachat.network.NetworkClient
import com.example.zetachat.network.VerifyCodeRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyCodeActivity : AppCompatActivity() {

    private lateinit var etCode1: EditText
    private lateinit var etCode2: EditText
    private lateinit var etCode3: EditText
    private lateinit var etCode4: EditText
    private lateinit var etCode5: EditText
    private lateinit var etCode6: EditText
    private lateinit var btnVerify: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var authApi: AuthApi
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)

        etCode1 = findViewById(R.id.etCode1)
        etCode2 = findViewById(R.id.etCode2)
        etCode3 = findViewById(R.id.etCode3)
        etCode4 = findViewById(R.id.etCode4)
        etCode5 = findViewById(R.id.etCode5)
        etCode6 = findViewById(R.id.etCode6)
        btnVerify = findViewById(R.id.btnVerify)
        pbLoading = findViewById(R.id.pbLoading)

        authApi = NetworkClient.getAuthApi()

        // Get email from intent
        email = intent.getStringExtra("email") ?: ""

        // Setup OTP auto-move
        setupOtpInputs()

        btnVerify.setOnClickListener {
            val code = etCode1.text.toString() +
                    etCode2.text.toString() +
                    etCode3.text.toString() +
                    etCode4.text.toString() +
                    etCode5.text.toString() +
                    etCode6.text.toString()

            if (code.length != 6) {
                Toast.makeText(this, "Please fill in all 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyCode(email, code)
        }
    }

    private fun setupOtpInputs() {
        val editTexts = arrayOf(etCode1, etCode2, etCode3, etCode4, etCode5, etCode6)

        // Move to next field when current is filled
        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < editTexts.lastIndex) {
                        editTexts[i + 1].requestFocus()
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // Move to previous field when deleting and current is empty
        for (i in editTexts.indices) {
            editTexts[i].setOnKeyListener { v, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                        event.action == android.view.KeyEvent.ACTION_DOWN &&
                        editTexts[i].text.isEmpty() &&
                        i > 0) {
                    editTexts[i - 1].requestFocus()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    private fun verifyCode(email: String, code: String) {
        pbLoading.visibility = View.VISIBLE
        btnVerify.isEnabled = false

        val call = authApi.verifyCode(VerifyCodeRequest(email, code))
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                pbLoading.visibility = View.GONE
                btnVerify.isEnabled = true
                if (response.isSuccessful()) {
                    // Save email as logged-in user (or however you manage session)
                    // For simplicity, we'll just go to main activity
                    val intent = Intent(this@VerifyCodeActivity, MainActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@VerifyCodeActivity, "Invalid code: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                pbLoading.visibility = View.GONE
                btnVerify.isEnabled = true
                Toast.makeText(this@VerifyCodeActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }
}