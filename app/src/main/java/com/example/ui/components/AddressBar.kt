package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.*
import com.example.data.model.BrowserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBar(
    activeTab: ActiveTabState?,
    currentProfile: BrowserProfile?,
    isPrivateMode: Boolean,
    tabCount: Int,
    isBookmarked: Boolean,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPrivacyShield: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val profileColor = if (isPrivateMode) Color(0xFF9333EA) else {
        try {
            Color(android.graphics.Color.parseColor(currentProfile?.colorHex ?: "#3B82F6"))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar / Private Badge Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(profileColor.copy(alpha = 0.2f))
                        .clickable { onOpenProfiles() }
                        .testTag("profile_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPrivateMode) Icons.Default.VpnKey else getProfileIcon(currentProfile?.iconName),
                        contentDescription = "Switch Profile",
                        tint = profileColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Address / Search Bar Input Field
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Security Indicator or Privacy Shield Badge
                        if (!isEditing && (activeTab?.url?.isNotBlank() == true)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenPrivacyShield() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("privacy_shield_indicator"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (activeTab.isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = if (activeTab.isSecure) "Secure HTTPS" else "Insecure HTTP",
                                        tint = if (activeTab.isSecure) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    if (activeTab.blockedCount > 0) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Shield Active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${activeTab.blockedCount}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Text Field
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                isEditing = false
                                focusManager.clearFocus()
                                onNavigate(inputText)
                            }),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty() && !isEditing) {
                                    Text(
                                        text = "Search or enter URL",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isEditing = focusState.isFocused
                                    if (focusState.isFocused && inputText.isBlank() && activeTab?.url?.isNotBlank() == true) {
                                        inputText = activeTab.url
                                    }
                                }
                                .testTag("address_input")
                        )

                        // Clear Button or Reload / Stop
                        if (isEditing && inputText.isNotEmpty()) {
                            IconButton(
                                onClick = { inputText = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear input",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else if (activeTab?.isLoading == true) {
                            IconButton(
                                onClick = onStop,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Loading",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (activeTab?.url?.isNotBlank() == true) {
                            IconButton(
                                onClick = onReload,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload Page",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Bookmark Icon Button
                if (activeTab?.url?.isNotBlank() == true) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark this page",
                            tint = if (isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Tabs Counter Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenTabs() }
                        .padding(2.dp)
                        .testTag("tabs_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$tabCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Menu 3-dots Button
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Browser Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Linear Progress Bar
            AnimatedVisibility(
                visible = activeTab?.isLoading == true && (activeTab.progress in 1..99)
            ) {
                LinearProgressIndicator(
                    progress = { (activeTab?.progress ?: 0) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = profileColor,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

fun getProfileIcon(iconName: String?): ImageVector {
    return when (iconName?.lowercase()) {
        "work", "business" -> Icons.Default.Work
        "school", "university" -> Icons.Default.School
        "science", "testing" -> Icons.Default.Science
        "shopping", "cart" -> Icons.Default.ShoppingCart
        "public", "globe" -> Icons.Default.Public
        "star", "favorite" -> Icons.Default.Star
        "code", "dev" -> Icons.Default.Code
        else -> Icons.Default.Person
    }
}
