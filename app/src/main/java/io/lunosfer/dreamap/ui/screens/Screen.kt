package io.lunosfer.dreamap.ui.screens

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Main : Screen("main") // Container for bottom nav screens
    
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Vision : Screen("vision")
    object Messages : Screen("messages")

    /** Route şablonu {otherUserId} taşır. Navigasyon için Thread.routeFor(id) kullan. */
    object Thread : Screen("thread/{otherUserId}") {
        fun routeFor(otherUserId: String) = "thread/$otherUserId"
    }

    object CreateDream : Screen("create_dream")
    object CreateVision : Screen("create_vision")
    object Profile : Screen("profile")
    object DreamDetail : Screen("dream/{dreamId}") {
        fun createRoute(dreamId: Long) = "dream/$dreamId"
    }
    object GoalDetail : Screen("goal/{goalId}") {
        fun createRoute(goalId: String) = "goal/$goalId"
    }
    object AddFriend : Screen("add_friend")
    object Notifications : Screen("notifications")
    object PublicProfile : Screen("public_profile/{userId}") {
        fun createRoute(userId: String) = "public_profile/$userId"
    }
    object DiaryComposer : Screen("diary_composer")
    object DiaryStoryViewer : Screen("diary_viewer/{userId}") {
        fun routeFor(userId: String) = "diary_viewer/$userId"
    }
    object DiaryJournal : Screen("diary_journal/{userId}") {
        fun routeFor(userId: String) = "diary_journal/$userId"
    }

    // "Vizyonu İzle" — GoalDetailScreen'deki tek giriş noktası şu ikisinden
    // birine yönlendirir: goal.visionVideoUrl varsa VisionVideoPlayer'a,
    // yoksa (eski/video'suz vizyon) SlidesViewer'a (goal_slides fallback).
    object VisionVideoPlayer : Screen("vision_video/{goalId}") {
        fun createRoute(goalId: String) = "vision_video/$goalId"
    }
    object SlidesViewer : Screen("slides_viewer/{goalId}") {
        fun createRoute(goalId: String) = "slides_viewer/$goalId"
    }
    object SlideCreator : Screen("slide_creator/{goalId}") {
        fun createRoute(goalId: String) = "slide_creator/$goalId"
    }
    object SpiritualTools : Screen("spiritual_tools")

    // Tam ekran Reels editörü — bottom nav/top bar'ın GÖRÜNMEDİĞİ route.
    // Goal her zaman önceden var (GoalDetailScreen'den açılıyor).
    object VideoEditor : Screen("video_editor/{goalId}") {
        fun createRoute(goalId: String) = "video_editor/$goalId"
    }
}
