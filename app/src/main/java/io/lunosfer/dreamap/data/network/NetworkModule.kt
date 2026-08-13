package io.lunosfer.dreamap.data.network

import io.lunosfer.dreamap.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Base URL: lunosfer.com web app'inin kendisi (Next.js API route'ları burada
 * yaşıyor), Supabase URL'i DEĞİL. BuildConfig.APP_URL .env / .env.example
 * üzerinden Secrets Gradle Plugin ile enjekte edilir — web tarafındaki
 * NEXT_PUBLIC_APP_URL ile aynı konvansiyon (bkz. .env.example'a eklenen satır).
 */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.APP_URL.replace("https://lunosfer.com", "https://www.lunosfer.com"))
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: LunosferApi by lazy { retrofit.create(LunosferApi::class.java) }
}
