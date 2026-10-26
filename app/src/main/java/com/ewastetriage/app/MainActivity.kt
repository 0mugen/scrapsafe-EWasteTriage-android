package com.ewastetriage.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ewastetriage.app.ui.*
import com.ewastetriage.app.ui.theme.EWasteTriageTheme

enum class AppScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SCAN("Scan", Icons.Filled.CenterFocusStrong, Icons.Outlined.CenterFocusStrong),
    HAZARDS("Hazards", Icons.Filled.Warning, Icons.Outlined.Warning),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    ABOUT("About", Icons.Filled.Info, Icons.Outlined.Info)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EWasteTriageTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(vm: ScanViewModel = viewModel()) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.SCAN) }
    var showGalleryTest by remember { mutableStateOf(false) }

    // First-launch safety acknowledgement
    val prefs = remember { context.getSharedPreferences("app_safety_prefs", Context.MODE_PRIVATE) }
    var showSafetyAck by remember {
        mutableStateOf(!prefs.getBoolean("has_acknowledged_safety", false))
    }

    Scaffold(
        bottomBar = {
            if (!showGalleryTest) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    AppScreen.values().forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                currentScreen = screen
                                showGalleryTest = false
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showGalleryTest) PaddingValues(0.dp) else padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                AppScreen.SCAN -> {
                    if (showGalleryTest) {
                        ImageTestScreen(
                            vm = vm,
                            onBack = { showGalleryTest = false }
                        )
                    } else {
                        CameraPermissionWrapper(
                            onChoosePhoto = { showGalleryTest = true }
                        ) {
                            ScanScreen(
                                vm = vm,
                                onOpenGallery = { showGalleryTest = true },
                                onNavigateToHistory = { currentScreen = AppScreen.HISTORY }
                            )
                        }
                    }
                }
                AppScreen.HAZARDS -> {
                    CategoriesScreen(vm)
                }
                AppScreen.HISTORY -> {
                    HistoryScreen(vm)
                }
                AppScreen.ABOUT -> {
                    AboutScreen()
                }
            }
        }
    }

    // First-launch Safety Acknowledgement Dialog
    if (showSafetyAck) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "No detection does not mean safe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Before you begin scanning e-waste, please understand the physical safety limits:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SafetyPointRow(
                        icon = Icons.Default.BatteryAlert,
                        text = "Cannot identify battery chemistry from shape. Cylindrical cells may be dangerous lithium-ion."
                    )

                    SafetyPointRow(
                        icon = Icons.Default.VisibilityOff,
                        text = "Many hazards are invisible to cameras (high voltage charges, chemical leakage, internal shorts)."
                    )

                    SafetyPointRow(
                        icon = Icons.Default.PanTool,
                        text = "Always wear protective gloves and eye protection before handling discarded electronics."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putBoolean("has_acknowledged_safety", true).apply()
                        showSafetyAck = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I understand", style = MaterialTheme.typography.labelLarge)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SafetyPointRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CameraPermissionWrapper(
    onChoosePhoto: () -> Unit,
    content: @Composable () -> Unit
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    if (hasCameraPermission) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Camera Access Needed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Camera access is needed to scan and identify hazardous components in electronic waste in real time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Allow camera", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onChoosePhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose a photo instead", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
