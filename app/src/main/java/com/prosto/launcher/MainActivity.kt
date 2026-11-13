package com.prosto.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class LauncherApp(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProstoTheme { ProstoHome() } }
    }
}

@Composable
private fun ProstoTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProstoHome() {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = remember { context.getSharedPreferences("prosto_launcher", 0) }

    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var search by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var homePackages by remember {
        mutableStateOf(prefs.getStringSet("home_apps", emptySet())?.toSet().orEmpty())
    }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var contentVisible by remember { mutableStateOf(false) }

    val startupScale by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0.965f,
        animationSpec = tween(650),
        label = "startupScale"
    )
    val startupAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(550),
        label = "startupAlpha"
    )

    LaunchedEffect(Unit) {
        delay(80)
        contentVisible = true
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }

    LaunchedEffect(Unit) {
        apps = runCatching {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }

            activities
                .filter { it.activityInfo?.packageName != context.packageName }
                .mapNotNull { resolveInfo ->
                    runCatching {
                        val packageName = resolveInfo.activityInfo.packageName
                        LauncherApp(
                            resolveInfo.loadLabel(pm)?.toString().orEmpty().ifBlank { packageName },
                            packageName,
                            resolveInfo.loadIcon(pm)
                        )
                    }.getOrNull()
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }.getOrElse { emptyList() }
    }

    val favorites = remember(apps) {
        val preferred = listOf(
            "com.google.android.dialer", "com.android.dialer",
            "com.google.android.apps.messaging", "com.android.messaging",
            "com.google.android.apps.chrome", "com.android.chrome",
            "com.google.android.GoogleCamera", "com.android.camera2"
        )
        preferred.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
            .distinctBy { it.packageName }
            .take(4)
            .ifEmpty { apps.take(4) }
    }

    val homeApps = remember(apps, homePackages, favorites) {
        if (homePackages.isEmpty()) favorites
        else homePackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    }
    val visibleApps = apps.filter { it.label.contains(search, ignoreCase = true) }

    Box(Modifier.fillMaxSize()) {
        // Main screen stays underneath the drawer so it can blur during the transition.
        Column(
            Modifier.fillMaxSize()
                .blur(if (drawerOpen) 14.dp else 0.dp)
                .graphicsLayer {
                    alpha = startupAlpha
                    scaleX = startupScale
                    scaleY = startupScale
                }
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(76.dp))
            Text(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 76.sp,
                lineHeight = 82.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
                    .replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Long-press this empty area to open the settings placeholder.
            Spacer(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { settingsOpen = true }
                    )
            )

            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 22.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                homeApps.forEach { app ->
                    AppIcon(
                        app = app,
                        showLabel = false,
                        onClick = { launchApp(context, app.packageName) }
                    )
                }
                if (homeApps.isEmpty()) {
                    Text("Установите приложения", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.padding(bottom = 12.dp)
                    .size(42.dp)
                    .clickable { drawerOpen = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Все приложения")
                }
            }
        }

        AnimatedVisibility(
            visible = drawerOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(220)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(300)) +
                slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(300)),
            exit = fadeOut(tween(160)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(180)) +
                slideOutVertically(targetOffsetY = { it / 18 }, animationSpec = tween(180))
        ) {
            Column(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Все приложения",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { drawerOpen = false }) { Text("Готово") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    placeholder = { Text("Поиск приложений") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        AppIcon(
                            app = app,
                            showLabel = true,
                            onClick = { launchApp(context, app.packageName) },
                            onLongClick = {
                                homePackages = homePackages + app.packageName
                                prefs.edit().putStringSet("home_apps", homePackages).apply()
                                Toast.makeText(
                                    context,
                                    "Добавлено на главный экран",
                                    Toast.LENGTH_SHORT
                                ).show()
                                drawerOpen = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("Настройки Prosto Launcher") },
            text = {
                Text("Здесь скоро появятся настройки главного экрана, иконок и внешнего вида.")
            },
            confirmButton = {
                TextButton(onClick = { settingsOpen = false }) { Text("Понятно") }
            }
        )
    }
}

private fun launchApp(context: android.content.Context, packageName: String) {
    runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) context.startActivity(intent)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(
    app: LauncherApp,
    onClick: () -> Unit,
    showLabel: Boolean,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.width(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    setImageDrawable(app.icon)
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
            },
            modifier = Modifier.size(52.dp).padding(3.dp)
        )
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                app.label,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
