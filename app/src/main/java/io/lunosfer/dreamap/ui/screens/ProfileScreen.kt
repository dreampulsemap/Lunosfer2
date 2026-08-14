package io.lunosfer.dreamap.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatDelegate
import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.FullUserProfile
import io.lunosfer.dreamap.data.model.PremiumStatusResponse
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.components.AISummariesCard
import io.lunosfer.dreamap.ui.components.ReferralCard
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.ProfileUiState
import io.lunosfer.dreamap.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onAddFriendClick: () -> Unit = {},
    onDiaryJournalClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val languages = listOf(
        "en" to "English",
        "tr" to "Türkçe",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "pt" to "Português",
        "ru" to "Русский",
        "ar" to "العربية",
        "zh" to "中文",
        "ja" to "日本語",
        "hi" to "हिन्दी"
    )

    LaunchedEffect(state) {
        val content = state as? ProfileUiState.Content
        content?.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
        content?.actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void950),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val s = state) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            }
            is ProfileUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, color = Color(0xFFF87171))
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.loadData() },
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                    ) {
                        Text(stringResource(R.string.profile_retry_btn), color = Void950)
                    }
                }
            }
            is ProfileUiState.Content -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_title),
                                color = AstralGold,
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SerifFontFamily)
                            )

                            ProfileSummaryCard(
                                profile = s.profile,
                                onEditClick = { viewModel.openEditModal() }
                            )

                            NotificationPermissionBanner()
                            PremiumStatusCard(status = s.premiumStatus)
                            AISummariesCard()
                            ReferralCard()

                            Button(
                                onClick = onAddFriendClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Void900),
                                border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = AstralGold)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.profile_find_friends_btn), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            
                            // Logout moved to top right or bottom of this header block
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        supabaseClient.auth.signOut()
                                        onLogout()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ShadowWorkRose),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.profile_logout_btn), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Tabs
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TabRow(
                            selectedTabIndex = s.selectedTab,
                            containerColor = Void950,
                            contentColor = AstralGold,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[s.selectedTab]),
                                    color = AstralGold
                                )
                            }
                        ) {
                            val tabs = listOf(
                                stringResource(R.string.profile_tab_visions),
                                stringResource(R.string.profile_tab_dreams),
                                stringResource(R.string.profile_tab_journal),
                                stringResource(R.string.profile_tab_saved)
                            )
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = s.selectedTab == index,
                                    onClick = { if (index == 2) onDiaryJournalClick() else viewModel.selectTab(index) },
                                    text = { Text(title, fontSize = 12.sp, fontWeight = if (s.selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                                    selectedContentColor = AstralGold,
                                    unselectedContentColor = Color.Gray
                                )
                            }
                        }
                    }

                    // Grid Items
                    when (s.selectedTab) {
                        0 -> {
                            if (s.visions.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGridMessage(stringResource(R.string.profile_empty_visions)) }
                            } else {
                                items(s.visions) { goal ->
                                    ProfileGridItem(
                                        imageUrl = goal.coverImageUrl,
                                        title = goal.title,
                                        onClick = { /* Handle goal click */ }
                                    )
                                }
                            }
                        }
                        1 -> {
                            if (s.dreams.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGridMessage(stringResource(R.string.profile_empty_dreams)) }
                            } else {
                                items(s.dreams) { dream ->
                                    ProfileGridItem(
                                        imageUrl = dream.aiImageUrl,
                                        title = dream.displayTitle,
                                        onClick = { /* Handle dream click */ }
                                    )
                                }
                            }
                        }
                        3 -> {
                            if (s.savedVisions.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGridMessage(stringResource(R.string.profile_empty_saved)) }
                            } else {
                                items(s.savedVisions) { goal ->
                                    ProfileGridItem(
                                        imageUrl = goal.coverImageUrl,
                                        title = goal.title,
                                        onClick = { /* Handle goal click */ }
                                    )
                                }
                            }
                        }
                    }
                }

@Composable
fun ProfileGridItem(imageUrl: String?, title: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Void900)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = title ?: "",
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 2,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun EmptyGridMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = Color.Gray, fontSize = 14.sp)
    }
}

                                // Edit Profile Dialog
                if (s.isEditModalOpen) {
                    EditProfileDialog(
                        profile = s.profile,
                        isSaving = s.isSavingProfile,
                        onDismiss = { viewModel.closeEditModal() },
                        onSave = { username, displayName, avatarUrl, isPrivate, language, gender ->
                            val localeList = LocaleListCompat.forLanguageTags(language)
                            AppCompatDelegate.setApplicationLocales(localeList)
                            viewModel.updateProfile(
                                username = username,
                                displayName = displayName,
                                avatarUrl = avatarUrl,
                                isPrivate = isPrivate,
                                language = language,
                                gender = gender
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    profile: FullUserProfile,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Void800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!profile.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, AstralGold, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AstralGold.copy(alpha = 0.2f))
                        .border(2.dp, AstralGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.nameOrFallback.take(1).uppercase(),
                        color = AstralGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.nameOrFallback,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${profile.username ?: "user"}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                if (profile.isPrivate) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AstralGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.profile_private_badge), color = AstralGold, fontSize = 11.sp)
                    }
                }
            }

            OutlinedButton(
                onClick = onEditClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
                border = BorderStroke(1.dp, AstralGold),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.profile_edit_action), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PremiumStatusCard(status: PremiumStatusResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isPremium) Void800 else Void900
        ),
        border = BorderStroke(
            1.dp,
            if (status.isPremium) AstralGold else Void800
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (status.isPremium) AstralGold.copy(alpha = 0.2f) else Void800
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (status.isPremium) Icons.Default.Star else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = if (status.isPremium) AstralGold else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (status.isPremium) {
                    Text(
                        text = stringResource(R.string.profile_premium_badge),
                        color = AstralGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.profile_premium_desc),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.profile_free_badge),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!status.canPickVideo) {
                        val formattedDate = formatNextAvailableDate(status.nextAvailableAt)
                        Text(
                            text = stringResource(R.string.profile_free_video_wait, formattedDate),
                            color = Color(0xFFF87171),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.profile_free_video_ready),
                            color = Color(0xFF4ADE80),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    profile: FullUserProfile,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (username: String, displayName: String, avatarUrl: String, isPrivate: Boolean, language: String, gender: String) -> Unit
) {
    var username by remember { mutableStateOf(profile.username ?: "") }
    var displayName by remember { mutableStateOf(profile.displayName ?: "") }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl ?: "") }
    var isPrivate by remember { mutableStateOf(profile.isPrivate) }
    var language by remember { mutableStateOf(profile.language ?: "tr") }
    var gender by remember { mutableStateOf(profile.gender ?: "unspecified") }

    val languages = listOf(
        "en" to "English",
        "tr" to "Türkçe",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "pt" to "Português",
        "ru" to "Русский",
        "ar" to "العربية",
        "zh" to "中文",
        "ja" to "日本語",
        "hi" to "हिन्दी"
    )

    val genders = listOf(
        "female" to "Kadın",
        "male" to "Erkek",
        "unspecified" to "Belirtmek İstemiyorum"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Void900),
            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_edit_btn),
                    color = AstralGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                )

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.profile_edit_username_label), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Display Name
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.profile_edit_display_name_label), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Avatar URL
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text(stringResource(R.string.profile_edit_avatar_url_label), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Privacy Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Void800)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.profile_edit_private_title), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.profile_edit_private_desc), color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Void950, checkedTrackColor = AstralGold)
                    )
                }

                // Gender Selection
                Text(stringResource(R.string.profile_edit_gender_label), color = AstralGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    genders.forEach { g ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { gender = g.first }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = gender == g.first,
                                onClick = { gender = g.first },
                                colors = RadioButtonDefaults.colors(selectedColor = AstralGold)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(g.second, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        border = BorderStroke(1.dp, Void800)
                    ) {
                        Text(stringResource(R.string.profile_edit_cancel))
                    }

                    Button(
                        onClick = {
                            onSave(username, displayName, avatarUrl, isPrivate, language, gender)
                        },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Void950)
                        } else {
                            Text(stringResource(R.string.profile_edit_save), color = Void950, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatNextAvailableDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "yakında"
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(isoString.take(19))
        val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr")).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        date?.let { outputFormat.format(it) } ?: isoString.take(16).replace("T", " ")
    } catch (_: Exception) {
        isoString.take(16).replace("T", " ")
    }
}

@Composable
private fun NotificationPermissionBanner() {
    val context = LocalContext.current
    val isGranted = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    if (!isGranted) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Void900),
            border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(fallbackIntent)
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_notifications_off),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.profile_notifications_desc),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileGridItem(imageUrl: String?, title: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Void900)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = title ?: "",
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun EmptyGridMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = Color.Gray, fontSize = 14.sp)
    }
}
