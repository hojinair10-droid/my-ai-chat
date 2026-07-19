package com.example.zetachat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zetachat.network.NetworkClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar pbLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        pbLoading = findViewById(R.id.pbLoading);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("유효한 이메일을 입력하세요");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("비밀번호를 입력하세요");
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // 사용자 인증 API 호출 (예시)
        NetworkClient.getInstance().login(email, password, new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                pbLoading.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    // 토큰 저장 등 메인 액티비티로 이동
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "로그인 실패: " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}