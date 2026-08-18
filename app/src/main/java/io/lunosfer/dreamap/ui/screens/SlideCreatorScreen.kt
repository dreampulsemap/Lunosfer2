package io.lunosfer.dreamap.ui.screens

import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.GoalSlide
import io.lunosfer.dreamap.ui.components.PixabayMediaPickerDialog
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.MAX_GOAL_SLIDES
import io.lunosfer.dreamap.ui.viewmodel.SlideCreatorUiState
import io.lunosfer.dreamap.ui.viewmodel.SlideCreatorViewModel

/**
 * Slayt oluşturma/düzenleme — components/SlideEditor.jsx + SlideCaptionEditor.jsx
 * karşılığı. NOT: web'de bu akış artık UI'da yok (VisionVideoEditor'a
 * geçildi), ama backend (goal_slides tablosu + slides/create-update-reorder
 * endpoint'leri) tam çalışır durumda — bu ekran Android'e özgü, kullanıcının
 * düşük efortlu, sık paylaşılabilir slayt gösterileri oluşturmasını sağlıyor.
 *
 * Bilinçli sadeleştirmeler: konum için serbest sürükleme yerine 3 sabit ön
 * ayar (Üst/Orta/Alt), renk için serbest seçici yerine 6 hazır ton,
 * sürükle-bırak sıralama yerine yukarı/aşağı butonları.
 */
private val PRESET_COLORS = listOf(
    "#ffffff" to "Beyaz",
    "#04060E" to "Siyah",
    "#FBBF24" to "Altın",
    "#F43F5E" to "Gül",
    "#22D3EE" to "Camgöbeği",
    "#A855F7" to "Mor"
)

@Composable
fun SlideCreatorScreen(
    goalId: String,
    onBack: () -> Unit
) {
    val factory = remember(goalId) { SlideCreatorViewModel.Factory(goalId) }
    val viewModel: SlideCreatorViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showAddMenu by remember { mutableStateOf(false) }
    var showPixabay by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addSlideFromDevice(context, it) }
    }

    Scaffold(
        containerColor = Void950,
        topBar = {
            TopAppBar(
                title = { Text("Slaytlar", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        }
    ) { padding ->
        when (val s = state) {
            is SlideCreatorUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            }
            is SlideCreatorUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(s.message, color = Color.White)
                }
            }
            is SlideCreatorUiState.Content -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Text(
                        text = "${s.slides.size} / $MAX_GOAL_SLIDES slayt",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    s.error?.let { err ->
                        Surface(
                            color = SemanticDanger500.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SemanticDanger500.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(err, color = SemanticDanger400, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.slides, key = { it.id }) { slide ->
                            val index = s.slides.indexOf(slide)
                            SlideRow(
                                slide = slide,
                                index = index,
                                total = s.slides.size,
                                isEditing = s.editingSlideId == slide.id,
                                onEditToggle = {
                                    if (s.editingSlideId == slide.id) viewModel.stopEditing()
                                    else viewModel.startEditing(slide.id)
                                },
                                onDelete = { viewModel.deleteSlide(slide.id) },
                                onMoveUp = { viewModel.moveSlide(slide.id, -1) },
                                onMoveDown = { viewModel.moveSlide(slide.id, 1) },
                                onSave = { caption, duration, font, color, yPreset, size ->
                                    viewModel.saveSlideStyle(slide.id, caption, duration, font, color, yPreset, size)
                                }
                            )
                        }

                        item {
                            if (s.isUploading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Yükleniyor…", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            } else if (s.canAddMore) {
                                Surface(
                                    onClick = { showAddMenu = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = AstralGold)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Slayt Ekle", color = AstralGold, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            containerColor = Void950,
            title = { Text("Görsel Ekle", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = {
                            showAddMenu = false
                            photoPickerLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.06f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                            Text("Cihazdan Seç", color = Color.White)
                        }
                    }
                    Surface(
                        onClick = {
                            showAddMenu = false
                            showPixabay = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.06f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                            Text("Pixabay'da Ara", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text("Vazgeç", color = Color.White.copy(alpha = 0.6f)) }
            }
        )
    }

    if (showPixabay) {
        PixabayMediaPickerDialog(
            onDismissRequest = { showPixabay = false },
            onImageSelected = { _, imageUrl, _, _ ->
                showPixabay = false
                viewModel.addSlideFromUrl(imageUrl)
            }
        )
    }
}

@Composable
private fun SlideRow(
    slide: GoalSlide,
    index: Int,
    total: Int,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSave: (caption: String, duration: Int, font: String, color: String, yPreset: Float, size: Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isEditing) AstralGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(
                    model = slide.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!slide.caption.isNullOrBlank()) slide.caption else "Altyazı yok",
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${slide.durationSeconds ?: 4} sn",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Column {
                    IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = index < total - 1, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = if (index < total - 1) Color.White else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onEditToggle) {
                    Icon(if (isEditing) Icons.Default.Close else Icons.Default.Edit, contentDescription = null, tint = AstralGold)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SemanticDanger400)
                }
            }

            if (isEditing) {
                SlideStyleEditor(slide = slide, onSave = onSave)
            }
        }
    }
}

@Composable
private fun SlideStyleEditor(
    slide: GoalSlide,
    onSave: (caption: String, duration: Int, font: String, color: String, yPreset: Float, size: Float) -> Unit
) {
    var caption by remember(slide.id) { mutableStateOf(slide.caption ?: "") }
    var duration by remember(slide.id) { mutableStateOf((slide.durationSeconds ?: 4).toFloat()) }
    var font by remember(slide.id) { mutableStateOf(slide.captionFont ?: "sans") }
    var color by remember(slide.id) { mutableStateOf(slide.captionColor ?: "#ffffff") }
    var yPreset by remember(slide.id) { mutableStateOf(slide.captionY ?: 85f) }
    var size by remember(slide.id) { mutableStateOf(slide.captionSize ?: 1f) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        OutlinedTextField(
            value = caption,
            onValueChange = { if (it.length <= 200) caption = it },
            label = { Text("Altyazı (${caption.length}/200)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AstralGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = AstralGold,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            ),
            maxLines = 3
        )

        Text("Yazı Tipi", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("sans" to "Sans", "serif" to "Serif", "mono" to "Mono").forEach { (key, label) ->
                val selected = font == key
                Surface(
                    onClick = { font = key },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) AstralGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (selected) AstralGold else Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        label,
                        color = if (selected) AstralGold else Color.White.copy(alpha = 0.8f),
                        fontFamily = when (key) { "serif" -> SerifFontFamily; "mono" -> FontFamily.Monospace; else -> SansFontFamily },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Text("Renk", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_COLORS.forEach { (hex, _) ->
                val selected = color.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color.White))
                        .border(if (selected) 2.dp else 1.dp, if (selected) AstralGold else Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable { color = hex }
                )
            }
        }

        Text("Konum", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15f to "Üst", 50f to "Orta", 85f to "Alt").forEach { (yVal, label) ->
                val selected = yPreset == yVal
                Surface(
                    onClick = { yPreset = yVal },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) AstralGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (selected) AstralGold else Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        label,
                        color = if (selected) AstralGold else Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Text("Boyut: ${String.format("%.1f", size)}x", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Slider(
            value = size,
            onValueChange = { size = it },
            valueRange = 0.4f..3.5f,
            colors = SliderDefaults.colors(thumbColor = AstralGold, activeTrackColor = AstralGold)
        )

        Text("Süre: ${duration.toInt()} sn", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Slider(
            value = duration,
            onValueChange = { duration = it },
            valueRange = 1f..15f,
            steps = 13,
            colors = SliderDefaults.colors(thumbColor = AstralGold, activeTrackColor = AstralGold)
        )

        Button(
            onClick = { onSave(caption, duration.toInt(), font, color, yPreset, size) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary500)
        ) {
            Text("Kaydet", fontWeight = FontWeight.Bold)
        }
    }
}
