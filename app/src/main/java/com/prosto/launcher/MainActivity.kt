package com.prosto.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
private enum class IconShapeChoice { CIRCLE, SQUARE, TRIANGLE, COOKIE_12, SOFT_BURST, CLOVER_4 }

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
        primary = Lavender, secondary = Purple, background = DarkInk,
        surface = Color(0xFF211B35), onPrimary = DarkInk,
        onBackground = Color.White, onSurface = Color(0xFFF5F0FF),
        surfaceVariant = Color(0xFF39304F), onSurfaceVariant = Color(0xFFD4C9E8)
    ) else lightColorScheme(
        primary = Purple, secondary = Lavender, background = Color(0xFFF5F0FF),
        surface = Color(0xFFF8F4FF), onPrimary = Color.White,
        onBackground = DarkInk, onSurface = DarkInk,
        surfaceVariant = Color(0xFFE8DFFF), onSurfaceVariant = Color(0xFF514767)
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

    val oldHome = remember {
        prefs.getString("home_apps_ordered", null)?.split("|")?.filter { it.isNotBlank() }.orEmpty()
    }
    var dockPackages by remember {
        mutableStateOf(
            prefs.getString("dock_apps_ordered", null)?.split("|")?.filter { it.isNotBlank() }
                ?: oldHome.take(4)
        )
    }
    var workspacePackages by remember {
        mutableStateOf(
            (prefs.getString("workspace_slots", null)?.split("|")
                ?: oldHome.drop(4)).take(20).let { saved ->
                    saved + List((20 - saved.size).coerceAtLeast(0)) { "" }
                }
        )
    }
    var selectedApp by remember { mutableStateOf<LauncherApp?>(null) }
    var openedFolder by remember { mutableStateOf<String?>(null) }
    var iconShapeChoice by remember {
        mutableStateOf(runCatching { IconShapeChoice.valueOf(prefs.getString("icon_shape", "CIRCLE") ?: "CIRCLE") }
            .getOrDefault(IconShapeChoice.CIRCLE))
    }
    var folderOpacity by remember { mutableFloatStateOf(prefs.getFloat("folder_opacity", 0.88f).coerceIn(0.15f, 1f)) }
    var folderContents by remember {
        mutableStateOf(prefs.getStringSet("folder_contents", emptySet())?.mapNotNull { entry ->
            val split = entry.split(":", limit = 2)
            if (split.size == 2) split[0] to split[1].split(",").filter { it.isNotBlank() } else null
        }?.toMap().orEmpty())
    }
    var wallpaperUri by remember { mutableStateOf(prefs.getString("wallpaper_uri", null)) }

    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            wallpaperUri = uri.toString()
            prefs.edit().putString("wallpaper_uri", wallpaperUri).apply()
        }
    }
    val defaultWallpaper = remember {
        runCatching {
            val encoded = context.resources.openRawResource(R.raw.prosto_wallpaper).bufferedReader().use { it.readText() }
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

    val slotRects = remember { mutableStateMapOf<Int, Rect>() }
    val blockedRects = remember { mutableStateMapOf<String, Rect>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
    var dragPoint by remember { mutableStateOf(Offset.Zero) }

    fun saveDock(updated: List<String>) {
        dockPackages = updated.distinct().take(4)
        prefs.edit().putString("dock_apps_ordered", dockPackages.joinToString("|")).apply()
    }
    fun saveWorkspace(updated: List<String>) {
        workspacePackages = updated.take(20).let { slots ->
            slots + List((20 - slots.size).coerceAtLeast(0)) { "" }
        }
        prefs.edit()
            .putString("workspace_slots", workspacePackages.joinToString("|"))
            .putString("workspace_apps_ordered", workspacePackages.filter { it.isNotBlank() }.joinToString("|"))
            .apply()
    }
    fun addToWorkspace(pkg: String) {
        if (pkg in workspacePackages) {
            Toast.makeText(context, "Уже на рабочем столе", Toast.LENGTH_SHORT).show()
            return
        }
        val slots = workspacePackages.toMutableList()
        val emptyIndex = slots.indexOfFirst { it.isBlank() }
        if (emptyIndex < 0) {
            Toast.makeText(context, "Рабочий стол заполнен", Toast.LENGTH_SHORT).show()
            return
        }
        saveDock(dockPackages.filterNot { it == pkg })
        slots[emptyIndex] = pkg
        saveWorkspace(slots)
        Toast.makeText(context, "Добавлено на главный экран", Toast.LENGTH_SHORT).show()
    }
    fun addToDock(pkg: String) {
        if (pkg in dockPackages) {
            Toast.makeText(context, "Уже в нижней панели", Toast.LENGTH_SHORT).show()
            return
        }
        if (dockPackages.size >= 4) {
            Toast.makeText(context, "Нижняя панель заполнена", Toast.LENGTH_SHORT).show()
            return
        }
        saveWorkspace(workspacePackages.filterNot { it == pkg })
        saveDock(dockPackages + pkg)
        Toast.makeText(context, "Добавлено в нижнюю панель", Toast.LENGTH_SHORT).show()
    }
    fun removeFromLauncher(pkg: String) {
        saveWorkspace(workspacePackages.map { if (it == pkg) "" else it })
        saveDock(dockPackages.filterNot { it == pkg })
    }
    fun persistFolders(updated: Map<String, List<String>>) {
        folderContents = updated
        prefs.edit().putStringSet("folder_contents", updated.map { (key, values) ->
            "$key:${values.joinToString(",")}"
        }.toSet()).apply()
    }
    fun moveApp(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in workspacePackages.indices || toIndex !in workspacePackages.indices || fromIndex == toIndex) return
        val changed = workspacePackages.toMutableList()
        val moved = changed[fromIndex]
        val target = changed[toIndex]
        if (moved.isBlank()) return
        if (target.isNotBlank() && moved != target && !moved.startsWith("@folder@") && !target.startsWith("@folder@")) {
            val folderKey = "@folder@${System.currentTimeMillis()}"
            persistFolders(folderContents + (folderKey to listOf(moved, target)))
            changed[fromIndex] = ""
            changed[toIndex] = folderKey
        } else if (target.startsWith("@folder@") && !moved.startsWith("@folder@")) {
            val contents = folderContents[target].orEmpty()
            if (moved !in contents) persistFolders(folderContents + (target to (contents + moved)))
            changed[fromIndex] = ""
        } else {
            changed[fromIndex] = target
            changed[toIndex] = moved
        }
        saveWorkspace(changed)
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }
    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        apps = queryLauncherApps(context)
    }
    LaunchedEffect(Unit) {
        apps = queryLauncherApps(context)
    }

    val appMap = remember(apps) { apps.associateBy { it.packageName } }
    val dockApps = dockPackages.mapNotNull { appMap[it] }
    val filteredApps = apps.filter { it.label.contains(search, ignoreCase = true) }

    Box(
        Modifier.fillMaxSize().pointerInput(drawerOpen, settingsOpen, blockedRects.size) {
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
        } else Box(Modifier.fillMaxSize().background(DarkInk))
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) 0.12f else 0.03f)))

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(56.dp))
            Text(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 72.sp, lineHeight = 78.sp, fontWeight = FontWeight.Light, color = Color.White
            )
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")))
                    .replaceFirstChar { it.titlecase(Locale("ru")) },
                style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.88f)
            )
            Spacer(Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(20, key = { it }) { index ->
                    val pkg = workspacePackages.getOrNull(index).orEmpty()
                    val app = appMap[pkg]
                    val isFolder = pkg.startsWith("@folder@") && pkg in folderContents
                    Box(
                        Modifier.fillMaxWidth().height(86.dp)
                            .onGloballyPositioned { coordinates ->
                                val rect = coordinates.boundsInRoot()
                                slotRects[index] = rect
                                blockedRects["workspace-$index"] = rect
                            }
                            .pointerInput(index, workspacePackages) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { start ->
                                        if (app != null || isFolder) {
                                            draggingIndex = index
                                            dropTargetIndex = null
                                            val rect = slotRects[index]
                                            if (rect != null) dragPoint = Offset(rect.left + start.x, rect.top + start.y)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        if (draggingIndex != null) {
                                            change.consume()
                                            val rect = slotRects[index]
                                            if (rect != null) {
                                                dragPoint = Offset(rect.left + change.position.x, rect.top + change.position.y)
                                                dropTargetIndex = slotRects.entries.firstOrNull { (_, bounds) ->
                                                    bounds.contains(dragPoint)
                                                }?.key
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val from = draggingIndex
                                        val target = dropTargetIndex
                                        if (from != null && target != null) moveApp(from, target)
                                        draggingIndex = null
                                        dropTargetIndex = null
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dropTargetIndex = null
                                    }
                                )
                            }
                            .clickable(enabled = (app != null || isFolder) && draggingIndex == null) {
                                if (isFolder) openedFolder = pkg else app?.let { launchApp(context, it.packageName) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFolder) {
                            Column(
                                Modifier.fillMaxWidth().graphicsLayer {
                                    alpha = if (draggingIndex == index) 0.45f else 1f
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier.size(54.dp).clip(RoundedCornerShape(15.dp))
                                        .background(Purple.copy(alpha = folderOpacity))
                                        .border(1.dp, Lavender.copy(alpha = 0.75f), RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        val members = folderContents[pkg].orEmpty().take(4).mapNotNull { appMap[it] }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            members.take(2).forEach { AppImage(it, 20, iconShapeChoice) }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            members.drop(2).take(2).forEach { AppImage(it, 20, iconShapeChoice) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Папка", fontSize = 11.sp, maxLines = 1, color = Color.White)
                            }
                        } else if (app != null) {
                            Column(
                                Modifier.fillMaxWidth().graphicsLayer {
                                    alpha = if (draggingIndex == index) 0.45f else 1f
                                    scaleX = if (draggingIndex == index) 1.06f else 1f
                                    scaleY = if (draggingIndex == index) 1.06f else 1f
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppImage(app, 54, iconShapeChoice)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    app.label, fontSize = 11.sp, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = Color.White
                                )
                            }
                        }
                        if (dropTargetIndex == index) {
                            Box(
                                Modifier.matchParentSize().padding(2.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Lavender.copy(alpha = 0.28f))
                                    .border(2.dp, Lavender, RoundedCornerShape(14.dp))
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DockSlot(dockApps.getOrNull(0), "dock-left-1", blockedRects, iconShapeChoice) { launchApp(context, it.packageName) }
                DockSlot(dockApps.getOrNull(1), "dock-left-2", blockedRects, iconShapeChoice) { launchApp(context, it.packageName) }
                Surface(
                    modifier = Modifier.size(54.dp).onGloballyPositioned { blockedRects["drawer-button"] = it.boundsInRoot() }
                        .clickable { drawerOpen = true },
                    shape = CircleShape, color = Color(0xFFEDE5FF).copy(alpha = 0.92f), shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Все приложения", tint = DarkInk, modifier = Modifier.size(34.dp))
                    }
                }
                DockSlot(dockApps.getOrNull(2), "dock-right-1", blockedRects, iconShapeChoice) { launchApp(context, it.packageName) }
                DockSlot(dockApps.getOrNull(3), "dock-right-2", blockedRects, iconShapeChoice) { launchApp(context, it.packageName) }
            }
        }

        AnimatedVisibility(
            visible = drawerOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(300)) +
                slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(300)),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.97f, animationSpec = tween(180)) +
                slideOutVertically(targetOffsetY = { it / 8 }, animationSpec = tween(180))
        ) {
            Column(Modifier.fillMaxSize().background(Color(0xFF171326).copy(alpha = 0.97f)).padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Все приложения", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { drawerOpen = false }) { Text("Готово", color = Lavender) }
                }
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = RoundedCornerShape(28.dp), placeholder = { Text("Поиск приложений") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Lavender, unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedLeadingIconColor = Lavender, unfocusedLeadingIconColor = Color.White
                    )
                )
                Text(
                    "Зажми приложение, чтобы открыть меню действий",
                    color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp), modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(
                                onClick = { launchApp(context, app.packageName) },
                                onLongClick = { selectedApp = app }
                            ).padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppImage(app, 52, iconShapeChoice)
                            Spacer(Modifier.height(4.dp))
                            Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 2,
                                textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        selectedApp?.let { app ->
            val alreadyOnHome = app.packageName in workspacePackages || app.packageName in dockPackages
            AlertDialog(
                onDismissRequest = { selectedApp = null },
                title = { Text(app.label) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!alreadyOnHome) {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { addToWorkspace(app.packageName); selectedApp = null; drawerOpen = false }
                            ) { Text("На главный экран", modifier = Modifier.fillMaxWidth()) }
                        } else {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    removeFromLauncher(app.packageName)
                                    selectedApp = null
                                    Toast.makeText(context, "Убрано с главного экрана", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Убрать с главного экрана", color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth())
                            }
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { addToDock(app.packageName); selectedApp = null; drawerOpen = false }
                        ) { Text("В нижн. панель", modifier = Modifier.fillMaxWidth()) }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedApp = null
                                runCatching {
                                    context.startActivity(Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${app.packageName}")
                                    ))
                                }
                            }
                        ) { Text("О приложении", modifier = Modifier.fillMaxWidth()) }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedApp = null
                                runCatching {
                                    uninstallLauncher.launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
                                }.onFailure {
                                    Toast.makeText(context, "Не удалось открыть удаление", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selectedApp = null }) { Text("Закрыть") } }
            )
        }

        if (settingsOpen) {
            Dialog(
                onDismissRequest = { settingsOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Настройки главного экрана", style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = { settingsOpen = false }) { Text("Готово") }
                        }
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 30.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text("Тема", style = MaterialTheme.typography.titleLarge, color = Purple)
                                ThemeRow("System", themeChoice == LauncherTheme.SYSTEM) { onThemeChange(LauncherTheme.SYSTEM) }
                                ThemeRow("Light", themeChoice == LauncherTheme.LIGHT) { onThemeChange(LauncherTheme.LIGHT) }
                                ThemeRow("Dark", themeChoice == LauncherTheme.DARK) { onThemeChange(LauncherTheme.DARK) }
                            }
                            item {
                                Divider()
                                Text("Настройка иконок", style = MaterialTheme.typography.titleLarge, color = Purple)
                                IconShapeChoice.values().forEach { choice ->
                                    val label = when (choice) {
                                        IconShapeChoice.CIRCLE -> "Circle — круг"
                                        IconShapeChoice.SQUARE -> "Square — квадрат"
                                        IconShapeChoice.TRIANGLE -> "Triangle — треугольник"
                                        IconShapeChoice.COOKIE_12 -> "12-sided cookie — 12 сторон"
                                        IconShapeChoice.SOFT_BURST -> "Soft burst — мягкая звезда"
                                        IconShapeChoice.CLOVER_4 -> "4-leaf clover — клевер"
                                    }
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            iconShapeChoice = choice
                                            prefs.edit().putString("icon_shape", choice.name).apply()
                                        }.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = iconShapeChoice == choice, onClick = {
                                            iconShapeChoice = choice
                                            prefs.edit().putString("icon_shape", choice.name).apply()
                                        })
                                        Spacer(Modifier.width(8.dp))
                                        apps.firstOrNull()?.let { AppImage(it, 34, choice) }
                                        Spacer(Modifier.width(10.dp))
                                        Text(label)
                                    }
                                }
                                Divider()
                                Text("Прозрачность папок", style = MaterialTheme.typography.titleMedium)
                                Text("${(folderOpacity * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(value = folderOpacity, onValueChange = {
                                    folderOpacity = it
                                    prefs.edit().putFloat("folder_opacity", it).apply()
                                }, valueRange = 0.15f..1f)
                                Divider()
                                Text("Обои", style = MaterialTheme.typography.titleMedium)
                                Text("Выбери изображение из галереи или верни стандартные обои.",
                                    style = MaterialTheme.typography.bodySmall)
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
                        }
                    }
                }
            }
        }

        openedFolder?.let { folderKey ->
            val members = folderContents[folderKey].orEmpty()
            AlertDialog(
                onDismissRequest = { openedFolder = null },
                title = { Text("Папка") },
                text = {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Приложения в папке", style = MaterialTheme.typography.titleSmall)
                        members.forEach { pkg ->
                            val memberApp = appMap[pkg]
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(memberApp?.label ?: pkg, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val updatedMembers = members - pkg
                                    val updatedFolders = if (updatedMembers.size < 2) folderContents - folderKey
                                        else folderContents + (folderKey to updatedMembers)
                                    persistFolders(updatedFolders)
                                    if (updatedMembers.size < 2) {
                                        val slots = workspacePackages.toMutableList()
                                        val slot = slots.indexOf(folderKey)
                                        if (slot >= 0) {
                                            slots[slot] = updatedMembers.firstOrNull().orEmpty()
                                            saveWorkspace(slots)
                                        }
                                        openedFolder = null
                                    }
                                }) { Text("Убрать") }
                            }
                        }
                        Divider()
                        Text("Добавить приложение", style = MaterialTheme.typography.titleSmall)
                        apps.filter { it.packageName !in folderContents.values.flatten() }.forEach { candidate ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    saveWorkspace(workspacePackages.map { if (it == candidate.packageName) "" else it })
                                    saveDock(dockPackages.filterNot { it == candidate.packageName })
                                    persistFolders(folderContents + (folderKey to (members + candidate.packageName)))
                                }
                            ) { Text(candidate.label, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { openedFolder = null }) { Text("Закрыть") } }
            )
        }

    }
}

private fun queryLauncherApps(context: android.content.Context): List<LauncherApp> {
    val pm = context.packageManager
    return runCatching {
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

private fun iconShape(choice: IconShapeChoice): Shape {
    return when (choice) {
        IconShapeChoice.CIRCLE -> CircleShape
        IconShapeChoice.SQUARE -> RectangleShape
        IconShapeChoice.TRIANGLE -> GenericShape { size, _ ->
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        IconShapeChoice.COOKIE_12 -> GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            for (i in 0 until 12) {
                val angle = (Math.PI * 2.0 * i / 12.0) - Math.PI / 2.0
                val x = cx + radius * kotlin.math.cos(angle).toFloat()
                val y = cy + radius * kotlin.math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        IconShapeChoice.SOFT_BURST -> GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            for (i in 0 until 24) {
                val angle = (Math.PI * 2.0 * i / 24.0) - Math.PI / 2.0
                val r = radius * if (i % 2 == 0) 1f else 0.82f
                val x = cx + r * kotlin.math.cos(angle).toFloat()
                val y = cy + r * kotlin.math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        IconShapeChoice.CLOVER_4 -> GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            moveTo(w * 0.5f, h * 0.98f)
            cubicTo(w * 0.38f, h * 0.78f, w * 0.12f, h * 0.78f, w * 0.08f, h * 0.58f)
            cubicTo(w * 0.02f, h * 0.38f, w * 0.22f, h * 0.28f, w * 0.42f, h * 0.44f)
            cubicTo(w * 0.28f, h * 0.22f, w * 0.38f, h * 0.02f, w * 0.58f, h * 0.08f)
            cubicTo(w * 0.78f, h * 0.12f, w * 0.78f, h * 0.38f, w * 0.56f, h * 0.48f)
            cubicTo(w * 0.78f, h * 0.28f, w * 0.98f, h * 0.38f, w * 0.92f, h * 0.58f)
            cubicTo(w * 0.88f, h * 0.78f, w * 0.62f, h * 0.78f, w * 0.5f, h * 0.98f)
            close()
        }
    }
}

@Composable
private fun DockSlot(
    app: LauncherApp?,
    key: String,
    blockedRects: MutableMap<String, Rect>,
    iconShapeChoice: IconShapeChoice,
    onClick: (LauncherApp) -> Unit
) {
    Box(Modifier.size(48.dp).onGloballyPositioned { blockedRects[key] = it.boundsInRoot() }, contentAlignment = Alignment.Center) {
        if (app != null) {
            Box(Modifier.fillMaxSize().clickable { onClick(app) }, contentAlignment = Alignment.Center) {
                AppImage(app, 46, iconShapeChoice)
            }
        }
    }
}

@Composable
private fun AppImage(app: LauncherApp, size: Int, iconShapeChoice: IconShapeChoice = IconShapeChoice.CIRCLE) {
    AndroidView(
        factory = { ctx -> ImageView(ctx).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { it.setImageDrawable(app.icon) },
        modifier = Modifier.size(size.dp).padding(2.dp).clip(iconShape(iconShapeChoice))
    )
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
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
