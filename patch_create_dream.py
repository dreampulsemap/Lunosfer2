import re

with open("app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt", "r") as f:
    code = f.read()

# Add imports for Speech, Coil, Intent, etc.
imports = """
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import io.lunosfer.dreamap.data.model.PixabayHit
import io.lunosfer.dreamap.data.model.PixabayImageRequest
"""
if "import android.Manifest" not in code:
    code = code.replace("import java.util.Locale\n", "import java.util.Locale\n" + imports)

# Add state variables inside CreateDreamScreen
state_vars = """
    var isListening by remember { mutableStateOf(false) }
    var showPixabayDialog by remember { mutableStateOf(false) }
    var aiImageUrl by remember { mutableStateOf<String?>(null) }
    var imageSource by remember { mutableStateOf<String?>(null) }
    var imageWidth by remember { mutableStateOf<Int?>(null) }
    var imageHeight by remember { mutableStateOf<Int?>(null) }
    
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    val newContent = if (content.isEmpty()) recognizedText else "$content $recognizedText"
                    content = newContent.take(12000)
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
    
    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer.startListening(intent)
            isListening = true
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun toggleListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizer.startListening(intent)
                isListening = true
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
"""
if "var isListening by remember" not in code:
    code = code.replace("var errorMessage by remember { mutableStateOf<String?>(null) }", "var errorMessage by remember { mutableStateOf<String?>(null) }\n" + state_vars)

# Replace the OutlinedTextField for 'content' to include a trailing mic icon
pulse_anim = """
            val pulseAlpha by animateFloatAsState(
                targetValue = if (isListening) 0.5f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )
"""
new_content_field = pulse_anim + """
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= charLimit) content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text(stringResource(R.string.dream_placeholder), color = Color.Gray) },
                trailingIcon = {
                    IconButton(onClick = { toggleListening() }) {
                        Icon(
                            imageVector = if (isListening) Icons.Filled.Mic else Icons.Outlined.MicNone,
                            contentDescription = "Dictate",
                            tint = if (isListening) Color.Red.copy(alpha = pulseAlpha) else Color.White
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
"""
import re
content_field_pattern = r"OutlinedTextField\(\s*value = content,\s*onValueChange = \{ if \(it.length <= charLimit\) content = it \},\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.height\(200\.dp\),\s*placeholder = \{ Text\(stringResource\(R\.string\.dream_placeholder\), color = Color\.Gray\) \},\s*colors = OutlinedTextFieldDefaults\.colors\(\s*focusedBorderColor = AstralGold,\s*unfocusedBorderColor = Void800,\s*focusedTextColor = Color\.White,\s*unfocusedTextColor = Color\.White\s*\)\s*\)"
if not "pulseAlpha" in code:
    code = re.sub(content_field_pattern, new_content_field, code)

# Add Cover Image section before Content field
cover_section = """
            if (aiImageUrl != null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(
                        model = aiImageUrl,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    IconButton(
                        onClick = { aiImageUrl = null; imageSource = null; imageWidth = null; imageHeight = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove Cover", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Button(
                    onClick = { showPixabayDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Void800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = AstralGold)
                    Spacer(Modifier.width(8.dp))
                    Text("Pixabay'dan Seç", color = AstralGold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
"""
if "Pixabay'dan Seç" not in code:
    code = code.replace("val charCount = content.length\n            val isNearLimit = charCount > charLimit * 0.9\n            Text(\n                text = \"$charCount / $charLimit\",", cover_section + "val charCount = content.length\n            val isNearLimit = charCount > charLimit * 0.9\n            Text(\n                text = \"$charCount / $charLimit\",")

# Include the fields in DreamInsertPayload
payload = """val insertData = DreamInsertPayload(
                                userId = user.id,
                                content = content.trim(),
                                locationName = finalLocation,
                                inFeed = inFeed,
                                visibility = visibility,
                                userSelectedSentiment = sentiments,
                                dreamDate = dreamDate,
                                originalLanguage = currentLang,
                                tags = tags
                            )"""

new_payload = """val insertData = DreamInsertPayload(
                                userId = user.id,
                                content = content.trim(),
                                locationName = finalLocation,
                                inFeed = inFeed,
                                visibility = visibility,
                                userSelectedSentiment = sentiments,
                                dreamDate = dreamDate,
                                originalLanguage = currentLang,
                                tags = tags,
                                aiImageUrl = aiImageUrl,
                                imageSource = imageSource,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            )"""
if "aiImageUrl = aiImageUrl" not in code:
    code = code.replace(payload, new_payload)

# Pixabay Dialog component
pixabay_dialog = """
    if (showPixabayDialog) {
        Dialog(
            onDismissRequest = { showPixabayDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Void950) {
                var query by remember { mutableStateOf("") }
                var results by remember { mutableStateOf<List<PixabayHit>>(emptyList()) }
                var isSearching by remember { mutableStateOf(false) }

                LaunchedEffect(query) {
                    delay(500)
                    isSearching = true
                    try {
                        val response = NetworkModule.api.searchPixabay(query)
                        results = response.hits
                    } catch (e: Exception) {
                        // Handle
                    }
                    isSearching = false
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showPixabayDialog = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search Pixabay...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AstralGold,
                                unfocusedBorderColor = Void800,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AstralGold)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results) { hit ->
                                AsyncImage(
                                    model = hit.webformatURL,
                                    contentDescription = hit.tags,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                try {
                                                    val req = PixabayImageRequest(
                                                        pixabayId = hit.id,
                                                        imageUrl = hit.webformatURL,
                                                        tags = hit.tags ?: "",
                                                        pixabayUser = hit.user,
                                                        width = hit.imageWidth,
                                                        height = hit.imageHeight
                                                    )
                                                    val res = NetworkModule.api.savePixabayImage(req)
                                                    aiImageUrl = res.url
                                                    imageSource = "pixabay"
                                                    imageWidth = hit.imageWidth
                                                    imageHeight = hit.imageHeight
                                                    showPixabayDialog = false
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error selecting image", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
"""
if "DialogProperties(usePlatformDefaultWidth = false)" not in code:
    code = code.replace("Scaffold(", pixabay_dialog + "\n    Scaffold(")

with open("app/src/main/java/io/lunosfer/dreamap/ui/screens/CreateDreamScreen.kt", "w") as f:
    f.write(code)
