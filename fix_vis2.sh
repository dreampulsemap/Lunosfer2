sed -i 's/"Public" -> stringResource(R.string.dream_public)/"public" -> stringResource(R.string.dream_public)/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt
sed -i 's/"Friends only" -> stringResource(R.string.dream_friends)/"friends" -> stringResource(R.string.dream_friends)/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt
sed -i 's/"Private" -> stringResource(R.string.dream_private)/"private" -> stringResource(R.string.dream_private)/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt

sed -i 's/visibility = "Public"/visibility = "public"/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt
sed -i 's/visibility = "Friends only"/visibility = "friends"/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt
sed -i 's/visibility = "Private"/visibility = "private"/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt

sed -i 's/var visibility by remember { mutableStateOf("Public") }/var visibility by remember { mutableStateOf("public") }/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt
