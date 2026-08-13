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
                            val tabs = listOf("Vizyonlar", "Rüyalar", "Günce", "Kaydedilenler")
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = s.selectedTab == index,
                                    onClick = { viewModel.selectTab(index) },
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
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGridMessage("Henüz vizyonunuz yok.") }
                            } else {
                                items(s.visions) { goal ->
                                    ProfileGridItem(
                                        imageUrl = goal.coverImageUrl ?: goal.images?.firstOrNull()?.imageUrl,
                                        title = goal.title,
                                        onClick = { /* Handle goal click */ }
                                    )
                                }
                            }
                        }
                        1 -> {
                            if (s.dreams.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGridMessage("Henüz rüyanız yok.") }
                            } else {
                                items(s.dreams) { dream ->
                                    ProfileGridItem(
                                        imageUrl = dream.mediaUrls?.firstOrNull(),
                                        title = dream.title,
                                        onClick = { /* Handle dream click */ }
                                    )
                                }
                            }
                        }
                        2 -> {
                            if (s.diaryEntries.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGridMessage("Henüz günce kaydınız yok.") }
                            } else {
                                items(s.diaryEntries) { entry ->
                                    ProfileGridItem(
                                        imageUrl = entry.posterUrl ?: entry.mediaUrl,
                                        title = entry.caption ?: "Günce Kaydı",
                                        onClick = onDiaryJournalClick
                                    )
                                }
                            }
                        }
                        3 -> {
                            if (s.savedVisions.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGridMessage("Henüz kaydedilmiş vizyonunuz yok.") }
                            } else {
                                items(s.savedVisions) { goal ->
                                    ProfileGridItem(
                                        imageUrl = goal.coverImageUrl ?: goal.images?.firstOrNull()?.imageUrl,
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
