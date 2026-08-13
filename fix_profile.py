import sys

with open('app/src/main/java/io/lunosfer/dreamap/ui/screens/ProfileScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("goal.coverImageUrl ?: goal.images?.firstOrNull()?.imageUrl", "goal.coverImageUrl")
content = content.replace("imageUrl = dream.mediaUrls?.firstOrNull()", "imageUrl = dream.aiImageUrl")
content = content.replace("title = dream.title", "title = dream.displayTitle")

with open('app/src/main/java/io/lunosfer/dreamap/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(content)
