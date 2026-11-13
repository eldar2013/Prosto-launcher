package com.prosto.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class LauncherApp(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

private enum class LauncherTheme { SYSTEM, LIGHT, DARK }

private val Purple = Color(0xFF9A78F2)
private val Lavender = Color(0xFFC5A9FF)
private val DarkInk = Color(0xFF171326)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProstoHome() }
    }
}

@Composable
private fun ProstoHome() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prosto_launcher", 0) }
    var themeChoice by remember {
        mutableStateOf(runCatching {
            LauncherTheme.valueOf(prefs.getString("theme_choice", "SYSTEM") ?: "SYSTEM")
        }.getOrDefault(LauncherTheme.SYSTEM))
    }
    val dark = when (themeChoice) {
        LauncherTheme.LIGHT -> false
        LauncherTheme.DARK -> true
        LauncherTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val colors = if (dark) darkColorScheme(
        primary = Lavender,
        secondary = Purple,
        background = DarkInk,
        surface = Color(0xFF211B35),
        onPrimary = DarkInk,
        onBackground = Color.White,
        onSurface = Color(0xFFF5F0FF),
        surfaceVariant = Color(0xFF39304F),
        onSurfaceVariant = Color(0xFFD4C9E8)
    ) else lightColorScheme(
        primary = Purple,
        secondary = Lavender,
        background = Color(0xFFF5F0FF),
        surface = Color(0xFFF8F4FF),
        onPrimary = Color.White,
        onBackground = DarkInk,
        onSurface = DarkInk,
        surfaceVariant = Color(0xFFE8DFFF),
        onSurfaceVariant = Color(0xFF514767)
    )
    MaterialTheme(colorScheme = colors) {
        ProstoContent(
            prefs = prefs,
            themeChoice = themeChoice,
            dark = dark,
            onThemeChange = {
                themeChoice = it
                prefs.edit().putString("theme_choice", it.name).apply()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProstoContent(
    prefs: android.content.SharedPreferences,
    themeChoice: LauncherTheme,
    dark: Boolean,
    onThemeChange: (LauncherTheme) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var search by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var homePackages by remember {
        mutableStateOf(
            prefs.getString("home_apps_ordered", null)?.split("|")?.filter { it.isNotBlank() }
                ?: prefs.getStringSet("home_apps", emptySet())?.toList().orEmpty()
        )
    }
    var wallpaperUri by remember { mutableStateOf(prefs.getString("wallpaper_uri", null)) }
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            wallpaperUri = uri.toString()
            prefs.edit().putString("wallpaper_uri", wallpaperUri).apply()
        }
    }
    val defaultWallpaper = remember {
        runCatching {
            val encoded = context.resources.openRawResource(R.raw.prosto_wallpaper)
                .bufferedReader().use { it.readText() }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    val customWallpaper = remember(wallpaperUri) {
        wallpaperUri?.let { raw ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(raw)).use { stream ->
                    if (stream != null) BitmapFactory.decodeStream(stream)?.asImageBitmap() else null
                }
            }.getOrNull()
        }
    }
    val itemRects = remember { mutableStateMapOf<String, Rect>() }
    var draggingPackage by remember { mutableStateOf<String?>(null) }
    var dragPoint by remember { mutableStateOf(Offset.Zero) }
    val blockedRects = remember { mutableStateMapOf<String, Rect>() }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }
    LaunchedEffect(Unit) {
        apps = runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val entries = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            entries.mapNotNull { info ->
                runCatching {
                    val pkg = info.activityInfo?.packageName ?: return@runCatching null
                    if (pkg == context.packageName) return@runCatching null
                    LauncherApp(info.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }, pkg, info.loadIcon(pm))
                }.getOrNull()
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
        }.getOrElse { emptyList() }
    }
    val appMap = remember(apps) { apps.associateBy { it.packageName } }
    val homeApps = homePackages.mapNotNull { appMap[it] }
    val dockApps = homeApps.take(4)
    val workspaceApps = homeApps.drop(4)
    val filteredApps = apps.filter { it.label.contains(search, ignoreCase = true) }

    fun saveHome(updated: List<String>) {
        homePackages = updated.distinct()
        prefs.edit().putString("home_apps_ordered", homePackages.joinToString("|")).apply()
    }
    fun moveApp(fromPackage: String, toPackage: String) {
        val from = homePackages.indexOf(fromPackage)
        val to = homePackages.indexOf(toPackage)
        if (from >= 0 && to >= 0 && from != to) {
            val changed = homePackages.toMutableList()
            val moved = changed.removeAt(from)
            changed.add(to.coerceIn(0, changed.size), moved)
            saveHome(changed)
        }
    }

    Box(
        Modifier.fillMaxSize().onGloballyPositioned { }
            .pointerInput(drawerOpen, settingsOpen, blockedRects.size) {
                detectTapGestures(onLongPress = { point ->
                    val blocked = blockedRects.values.any { it.contains(point) }
                    if (!blocked && !drawerOpen) settingsOpen = true
                })
            }
    ) {
        val activeWallpaper = customWallpaper ?: defaultWallpaper
        if (activeWallpaper != null) {
            Image(
                bitmap = activeWallpaper,
                contentDescription = "Обои Prosto Launcher",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(DarkInk))
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) 0.12f else 0.03f)))

        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            Text(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 72.sp,
                lineHeight = 78.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")))
                    .replaceFirstChar { it.titlecase(Locale("ru")) },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
            Spacer(Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workspaceApps, key = { it.packageName }) { app ->
                    Column(
                        Modifier.fillMaxWidth()
                            .onGloballyPositioned {
                                itemRects[app.packageName] = it.boundsInRoot()
                                blockedRects[app.packageName] = it.boundsInRoot()
                            }
                            .pointerInput(app.packageName, homePackages) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { start ->
                                        draggingPackage = app.packageName
                                        val rect = itemRects[app.packageName]
                                        dragPoint = if (rect != null) Offset(rect.left + start.x, rect.top + start.y) else start
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragPoint += amount
                                    },
                                    onDragEnd = {
                                        val target = itemRects.entries.firstOrNull { (pkg, rect) ->
                                            pkg != app.packageName && rect.contains(dragPoint)
                                        }?.key
                                        if (target != null) moveApp(app.packageName, target)
                                        draggingPackage = null
                                    },
                                    onDragCancel = { draggingPackage = null }
                                )
                            }
                            .clickable { launchApp(context, app.packageName) }
                            .graphicsLayer {
                                alpha = if (draggingPackage == app.packageName) 0.65f else 1f
                                scaleX = if (draggingPackage == app.packageName) 1.08f else 1f
                                scaleY = if (draggingPackage == app.packageName) 1.08f else 1f
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppImage(app, 54)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            app.label,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
            }

            // Four dock slots with the app drawer button in the exact center.
            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DockSlot(dockApps.getOrNull(0), "dock-left-1", blockedRects) { app -> launchApp(context, app.packageName) }
                DockSlot(dockApps.getOrNull(1), "dock-left-2", blockedRects) { app -> launchApp(context, app.packageName) }
                Surface(
                    modifier = Modifier.size(54.dp).onGloballyPositioned { blockedRects["drawer-button"] = it.boundsInRoot() }
                        .clickable { drawerOpen = true },
                    shape = CircleShape,
                    color = Color(0xFFEDE5FF).copy(alpha = 0.92f),
                    shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Все приложения", tint = DarkInk, modifier = Modifier.size(34.dp))
                    }
                }
                DockSlot(dockApps.getOrNull(2), "dock-right-1", blockedRects) { app -> launchApp(context, app.packageName) }
                DockSlot(dockApps.getOrNull(3), "dock-right-2", blockedRects) { app -> launchApp(context, app.packageName) }
            }
        }

        AnimatedVisibility(
            visible = drawerOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(300)),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.97f, animationSpec = tween(180)) + slideOutVertically(targetOffsetY = { it / 8 }, animationSpec = tween(180))
        ) {
            Column(
                Modifier.fillMaxSize().background(Color(0xFF171326).copy(alpha = 0.97f)).padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Все приложения", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { drawerOpen = false }) { Text("Готово", color = Lavender) }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    placeholder = { Text("Поиск приложений") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Lavender,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedLeadingIconColor = Lavender,
                        unfocusedLeadingIconColor = Color.White
                    )
                )
                Text("Зажми приложение, чтобы добавить его на главный экран", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .pointerInput(app.packageName, homePackages) {
                                    detectTapGestures(onLongPress = {
                                        if (app.packageName !in homePackages) {
                                            saveHome(homePackages + app.packageName)
                                            Toast.makeText(context, "Добавлено на главный экран", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Уже на главном экране", Toast.LENGTH_SHORT).show()
                                        }
                                        drawerOpen = false
                                    })
                                }
                                .clickable { launchApp(context, app.packageName) }
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppImage(app, 52)
                            Spacer(Modifier.height(4.dp))
                            Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Тема", style = MaterialTheme.typography.titleMedium)
                        ThemeRow("Системная", themeChoice == LauncherTheme.SYSTEM) { onThemeChange(LauncherTheme.SYSTEM) }
                        ThemeRow("Светлая", themeChoice == LauncherTheme.LIGHT) { onThemeChange(LauncherTheme.LIGHT) }
                        ThemeRow("Тёмная", themeChoice == LauncherTheme.DARK) { onThemeChange(LauncherTheme.DARK) }
                        Divider()
                        Text("Обои", style = MaterialTheme.typography.titleMedium)
                        Text("Сейчас используются фиолетовые обои. Можно выбрать свои из галереи.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { wallpaperPicker.launch(arrayOf("image/*")) }) {
                            Text("Выбрать обои из галереи")
                        }
                        if (wallpaperUri != null) {
                            TextButton(onClick = {
                                wallpaperUri = null
                                prefs.edit().remove("wallpaper_uri").apply()
                            }) { Text("Вернуть стандартные обои") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("Готово") } }
            )
        }
    }
}

@Composable
private fun DockSlot(
    app: LauncherApp?,
    key: String,
    blockedRects: MutableMap<String, Rect>,
    onClick: (LauncherApp) -> Unit
) {
    Box(
        Modifier.size(48.dp).onGloballyPositioned { blockedRects[key] = it.boundsInRoot() },
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Box(Modifier.fillMaxSize().clickable { onClick(app) }, contentAlignment = Alignment.Center) {
                AppImage(app, 46)
            }
        }
    }
}

@Composable
private fun AppImage(app: LauncherApp, size: Int) {
    AndroidView(
        factory = { ctx -> ImageView(ctx).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        } },
        update = { it.setImageDrawable(app.icon) },
        modifier = Modifier.size(size.dp).padding(2.dp)
    )
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun launchApp(context: android.content.Context, packageName: String) {
    runCatching {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let(context::startActivity)
    }
}
