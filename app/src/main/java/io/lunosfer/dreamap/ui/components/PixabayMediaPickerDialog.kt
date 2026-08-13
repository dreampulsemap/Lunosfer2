package io.lunosfer.dreamap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.PixabayHit
import io.lunosfer.dreamap.data.model.PixabayVideoHit
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PixabayMediaPickerDialog(
    onDismissRequest: () -> Unit,
    onImageSelected: (pixabayId: Long, imageUrl: String, tags: String, user: String) -> Unit,
    onVideoSelected: ((pixabayId: Long, videoUrl: String, tags: String, user: String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("nebula abstract") }
    var mediaType by remember { mutableStateOf("image") } // "image" or "video"
    var isLoading by remember { mutableStateOf(false) }

    var imageResults by remember { mutableStateOf<List<PixabayHit>>(emptyList()) }
    var videoResults by remember { mutableStateOf<List<PixabayVideoHit>>(emptyList()) }

    fun doSearch() {
        if (searchQuery.isBlank()) return
        isLoading = true
        coroutineScope.launch {
            runCatching {
                if (mediaType == "image") {
                    val res = NetworkModule.api.searchPixabay(searchQuery)
                    imageResults = res.hits
                } else {
                    val res = NetworkModule.api.searchPixabayVideos(searchQuery)
                    videoResults = res.hits
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(mediaType) {
        doSearch()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Void900,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pixabay_title),
                    color = AstralGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SerifFontFamily
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.pixabay_close), tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Media Type Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Void800)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (mediaType == "image") AetherViolet else Color.Transparent)
                            .clickable { mediaType = "image" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pixabay_image), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (mediaType == "video") AetherViolet else Color.Transparent)
                            .clickable { mediaType = "video" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pixabay_video), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.pixabay_search_placeholder), color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstralGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    IconButton(
                        onClick = { doSearch() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AetherViolet)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.pixabay_search), tint = Color.White)
                    }
                }

                // Results Grid
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AstralGold)
                    }
                } else if (mediaType == "image") {
                    if (imageResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.pixabay_images_not_found), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(imageResults) { hit ->
                                AsyncImage(
                                    model = hit.previewURL ?: hit.webformatURL,
                                    contentDescription = hit.tags.joinToString(", "),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            onImageSelected(
                                                hit.id,
                                                hit.webformatURL,
                                                hit.tags.joinToString(", "),
                                                hit.user
                                            )
                                            onDismissRequest()
                                        }
                                )
                            }
                        }
                    }
                } else {
                    if (videoResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.pixabay_videos_not_found), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(videoResults) { hit ->
                                val thumb = hit.videos?.tiny?.thumbnail ?: hit.videos?.small?.thumbnail
                                val videoUrl = hit.videos?.medium?.url ?: hit.videos?.large?.url ?: ""
                                Box(
                                    modifier = Modifier
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Void800)
                                        .border(1.dp, AetherViolet.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (onVideoSelected != null && videoUrl.isNotBlank()) {
                                                onVideoSelected(hit.id, videoUrl, hit.tags.joinToString(", "), hit.user)
                                            } else {
                                                // Fallback to image thumb
                                                onImageSelected(hit.id, thumb ?: "", hit.tags.joinToString(", "), hit.user)
                                            }
                                            onDismissRequest()
                                        }
                                ) {
                                    if (!thumb.isNullOrBlank()) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = AstralGold,
                                        modifier = Modifier.align(Alignment.Center).size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
