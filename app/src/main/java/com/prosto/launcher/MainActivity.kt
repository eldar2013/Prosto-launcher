
package com.prosto.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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

private enum class LauncherTheme {
    SYSTEM, LIGHT, DARK
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProstoHome()
        }
    }
}

@Composable
private fun ProstoHome() {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = remember {
        context.getSharedPreferences("prosto_launcher", 0)
    }

    var themeChoice by remember {
        mutableStateOf(
            runCatching {
                LauncherTheme.valueOf(
                    prefs.getString("theme_choice", "SYSTEM") ?: "SYSTEM"
                )
            }.getOrDefault(LauncherTheme.SYSTEM)
        )
    }

    val dark = when (themeChoice) {
        LauncherTheme.LIGHT -> false
        LauncherTheme.DARK -> true
        LauncherTheme.SYSTEM ->
            androidx.compose.foundation.isSystemInDarkTheme()
    }

    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark ->
            dynamicDarkColorScheme(context)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)

        dark -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = scheme) {
        ProstoContent(
            pm = pm,
            prefs = prefs,
            themeChoice = themeChoice
        ) { selected ->
            themeChoice = selected
            prefs.edit()
                .putString("theme_choice", selected.name)
                .apply()
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun ProstoContent(
    pm: PackageManager,
    prefs: android.content.SharedPreferences,
    themeChoice: LauncherTheme,
    onThemeChange: (LauncherTheme) -> Unit
) {
    val context = LocalContext.current

    var apps by remember {
        mutableStateOf(emptyList<LauncherApp>())
    }

    var search by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var visible by remember { mutableStateOf(false) }

    // Сохраняем порядок приложений
    var homePackages by remember {
        mutableStateOf(
            prefs.getString("home_apps_ordered", null)
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?: prefs.getStringSet("home_apps", emptySet())
                    ?.toList()
                    .orEmpty()
        )
    }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "homeAlpha"
    )

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(700),
        label = "homeScale"
    )

    // Анимация запуска и обновление времени
    LaunchedEffect(Unit) {
        delay(120)
        visible = true

        while (true) {
            now = LocalDateTime.now()
            delay(15000)
        }
    }

    // Получение списка приложений
    LaunchedEffect(Unit) {
        apps = runCatching {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)

            val entries = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            entries.mapNotNull { info ->
                runCatching {
                    val pkg = info.activityInfo?.packageName
                        ?: return@runCatching null

                    if (pkg == context.packageName) {
                        return@runCatching null
                    }

                    LauncherApp(
                        info.loadLabel(pm)?.toString()
                            .orEmpty()
                            .ifBlank { pkg },
                        pkg,
                        info.loadIcon(pm)
                    )
                }.getOrNull()
            }
                .distinctBy { it.packageName }
                .sortedBy {
                    it.label.lowercase(Locale.getDefault())
                }
        }.getOrElse {
            emptyList()
        }
    }

    val appMap = remember(apps) {
        apps.associateBy { it.packageName }
    }

    val homeApps = homePackages.mapNotNull {
        appMap[it]
    }

    val filteredApps = apps.filter {
        it.label.contains(search, ignoreCase = true)
    }

    fun saveHome(updated: List<String>) {
        homePackages = updated.distinct()

        prefs.edit()
            .putString(
                "home_apps_ordered",
                homePackages.joinToString("|")
            )
            .apply()
    }

    Box(Modifier.fillMaxSize()) {

        // ГЛАВНЫЙ ЭКРАН
        Column(
            Modifier
                .fillMaxSize()
                .blur(if (drawerOpen) 12.dp else 0.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = 18.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(66.dp))

            Text(
                now.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                ),
                fontSize = 76.sp,
                lineHeight = 82.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                now.format(
                    DateTimeFormatter.ofPattern(
                        "EEEE, d MMMM",
                        Locale.getDefault()
                    )
                ).replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale.getDefault())
                    } else {
                        it.toString()
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Долгое нажатие на пустое место
            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            settingsOpen = true
                        }
                    )
            )

            // Сетка приложений
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.25f),

                contentPadding = PaddingValues(vertical = 8.dp),

                horizontalArrangement =
                    Arrangement.spacedBy(4.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp),

                userScrollEnabled = homeApps.size > 8
            ) {
                items(
                    homeApps,
                    key = { it.packageName }
                ) { app ->

                    AppIcon(
                        app = app,
                        showLabel = true,

                        onClick = {
                            launchApp(context, app.packageName)
                        },

                        onLongClick = {
                            saveHome(
                                homePackages.filterNot {
                                    it == app.packageName
                                }
                            )

                            Toast.makeText(
                                context,
                                "Убрано с главного экрана",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // Кнопка меню приложений
            Surface(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(44.dp)
                    .combinedClickable(
                        onClick = { drawerOpen = true },
                        onLongClick = { drawerOpen = true }
                    ),

                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Все приложения"
                    )
                }
            }
        }

        // АНИМИРОВАННОЕ МЕНЮ ПРИЛОЖЕНИЙ
        AnimatedVisibility(
            visible = drawerOpen,
            modifier = Modifier.fillMaxSize(),

            enter =
                fadeIn(tween(260)) +
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = tween(360)
                ) +
                slideInVertically(
                    initialOffsetY = { it / 5 },
                    animationSpec = tween(360)
                ),

            exit =
                fadeOut(tween(180)) +
                scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(220)
                ) +
                slideOutVertically(
                    targetOffsetY = { it / 8 },
                    animationSpec = tween(220)
                )
        ) {

            Column(
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.985f
                        )
                    )
                    .padding(18.dp)
            ) {

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Все приложения",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = {
                            drawerOpen = false
                        }
                    ) {
                        Text("Готово")
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),

                    placeholder = {
                        Text("Поиск приложений")
                    },

                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                )

                Text(
                    "Зажми приложение, чтобы добавить его на главный экран",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 4.dp
                    )
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(
                        top = 10.dp,
                        bottom = 24.dp
                    ),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    items(
                        filteredApps,
                        key = { it.packageName }
                    ) { app ->

                        AppIcon(
                            app = app,
                            showLabel = true,

                            onClick = {
                                launchApp(context, app.packageName)
                            },

                            onLongClick = {
                                if (app.packageName in homePackages) {

                                    Toast.makeText(
                                        context,
                                        "Уже на главном экране",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {

                                    saveHome(
                                        homePackages + app.packageName
                                    )

                                    Toast.makeText(
                                        context,
                                        "Добавлено на главный экран",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                drawerOpen = false
                            }
                        )
                    }
                }
            }
        }
    }

    // НАСТРОЙКИ ЛАУНЧЕРА
    if (settingsOpen) {

        AlertDialog(
            onDismissRequest = {
                settingsOpen = false
            },

            title = {
                Text("Настройки Prosto Launcher")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        "Тема",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {

                        Column(
                            Modifier.padding(8.dp)
                        ) {

                            ThemeRow(
                                "Системная",
                                themeChoice == LauncherTheme.SYSTEM
                            ) {
                                onThemeChange(LauncherTheme.SYSTEM)
                            }

                            ThemeRow(
                                "Светлая",
                                themeChoice == LauncherTheme.LIGHT
                            ) {
                                onThemeChange(LauncherTheme.LIGHT)
                            }

                            ThemeRow(
                                "Тёмная",
                                themeChoice == LauncherTheme.DARK
                            ) {
                                onThemeChange(LauncherTheme.DARK)
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        settingsOpen = false
                    }
                ) {
                    Text("Готово")
                }
            }
        )
    }
}

// ВЫБОР ТЕМЫ
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onClick
            )
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Spacer(Modifier.width(8.dp))

        Text(label)
    }
}

// ЗАПУСК ПРИЛОЖЕНИЯ
private fun launchApp(
    context: android.content.Context,
    packageName: String
) {
    runCatching {
        context.packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let(context::startActivity)
    }
}

// ИКОНКА ПРИЛОЖЕНИЯ
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(
    app: LauncherApp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick?.invoke()
                }
            )
            .padding(
                vertical = 5.dp,
                horizontal = 2.dp
            ),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    setImageDrawable(app.icon)
                    scaleType =
                        android.widget.ImageView.ScaleType.FIT_CENTER
                }
            },

            update = {
                it.setImageDrawable(app.icon)
            },

            modifier = Modifier
                .size(52.dp)
                .padding(3.dp)
        )

        if (showLabel) {

            Spacer(Modifier.height(4.dp))

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
