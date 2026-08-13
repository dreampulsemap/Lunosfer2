package io.lunosfer.dreamap.data.network

import io.lunosfer.dreamap.data.model.ConversationsResponse
import io.lunosfer.dreamap.data.model.DreamsFeedResponse
import io.lunosfer.dreamap.data.model.ExploreFeedResponse
import io.lunosfer.dreamap.data.model.GoalsListResponse
import io.lunosfer.dreamap.data.model.ThreadResponse
import io.lunosfer.dreamap.data.model.UnreadCountResponse
import io.lunosfer.dreamap.data.model.VisionsFeedResponse
import io.lunosfer.dreamap.data.model.AnalyzeDreamRequest
import io.lunosfer.dreamap.data.model.SendMessageRequest
import io.lunosfer.dreamap.data.model.SendMessageResponse
import io.lunosfer.dreamap.data.model.ReactMessageRequest
import io.lunosfer.dreamap.data.model.PushSubscriptionRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.HTTP
import retrofit2.http.Query
import retrofit2.http.Path

/**
 * lunosfer.com'daki Next.js API route'larına  karşılık gelen
 * Retrofit arayüzü. Base URL BuildConfig.SUPABASE_URL DEĞİL, web app'in
 * kendi domain'i olmalı (bkz. NetworkModule.kt) — bu route'lar Supabase'e
 * service-role client ile server tarafında bağlanıyor, cihazdan doğrudan
 * Supabase'e gidilmiyor.
 *
 * Auth: her istek AuthInterceptor tarafından enjekte edilen
 * "Authorization: Bearer <supabase_access_token>" header'ına ihtiyaç duyar
 * (bkz. lib/supabaseAdmin.js getAuthedUser). Girişsiz kullanıcı için token
 * yoksa interceptor header'ı hiç eklemez; sunucu tarafı buna göre ya misafir
 * modunda (yalnız public içerik) davranır ya da 401 döner (mode=own gibi).
 *
 * ÖNEMLİ: Retrofit'in Kotlin interface'leri implement etmek için kullandığı
 * dinamik proxy, Kotlin default parametre değerlerini (metod imzasına
 * gömülen sentetik $default çağrılarını) doğru işlemez ve runtime'da
 * NoSuchMethodError/UnsupportedOperationException fırlatabilir. Bu yüzden
 * BURADA hiçbir parametre default DEĞERE sahip değil — tüm defaultlar
 * çağıran taraf olan Repository sınıflarında (data/repository/) verilir.
 */
interface LunosferApi {

    // --- Home  ---
    // type=dreams ve type=visions ayrı çağrılıyor (bkz. HomeFeed.kt açıklaması).

    @GET("api/home-feed")
    suspend fun getHomeDreams(
        @Query("type") type: String,
        @Query("dreamsBefore") dreamsBefore: String?
    ): DreamsFeedResponse

    @GET("api/home-feed")
    suspend fun getHomeVisions(
        @Query("type") type: String,
        @Query("visionsBefore") visionsBefore: String?
    ): VisionsFeedResponse

    // --- Explore  ---

    @GET("api/explore/feed")
    suspend fun getExploreFeed(
        @Query("page") page: Int,
        @Query("rankToken") rankToken: String?,
        @Query("asOf") asOf: String?
    ): ExploreFeedResponse

    // --- Vision / Goals  ---
    // mode=feed: genel keşfet akışı (yalnızca public). "Vizyon" sekmesi bunu kullanıyor.

    @GET("api/goals/list")
    suspend fun getGoalsFeed(
        @Query("mode") mode: String,
        @Query("page") page: Int,
        @Query("status") status: String?
    ): GoalsListResponse

    // --- Messages  ---

    @GET("api/messages/conversations")
    suspend fun getConversations(): ConversationsResponse

    @GET("api/messages/thread")
    suspend fun getThread(
        @Query("with") otherUserId: String,
        @Query("before") before: String?
    ): ThreadResponse

    @GET("api/messages/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @POST("api/messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendMessageResponse

    @POST("api/messages/react")
    suspend fun reactMessage(@Body request: ReactMessageRequest)

    @POST("api/push/subscribe")
    suspend fun subscribePush(@Body request: PushSubscriptionRequest)

    @POST("api/analyze-dream")
    suspend fun analyzeDream(@Body request: AnalyzeDreamRequest)

    @GET("api/get-dream")
    suspend fun getDream(@Query("id") id: Long): io.lunosfer.dreamap.data.model.DreamDetailResponse


    @GET("api/pixabay/search")
    suspend fun searchPixabay(@Query("q") query: String): io.lunosfer.dreamap.data.model.PixabaySearchResponse

    @POST("api/dreams/pixabay-image")
    suspend fun savePixabayImage(@Body request: io.lunosfer.dreamap.data.model.PixabayImageRequest): io.lunosfer.dreamap.data.model.PixabayImageResponse

    // --- Dream Interactions ---

    @POST("api/like")
    suspend fun likeDream(@Body request: io.lunosfer.dreamap.data.model.LikeRequest): io.lunosfer.dreamap.data.model.LikeResponse

    @HTTP(method = "DELETE", path = "api/like", hasBody = true)
    suspend fun unlikeDream(@Body request: io.lunosfer.dreamap.data.model.LikeRequest): io.lunosfer.dreamap.data.model.LikeResponse

    @GET("api/comment")
    suspend fun getComments(@Query("dreamId") dreamId: Long): io.lunosfer.dreamap.data.model.CommentsResponse

    @POST("api/comment")
    suspend fun createComment(@Body request: io.lunosfer.dreamap.data.model.CreateCommentRequest): io.lunosfer.dreamap.data.model.CreateCommentResponse

    @HTTP(method = "DELETE", path = "api/comment", hasBody = true)
    suspend fun deleteComment(@Body request: io.lunosfer.dreamap.data.model.DeleteCommentRequest): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    @PUT("api/update-dream")
    suspend fun updateDream(@Body request: io.lunosfer.dreamap.data.model.UpdateDreamRequest): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    @HTTP(method = "DELETE", path = "api/delete-dream", hasBody = true)
    suspend fun deleteDream(@Body request: io.lunosfer.dreamap.data.model.DeleteDreamRequest): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    @POST("api/boost-dream")
    suspend fun boostDream(@Body request: io.lunosfer.dreamap.data.model.BoostDreamRequest): io.lunosfer.dreamap.data.model.BoostDreamResponse

    @POST("api/add-bounty")
    suspend fun addBounty(@Body request: io.lunosfer.dreamap.data.model.AddBountyRequest): io.lunosfer.dreamap.data.model.AddBountyResponse

    // --- Goal / Vision Interactions ---

    @POST("api/goals/create")
    suspend fun createGoal(@Body request: io.lunosfer.dreamap.data.model.CreateGoalRequest): io.lunosfer.dreamap.data.model.CreateGoalResponse

    @POST("api/goals/update-status")
    suspend fun updateGoalStatus(@Body request: io.lunosfer.dreamap.data.model.UpdateGoalStatusRequest): io.lunosfer.dreamap.data.model.UpdateGoalStatusResponse

    @HTTP(method = "DELETE", path = "api/goals/delete", hasBody = true)
    suspend fun deleteGoal(@Body request: io.lunosfer.dreamap.data.model.DeleteGoalRequest): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    @POST("api/goals/save")
    suspend fun saveGoal(@Body request: io.lunosfer.dreamap.data.model.SaveGoalRequest): io.lunosfer.dreamap.data.model.SaveGoalResponse

    @POST("api/goals/give-mana")
    suspend fun giveMana(@Body request: io.lunosfer.dreamap.data.model.GiveManaRequest): io.lunosfer.dreamap.data.model.GiveManaResponse

    @HTTP(method = "DELETE", path = "api/goals/give-mana", hasBody = true)
    suspend fun removeMana(@Body request: io.lunosfer.dreamap.data.model.DeleteGoalRequest): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    @GET("api/goals/comment")
    suspend fun getGoalComments(@Query("goalId") goalId: String): io.lunosfer.dreamap.data.model.GoalCommentsResponse

    @POST("api/goals/comment")
    suspend fun createGoalComment(@Body request: io.lunosfer.dreamap.data.model.CreateGoalCommentRequest): io.lunosfer.dreamap.data.model.CreateGoalCommentResponse

    @HTTP(method = "DELETE", path = "api/goals/comment", hasBody = true)
    suspend fun deleteGoalComment(@Body request: io.lunosfer.dreamap.data.model.DeleteGoalCommentRequest): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    // --- Friends / Social ---

    @GET("api/friends/list")
    suspend fun getFriendsList(
        @Query("userId") userId: String,
        @Query("type") type: String?
    ): io.lunosfer.dreamap.data.model.FriendsListResponse

    @POST("api/friends/request")
    suspend fun sendFriendRequest(
        @Body request: io.lunosfer.dreamap.data.model.FriendRequestInput
    ): io.lunosfer.dreamap.data.model.FriendRequestResponse

    @PUT("api/friends/respond")
    suspend fun respondToFriendRequest(
        @Body request: io.lunosfer.dreamap.data.model.FriendRespondInput
    ): io.lunosfer.dreamap.data.model.FriendRespondResponse

    @GET("api/friends/search")
    suspend fun searchFriends(
        @Query("query") query: String,
        @Query("userId") userId: String
    ): io.lunosfer.dreamap.data.model.UserSearchResponse

    // --- Notifications ---

    @GET("api/notifications")
    suspend fun getNotifications(): io.lunosfer.dreamap.data.model.NotificationsResponse

    @POST("api/notifications")
    suspend fun markNotificationsRead(
        @Body request: io.lunosfer.dreamap.data.model.MarkNotificationReadInput
    ): io.lunosfer.dreamap.data.model.MarkNotificationReadResponse

    // --- Public Profile ---

    @GET("api/public-profile/{userId}")
    suspend fun getPublicProfile(
        @Path("userId") userId: String,
        @Query("page") page: Int
    ): io.lunosfer.dreamap.data.model.PublicProfileResponse

    // --- Profile & Premium ---

    @PUT("api/update-profile")
    suspend fun updateProfile(
        @Body request: io.lunosfer.dreamap.data.model.UpdateProfileRequest
    ): io.lunosfer.dreamap.data.model.UpdateProfileResponse

    @GET("api/user/premium-status")
    suspend fun getPremiumStatus(): io.lunosfer.dreamap.data.model.PremiumStatusResponse

    // --- Diary / Stories ---

    @GET("api/diary/feed")
    suspend fun getDiaryFeed(): io.lunosfer.dreamap.data.model.DiaryFeedResponse

    @GET("api/diary/list-for-user")
    suspend fun getDiaryListForUser(
        @Query("userId") userId: String
    ): io.lunosfer.dreamap.data.model.DiaryListResponse

    @POST("api/diary/mark-seen")
    suspend fun markDiarySeen(
        @Body input: io.lunosfer.dreamap.data.model.MarkDiarySeenInput
    ): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    @POST("api/diary/create")
    suspend fun createDiaryEntry(
        @Body input: io.lunosfer.dreamap.data.model.CreateDiaryInput
    ): io.lunosfer.dreamap.data.model.CreateDiaryResponse

    @POST("api/diary/delete")
    suspend fun deleteDiaryEntry(
        @Body input: io.lunosfer.dreamap.data.model.DeleteDiaryInput
    ): io.lunosfer.dreamap.data.model.GenericSuccessResponse

    // --- Deep Analysis & Compass & Daily Seeds ---

    @POST("api/generate-deep-analysis")
    suspend fun generateDeepAnalysis(
        @Body request: io.lunosfer.dreamap.data.model.GenerateDeepAnalysisRequest
    ): io.lunosfer.dreamap.data.model.GenerateDeepAnalysisResponse

    @POST("api/daily-compass")
    suspend fun getDailyCompass(
        @Body request: io.lunosfer.dreamap.data.model.DailyCompassRequest
    ): io.lunosfer.dreamap.data.model.DailyCompassResponse

    @GET("api/daily-seeds/complete")
    suspend fun getDailySeeds(): io.lunosfer.dreamap.data.model.DailySeedsResponse

    @POST("api/daily-seeds/generate")
    suspend fun generateDailySeed(
        @Body request: io.lunosfer.dreamap.data.model.GenerateSeedRequest
    ): io.lunosfer.dreamap.data.model.GenerateSeedResponse

    @POST("api/daily-seeds/complete")
    suspend fun completeDailySeed(
        @Body request: io.lunosfer.dreamap.data.model.CompleteSeedRequest
    ): io.lunosfer.dreamap.data.model.CompleteSeedResponse

    // --- Spiritual Tools (Mental Wall, Psyche Map, Prophet) ---

    @POST("api/mental-wall/generate")
    suspend fun generateMentalWall(
        @Body request: io.lunosfer.dreamap.data.model.MentalWallRequest
    ): io.lunosfer.dreamap.data.model.MentalWallResponse

    @GET("api/psyche-map")
    suspend fun getPsycheMap(): io.lunosfer.dreamap.data.model.PsycheMapResponse

    @POST("api/prophet")
    suspend fun consultProphet(
        @Body request: io.lunosfer.dreamap.data.model.ProphetRequest
    ): io.lunosfer.dreamap.data.model.ProphetResponse

    // --- AI Summaries (Weekly / Monthly) ---

    @POST("api/summaries/generate")
    suspend fun generateSummary(
        @Body request: io.lunosfer.dreamap.data.model.GenerateSummaryRequest
    ): io.lunosfer.dreamap.data.model.SummaryResponse

    @GET("api/summaries/latest")
    suspend fun getLatestSummary(
        @Query("periodType") periodType: String
    ): io.lunosfer.dreamap.data.model.SummaryResponse

    // --- Vision Cover & Gallery Management ---

    @POST("api/goals/generate-cover")
    suspend fun generateGoalCover(
        @Body request: io.lunosfer.dreamap.data.model.GenerateGoalCoverRequest
    ): io.lunosfer.dreamap.data.model.GoalCoverResponse

    @POST("api/goals/add-image-from-pixabay")
    suspend fun addGoalImageFromPixabay(
        @Body request: io.lunosfer.dreamap.data.model.GoalPixabayImageRequest
    ): io.lunosfer.dreamap.data.model.GoalImageResponse

    @POST("api/goals/add-image")
    suspend fun addGoalImage(
        @Body request: io.lunosfer.dreamap.data.model.GoalAddImageRequest
    ): io.lunosfer.dreamap.data.model.GoalImageResponse

    @POST("api/goals/set-cover")
    suspend fun setGoalCover(
        @Body request: io.lunosfer.dreamap.data.model.GoalSetCoverRequest
    ): io.lunosfer.dreamap.data.model.GoalCoverResponse

    @HTTP(method = "DELETE", path = "api/goals/remove-image", hasBody = true)
    suspend fun removeGoalImage(
        @Body request: io.lunosfer.dreamap.data.model.GoalRemoveImageRequest
    ): io.lunosfer.dreamap.data.model.GoalImageResponse

    // --- Referral System ---

    @GET("api/referrals/stats")
    suspend fun getReferralStats(): io.lunosfer.dreamap.data.model.ReferralStatsResponse

    @POST("api/referrals/claim")
    suspend fun claimReferral(
        @Body request: io.lunosfer.dreamap.data.model.ClaimReferralRequest
    ): io.lunosfer.dreamap.data.model.ClaimReferralResponse

    // --- Additional Pixabay Endpoints ---

    @POST("api/pixabay/import-image")
    suspend fun importPixabayImage(
        @Body request: io.lunosfer.dreamap.data.model.PixabayImageRequest
    ): io.lunosfer.dreamap.data.model.PixabayImageResponse

    @POST("api/pixabay/import-video")
    suspend fun importPixabayVideo(
        @Body request: io.lunosfer.dreamap.data.model.PixabayVideoImportRequest
    ): io.lunosfer.dreamap.data.model.PixabayVideoImportResponse

    @GET("api/pixabay/search-videos")
    suspend fun searchPixabayVideos(
        @Query("q") query: String
    ): io.lunosfer.dreamap.data.model.PixabayVideoSearchResponse

    // --- Translation ---

    @POST("api/translate")
    suspend fun translateText(
        @Body request: io.lunosfer.dreamap.data.model.TranslateRequest
    ): io.lunosfer.dreamap.data.model.TranslateResponse
}

