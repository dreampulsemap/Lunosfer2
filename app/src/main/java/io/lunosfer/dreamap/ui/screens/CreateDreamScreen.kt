package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.AnalyzeDreamRequest
import io.lunosfer.dreamap.data.model.DreamInsertPayload
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDreamScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600

    var content by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("public") }
    var inFeed by remember { mutableStateOf(true) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var selectedEmotions by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        coroutineScope.launch(Dispatchers.IO) {
            val sysLoc = getSystemLocationName(context)
            if (sysLoc.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    if (location.isEmpty()) location = sysLoc
                }
            }
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


    val charLimit = 12000
    val maxTags = 10

    val unknownLocationStr = stringResource(R.string.dream_unknownLocation)

    val emotions = listOf(
        stringResource(R.string.dream_emotion_joy),
        stringResource(R.string.dream_emotion_peace),
        stringResource(R.string.dream_emotion_love),
        stringResource(R.string.dream_emotion_hope),
        stringResource(R.string.dream_emotion_awe),
        stringResource(R.string.dream_emotion_surprise),
        stringResource(R.string.dream_emotion_curiosity),
        stringResource(R.string.dream_emotion_confusion),
        stringResource(R.string.dream_emotion_fear),
        stringResource(R.string.dream_emotion_anxiety),
        stringResource(R.string.dream_emotion_sadness),
        stringResource(R.string.dream_emotion_loneliness),
        stringResource(R.string.dream_emotion_anger),
        stringResource(R.string.dream_emotion_shame),
        stringResource(R.string.dream_emotion_disgust),
        stringResource(R.string.dream_emotion_relief)
    )

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

        withContext(Dispatchers.IO) {
            val sysLoc = getSystemLocationName(context)
            if (sysLoc.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    if (location.isEmpty()) location = sysLoc
                }
            } else {
                try {
                    val url = URL("https://ipinfo.io/json")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val city = json.optString("city")
                    val countryCode = json.optString("country")
                    val country = if (countryCode.isNotBlank()) Locale("", countryCode).displayCountry else ""
                    val loc = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
                    withContext(Dispatchers.Main) {
                        if (loc.isNotBlank() && location.isEmpty()) {
                            location = loc
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CreateDreamScreen", "Failed to fetch location", e)
                }
            }
        }
    }

    
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
                            placeholder = { Text(stringResource(R.string.create_dream_pixabay_search)) },
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
                                    contentDescription = hit.tags.joinToString(", "),
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
                                                        tags = hit.tags.joinToString(", "),
                                                        pixabayUser = hit.user,
                                                        width = hit.width,
                                                        height = hit.height
                                                    )
                                                    val res = NetworkModule.api.savePixabayImage(req)
                                                    aiImageUrl = res.url
                                                    imageSource = "pixabay"
                                                    imageWidth = hit.width
                                                    imageHeight = hit.height
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Void800)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✦ LUNOSFER JOURNAL",
                    color = AstralGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.dream_addTitle),
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            
            val pulseAlpha by animateFloatAsState(
                targetValue = if (isListening) 0.5f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )

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
                    Text(stringResource(R.string.create_dream_pixabay_btn), color = AstralGold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
val charCount = content.length
            val isNearLimit = charCount > charLimit * 0.9
            Text(
                text = "$charCount / $charLimit",
                color = if (isNearLimit) ShadowWorkRose else Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isWideScreen) 8 else 4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emotions) { emotion ->
                    val isSelected = selectedEmotions.contains(emotion)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AstralGold.copy(alpha = 0.1f) else Void800)
                            .clickable {
                                selectedEmotions = if (isSelected) {
                                    selectedEmotions - emotion
                                } else {
                                    selectedEmotions + emotion
                                }
                            }
                            .then(
                                if (isSelected) Modifier.shadow(8.dp, spotColor = AstralGold, ambientColor = AstralGold)
                                else Modifier
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emotion,
                            color = if (isSelected) AstralGold else Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = tagInput,
                onValueChange = { 
                    val newValue = it
                    if (newValue.contains(",")) {
                        val newTag = newValue.replace(",", "").trim()
                        if (newTag.isNotEmpty() && tags.size < maxTags && !tags.contains(newTag)) {
                            tags = tags + newTag
                        }
                        tagInput = ""
                    } else {
                        tagInput = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.create_dream_tags_placeholder), color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val newTag = tagInput.trim()
                        if (newTag.isNotEmpty() && tags.size < maxTags && !tags.contains(newTag)) {
                            tags = tags + newTag
                        }
                        tagInput = ""
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { tags = tags - tag },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Void800, labelColor = Color.White, trailingIconContentColor = Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.dream_location)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedLabelColor = AstralGold,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            var visibilityExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = visibilityExpanded,
                onExpandedChange = { visibilityExpanded = !visibilityExpanded }
            ) {
                OutlinedTextField(
                    value = when (visibility) {
                        "public" -> stringResource(R.string.dream_public)
                        "friends" -> stringResource(R.string.dream_friends)
                        "private" -> stringResource(R.string.dream_private)
                        else -> stringResource(R.string.dream_public)
                    },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = visibilityExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = visibilityExpanded,
                    onDismissRequest = { visibilityExpanded = false },
                    modifier = Modifier.background(Void800)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dream_public), color = Color.White) },
                        onClick = { visibility = "public"; visibilityExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dream_friends), color = Color.White) },
                        onClick = { visibility = "friends"; visibilityExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dream_private), color = Color.White) },
                        onClick = { visibility = "private"; visibilityExpanded = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = inFeed,
                    onCheckedChange = { inFeed = it },
                    colors = CheckboxDefaults.colors(checkedColor = AstralGold, checkmarkColor = Void950, uncheckedColor = Color.Gray)
                )
                Text(stringResource(R.string.dream_shareInFeed), color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = ShadowWorkRose, modifier = Modifier.padding(bottom = 16.dp))
            }

            val currentLang = Locale.getDefault().language

            Button(
                onClick = {
                    if (content.isBlank()) {
                        errorMessage = context.getString(R.string.dream_validationContent)
                        return@Button
                    }
                    if (content.length > 12000) {
                        errorMessage = "Rüya metni 12.000 karakteri geçemez"
                        return@Button
                    }
                    errorMessage = null
                    isSubmitting = true
                    
                    coroutineScope.launch {
                        try {
                            val user = supabaseClient.auth.currentUserOrNull()
                            if (user == null) {
                                errorMessage = "Not logged in"
                                isSubmitting = false
                                return@launch
                            }

                            val dreamDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            val sentiments = selectedEmotions.joinToString(", ")
                            val finalLocation = location.trim().ifEmpty { unknownLocationStr }
                            
                            val insertData = DreamInsertPayload(
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
                            )

                            // Type mismatch fix: ensure we deserialize as a Map returning Any values or define a specific class
                            val result = supabaseClient.postgrest["dreams"].insert(insertData) {
                                select()
                            }.decodeList<io.lunosfer.dreamap.data.model.DreamInsertResponse>()

                            val insertedId = result.firstOrNull()?.id
                            
                            if (insertedId != null) {
                                // Fire and forget analyze
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        NetworkModule.api.analyzeDream(
                                            AnalyzeDreamRequest(insertedId, content.trim(), currentLang)
                                        )
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            } else {
                                errorMessage = context.getString(R.string.dream_createFailed)
                                isSubmitting = false
                            }

                        } catch (e: Exception) {
                            errorMessage = context.getString(R.string.dream_createFailed) + ": ${e.message}"
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold, disabledContainerColor = Void800),
                enabled = !isSubmitting && content.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Void950, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.dream_submit), color = Void950, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun getSystemLocationName(context: Context): String {
    try {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return ""

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return ""
        val providers = locationManager.getProviders(true)
        var bestLocation: android.location.Location? = null
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }
        if (bestLocation != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                val country = addr.countryName ?: ""
                return listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("CreateDreamScreen", "System location error", e)
    }
    return ""
}
