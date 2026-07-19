package com.example.zetachat.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class SendCodeRequest(val email: String)
data class VerifyCodeRequest(val email: String, val code: String)

interface AuthApi {
    @POST("auth/send-code")
    fun sendEmailCode(@Body request: SendCodeRequest): Call<Void>
    
    @POST("auth/verify-code")
    fun verifyCode(@Body request: VerifyCodeRequest): Call<Void>
}
