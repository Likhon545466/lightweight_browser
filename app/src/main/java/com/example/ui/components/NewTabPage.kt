package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.SearchEngine
import com.example.data.model.Bookmark
import com.example.data.model.BrowserProfile
import com.example.privacy.ContentBlocker

data class QuickShortcut(
    val title: String,
    val url: String,
    val initial: String,
    val color: Color
)

@Composable
fun NewTabPage(
    currentProfile: BrowserProfile?,
    isPrivateMode: Boolean,
    searchEngine: SearchEngine,
    bookmarks: List<Bookmark>,
    newTabStyle: com.example.browser.NewTabStyle = com.example.browser.NewTabStyle.PRODUCTIVITY,
    onNavigate: (String) -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPrivacyShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val defaultShortcuts = remember {
        listOf(
            QuickShortcut("Google", "https://www.google.com", "G", Color(0xFF4285F4)),
            QuickShortcut("Wikipedia", "https://www.wikipedia.org", "W", Color(0xFF1E293B)),
            QuickShortcut("GitHub", "https://github.com", "GH", Color(0xFF24292F)),
            QuickShortcut("Reddit", "https://www.reddit.com", "R", Color(0xFFFF4500)),
            QuickShortcut("DuckDuckGo", "https://duckduckgo.com", "D", Color(0xFFDE5833)),
            QuickShortcut("Hacker News", "https://news.ycombinator.com", "Y", Color(0xFFFF6600)),
            QuickShortcut("MDN Docs", "https://developer.mozilla.org", "M", Color(0xFF000000)),
            QuickShortcut("Stack Overflow", "https://stackoverflow.com", "SO", Color(0xFFF48024))
        )
    }

    val profileColor = if (isPrivateMode) Color(0xFF9333EA) else {
        try {
            Color(android.graphics.Color.parseColor(currentProfile?.colorHex ?: "#3B82F6"))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(force = true)
                })
            }
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = if (newTabStyle == com.example.browser.NewTabStyle.MINIMALIST) 40.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (newTabStyle == com.example.browser.NewTabStyle.PRODUCTIVITY) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Badge Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = profileColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, profileColor.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clickable { onOpenProfiles() }
                    .testTag("new_tab_profile_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPrivateMode) Icons.Default.VpnKey else getProfileIcon(currentProfile?.iconName),
                        contentDescription = null,
                        tint = profileColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPrivateMode) "Private Mode (Temporary Session)" else "${currentProfile?.displayName ?: "Personal"} Profile",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = profileColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch profile",
                        tint = profileColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Compass / Browser Logo & Title
        Box(
            modifier = Modifier
                .size(if (newTabStyle == com.example.browser.NewTabStyle.MINIMALIST) 72.dp else 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(profileColor, profileColor.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPrivateMode) Icons.Default.Security else Icons.Default.Explore,
                contentDescription = "Browser",
                tint = Color.White,
                modifier = Modifier.size(if (newTabStyle == com.example.browser.NewTabStyle.MINIMALIST) 40.dp else 34.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isPrivateMode) "Private Browsing" else if (newTabStyle == com.example.browser.NewTabStyle.MINIMALIST) "Zen Browsing" else "Lightweight Browser",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isPrivateMode) "Tabs, cookies and history disappear upon exit" else if (newTabStyle == com.example.browser.NewTabStyle.MINIMALIST) "Clean • Focused • Privacy First" else "Fast • Isolated Profiles • Tracker-Free",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(if (newTabStyle == com.example.browser.NewTabStyle.MINIMALIST) 28.dp else 32.dp))

        // Shortcuts Grid
        Text(
            text = "QUICK SHORTCUTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4 x 2 Grid for shortcuts
        val rowsToShow = if (newTabStyle == com.example.browser.NewTabStyle.MINIMALIST) 1 else 2
        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until rowsToShow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 4) {
                        val index = row * 4 + col
                        if (index < defaultShortcuts.size) {
                            val shortcut = defaultShortcuts[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigate(shortcut.url) }
                                    .padding(vertical = 8.dp)
                                    .testTag("shortcut_${shortcut.title}")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = shortcut.color.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, shortcut.color.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = shortcut.initial,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = shortcut.color
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = shortcut.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (newTabStyle == com.example.browser.NewTabStyle.PRODUCTIVITY) {
            Spacer(modifier = Modifier.height(28.dp))

            // Privacy Shield Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPrivacyShield() }
                    .testTag("privacy_shield_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy Shield Active",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${ContentBlocker.totalBlockedCount.get()} Blocked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FeatureCheckItem(icon = Icons.Default.Check, text = "Tracker & Ad Blocker")
                        FeatureCheckItem(icon = Icons.Default.Check, text = "Isolated Storage")
                        FeatureCheckItem(icon = Icons.Default.Check, text = "Zero Telemetry")
                    }
                }
            }
        }

        // Recent Bookmarks if any
        if (bookmarks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SAVED BOOKMARKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bookmarks.take(6)) { bm ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onNavigate(bm.url) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bm.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun FeatureCheckItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
