sed -i 's/fun VisionFeedCard(goal: Goal) {/fun VisionFeedCard(goal: Goal) {/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/HomeScreen.kt
sed -i '230s/modifier = Modifier.fillMaxWidth().clickable { onDreamClick(dream.id) },/modifier = Modifier.fillMaxWidth(),/g' app/src/main/java/io/lunosfer/dreamap/ui/screens/HomeScreen.kt
