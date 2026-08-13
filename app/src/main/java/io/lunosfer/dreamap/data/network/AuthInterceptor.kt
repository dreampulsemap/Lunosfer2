package io.lunosfer.dreamap.data.network

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.lunosfer.dreamap.BuildConfig
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Her isteğe "Authorization: Bearer <access_token>" ve "apikey: <SUPABASE_ANON_KEY>" ekler.
 *
 * Supabase client başlatılırken (app açılışında) sessionStatus varsayılan olarak `Initializing` durumundadır.
 * Senkron OkHttp Interceptor içinde eğer status `Initializing` ise, Supabase session'ın yerel hafızadan
 * yüklenmesini (max 5 saniye) runBlocking ile bekliyoruz. Böylece giriş yapılmış kullanıcının token'ı
 * yarış durumuna (race condition) takılmadan doğru şekilde okunur.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var status = supabaseClient.auth.sessionStatus.value

        if (status is SessionStatus.Initializing) {
            Log.d("AuthInterceptor", "Session is Initializing, waiting for session status...")
            runBlocking {
                withTimeoutOrNull(5000) {
                    supabaseClient.auth.sessionStatus.first { it !is SessionStatus.Initializing }
                }
            }
            status = supabaseClient.auth.sessionStatus.value
        }

        val statusToken = (status as? SessionStatus.Authenticated)?.session?.accessToken
        val sessionToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
        val token = statusToken ?: sessionToken
        val hasToken = token != null
        val tokenPrefix = token?.take(20) ?: "null"

        Log.d("AuthInterceptor", "Final sessionStatus: $status, token present: $hasToken, token snippet: $tokenPrefix")

        val builder = chain.request().newBuilder()

        if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY != "dummy") {
            builder.header("apikey", BuildConfig.SUPABASE_ANON_KEY)
        }

        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        var response = chain.proceed(builder.build())

        if (response.code == 401 && token != null) {
            Log.w("AuthInterceptor", "HTTP 401 received. Attempting session refresh...")
            val newToken = runBlocking {
                try {
                    supabaseClient.auth.refreshCurrentSession()
                    supabaseClient.auth.currentSessionOrNull()?.accessToken
                } catch (e: Exception) {
                    Log.e("AuthInterceptor", "Session refresh failed", e)
                    null
                }
            }

            if (newToken != null && newToken != token) {
                Log.d("AuthInterceptor", "Session refreshed. Retrying request...")
                response.close()
                val retryBuilder = chain.request().newBuilder()
                if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY != "dummy") {
                    retryBuilder.header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                }
                retryBuilder.header("Authorization", "Bearer $newToken")
                response = chain.proceed(retryBuilder.build())
            }
        }

        return response
    }
}

