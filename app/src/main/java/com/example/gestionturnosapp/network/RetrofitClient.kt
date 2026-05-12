package com.example.gestionturnosapp.network

import com.example.gestionturnosapp.data.UserManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // URL de Render
    private const val BASE_URL = "https://saludactiva-backend.onrender.com/"
    // URL Local para pruebas rápidas
    // private const val BASE_URL = "http://192.168.40.7:3000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            
            // Intentamos obtener el token de memoria o de SharedPreferences
            val token = UserManager.token ?: UserManager.getToken()
            
            token?.let {
                builder.addHeader("Authorization", "Bearer $it")
            }
            
            val request = builder.build()
            chain.proceed(request)
        }
        .retryOnConnectionFailure(true)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
