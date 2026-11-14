package com.prosto.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.widget.ImageView
import android.widget.Toast
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 2048) }
    var hostedWidgetIds by remember {
        mutableStateOf(prefs.getString("hosted_widget_ids", "").orEmpty()
            .split(",").mapNotNull { it.toIntOrNull() })
    }
    DisposableEffect(appWidgetHost) {
        runCatching { appWidgetHost.startListening() }
        onDispose { runCatching { appWidgetHost.stopListening() } }
    }
    val widgetPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == android.app.Activity.RESULT_OK && id > 0) {
            hostedWidgetIds = hostedWidgetIds + id
            prefs.edit().putString("hosted_widget_ids", hostedWidgetIds.joinToString(",")).apply()
        } else if (id > 0) {
            appWidgetHost.deleteAppWidgetId(id)
        }
    }
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var search by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var widgetsOpen by remember { mutableStateOf(false) }
    var quickMenuOpen by remember { mutableStateOf(false) }
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
    // Отдельный флаг видимости нужен, чтобы карточка папки успевала плавно закрыться.
    var folderVisible by remember { mutableStateOf(false) }
    var folderOverlayAlive by remember { mutableStateOf(false) }
    var folderOrigin by remember { mutableStateOf(Offset.Unspecified) }
    var overlayCenter by remember { mutableStateOf(Offset.Zero) }
    val folderProgress = remember { Animatable(0f) }
    var folderMenuKey by remember { mutableStateOf<String?>(null) }
    var renamingFolder by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var memberMenu by remember { mutableStateOf<Pair<String, String>?>(null) }
    var addToFolderKey by remember { mutableStateOf<String?>(null) }
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
    var folderNames by remember {
        mutableStateOf(prefs.getStringSet("folder_names", emptySet())?.mapNotNull { entry ->
            val split = entry.split(":", limit = 2)
            if (split.size == 2 && split[1].isNotBlank()) split[0] to split[1] else null
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
    fun persistFolderNames(updated: Map<String, String>) {
        folderNames = updated
        prefs.edit().putStringSet("folder_names", updated.map { (key, value) -> "$key:$value" }.toSet()).apply()
    }
    fun renameFolder(key: String, newName: String) {
        val clean = newName.trim().take(24)
        persistFolderNames(if (clean.isBlank()) folderNames - key else folderNames + (key to clean))
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

    fun folderTitle(key: String): String = folderNames[key] ?: "Папка"

    // Достаёт приложение из папки. Если в папке остаётся меньше двух приложений, папка исчезает,
    // а оставшееся приложение встаёт на её место. Возвращает true, если папка исчезла.
    fun takeOutOfFolder(folderKey: String, pkg: String, placeOnDesktop: Boolean): Boolean {
        val members = folderContents[folderKey].orEmpty()
        val rest = members - pkg
        val slots = workspacePackages.toMutableList()
        val dissolve = rest.size < 2
        if (dissolve) {
            persistFolders(folderContents - folderKey)
            persistFolderNames(folderNames - folderKey)
            val remaining = rest.firstOrNull().orEmpty()
            val slot = slots.indexOf(folderKey)
            if (slot >= 0) {
                slots[slot] = remaining
            } else if (remaining.isNotBlank()) {
                val empty = slots.indexOfFirst { it.isBlank() }
                if (empty >= 0) slots[empty] = remaining
            }
        } else {
            persistFolders(folderContents + (folderKey to rest))
        }
        if (placeOnDesktop) {
            val empty = slots.indexOfFirst { it.isBlank() }
            if (empty >= 0) {
                slots[empty] = pkg
            } else {
                Toast.makeText(context, "На рабочем столе нет места — приложение осталось в списке всех приложений", Toast.LENGTH_LONG).show()
            }
        }
        saveWorkspace(slots)
        return dissolve
    }

    fun addAppToFolder(folderKey: String, pkg: String) {
        val members = folderContents[folderKey].orEmpty()
        if (pkg in members) return
        saveWorkspace(workspacePackages.map { if (it == pkg) "" else it })
        saveDock(dockPackages.filterNot { it == pkg })
        persistFolders(folderContents + (folderKey to (members + pkg)))
    }

    fun moveAppBetweenFolders(fromKey: String, pkg: String, toKey: String) {
        if (fromKey == toKey) return
        takeOutOfFolder(fromKey, pkg, placeOnDesktop = false)
        persistFolders(folderContents + (toKey to (folderContents[toKey].orEmpty() + pkg)))
    }

    // Удаляет папку целиком. Приложения из неё возвращаются на рабочий стол (если есть место).
    fun deleteFolder(key: String) {
        val members = folderContents[key].orEmpty().filter { it in appMap }
        val slots = workspacePackages.toMutableList()
        val slot = slots.indexOf(key)
        if (slot >= 0) slots[slot] = ""
        var notPlaced = 0
        members.forEach { member ->
            val empty = slots.indexOfFirst { it.isBlank() }
            if (empty >= 0) slots[empty] = member else notPlaced++
        }
        saveWorkspace(slots)
        persistFolders(folderContents - key)
        persistFolderNames(folderNames - key)
        if (notPlaced > 0) {
            Toast.makeText(context, "Не все приложения поместились на рабочий стол — остальные есть в списке всех приложений", Toast.LENGTH_LONG).show()
        }
    }

    // Запуск приложения с анимацией «из иконки» (как на iPhone): система увеличивает окно приложения из точки, где лежала иконка.
    val rootView = LocalView.current
    fun launch(pkg: String, bounds: Rect?) {
        launchApp(context, pkg, rootView, bounds?.let { iconRectFor(it, rootView.resources.displayMetrics.density) })
    }

    // Открывает папку «из иконки»: запоминаем, где иконка папки лежит на экране.
    fun openFolder(key: String) {
        val slot = workspacePackages.indexOf(key)
        folderOrigin = slotRects[slot]?.center ?: Offset.Unspecified
        openedFolder = key
        folderVisible = true
    }

    // Анимация как на iPhone: карточка пружинисто вырастает из иконки, закрытие — плавно сворачивается обратно.
    LaunchedEffect(folderVisible) {
        if (folderVisible) {
            folderOverlayAlive = true
            folderProgress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f))
        } else {
            folderProgress.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            folderOverlayAlive = false
        }
    }

    BackHandler(enabled = folderVisible) { folderVisible = false }

    Box(
        Modifier.fillMaxSize().pointerInput(drawerOpen, settingsOpen, widgetsOpen, quickMenuOpen, folderVisible, blockedRects.size, workspacePackages, dockPackages) {
            detectTapGestures(
                onTap = { point ->
                    val blocked = blockedRects.any { (key, rect) ->
                        val occupiedWorkspace = key.startsWith("workspace-") &&
                            workspacePackages.getOrNull(key.substringAfter("-").toIntOrNull() ?: -1).orEmpty().isNotBlank()
                        val occupiedDock = key.startsWith("dock-") &&
                            dockPackages.getOrNull(key.substringAfterLast("-").toIntOrNull()?.minus(1) ?: -1) != null
                        (occupiedWorkspace || occupiedDock || key == "drawer-button") && rect.contains(point)
                    }
                    if (!blocked && !drawerOpen && !folderVisible && !settingsOpen && !widgetsOpen) {
                        quickMenuOpen = true
                    }
                },
                onLongPress = { point ->
                    val blocked = blockedRects.any { (key, rect) ->
                        val occupiedWorkspace = key.startsWith("workspace-") &&
                            workspacePackages.getOrNull(key.substringAfter("-").toIntOrNull() ?: -1).orEmpty().isNotBlank()
                        val occupiedDock = key.startsWith("dock-") &&
                            dockPackages.getOrNull(key.substringAfterLast("-").toIntOrNull()?.minus(1) ?: -1) != null
                        (occupiedWorkspace || occupiedDock || key == "drawer-button") && rect.contains(point)
                    }
                    if (!blocked && !drawerOpen && !folderVisible) quickMenuOpen = true
                }
            )
        }
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val p = folderProgress.value.coerceIn(0f, 1f)
                scaleX = 1f - 0.06f * p
                scaleY = 1f - 0.06f * p
                if (p > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val radius = 24f * p * density
                    renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                }
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
            Spacer(Modifier.height(24.dp))

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
                                var pressStart = Offset.Zero
                                var movedFar = false
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { start ->
                                        pressStart = start
                                        movedFar = false
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
                                            if ((change.position - pressStart).getDistance() > 24f) movedFar = true
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
                                        // Долгое нажатие на папку без перетаскивания — меню действий.
                                        if (from != null && isFolder && !movedFar) folderMenuKey = pkg
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
                                if (isFolder) openFolder(pkg) else app?.let { launch(it.packageName, slotRects[index]) }
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
                                Text(
                                    folderNames[pkg] ?: "Папка", fontSize = 11.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = Color.White
                                )
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
                DockSlot(dockApps.getOrNull(0), "dock-left-1", blockedRects, iconShapeChoice) { a, r -> launch(a.packageName, r) }
                DockSlot(dockApps.getOrNull(1), "dock-left-2", blockedRects, iconShapeChoice) { a, r -> launch(a.packageName, r) }
                Surface(
                    modifier = Modifier.size(54.dp).onGloballyPositioned { blockedRects["drawer-button"] = it.boundsInRoot() }
                        .clickable { drawerOpen = true },
                    shape = CircleShape, color = Color(0xFFEDE5FF).copy(alpha = 0.92f), shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Все приложения", tint = DarkInk, modifier = Modifier.size(34.dp))
                    }
                }
                DockSlot(dockApps.getOrNull(2), "dock-right-1", blockedRects, iconShapeChoice) { a, r -> launch(a.packageName, r) }
                DockSlot(dockApps.getOrNull(3), "dock-right-2", blockedRects, iconShapeChoice) { a, r -> launch(a.packageName, r) }
            }
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
                        val cellBounds = remember { arrayOfNulls<Rect>(1) }
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .onGloballyPositioned { cellBounds[0] = it.boundsInRoot() }
                                .combinedClickable(
                                onClick = { launch(app.packageName, cellBounds[0]) },
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
                                    uninstallLauncher.launch(Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:${app.packageName}")).putExtra(Intent.EXTRA_RETURN_RESULT, true))
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

        if (quickMenuOpen) {
            Dialog(onDismissRequest = { quickMenuOpen = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Prosto", style = MaterialTheme.typography.titleLarge)
                        FilledTonalButton(
                            onClick = { quickMenuOpen = false; widgetsOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Виджеты") }
                        FilledTonalButton(
                            onClick = { quickMenuOpen = false; settingsOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Настройки лаунчера") }
                    }
                }
            }
        }

        if (widgetsOpen) {
            Dialog(
                onDismissRequest = { widgetsOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Виджеты", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { widgetsOpen = false }) { Text("Готово") }
                        }
                        Text("Часы", style = MaterialTheme.typography.titleMedium, color = Purple)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DigitalClockWidget(now, Modifier.weight(1f))
                            AnalogClockWidget(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Другие виджеты Android", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            FilledTonalButton(onClick = {
                                val id = appWidgetHost.allocateAppWidgetId()
                                val pick = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                                }
                                runCatching { widgetPicker.launch(pick) }
                                    .onFailure { appWidgetHost.deleteAppWidgetId(id) }
                            }) { Text("Добавить") }
                        }
                        Text("Выбери виджет из приложений, установленных на устройстве.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            lazyItems(hostedWidgetIds, key = { it }) { widgetId ->
                                val provider = appWidgetManager.getAppWidgetInfo(widgetId)
                                if (provider != null) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                            Text(provider.loadLabel(pm), style = MaterialTheme.typography.labelLarge)
                                            AndroidView(
                                                factory = { appWidgetHost.createView(it, widgetId, provider).apply {
                                                    setAppWidget(widgetId, provider)
                                                } },
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 320.dp)
                                            )
                                            TextButton(onClick = {
                                                appWidgetHost.deleteAppWidgetId(widgetId)
                                                hostedWidgetIds = hostedWidgetIds - widgetId
                                                prefs.edit().putString("hosted_widget_ids", hostedWidgetIds.joinToString(",")).apply()
                                            }) { Text("Удалить виджет", color = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (settingsOpen) {
            Dialog(
                onDismissRequest = { settingsOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Настройки лаунчера", style = MaterialTheme.typography.headlineSmall,
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

        // ---------- Открытая папка: светлая полупрозрачная карточка по центру ----------
        if (folderOverlayAlive) {
            val screenHeightDp = LocalConfiguration.current.screenHeightDp
            val folderKey = openedFolder
            val memberApps = folderKey?.let { folderContents[it] }.orEmpty().mapNotNull { appMap[it] }
            val cardAlpha = (0.45f + folderOpacity * 0.5f).coerceIn(0.5f, 0.95f)
            Box(
                Modifier.fillMaxSize()
                    .onGloballyPositioned { overlayCenter = Offset(it.size.width / 2f, it.size.height / 2f) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        folderVisible = false
                    },
                contentAlignment = Alignment.Center
            ) {
                // Затемнение фона плавно проявляется вместе с размытием.
                Box(
                    Modifier.matchParentSize()
                        .graphicsLayer { alpha = folderProgress.value.coerceIn(0f, 1f) }
                        .background(Color.Black.copy(alpha = 0.28f))
                )
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val raw = folderProgress.value
                            val scale = 0.14f + 0.86f * raw
                            scaleX = scale
                            scaleY = scale
                            if (folderOrigin.isSpecified) {
                                translationX = (folderOrigin.x - overlayCenter.x) * (1f - raw)
                                translationY = (folderOrigin.y - overlayCenter.y) * (1f - raw)
                            }
                            alpha = (raw.coerceIn(0f, 1f) * 2.2f).coerceIn(0f, 1f)
                        }
                        // Нажатие внутри карточки не должно закрывать папку.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFFF4EEFF).copy(alpha = cardAlpha),
                    shadowElevation = 20.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                ) {
                    Column(
                        Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            Modifier.fillMaxWidth()
                                .heightIn(max = (screenHeightDp * 0.5f).dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Сетка по 3 иконки в ряд; последней ячейкой идёт «Добавить».
                            val cells = memberApps.map<LauncherApp, LauncherApp?> { it } + listOf<LauncherApp?>(null)
                            cells.chunked(3).forEach { rowCells ->
                                Row(Modifier.fillMaxWidth()) {
                                    rowCells.forEach { cell ->
                                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                            if (cell != null && folderKey != null) {
                                                FolderAppCell(
                                                    app = cell,
                                                    iconShapeChoice = iconShapeChoice,
                                                    onClick = { bounds ->
                                                        folderVisible = false
                                                        launch(cell.packageName, bounds)
                                                    },
                                                    onLongClick = { memberMenu = folderKey to cell.packageName }
                                                )
                                            } else if (folderKey != null) {
                                                FolderAddCell { addToFolderKey = folderKey }
                                            }
                                        }
                                    }
                                    repeat(3 - rowCells.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = folderKey?.let { folderTitle(it) } ?: "Папка",
                            color = DarkInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = folderKey != null) {
                                folderKey?.let {
                                    renameText = folderNames[it].orEmpty()
                                    renamingFolder = it
                                }
                            }.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // ---------- Меню долгого нажатия на папку на рабочем столе ----------
        folderMenuKey?.let { key ->
            AlertDialog(
                onDismissRequest = { folderMenuKey = null },
                title = { Text(folderTitle(key)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                folderMenuKey = null
                                openFolder(key)
                            }
                        ) { Text("Открыть", modifier = Modifier.fillMaxWidth()) }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                folderMenuKey = null
                                renameText = folderNames[key].orEmpty()
                                renamingFolder = key
                            }
                        ) { Text("Переименовать", modifier = Modifier.fillMaxWidth()) }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                folderMenuKey = null
                                deleteFolder(key)
                                Toast.makeText(context, "Папка удалена", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Удалить папку", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { folderMenuKey = null }) { Text("Закрыть") } }
            )
        }

        // ---------- Переименование папки ----------
        renamingFolder?.let { key ->
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(key) { runCatching { focusRequester.requestFocus() } }
            AlertDialog(
                onDismissRequest = { renamingFolder = null },
                title = { Text("Переименовать папку") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it.take(24) },
                        singleLine = true,
                        placeholder = { Text("Папка") },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        renameFolder(key, renameText)
                        renamingFolder = null
                    }) { Text("Сохранить") }
                },
                dismissButton = { TextButton(onClick = { renamingFolder = null }) { Text("Отмена") } }
            )
        }

        // ---------- Меню приложения внутри папки ----------
        memberMenu?.let { (folderKey, pkg) ->
            val memberApp = appMap[pkg]
            val otherFolders = folderContents.keys.filter { it != folderKey }
            val otherTitles = otherFolders.map { folderTitle(it) }
            AlertDialog(
                onDismissRequest = { memberMenu = null },
                title = { Text(memberApp?.label ?: pkg) },
                text = {
                    Column(
                        Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                memberMenu = null
                                val dissolved = takeOutOfFolder(folderKey, pkg, placeOnDesktop = true)
                                if (dissolved) folderVisible = false
                            }
                        ) { Text("Убрать из папки", modifier = Modifier.fillMaxWidth()) }
                        otherFolders.forEachIndexed { i, otherKey ->
                            val title = otherTitles[i]
                            val label = if (otherTitles.count { it == title } > 1) {
                                "$title (${otherTitles.take(i + 1).count { it == title }})"
                            } else title
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    memberMenu = null
                                    val willDissolve = (folderContents[folderKey]?.size ?: 0) < 3
                                    moveAppBetweenFolders(folderKey, pkg, otherKey)
                                    if (willDissolve) folderVisible = false
                                    Toast.makeText(context, "Перемещено в «$title»", Toast.LENGTH_SHORT).show()
                                }
                            ) { Text("Переместить в «$label»", modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { memberMenu = null }) { Text("Закрыть") } }
            )
        }

        // ---------- Добавление приложений в папку ----------
        addToFolderKey?.let { folderKey ->
            val inFolders = folderContents.values.flatten().toSet()
            val candidates = apps.filter { it.packageName !in inFolders }
            AlertDialog(
                onDismissRequest = { addToFolderKey = null },
                title = { Text("Добавить в папку") },
                text = {
                    if (candidates.isEmpty()) {
                        Text("Все приложения уже лежат в папках")
                    } else {
                        LazyColumn(
                            Modifier.heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            lazyItems(candidates, key = { it.packageName }) { candidate ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .clickable { addAppToFolder(folderKey, candidate.packageName) }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppImage(candidate, 38, iconShapeChoice)
                                    Spacer(Modifier.width(12.dp))
                                    Text(candidate.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { addToFolderKey = null }) { Text("Готово") } }
            )
        }
    }
}

@Composable
private fun DigitalClockWidget(now: LocalDateTime, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF171326),
        border = BorderStroke(1.dp, Purple.copy(alpha = 0.8f))
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 38.sp,
                fontWeight = FontWeight.Bold, color = Lavender)
            Text(now.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))),
                color = Lavender, fontSize = 13.sp)
            Text("C6", color = Lavender.copy(alpha = 0.75f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun AnalogClockWidget(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(150.dp),
        shape = CircleShape,
        color = Color(0xFF171326),
        border = BorderStroke(2.dp, Purple)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("12\n\n9                         3\n\n6", color = Lavender,
                textAlign = TextAlign.Center, fontSize = 16.sp, lineHeight = 18.sp)
            Box(Modifier.width(3.dp).height(48.dp).offset(y = (-13).dp)
                .clip(CircleShape).background(Lavender))
            Box(Modifier.width(3.dp).height(35.dp).offset(x = 13.dp, y = 7.dp)
                .graphicsLayer { rotationZ = 55f }.clip(CircleShape).background(Purple))
            Box(Modifier.size(10.dp).clip(CircleShape).background(Purple))
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
    onClick: (LauncherApp, Rect?) -> Unit
) {
    Box(Modifier.size(48.dp).onGloballyPositioned { blockedRects[key] = it.boundsInRoot() }, contentAlignment = Alignment.Center) {
        if (app != null) {
            Box(Modifier.fillMaxSize().clickable { onClick(app, blockedRects[key]) }, contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderAppCell(
    app: LauncherApp,
    iconShapeChoice: IconShapeChoice,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit
) {
    val bounds = remember { arrayOfNulls<Rect>(1) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .onGloballyPositioned { bounds[0] = it.boundsInRoot() }
            .combinedClickable(onClick = { onClick(bounds[0]) }, onLongClick = onLongClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppImage(app, 58, iconShapeChoice)
        Spacer(Modifier.height(4.dp))
        Text(
            app.label, fontSize = 12.sp, maxLines = 2, color = DarkInk,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FolderAddCell(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.padding(2.dp).size(54.dp).clip(CircleShape).background(Purple.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить приложение", tint = Purple, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("Добавить", fontSize = 12.sp, maxLines = 1, color = DarkInk, textAlign = TextAlign.Center)
    }
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

// Квадрат вокруг иконки: от него система «разворачивает» окно запускаемого приложения.
private fun iconRectFor(bounds: Rect, density: Float): Rect {
    val side = minOf(bounds.width, 56f * density)
    val topShift = if (bounds.height > side * 1.3f) 6f * density else 0f
    return Rect(Offset(bounds.center.x, bounds.top + topShift + side / 2f), side / 2f)
}

private fun launchApp(
    context: android.content.Context,
    packageName: String,
    view: android.view.View? = null,
    from: Rect? = null
) {
    runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return@runCatching
        val options = if (view != null && from != null) {
            android.app.ActivityOptions.makeScaleUpAnimation(
                view,
                from.left.toInt(),
                from.top.toInt(),
                from.width.toInt().coerceAtLeast(1),
                from.height.toInt().coerceAtLeast(1)
            ).toBundle()
        } else null
        context.startActivity(intent, options)
    }
}
