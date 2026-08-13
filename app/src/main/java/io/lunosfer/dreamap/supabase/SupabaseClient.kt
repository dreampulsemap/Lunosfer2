package io.lunosfer.dreamap.supabase

import io.lunosfer.dreamap.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

val supabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth) {
        scheme = "io.lunosfer.dreamap"
        host = "auth-callback"
    }
    install(Postgrest)
    install(Storage)
}
