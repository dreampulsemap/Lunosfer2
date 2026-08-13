package io.lunosfer.dreamap.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.CreateGoalRequest
import io.lunosfer.dreamap.data.model.RoadmapItemInput
import io.lunosfer.dreamap.data.repository.VisionRepository
import io.lunosfer.dreamap.ui.components.PixabayMediaPickerDialog
import io.lunosfer.dreamap.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { VisionRepository() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<String?>(null) }
    var visibility by remember { mutableStateOf("public") }
    var coverImageUrl by remember { mutableStateOf<String?>(null) }
    var showPixabayDialog by remember { mutableStateOf(false) }

    var roadmapInput by remember { mutableStateOf("") }
    var roadmapItems by remember { mutableStateOf<List<String>>(emptyList()) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            targetDate = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Yeni Vizyon Oluştur",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFF87171),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Title Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_title_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        if (it.length <= 120) title = it
                    },
                    placeholder = { Text(stringResource(R.string.create_vision_title_placeholder), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${title.length} / 120",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Description Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_desc_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        if (it.length <= 2000) description = it
                    },
                    placeholder = { Text(stringResource(R.string.create_vision_desc_placeholder), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = {
                        Text(
                            text = "${description.length} / 2000",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            if (showPixabayDialog) {
                PixabayMediaPickerDialog(
                    onDismissRequest = { showPixabayDialog = false },
                    onImageSelected = { _, url, _, _ ->
                        coverImageUrl = url
                    },
                    onVideoSelected = { _, url, _, _ ->
                        coverImageUrl = url
                    }
                )
            }

            // Cover Image Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_cover_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (coverImageUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { showPixabayDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
                        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.create_vision_cover_pixabay_btn))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = coverImageUrl,
                            contentDescription = "Kapak Görseli",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { coverImageUrl = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Void950.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = Color.White)
                        }
                    }
                }
            }

            // Target Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_date_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedCard(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = Void900),
                    border = BorderStroke(1.dp, Void800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = targetDate ?: "Tarih Seçin",
                            color = if (targetDate != null) Color.White else Color.Gray,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Tarih Seç",
                            tint = AstralGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Visibility Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_visibility_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        "public" to "Herkese Açık",
                        "friends" to "Arkadaşlara Özel",
                        "private" to "Gizli"
                    )
                    options.forEach { (key, label) ->
                        val selected = visibility == key
                        FilterChip(
                            selected = selected,
                            onClick = { visibility = key },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AstralGold,
                                selectedLabelColor = Void950,
                                containerColor = Void800,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Roadmap Items (Yol Haritası)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.create_vision_roadmap_label, roadmapItems.size),
                    color = AstralGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = roadmapInput,
                        onValueChange = { roadmapInput = it },
                        placeholder = { Text(stringResource(R.string.create_vision_roadmap_placeholder), color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstralGold,
                            unfocusedBorderColor = Void800,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    IconButton(
                        onClick = {
                            if (roadmapInput.isNotBlank() && roadmapItems.size < 20) {
                                roadmapItems = roadmapItems + roadmapInput.trim()
                                roadmapInput = ""
                            }
                        },
                        enabled = roadmapInput.isNotBlank() && roadmapItems.size < 20,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (roadmapInput.isNotBlank() && roadmapItems.size < 20) AstralGold else Void800)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adım Ekle",
                            tint = Void950
                        )
                    }
                }

                if (roadmapItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roadmapItems.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Void900),
                                border = BorderStroke(1.dp, Void800)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. $item",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            roadmapItems = roadmapItems.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Kaldır",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Başlık girilmesi zorunludur"
                        return@Button
                    }
                    if (title.length > 120) {
                        errorMessage = "Başlık 120 karakteri geçemez"
                        return@Button
                    }
                    if (description.length > 2000) {
                        errorMessage = "Açıklama 2000 karakteri geçemez"
                        return@Button
                    }

                    errorMessage = null
                    isSubmitting = true

                    coroutineScope.launch {
                        val roadmapList = roadmapItems.map { RoadmapItemInput(title = it) }
                        val req = CreateGoalRequest(
                            title = title.trim(),
                            description = description.trim().ifEmpty { null },
                            coverImageUrl = coverImageUrl,
                            coverImageSource = if (coverImageUrl != null) "pixabay" else "ai_generated",
                            targetDate = targetDate,
                            visibility = visibility,
                            roadmap = if (roadmapList.isNotEmpty()) roadmapList else null
                        )

                        repository.createGoal(req).onSuccess { newGoal ->
                            isSubmitting = false
                            Toast.makeText(context, "Vizyon başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }.onFailure { err ->
                            isSubmitting = false
                            val msg = err.message ?: ""
                            errorMessage = when {
                                msg.contains("title_required", ignoreCase = true) -> "Başlık girmelisiniz"
                                msg.contains("title_too_long", ignoreCase = true) -> "Başlık çok uzun"
                                msg.contains("description_too_long", ignoreCase = true) -> "Açıklama çok uzun"
                                else -> err.message ?: "Vizyon oluşturulamadı"
                            }
                        }
                    }
                },
                enabled = !isSubmitting && title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Void950, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(R.string.create_vision_submit_btn),
                        color = Void950,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
