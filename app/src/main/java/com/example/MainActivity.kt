package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.local.TopiaDatabase
import com.example.data.remote.GeminiService
import com.example.data.repository.TopiaRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FlashcardScreen
import com.example.ui.screens.PhotoTestScreen
import com.example.ui.screens.VocaBookScreen
import com.example.ui.screens.VoiceTalkScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.TtsManager

data class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager
    private lateinit var repository: TopiaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TopiaDatabase.getDatabase(this, lifecycleScope)
        val geminiService = GeminiService()
        repository = TopiaRepository(database.wordDao(), database.testDao(), geminiService)
        ttsManager = TtsManager(this)

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    repository = repository,
                    ttsManager = ttsManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@Composable
fun MainAppContainer(
    repository: TopiaRepository,
    ttsManager: TtsManager
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isInFlashcardMode by remember { mutableStateOf(false) }

    val navTabs = listOf(
        NavTab("Chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "nav_chat"),
        NavTab("Voice Talk", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver, "nav_voice"),
        NavTab("Photo Test", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt, "nav_test"),
        NavTab("Voca Book", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_voca")
    )

    if (isInFlashcardMode) {
        FlashcardScreen(
            repository = repository,
            ttsManager = ttsManager,
            onBack = { isInFlashcardMode = false }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> ChatScreen(repository = repository, ttsManager = ttsManager)
                    1 -> VoiceTalkScreen(repository = repository, ttsManager = ttsManager)
                    2 -> PhotoTestScreen(repository = repository, ttsManager = ttsManager)
                    3 -> VocaBookScreen(
                        repository = repository,
                        ttsManager = ttsManager,
                        onOpenFlashcards = { isInFlashcardMode = true }
                    )
                }
            }
        }
    }
}
